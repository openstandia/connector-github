package jp.openstandia.connector.github.testutil;

import jp.openstandia.connector.github.GitHubClient;
import jp.openstandia.connector.github.GitHubSchema;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Uid;
import org.kohsuke.github.SCIMUser;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MockClient implements GitHubClient {

    private static final MockClient INSTANCE = new MockClient();

    public static MockClient instance() {
        return INSTANCE;
    }

    private MockClient() {
    }

    public void init() {
    }

    @Override
    public void test() {

    }

    @Override
    public void auth() {

    }

    @Override
    public Uid createUser(GitHubSchema schema, SCIMUser scimUser) throws AlreadyExistsException {
        return null;
    }

    @Override
    public String updateUser(GitHubSchema schema, Uid uid, String scimUserName, String scimEmail, String scimGivenName, String scimFamilyName, String login, OperationOptions options) throws UnknownUidException {
        return null;
    }

    @Override
    public void deleteUser(GitHubSchema schema, Uid uid, OperationOptions options) throws UnknownUidException {

    }

    @Override
    public void getUsers(GitHubSchema schema, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {

    }

    @Override
    public void getUser(GitHubSchema schema, Uid uid, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {

    }

    @Override
    public void getUser(GitHubSchema schema, Name name, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {

    }

    @Override
    public List<String> getTeamIdsByUsername(String userLogin, int pageSize) {
        return null;
    }

    @Override
    public boolean isOrganizationMember(String userLogin) {
        return false;
    }

    @Override
    public void assignOrganizationRole(String userLogin, String organizationRole) {

    }

    @Override
    public void assignTeams(String login, String role, Collection<String> teams) {

    }

    @Override
    public void unassignTeams(String login, Collection<String> teams) {

    }

    @Override
    public Uid createTeam(GitHubSchema schema, String teamName, String description, String privacy, Long parentTeamDatabaseId) throws AlreadyExistsException {
        return null;
    }

    @Override
    public Uid updateTeam(GitHubSchema schema, Uid uid, String teamName, String description, String privacy, Long parentTeamId, boolean clearParent, OperationOptions options) throws UnknownUidException {
        return null;
    }

    @Override
    public void deleteTeam(GitHubSchema schema, Uid uid, OperationOptions options) throws UnknownUidException {

    }

    @Override
    public void getTeams(GitHubSchema schema, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {

    }

    @Override
    public void getTeam(GitHubSchema schema, Uid uid, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {

    }

    @Override
    public void getTeam(GitHubSchema schema, Name name, ResultsHandler handler, OperationOptions options, Set<String> attributesToGet, boolean allowPartialAttributeValues, int queryPageSize) {

    }

    @Override
    public void close() {

    }
}
