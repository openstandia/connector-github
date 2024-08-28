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
package jp.openstandia.connector.github;

import jp.openstandia.connector.util.QueryHandler;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.Route;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.kohsuke.github.SCIMEMUGroup;
import org.kohsuke.github.SCIMEMUUser;
import org.kohsuke.github.SCIMPatchOperations;
import org.kohsuke.github.SCIMUser;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * GitHubClient interface.
 *
 * @author Hiroyuki Wada
 */
public interface GitHubClient<T extends AbstractGitHubSchema<? extends AbstractGitHubConfiguration>> {

    default OkHttpClient createClient(AbstractGitHubConfiguration configuration) {
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

    void setInstanceName(String instanceName);

    void test();

    void auth();

    void close();

    // User

    default Uid createUser(T schema, SCIMUser scimUser) throws AlreadyExistsException {
        throw new UnsupportedOperationException();
    }

    default String updateUser(T schema, Uid uid, String scimUserName, String scimEmail, String scimGivenName, String scimFamilyName, String login, OperationOptions options) throws UnknownUidException {
        throw new UnsupportedOperationException();
    }

    default void deleteUser(T schema, Uid uid, OperationOptions options) throws UnknownUidException {
        throw new UnsupportedOperationException();
    }

    default void getUsers(T schema, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {
        throw new UnsupportedOperationException();
    }

    default void getUser(T schema, Uid uid, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {
        throw new UnsupportedOperationException();
    }

    default void getUser(T schema, Name name, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {
        throw new UnsupportedOperationException();
    }

    // Team

    default List<String> getTeamIdsByUsername(String userLogin, int pageSize) {
        throw new UnsupportedOperationException();
    }

    default boolean isOrganizationMember(String userLogin) {
        throw new UnsupportedOperationException();
    }

    default void assignOrganizationRole(String userLogin, String organizationRole) {
        throw new UnsupportedOperationException();
    }

    default void assignTeams(String login, String role, Collection<String> teams) {
        throw new UnsupportedOperationException();
    }

    default void unassignTeams(String login, Collection<String> teams) {
        throw new UnsupportedOperationException();
    }

    default Uid createTeam(T schema, String teamName, String description, String privacy, Long parentTeamDatabaseId) throws AlreadyExistsException {
        throw new UnsupportedOperationException();
    }

    default Uid updateTeam(T schema, Uid uid, String teamName, String description, String privacy, Long parentTeamId, boolean clearParent, OperationOptions options) throws UnknownUidException {
        throw new UnsupportedOperationException();
    }

    default void deleteTeam(T schema, Uid uid, OperationOptions options) throws UnknownUidException {
        throw new UnsupportedOperationException();
    }

    default void getTeams(T schema, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {
        throw new UnsupportedOperationException();
    }

    default void getTeam(T schema, Uid uid, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {
        throw new UnsupportedOperationException();
    }

    default void getTeam(T schema, Name name, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {
        throw new UnsupportedOperationException();
    }

    // EMU User

    default Uid createEMUUser(SCIMEMUUser user) throws AlreadyExistsException {
        throw new UnsupportedOperationException();
    }

    default void patchEMUUser(Uid uid, SCIMPatchOperations operations) throws UnknownUidException {
        throw new UnsupportedOperationException();
    }

    default void deleteEMUUser(Uid uid, OperationOptions options) throws UnknownUidException {
        throw new UnsupportedOperationException();
    }

    default int getEMUUsers(QueryHandler<SCIMEMUUser> handler, OperationOptions options, Set<String> fetchFieldsSet, int pageSize, int pageOffset) {
        throw new UnsupportedOperationException();
    }

    default SCIMEMUUser getEMUUser(Uid uid, OperationOptions options, Set<String> attributesToGet) {
        throw new UnsupportedOperationException();
    }

    default SCIMEMUUser getEMUUser(Name name, OperationOptions options, Set<String> attributesToGet) {
        throw new UnsupportedOperationException();
    }

    // EMU Group

    default Uid createEMUGroup(T schema, SCIMEMUGroup group) throws AlreadyExistsException {
        throw new UnsupportedOperationException();
    }

    default void patchEMUGroup(Uid uid, SCIMPatchOperations operations) throws UnknownUidException {
        throw new UnsupportedOperationException();
    }

    default void deleteEMUGroup(Uid uid, OperationOptions options) throws UnknownUidException {
        throw new UnsupportedOperationException();
    }

    default int getEMUGroups(QueryHandler<SCIMEMUGroup> handler, OperationOptions options, Set<String> fetchFieldsSet, int pageSize, int pageOffset) {
        throw new UnsupportedOperationException();
    }

    default SCIMEMUGroup getEMUGroup(Uid uid, OperationOptions options, Set<String> attributesToGet) {
        throw new UnsupportedOperationException();
    }

    default SCIMEMUGroup getEMUGroup(Name name, OperationOptions options, Set<String> attributesToGet) {
        throw new UnsupportedOperationException();
    }
}

