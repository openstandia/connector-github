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

import jp.openstandia.connector.github.GitHubClient;
import jp.openstandia.connector.github.GitHubEMUConfiguration;
import jp.openstandia.connector.github.GitHubEMUSchema;
import jp.openstandia.connector.util.QueryHandler;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.kohsuke.github.*;
import org.kohsuke.github.extras.okhttp3.OkHttpConnector;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * GitHub EMU client implementation which uses Java API for GitHub.
 * <a href="https://docs.github.com/en/enterprise-cloud@latest/rest/enterprise-admin/scim?apiVersion=2022-11-28#list-scim-provisioned-identities-for-an-enterprise">See the API spec.</a>
 *
 * @author Hiroyuki Wada
 */
public class GitHubEMURESTClient implements GitHubClient<GitHubEMUSchema> {

    private static final Log LOGGER = Log.getLog(GitHubEMURESTClient.class);

    private final GitHubEMUConfiguration configuration;
    private String instanceName;
    private GitHubExt apiClient;
    private long lastAuthenticated;
    private GHEnterpriseExt enterpriseApiClient;

    public GitHubEMURESTClient(GitHubEMUConfiguration configuration) {
        this.configuration = configuration;

        auth();
    }

    public GitHubExt getApiClient() {
        return apiClient;
    }

    @Override
    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @Override
    public void test() {
        try {
            withAuth(() -> {
                // Checking using https://api.github.com/rate_limit with accessToken
                // If the access token is invalid, it returns 401 Bad credentials error
                apiClient.checkApiUrlValidity();
                return null;
            });
        } catch (RuntimeException e) {
            throw new ConnectorException("This GitHub EMU connector isn't active.", e);
        }
    }

    private static class UnauthorizedException extends ConnectionFailedException {
        public UnauthorizedException(Exception e) {
            super(e);
        }
    }

    @Override
    public void auth() {
        AtomicReference<String> accessToken = new AtomicReference<>();
        configuration.getAccessToken().access((val) -> {
            accessToken.set(String.valueOf(val));
        });

        try {
            GitHubBuilder builder = new GitHubBuilder()
                    .withConnector(new OkHttpConnector(createClient(configuration)))
                    .withOAuthToken(accessToken.get());

            apiClient = GitHubExt.build(builder);
            lastAuthenticated = System.currentTimeMillis();

            enterpriseApiClient = apiClient.getEnterprise(configuration.getEnterpriseSlug());

        } catch (IOException e) {
            throw new ConnectionFailedException("Failed to authenticate GitHub EMU API", e);
        }
    }

    protected ConnectorException handleApiException(Exception e) {
        String statusCode = "";

        if (e instanceof GHFileNotFoundException) {
            GHFileNotFoundException gfe = (GHFileNotFoundException) e;
            List<String> status = gfe.getResponseHeaderFields().get(null);

            if (!status.isEmpty()) {
                statusCode = status.get(0);
            }

            if (statusCode.contains("400")) {
                return new InvalidAttributeValueException(e);
            }

            if (statusCode.contains("401")) {
                return new UnauthorizedException(e);
            }

            if (statusCode.contains("403")) {
                // Including Rate limit error
                return new PermissionDeniedException(e);
            }

            if (statusCode.contains("404")) {
                return new UnknownUidException(e);
            }

            if (statusCode.contains("409")) {
                return new AlreadyExistsException(e);
            }

            if (statusCode.contains("429")) {
                return RetryableException.wrap("Too many requests", e);
            }
        }

        if (!statusCode.isEmpty()) {
            LOGGER.error(e, "[{0}] Unexpected exception when calling GitHub EMU API, statusCode: {1}", instanceName, statusCode);
        } else {
            LOGGER.error(e, "[{0}] Unexpected exception when calling GitHub EMU API", instanceName);
        }

        return new ConnectorIOException("Failed to call GitHub EMU API", e);
    }

    protected <T> T withAuth(Callable<T> callable) {
        // Currently, the access token for EMU must have no expiration
        // https://docs.github.com/en/enterprise-cloud@latest/admin/managing-iam/understanding-iam-for-enterprises/getting-started-with-enterprise-managed-users#create-a-personal-access-token
        if (lastAuthenticated != 0) {
            auth();
        }

        try {
            return callable.call();

        } catch (Exception e) {
            throw handleApiException(e);
        }
    }

    @Override
    public Uid createEMUUser(SCIMEMUUser newUser) throws AlreadyExistsException {
        return withAuth(() -> {
            SCIMEMUUser created = enterpriseApiClient.createSCIMEMUUser(newUser);

            return new Uid(created.id, new Name(created.userName));
        });
    }

    @Override
    public void patchEMUUser(Uid uid, SCIMPatchOperations operations) throws UnknownUidException {
        withAuth(() -> {
            SCIMEMUUser updated = enterpriseApiClient.updateSCIMEMUUser(uid.getUidValue(), operations);
            return updated;
        });
    }

    @Override
    public void deleteEMUUser(Uid uid, OperationOptions options) throws UnknownUidException {
        withAuth(() -> {
            enterpriseApiClient.deleteSCIMUser(uid.getUidValue());
            return null;
        });
    }

    @Override
    public SCIMEMUUser getEMUUser(Uid uid, OperationOptions options, Set<String> attributesToGet) {
        return withAuth(() -> {
            SCIMEMUUser scimEMUUser = enterpriseApiClient.getSCIMEMUUser(uid.getUidValue());
            return scimEMUUser;
        });
    }

    @Override
    public SCIMEMUUser getEMUUser(Name name, OperationOptions options, Set<String> attributesToGet) {
        return withAuth(() -> {
            SCIMEMUUser scimEMUUser = enterpriseApiClient.getSCIMEMUUserByUserName(name.getNameValue());
            return scimEMUUser;
        });
    }

    @Override
    public int getEMUUsers(QueryHandler<SCIMEMUUser> handler, OperationOptions options, Set<String> fetchFieldsSet, int pageSize, int pageOffset) {
        return withAuth(() -> {
            SCIMPagedSearchIterable<SCIMEMUUser> iterable = enterpriseApiClient.listSCIMUsers(pageSize, pageOffset);

            // 0 means no offset (requested all data)
            if (pageOffset < 1) {
                for (SCIMEMUUser next : iterable) {
                    if (!handler.handle(next)) {
                        break;
                    }
                }
                return iterable.getTotalCount();
            }

            // Pagination
            // SCIM starts from 1
            int count = 0;
            for (SCIMEMUUser next : iterable) {
                count++;
                if (!handler.handle(next)) {
                    break;
                }
                if (count >= pageSize) {
                    break;
                }
            }

            return iterable.getTotalCount();
        });
    }

    @Override
    public Uid createEMUGroup(GitHubEMUSchema schema, SCIMEMUGroup group) throws AlreadyExistsException {
        return withAuth(() -> {
            SCIMEMUGroup created = enterpriseApiClient.createSCIMEMUGroup(group);

            return new Uid(created.id, new Name(created.displayName));
        });
    }

    @Override
    public void patchEMUGroup(Uid uid, SCIMPatchOperations operations) throws UnknownUidException {
        withAuth(() -> {
            SCIMEMUGroup updated = enterpriseApiClient.updateSCIMEMUGroup(uid.getUidValue(), operations);
            return updated;
        });
    }

    @Override
    public void deleteEMUGroup(Uid uid, OperationOptions options) throws UnknownUidException {
        withAuth(() -> {
            enterpriseApiClient.deleteSCIMGroup(uid.getUidValue());
            return null;
        });
    }

    @Override
    public SCIMEMUGroup getEMUGroup(Uid uid, OperationOptions options, Set<String> attributesToGet) {
        return withAuth(() -> {
            SCIMEMUGroup scimEMUGroup = enterpriseApiClient.getSCIMEMUGroup(uid.getUidValue());
            return scimEMUGroup;
        });
    }

    @Override
    public SCIMEMUGroup getEMUGroup(Name name, OperationOptions options, Set<String> attributesToGet) {
        return withAuth(() -> {
            SCIMEMUGroup scimEMUGroup = enterpriseApiClient.getSCIMEMUGroupByDisplayName(name.getNameValue());
            return scimEMUGroup;
        });
    }

    @Override
    public int getEMUGroups(QueryHandler<SCIMEMUGroup> handler, OperationOptions options, Set<String> fetchFieldsSet, int pageSize, int pageOffset) {
        return withAuth(() -> {
            SCIMPagedSearchIterable<SCIMEMUGroup> iterable = enterpriseApiClient.listSCIMGroups(pageSize, pageOffset);

            // 0 means no offset (requested all data)
            if (pageOffset < 1) {
                for (SCIMEMUGroup next : iterable) {
                    if (!handler.handle(next)) {
                        break;
                    }
                }
                return iterable.getTotalCount();
            }

            // Pagination
            // SCIM starts from 1
            int count = 0;
            for (SCIMEMUGroup next : iterable) {
                count++;
                if (!handler.handle(next)) {
                    break;
                }
                if (count >= pageSize) {
                    break;
                }
            }

            return iterable.getTotalCount();
        });
    }

    @Override
    public void close() {
    }
}
