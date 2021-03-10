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
import jp.openstandia.connector.github.GitHubUtils;
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
import static jp.openstandia.connector.github.GitHubUtils.*;

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
                // Including Rate limit error
                return new PermissionDeniedException(e);
            }

            if (!status.isEmpty() && status.get(0).contains("404")) {
                return new UnknownUidException(e);
            }

            if (!status.isEmpty() && status.get(0).contains("409")) {
                return new AlreadyExistsException(e);
            }

            if (!status.isEmpty() && status.get(0).contains("422")) {
                // Create Team API return 422 error if exists
                return new AlreadyExistsException(e);
            }
        }

        LOGGER.error(e, "Unexpected exception when calling GitHub API");

        return new ConnectorIOException("Failed to call GitHub API", e);
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

            return toUserUid(created);
        });
    }

    @Override
    public String updateUser(GitHubSchema schema, Uid uid, String scimUserName, String scimEmail, String scimGivenName,
                             String scimFamilyName, String login, OperationOptions options) throws UnknownUidException {
        return withAuth(() -> {
            orgApiClient.updateSCIMUser(uid.getUidValue(), scimUserName, scimEmail, scimGivenName, scimFamilyName);

            // Detected NAME is changed
            String oldUserLogin = getUserLogin(uid);
            String oldScimUserName = getUserSCIMUserName(uid);

            if ((login != null && !oldUserLogin.equals(login))
                    || (scimUserName != null && !oldScimUserName.equals(scimUserName))) {
                String newLogin = login != null ? login : oldUserLogin;
                String newScimUserName = scimUserName != null ? scimUserName : oldScimUserName;

                // Return new NAME value
                return toUserName(newLogin, newScimUserName);
            }

            return null;
        });
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
            // We need to use NAME value in query Uid as user login.
            // It means IDM can't detect when the user login is changed in GitHub side.
            // To detect the situation, IDM need to do full reconciliation which calls getUsers method.
            String queryLogin = getUserLogin(uid);

            handler.handle(toConnectorObject(schema, queryLogin, user, attributesToGet, allowPartialAttributeValues, queryPageSize));

            return null;
        });
    }

    @Override
    public void getUser(GitHubSchema schema, Name name, ResultsHandler handler, OperationOptions options,
                        Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {
        withAuth(() -> {
            String scimUserName = getUserSCIMUserName(name);

            SCIMUser user = orgApiClient.getSCIMUserByUserName(scimUserName);

            // SCIM User doesn't contain database ID
            // We need to use NAME value in query Uid as user login.
            // It means IDM can't detect when the user login is changed in GitHub side.
            // To detect the situation, IDM need to do full reconciliation which calls getUsers method.
            String queryLogin = getUserLogin(name);

            handler.handle(toConnectorObject(schema, queryLogin, user, attributesToGet, allowPartialAttributeValues, queryPageSize));

            return null;
        });
    }

    @Override
    public List<String> getTeamIdsByUsername(String userLogin, int pageSize) {
        return withAuth(() -> {
            return orgApiClient.listTeams(userLogin, pageSize)
                    .toList().stream()
                    .filter(t -> t.node.members.totalCount == 1)
                    .map(GitHubUtils::toTeamUid)
                    .collect(Collectors.toList());
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

        // Always returns "_unknown_:<scimUserName>" or "<login>:<scimUserName>" as NAME
        String userNameValue = resolveUserLogin(queryLogin, login, scimUserName);
        builder.setName(userNameValue);

        // Attributes
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

        String userLogin = getUserLogin(userNameValue);

        // Readonly
        // We need to return user login always because it causes duplicate NAME if we don't return.
        // IDM detects no data, then try to update NAME.
        builder.addAttribute(ATTR_USER_LOGIN, userLogin);

        if (shouldReturn(attributesToGet, ATTR_SCIM_USER_NAME) &&
                scimUserName != null) {
            builder.addAttribute(ATTR_SCIM_USER_NAME, scimUserName);
        }

        if (allowPartialAttributeValues) {
            // Suppress fetching associations because they cost time and resource, also it consumes rate limit
            LOGGER.ok("[{0}] Suppress fetching associations because return partial attribute values is requested", instanceName);

            Stream.of(ATTR_TEAMS, ATTR_MAINTAINER_TEAMS, ATTR_ORGANIZATION_ROLE).forEach(attrName -> {
                AttributeBuilder ab = new AttributeBuilder();
                ab.setName(attrName).setAttributeValueCompleteness(AttributeValueCompleteness.INCOMPLETE);
                ab.addValue(Collections.EMPTY_LIST);
                builder.addAttribute(ab.build());
            });

            return builder.build();
        }

        if (attributesToGet == null) {
            // Suppress fetching associations default
            LOGGER.ok("[{0}] Suppress fetching associations because returned by default is true", instanceName);

            return builder.build();
        }

        if (userLogin.equals(UNKNOWN_USER_NAME)) {
            LOGGER.ok("[{0}] Suppress fetching associations because the user isn't complete the invitation", instanceName);

            return builder.build();
        }

        // Fetching associations if needed

        if (shouldReturn(attributesToGet, ATTR_TEAMS) || shouldReturn(attributesToGet, ATTR_MAINTAINER_TEAMS)) {
            // Fetch teams
            LOGGER.ok("[{0}] Fetching teams/maintainer teams because attributes to get is requested", instanceName);

            try {
                // Fetch teams by user's login name
                // It's supported by GraphQL API only...
                // If the user is not found in the organization (leave by self or change their login name), the GraphAPI returns all teams unfortunately.
                // That's why we do filtering by totalCount == 1 here.
                List<GraphQLTeamEdge> allTeams = orgApiClient.listTeams(userLogin, queryPageSize)
                        .toList().stream()
                        .filter(t -> t.node.members.totalCount == 1)
                        .collect(Collectors.toList());

                List<String> memberTeams = allTeams.stream()
                        .filter(t -> t.node.members.edges[0].role == GraphQLTeamMemberRole.MEMBER)
                        .map(GitHubUtils::toTeamUid)
                        .collect(Collectors.toList());

                List<String> maintainerTeams = allTeams.stream()
                        .filter(t -> t.node.members.edges[0].role == GraphQLTeamMemberRole.MAINTAINER)
                        .map(GitHubUtils::toTeamUid)
                        .collect(Collectors.toList());

                builder.addAttribute(ATTR_TEAMS, memberTeams);
                builder.addAttribute(ATTR_MAINTAINER_TEAMS, maintainerTeams);

            } catch (IOException ignore) {
                LOGGER.warn("Failed to fetch GitHub organization membership for user: {0}, error: {1}", userLogin, ignore.getMessage());
                // Ignore the error, IDM try to reconcile the memberships
            }
        }

        if (shouldReturn(attributesToGet, ATTR_ORGANIZATION_ROLE)) {
            try {
                GHMembership membership = orgApiClient.getOrganizationMembership(userLogin);
                builder.addAttribute(ATTR_ORGANIZATION_ROLE, membership.getRole().name().toLowerCase());

            } catch (IOException ignore) {
                // If the user is not found (leave by self or change their login name), IDM will do discovery process
                LOGGER.warn("Failed to fetch GitHub organization membership for user: {0}, error: {1}", userLogin, ignore.getMessage());
                // Ignore the error, IDM try to reconcile the memberships
            }
        }

        return builder.build();
    }

    private String resolveUserLogin(String queryLogin, String login, String scimUserName) {
        if (login != null) {
            return toUserName(login, scimUserName);
        }
        if (queryLogin != null) {
            return toUserName(queryLogin, scimUserName);
        }
        return toUserName(null, scimUserName);
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

                    orgApiClient.addTeamMembership(getTeamDatabaseId(team), login, role);

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
                orgApiClient.removeTeamMembership(getTeamDatabaseId(team), login);
            }

            return null;
        });
    }

    @Override
    public Uid createTeam(GitHubSchema schema, String teamName, String description, String privacy, Long parentTeamDatabaseId) throws AlreadyExistsException {
        return withAuth(() -> {
            GHTeamBuilder builder = orgApiClient.createTeam(teamName);

            if (description != null) {
                builder.description(description);
            }
            if (privacy != null) {
                GHTeam.Privacy ghPrivacy = toGHTeamPrivacy(privacy);
                builder.privacy(ghPrivacy);
            }
            if (parentTeamDatabaseId != null) {
                builder.parentTeamId(parentTeamDatabaseId);
            }

            GHTeam created = builder.create();

            // To use for REST API and GraphQL API, we combine databaseId and nodeId
            return new Uid(toTeamUid(created), new Name(created.getName()));
        });
    }

    @Override
    public Uid updateTeam(GitHubSchema schema, Uid uid, String teamName, String description, String privacy, Long parentTeamId,
                          boolean clearParent, OperationOptions options) throws UnknownUidException {
        return withAuth(() -> {
            GHTeam.Privacy ghPrivacy = null;
            if (privacy != null) {
                ghPrivacy = toGHTeamPrivacy(privacy);
            }

            GHTeam updated = orgApiClient.updateTeam(getTeamDatabaseId(uid), teamName, description, ghPrivacy, parentTeamId, clearParent);

            return new Uid(toTeamUid(updated), new Name(updated.getName()));
        });
    }

    @Override
    public void deleteTeam(GitHubSchema schema, Uid uid, OperationOptions options) throws UnknownUidException {
        withAuth(() -> {
            orgApiClient.deleteTeam(getTeamDatabaseId(uid));

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
            GHTeamExt team = orgApiClient.getTeam(getTeamDatabaseId(uid));

            handler.handle(toTeamConnectorObject(schema, team, attributesToGet, allowPartialAttributeValues, queryPageSize));

            return null;
        });
    }

    @Override
    public void getTeam(GitHubSchema schema, Name name, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {
        withAuth(() -> {
            PagedIterator<GraphQLTeamEdge> iter = orgApiClient.findTeam(name.getNameValue(), queryPageSize).iterator();
            while (iter.hasNext()) {
                GraphQLTeamEdge team = iter.next();
                if (team.node.name.equalsIgnoreCase(name.getNameValue())) {
                    // Found
                    handler.handle(toTeamConnectorObject(schema, team, attributesToGet, allowPartialAttributeValues, queryPageSize));

                    break;
                }
            }

            return null;
        });
    }

    private ConnectorObject toTeamConnectorObject(GitHubSchema schema, GHTeamExt team, Set<String> attributesToGet, boolean allowPartialAttributeValues, long queryPageSize) {
        String teamId = toTeamUid(team);

        String parentId = null;
        if (team.getParent() != null) {
            parentId = toTeamUid(team.getParent());
        }

        GraphQLTeamPrivacy privacy;
        if (team.getPrivacy() == GHTeam.Privacy.SECRET.SECRET) {
            privacy = GraphQLTeamPrivacy.SECRET;
        } else {
            privacy = GraphQLTeamPrivacy.VISIBLE;
        }

        return toTeamConnectorObject(schema, teamId, team.getId(), team.getNodeId(), team.getName(), team.getSlug(),
                team.getDescription(), privacy, parentId,
                attributesToGet, allowPartialAttributeValues, queryPageSize);
    }

    private ConnectorObject toTeamConnectorObject(GitHubSchema schema, GraphQLTeamEdge teamEdge, Set<String> attributesToGet, boolean allowPartialAttributeValues, long queryPageSize) {
        GraphQLTeam team = teamEdge.node;

        String teamId = toTeamUid(team);

        String parentId = null;
        if (team.parentTeam != null) {
            parentId = toTeamUid(team.parentTeam);
        }

        return toTeamConnectorObject(schema, teamId, team.databaseId, team.id, team.name, team.slug,
                team.description, team.privacy, parentId,
                attributesToGet, allowPartialAttributeValues, queryPageSize);
    }

    private ConnectorObject toTeamConnectorObject(GitHubSchema schema, String teamId, long databaseId, String nodeId, String teamName,
                                                  String slug, String description, GraphQLTeamPrivacy privacy, String parentId,
                                                  Set<String> attributesToGet, boolean allowPartialAttributeValues, long queryPageSize) {
        final ConnectorObjectBuilder builder = new ConnectorObjectBuilder()
                .setObjectClass(TEAM_OBJECT_CLASS)
                // Always returns "teamId"
                .setUid(teamId)
                // Always returns "slug"
                .setName(teamName);

        // Attributes
        if (shouldReturn(attributesToGet, ATTR_DESCRIPTION) &&
                !StringUtil.isEmpty(description)) {
            builder.addAttribute(ATTR_DESCRIPTION, description);
        }
        if (shouldReturn(attributesToGet, ATTR_PRIVACY)) {
            builder.addAttribute(ATTR_PRIVACY, privacy.name().toLowerCase());
        }
        if (shouldReturn(attributesToGet, ATTR_PARENT_TEAM_ID) &&
                parentId != null) {
            builder.addAttribute(ATTR_PARENT_TEAM_ID, parentId);
        }

        // Readonly
        if (shouldReturn(attributesToGet, ATTR_TEAM_DATABASE_ID)) {
            builder.addAttribute(ATTR_TEAM_DATABASE_ID, databaseId);
        }
        if (shouldReturn(attributesToGet, ATTR_SLUG)) {
            builder.addAttribute(ATTR_SLUG, slug);
        }
        if (shouldReturn(attributesToGet, ATTR_TEAM_NODE_ID)) {
            builder.addAttribute(ATTR_TEAM_NODE_ID, nodeId);
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
