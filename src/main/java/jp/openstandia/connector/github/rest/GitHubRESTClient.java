/*
 *  Copyright Nomura Research Institute, Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package jp.openstandia.connector.github.rest;

import com.spotify.github.v3.clients.PKCS1PEMKey;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jp.openstandia.connector.github.GitHubClient;
import jp.openstandia.connector.github.GitHubConfiguration;
import jp.openstandia.connector.github.GitHubSchema;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.Route;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.kohsuke.github.*;
import org.kohsuke.github.extras.okhttp3.OkHttpConnector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jp.openstandia.connector.github.GitHubTeamHandler.*;
import static jp.openstandia.connector.github.GitHubUserHandler.*;
import static jp.openstandia.connector.github.GitHubUtils.getTeamId;
import static jp.openstandia.connector.github.GitHubUtils.shouldReturn;

/**
 * GitHub client implementation which uses Java API for GitHub.
 *
 * @author Hiroyuki Wada
 */
public class GitHubRESTClient implements GitHubClient {

    private static final Log LOGGER = Log.getLog(GitHubRESTClient.class);

    private final String instanceName;
    private final GitHubConfiguration configuration;
    private GitHubExt apiClient;
    private long lastAuthenticated;
    private GHOrganizationExt orgApiClient;

    public GitHubRESTClient(String instanceName, GitHubConfiguration configuration) {
        this.instanceName = instanceName;
        this.configuration = configuration;

        auth();
    }

    public GitHubExt getApiClient() {
        return apiClient;
    }

    @Override
    public void test() {
        try {
            withAuth(() -> {
                apiClient.checkApiUrlValidity();
                return null;
            });
        } catch (RuntimeException e) {
            throw new ConnectorException("This GitHub connector isn't active.", e);
        }
    }

    public OkHttpClient createClient() {
        OkHttpClient.Builder okHttpBuilder = new OkHttpClient.Builder();
        okHttpBuilder.connectTimeout(configuration.getConnectionTimeoutInMilliseconds(), TimeUnit.MILLISECONDS);
        okHttpBuilder.readTimeout(configuration.getReadTimeoutInMilliseconds(), TimeUnit.MILLISECONDS);
        okHttpBuilder.writeTimeout(configuration.getWriteTimeoutInMilliseconds(), TimeUnit.MILLISECONDS);

        // Setup http proxy aware httpClient
        if (StringUtil.isNotEmpty(configuration.getHttpProxyHost())) {
            okHttpBuilder.proxy(new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(configuration.getHttpProxyHost(), configuration.getHttpProxyPort())));

            if (StringUtil.isNotEmpty(configuration.getHttpProxyUser()) && configuration.getHttpProxyPassword() != null) {
                configuration.getHttpProxyPassword().access(c -> {
                    okHttpBuilder.proxyAuthenticator((Route route, Response response) -> {
                        String credential = Credentials.basic(configuration.getHttpProxyUser(), String.valueOf(c));
                        return response.request().newBuilder()
                                .header("Proxy-Authorization", credential)
                                .build();
                    });
                });
            }
        }

        OkHttpClient httpClient = okHttpBuilder.build();

        return httpClient;
    }

    private class UnauthorizedException extends ConnectionFailedException {
        public UnauthorizedException(Exception e) {
            super(e);
        }
    }

    @Override
    public void auth() {
        AtomicReference<String> privateKey = new AtomicReference<>();
        configuration.getPrivateKey().access((val) -> {
            privateKey.set(String.valueOf(val));
        });

        try {
            // First, get app installation token
            GitHub api = new GitHubBuilder()
                    .withJwtToken(createJWT(configuration.getAppId(), 60000, privateKey.get()))
                    .withConnector(new OkHttpConnector(createClient()))
                    .build();
            GHAppInstallation appInstallation = api.getApp().getInstallationById(configuration.getInstallationId()); // Installation Id

            GHAppInstallationToken appInstallationToken = appInstallation.createToken().create();

            // Then, get scoped access token by app installation token

            GitHubBuilder builder = new GitHubBuilder()
                    .withConnector(new OkHttpConnector(createClient()))
                    .withAppInstallationToken(appInstallationToken.getToken());

            apiClient = GitHubExt.build(builder);
            lastAuthenticated = System.currentTimeMillis();

            orgApiClient = apiClient.getOrganization(configuration.getOrganizationName());

        } catch (IOException e) {
            throw new ConnectionFailedException("Failed to authenticate GitHub API", e);
        }
    }

    protected ConnectorException handleApiException(Exception e) {
        LOGGER.error(e, "Exception when calling github api");

        if (e instanceof GHFileNotFoundException) {
            GHFileNotFoundException gfe = (GHFileNotFoundException) e;
            List<String> status = gfe.getResponseHeaderFields().get(null);

            if (!status.isEmpty() && status.get(0).contains("400")) {
                return new InvalidAttributeValueException(e);
            }

            if (!status.isEmpty() && status.get(0).contains("401")) {
                return new UnauthorizedException(e);
            }

            if (!status.isEmpty() && status.get(0).contains("403")) {
                return new ConnectionFailedException(e);
            }

            if (!status.isEmpty() && status.get(0).contains("404")) {
                return new UnknownUidException(e);
            }

            if (!status.isEmpty() && status.get(0).contains("409")) {
                return new AlreadyExistsException(e);
            }
        }

        return new ConnectorIOException("Failed to call GitHub api", e);
    }

    protected <T> T withAuth(Callable<T> callable) {
        // Check the access token expiration
        long now = System.currentTimeMillis();
        if (now > lastAuthenticated + TimeUnit.MINUTES.toMillis(55)) {
            // Refresh the access token
            auth();
        }

        try {
            return callable.call();

        } catch (Exception e) {
            ConnectorException ce = handleApiException(e);

            if (ce instanceof UnauthorizedException) {
                // do re-Auth
                auth();

                try {
                    // retry
                    return callable.call();

                } catch (Exception e2) {
                    throw handleApiException(e2);
                }
            }

            throw ce;
        }
    }

    @Override
    public Uid createUser(GitHubSchema schema, SCIMUser newUser) throws AlreadyExistsException {
        return withAuth(() -> {
            SCIMUser created = orgApiClient.createSCIMUser(newUser);

            return new Uid(created.id, new Name(created.userName));
        });
    }

    @Override
    public Uid updateUser(GitHubSchema schema, Uid uid, String scimUserName, String scimEmail, String scimGivenName, String scimFamilyName, OperationOptions options) throws UnknownUidException {
        return withAuth(() -> {
            SCIMUser updated = orgApiClient.updateSCIMUser(uid.getUidValue(), scimUserName, scimEmail, scimGivenName, scimFamilyName);
            if (updated == null) {
                return null;
            }

            return null;
        });
    }

    protected boolean isPendingUser(String userId) {
        return false;
    }


    protected void assignRole(String userId, List<String> roleIds) throws IOException {
    }

    protected void unassignRole(String userId, List<String> roleIds) throws IOException {
    }

    @Override
    public void deleteUser(GitHubSchema schema, Uid uid, OperationOptions options) throws UnknownUidException {
        deleteUser(schema, uid.getUidValue(), options);
    }

    private void deleteUser(GitHubSchema schema, String scimUserId, OperationOptions options) throws UnknownUidException {
        withAuth(() -> {
            orgApiClient.deleteSCIMUser(scimUserId);

            return null;
        });
    }

    @Override
    public void getUsers(GitHubSchema schema, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet,
                         boolean allowPartialAttributeValues, int queryPageSize) {
        withAuth(() -> {
            orgApiClient.listExternalIdentities(queryPageSize)
                    .forEach(u -> {
                        // When we detect a dropped account, we need to delete it then return
                        // not found from the organization to re-invite the account.
                        if (u.node.isDropped()) {
                            try {
                                deleteUser(schema, u.node.guid, options);
                            } catch (UnknownUidException ignore) {
                                LOGGER.warn("Detected unknown Uid when deleting a dropped account");
                            }

                            return;
                        }
                        handler.handle(toConnectorObject(schema, null, u, attributesToGet, allowPartialAttributeValues, queryPageSize));
                    });
            return null;
        });
    }

    @Override
    public void getUser(GitHubSchema schema, Uid uid, ResultsHandler handler, OperationOptions options,
                        Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {
        withAuth(() -> {
            SCIMUser user = orgApiClient.getSCIMUser(uid.getUidValue());

            // SCIM User doesn't contain database ID
            // We need to use current database ID for the connectorObject as Name value to maintain the linking
            String queryLogin = null;
            Name name = uid.getNameHint();
            if (name != null) {
                queryLogin = name.getNameValue();
            }

            handler.handle(toConnectorObject(schema, queryLogin, user, attributesToGet, allowPartialAttributeValues, queryPageSize));

            return null;
        });
    }

    @Override
    public void getUser(GitHubSchema schema, Name name, ResultsHandler handler, OperationOptions options,
                        Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {
        withAuth(() -> {
            // Can't fetch directly... need to fetch all orgs members
            LOGGER.warn("Fetching all external identities for query by Name(SCIM userName), Name: {0}", name.getNameValue());

            String queryLogin = name.getNameValue();

            PagedIterator<GraphQLExternalIdentityEdge> iter = orgApiClient.listExternalIdentities(queryPageSize).iterator();
            while (iter.hasNext()) {
                GraphQLExternalIdentityEdge next = iter.next();

                if (next.node.user != null) {
                    // Already linked
                    if (next.node.user.login.equalsIgnoreCase(queryLogin)) {
                        // Found
                        handler.handle(toConnectorObject(schema, queryLogin, next, attributesToGet, allowPartialAttributeValues, queryPageSize));
                        break;
                    }

                } else {
                    // Not linked yet
                    if (next.node.scimIdentity.username.equalsIgnoreCase(queryLogin)) {
                        // Found
                        handler.handle(toConnectorObject(schema, null, next, attributesToGet, allowPartialAttributeValues, queryPageSize));
                        break;
                    }
                }
            }

            return null;
        });
    }

    private ConnectorObject toConnectorObject(GitHubSchema schema, String queryLogin, SCIMUser user,
                                              Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {

        final String scimEmail = (user.emails != null && user.emails.length > 0) ? user.emails[0].value : null;

        String scimGivenName = user.name != null ? user.name.givenName : null;
        String scimFamilyName = user.name != null ? user.name.familyName : null;

        return toConnectorObject(schema, queryLogin, user.id, user.userName, scimEmail,
                scimGivenName, scimFamilyName,
                null, // Can't fetch it from SCIMUser endpoint
                attributesToGet, allowPartialAttributeValues, queryPageSize);
    }

    private ConnectorObject toConnectorObject(GitHubSchema schema, String queryLogin, GraphQLExternalIdentityEdge user,
                                              Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {
        GraphQLExternalIdentityScimAttributes scimAttrs = user.node.scimIdentity;

        final String scimEmail = (scimAttrs.emails != null && scimAttrs.emails.length > 0) ? scimAttrs.emails[0].value : null;
        final String login = user.node.user != null ? user.node.user.login : null;

        return toConnectorObject(schema, queryLogin, user.node.guid, scimAttrs.username, scimEmail,
                scimAttrs.givenName, scimAttrs.familyName,
                login,
                attributesToGet, allowPartialAttributeValues, queryPageSize);
    }

    private ConnectorObject toConnectorObject(GitHubSchema schema, String queryLogin, String scimUserId, String scimUserName, String scimEmail,
                                              String scimGivenName, String scimFamilyName,
                                              String login,
                                              Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {
        final ConnectorObjectBuilder builder = new ConnectorObjectBuilder()
                .setObjectClass(USER_OBJECT_CLASS)
                // Always returns "scimUserId"
                .setUid(scimUserId);

        // Always returns "scimUserName" or "login" if resolved as Name value
        String userLogin = resolveUserLogin(queryLogin, login, scimUserName);
        builder.setName(userLogin);

        // Attributes
        if (shouldReturn(attributesToGet, ATTR_SCIM_USER_NAME) &&
                scimUserName != null) {
            builder.addAttribute(ATTR_SCIM_USER_NAME, scimUserName);
        }
        if (shouldReturn(attributesToGet, ATTR_SCIM_EMAIL) &&
                scimEmail != null) {
            builder.addAttribute(ATTR_SCIM_EMAIL, scimEmail);
        }
        if (shouldReturn(attributesToGet, ATTR_SCIM_GIVEN_NAME) &&
                scimGivenName != null) {
            builder.addAttribute(ATTR_SCIM_GIVEN_NAME, scimGivenName);
        }
        if (shouldReturn(attributesToGet, ATTR_SCIM_FAMILY_NAME) &&
                scimFamilyName != null) {
            builder.addAttribute(ATTR_SCIM_FAMILY_NAME, scimFamilyName);
        }

        if (allowPartialAttributeValues) {
            // Suppress fetching roleNames
            LOGGER.ok("[{0}] Suppress fetching associations because return partial attribute values is requested", instanceName);

            Stream.of(ATTR_TEAMS, ATTR_ORGANIZATION_ROLE).forEach(attrName -> {
                AttributeBuilder ab = new AttributeBuilder();
                ab.setName(attrName).setAttributeValueCompleteness(AttributeValueCompleteness.INCOMPLETE);
                ab.addValue(Collections.EMPTY_LIST);
                builder.addAttribute(ab.build());
            });

        } else {
            if (attributesToGet == null) {
                // Suppress fetching associations default
                LOGGER.ok("[{0}] Suppress fetching associations because returned by default is true", instanceName);

            } else {
                if (shouldReturn(attributesToGet, ATTR_TEAMS)) {
                    if (orgApiClient.isMember(userLogin)) {
                        // Fetch teams
                        LOGGER.ok("[{0}] Fetching roles because attributes to get is requested", instanceName);

                        try {
                            // Fetch teams by user's login name
                            // It's supported by GraphQL API only...
                            List<String> teams = orgApiClient.listTeams(userLogin, queryPageSize)
                                    .toList()
                                    .stream()
                                    .map(t -> t.node.databaseId + ":" + t.node.id)
                                    .collect(Collectors.toList());
                            builder.addAttribute(ATTR_TEAMS, teams);

                        } catch (IOException e) {
                            throw new ConnectorIOException("Failed to fetch GitHub teams by user's login naem");
                        }
                    }
                }
                if (shouldReturn(attributesToGet, ATTR_ORGANIZATION_ROLE)) {
                    try {
                        GHMembership membership = orgApiClient.getOrganizationMembership(userLogin);
                        builder.addAttribute(ATTR_ORGANIZATION_ROLE, membership.getRole().name().toLowerCase());

                    } catch (IOException ignore) {
                        LOGGER.info("Failed to fetch GitHub organization membership for user: {0}, error: {1}", userLogin, ignore.getMessage());
                    }
                }
            }
        }

        return builder.build();
    }

    private String resolveUserLogin(String queryLogin, String login, String scimUserName) {
        if (queryLogin == null) {
            if (login != null) {
                return login;
            } else {
                return scimUserName;
            }
        } else {
            if (login != null) {
                return login;
            } else if (queryLogin != null) {
                return queryLogin;
            } else {
                return scimUserName;
            }
        }
    }

    @Override
    public boolean isOrganizationMember(String userLogin) {
        return withAuth(() -> {
            return orgApiClient.isMember(userLogin);
        });
    }

    @Override
    public void assignOrganizationRole(String userLogin, String organizationRole) {
        withAuth(() -> {
            try {
                GHOrganization.Role role = GHOrganization.Role.valueOf(organizationRole.toUpperCase());

                orgApiClient.setOrganizationMembership(userLogin, role);

            } catch (IllegalArgumentException e) {
                throw new InvalidAttributeValueException("Invalid organizationRole: " + organizationRole);
            }

            return null;
        });
    }

    @Override
    public void assignTeams(String login, String teamRole, Collection<String> teams) {
        withAuth(() -> {
            for (String team : teams) {
                try {
                    GHTeam.Role role = GHTeam.Role.valueOf(teamRole.toUpperCase());

                    orgApiClient.addTeamMembership(getTeamId(team), login, role);

                } catch (IllegalArgumentException e) {
                    throw new InvalidAttributeValueException("Invalid teamRole: " + teamRole);
                }
            }

            return null;
        });
    }

    @Override
    public void unassignTeams(String login, Collection<String> teams) {
        withAuth(() -> {
            for (String team : teams) {
                orgApiClient.removeTeamMembership(getTeamId(team), login);
            }

            return null;
        });
    }

    @Override
    public Uid createTeam(GitHubSchema schema, String teamName, String description, String privacy, Long parentTeamId) throws AlreadyExistsException {
        return withAuth(() -> {
            GHTeamBuilder builder = orgApiClient.createTeam(teamName);

            if (description != null) {
                builder.description(description);
            }
            if (privacy != null) {
                try {
                    GHTeam.Privacy p = GHTeam.Privacy.valueOf(privacy.toUpperCase());
                    builder.privacy(p);

                } catch (IllegalArgumentException e) {
                    throw new InvalidAttributeValueException("Invalid privacy: " + privacy);
                }
            }
            if (parentTeamId != null) {
                builder.parentTeamId(parentTeamId);
            }

            GHTeam created = builder.create();

            // To use for REST API and GraphQL API, we combine databaseId and nodeId
            return new Uid(created.getId() + ":" + created.getNodeId(), new Name(created.getSlug()));
        });
    }


    @Override
    public Uid updateTeam(GitHubSchema schema, Uid uid, String teamName, String description, String privacy, Long parentTeamId, OperationOptions options) throws UnknownUidException {
        return withAuth(() -> {
            GHTeam.Privacy p = null;
            if (privacy != null) {
                try {
                    p = GHTeam.Privacy.valueOf(privacy.toUpperCase());

                } catch (IllegalArgumentException e) {
                    throw new InvalidAttributeValueException("Invalid privacy: " + privacy);
                }
            }

            GHTeam updated = orgApiClient.updateTeam(getTeamId(uid), teamName, description, p, parentTeamId);

            return new Uid(updated.getId() + ":" + updated.getNodeId(), new Name(updated.getSlug()));
        });
    }

    @Override
    public void deleteTeam(GitHubSchema schema, Uid uid, OperationOptions options) throws UnknownUidException {
        withAuth(() -> {
            orgApiClient.deleteTeam(getTeamId(uid));

            return null;
        });
    }

    @Override
    public void getTeams(GitHubSchema schema, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {
        withAuth(() -> {
            orgApiClient.listTeamsExt().withPageSize(queryPageSize)
                    .forEach(t -> {
                        handler.handle(toTeamConnectorObject(schema, t, attributesToGet, allowPartialAttributeValues, queryPageSize));
                    });

            return null;
        });
    }

    @Override
    public void getTeam(GitHubSchema schema, Uid uid, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {
        withAuth(() -> {
            GHTeamExt team = orgApiClient.getTeam(getTeamId(uid));

            handler.handle(toTeamConnectorObject(schema, team, attributesToGet, allowPartialAttributeValues, queryPageSize));

            return null;
        });
    }

    @Override
    public void getTeam(GitHubSchema schema, Name name, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {
        withAuth(() -> {
            GHTeamExt team = orgApiClient.getTeam(name.getNameValue());

            handler.handle(toTeamConnectorObject(schema, team, attributesToGet, allowPartialAttributeValues, queryPageSize));

            return null;
        });
    }

    private ConnectorObject toTeamConnectorObject(GitHubSchema schema, GHTeamExt team, Set<String> attributesToGet, boolean allowPartialAttributeValues, long queryPageSize) {
        final ConnectorObjectBuilder builder = new ConnectorObjectBuilder()
                .setObjectClass(TEAM_OBJECT_CLASS)
                // Always returns "teamId"
                .setUid(team.getId() + ":" + team.getNodeId())
                // Always returns "slug"
                .setName(team.getSlug());

        // Attributes
        if (shouldReturn(attributesToGet, ATTR_NAME)) {
            builder.addAttribute(ATTR_NAME, team.getName());
        }
        if (shouldReturn(attributesToGet, ATTR_DESCRIPTION) &&
                !StringUtil.isEmpty(team.getDescription())) {
            builder.addAttribute(ATTR_DESCRIPTION, team.getDescription());
        }
        if (shouldReturn(attributesToGet, ATTR_PRIVACY)) {
            builder.addAttribute(ATTR_PRIVACY, team.getPrivacy().name().toLowerCase());
        }
        if (shouldReturn(attributesToGet, ATTR_PARENT_TEAM_ID) &&
                team.getParent() != null) {
            builder.addAttribute(ATTR_PARENT_TEAM_ID, team.getParent().getId() + ":" + team.getParent().getNodeId());
        }

        // Readonly
        if (shouldReturn(attributesToGet, ATTR_TEAM_ID)) {
            builder.addAttribute(ATTR_TEAM_ID, team.getId());
        }
        if (shouldReturn(attributesToGet, ATTR_NODE_ID)) {
            builder.addAttribute(ATTR_NODE_ID, team.getNodeId());
        }

        return builder.build();
    }

    @Override
    public void close() {
    }

    private static PrivateKey get(String privateKeyPEM) {
        Optional<PKCS8EncodedKeySpec> keySpec = PKCS1PEMKey.loadKeySpec(privateKeyPEM.getBytes());

        if (!keySpec.isPresent()) {
            throw new ConnectionFailedException("Failed to load private key PEM");
        }

        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(keySpec.get());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ConnectionFailedException("Failed to load the privateKey from the configuration", e);
        }
    }

    public static String createJWT(String githubAppId, long ttlMillis, String privateKeyPEM) {
        //The JWT signature algorithm we will be using to sign the token
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.RS256;

        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);

        //We will sign our JWT with our private key
        Key signingKey = get(privateKeyPEM);

        //Let's set the JWT Claims
        JwtBuilder builder = Jwts.builder()
                .setIssuedAt(now)
                .setIssuer(githubAppId)
                .signWith(signingKey, signatureAlgorithm);

        //if it has been specified, let's add the expiration
        if (ttlMillis > 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp);
        }

        //Builds the JWT and serializes it to a compact, URL-safe string
        return builder.compact();
    }
}
