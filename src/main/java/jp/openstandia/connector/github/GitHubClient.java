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

import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.kohsuke.github.SCIMUser;

import java.util.Collection;
import java.util.Set;

/**
 * GitHubClient interface.
 *
 * @author Hiroyuki Wada
 */
public interface GitHubClient {
    void test();

    void auth();

    // User

    Uid createUser(GitHubSchema schema, SCIMUser scimUser) throws AlreadyExistsException;

    Uid updateUser(GitHubSchema schema, Uid uid, String scimUserName, String scimEmail, String scimGivenName, String scimFamilyName, OperationOptions options) throws UnknownUidException;

    void deleteUser(GitHubSchema schema, Uid uid, OperationOptions options) throws UnknownUidException;

    void getUsers(GitHubSchema schema, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize);

    void getUser(GitHubSchema schema, Uid uid, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize);

    void getUser(GitHubSchema schema, Name name, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize);

    boolean isOrganizationMember(String userLogin);

    void assignOrganizationRole(String userLogin, String organizationRole);

    void assignTeams(String login, String role, Collection<String> teams);

    void unassignTeams(String login, Collection<String> teams);

    // Team

    Uid createTeam(GitHubSchema schema, String teamName, String description, String privacy, Long parentTeamId) throws AlreadyExistsException;

    Uid updateTeam(GitHubSchema schema, Uid uid, String teamName, String description, String privacy, Long parentTeamId, OperationOptions options) throws UnknownUidException;

    void deleteTeam(GitHubSchema schema, Uid uid, OperationOptions options) throws UnknownUidException;

    void getTeams(GitHubSchema schema, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize);

    void getTeam(GitHubSchema schema, Uid uid, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize);

    void getTeam(GitHubSchema schema, Name name, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize);

    void close();

}

