package org.kohsuke.github;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * GHEnterprise class.
 *
 * @author Hiroyuki Wada
 */
public class GHEnterpriseExt extends GHOrganization {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    GHEnterpriseExt wrapUp(GitHub root) {
        return (GHEnterpriseExt) super.wrapUp(root);
    }

    public SCIMEMUUser createSCIMEMUUser(SCIMEMUUser newUser) throws IOException {
        String json = mapper.writeValueAsString(newUser);
        byte[] jsonBytes = json.getBytes();

        try (InputStream inputStream = new ByteArrayInputStream(jsonBytes)) {
            SCIMEMUUser u = root.createRequest()
                    .method("POST")
                    .with(inputStream)
                    .withUrlPath(String.format("/scim/v2/enterprises/%s/Users", login))
                    .fetch(SCIMEMUUser.class);
            return u;
        }
    }

    public SCIMEMUUser updateSCIMEMUUser(String scimUserId, SCIMPatchOperations operations) throws IOException {
        String json = mapper.writeValueAsString(operations);
        byte[] jsonBytes = json.getBytes();

        try (InputStream inputStream = new ByteArrayInputStream(jsonBytes)) {
            SCIMEMUUser u = root.createRequest()
                    .method("PATCH")
                    .with(inputStream)
                    .withUrlPath(String.format("/scim/v2/enterprises/%s/Users/%s", login, scimUserId))
                    .fetch(SCIMEMUUser.class);
            return u;
        }
    }

    public SCIMEMUUser getSCIMEMUUser(String scimUserId) throws IOException {
        SCIMEMUUser u = root.createRequest()
                .withUrlPath(String.format("/scim/v2/enterprises/%s/Users/%s", login, scimUserId))
                .fetch(SCIMEMUUser.class);
        return u;
    }

    public SCIMEMUUser getSCIMEMUUserByUserName(String scimUserName) throws IOException {
        SCIMEMUUser u = root.createRequest()
                .withUrlPath(String.format("/scim/v2/enterprises/%s/Users?filter=userName eq \"%s\"", login, scimUserName))
                .fetch(SCIMEMUUser.class);
        return u;
    }

    /**
     * Search users.
     *
     * @return the gh user search builder
     */
    public SCIMEMUUserSearchBuilder searchSCIMUsers() {
        return new SCIMEMUUserSearchBuilder(root, this);
    }

    public SCIMPagedSearchIterable<SCIMEMUUser> listSCIMUsers(int pageSize, int pageOffset)
            throws IOException {
        return searchSCIMUsers().list().withPageSize(pageSize).withPageOffset(pageOffset);
    }

    public void deleteSCIMUser(String scimUserId) throws IOException {
        root.createRequest()
                .method("DELETE")
                .withUrlPath(String.format("/scim/v2/enterprises/%s/Users/%s", login, scimUserId))
                .send();
    }

    public SCIMEMUGroup createSCIMEMUGroup(SCIMEMUGroup newGroup) throws IOException {
        String json = mapper.writeValueAsString(newGroup);
        byte[] jsonBytes = json.getBytes();

        try (InputStream inputStream = new ByteArrayInputStream(jsonBytes)) {
            SCIMEMUGroup g = root.createRequest()
                    .method("POST")
                    .with(inputStream)
                    .withUrlPath(String.format("/scim/v2/enterprises/%s/Groups", login))
                    .fetch(SCIMEMUGroup.class);
            return g;
        }
    }

    public SCIMEMUGroup updateSCIMEMUGroup(String scimGroupId, SCIMPatchOperations operations) throws IOException {
        String json = mapper.writeValueAsString(operations);
        byte[] jsonBytes = json.getBytes();

        try (InputStream inputStream = new ByteArrayInputStream(jsonBytes)) {
            SCIMEMUGroup g = root.createRequest()
                    .method("PATCH")
                    .with(inputStream)
                    .withUrlPath(String.format("/scim/v2/enterprises/%s/Groups/%s", login, scimGroupId))
                    .fetch(SCIMEMUGroup.class);
            return g;
        }
    }

    public SCIMEMUGroup getSCIMEMUGroup(String scimGroupId) throws IOException {
        SCIMEMUGroup g = root.createRequest()
                .withUrlPath(String.format("/scim/v2/enterprises/%s/Groups/%s", login, scimGroupId))
                .fetch(SCIMEMUGroup.class);
        return g;
    }

    public SCIMEMUGroup getSCIMEMUGroupByDisplayName(String scimGroupDisplayName) throws IOException {
        SCIMEMUGroup g = root.createRequest()
                .withUrlPath(String.format("/scim/v2/enterprises/%s/Groups?filter=displayName eq \"%s\"", login, scimGroupDisplayName))
                .fetch(SCIMEMUGroup.class);
        return g;
    }

    /**
     * Search groups.
     *
     * @return the gh group search builder
     */
    public SCIMEMUGroupSearchBuilder searchSCIMGroups() {
        return new SCIMEMUGroupSearchBuilder(root, this);
    }

    public SCIMPagedSearchIterable<SCIMEMUGroup> listSCIMGroups(int pageSize, int pageOffset)
            throws IOException {
        return searchSCIMGroups().list().withPageSize(pageSize).withPageOffset(pageOffset);
    }

    public void deleteSCIMGroup(String scimGroupId) throws IOException {
        root.createRequest()
                .method("DELETE")
                .withUrlPath(String.format("/scim/v2/enterprises/%s/Groups/%s", login, scimGroupId))
                .send();
    }
}