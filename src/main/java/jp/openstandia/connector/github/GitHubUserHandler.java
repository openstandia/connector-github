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

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.*;
import org.kohsuke.github.SCIMEmail;
import org.kohsuke.github.SCIMName;
import org.kohsuke.github.SCIMUser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static jp.openstandia.connector.github.GitHubUtils.*;

/**
 * Handle GitHub user object.
 *
 * @author Hiroyuki Wada
 */
public class GitHubUserHandler extends AbstractGitHubHandler<GitHubConfiguration, GitHubSchema> {

    public static final ObjectClass USER_OBJECT_CLASS = new ObjectClass("user");

    private static final Log LOGGER = Log.getLog(GitHubUserHandler.class);

    // Unique and unchangeable. This is SCIM user id.
    // Don't use "id" here because it conflicts midpoint side.
    private static final String ATTR_USER_ID = "scimUserId";

    // Unique and changeable. This is GitHub login(username) and scimUserName(login:scimUserName).
    public static final String ATTR_USER_NAME = "userName";

    // Attributes
    public static final String ATTR_SCIM_USER_NAME = "scimUserName";
    public static final String ATTR_SCIM_EMAIL = "scimEmail";
    public static final String ATTR_SCIM_GIVEN_NAME = "scimGivenName";
    public static final String ATTR_SCIM_FAMILY_NAME = "scimFamilyName";
    public static final String ATTR_SCIM_EXTERNAL_ID = "scimExternalId";
    public static final String ATTR_ORGANIZATION_ROLE = "organizationRole";

    // Readonly
    // Only fetched by GraphQL ExternalIdentity through all users query due to GitHub API limitation.
    public static final String ATTR_USER_LOGIN = "login";

    // Association
    public static final String ATTR_TEAMS = "teams"; // List of teamId(databaseId:nodeId)
    public static final String ATTR_MAINTAINER_TEAMS = "maintainerTeams"; // List of teamId(databaseId:nodeId)

    public GitHubUserHandler(GitHubConfiguration configuration, GitHubClient<GitHubSchema> client,
                             GitHubSchema schema) {
        super(configuration, client, schema);
    }

    public static ObjectClassInfo getUserSchema() {
        ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();
        builder.setType(USER_OBJECT_CLASS.getObjectClassValue());

        // scimUserId (__UID__)
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(Uid.NAME)
                        .setRequired(false) // Must be optional. It is not present for create operations
                        .setCreateable(false)
                        .setUpdateable(false)
                        .setSubtype(AttributeInfo.Subtypes.STRING_CASE_IGNORE)
                        .setNativeName(ATTR_USER_ID)
                        .build());

        // userName (__NAME__)
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(Name.NAME)
                        .setRequired(true)
                        .setSubtype(AttributeInfo.Subtypes.STRING_CASE_IGNORE)
                        .setNativeName(ATTR_USER_NAME)
                        .build());

        // attributes
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_SCIM_EMAIL)
                        .setRequired(true)
                        .setSubtype(AttributeInfo.Subtypes.STRING_CASE_IGNORE)
                        .build());
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_SCIM_GIVEN_NAME)
                        .setRequired(true)
                        .build());
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_SCIM_FAMILY_NAME)
                        .setRequired(true)
                        .build());
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_SCIM_EXTERNAL_ID)
                        .setRequired(false)
                        .build());

        // Readonly
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_USER_LOGIN)
                        .setRequired(false)
                        .setCreateable(false)
                        .setUpdateable(false)
                        .setSubtype(AttributeInfo.Subtypes.STRING_CASE_IGNORE)
                        .build());
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_SCIM_USER_NAME)
                        .setRequired(false)
                        .setCreateable(false)
                        .setUpdateable(false)
                        .setSubtype(AttributeInfo.Subtypes.STRING_CASE_IGNORE)
                        .build());

        // Association
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_TEAMS)
                        .setRequired(false)
                        .setMultiValued(true)
                        // We define the team's UID as string with <databaseId>:<nodeId> format
                        // .setType(Integer.class)
                        .setReturnedByDefault(false)
                        .build());
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_MAINTAINER_TEAMS)
                        .setRequired(false)
                        .setMultiValued(true)
                        // We define the team's UID as string with <databaseId>:<nodeId> format
                        // .setType(Integer.class)
                        .setReturnedByDefault(false)
                        .build());
        // TODO: Implement Organization Role schema?
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_ORGANIZATION_ROLE)
                        .setRequired(false)
                        .setReturnedByDefault(false)
                        .build());

        ObjectClassInfo userSchemaInfo = builder.build();

        LOGGER.ok("The constructed GitHub user schema: {0}", userSchemaInfo);

        return userSchemaInfo;
    }

    @Override
    public Uid create(Set<Attribute> attributes) {
        SCIMUser newUser = new SCIMUser();
        newUser.name = new SCIMName();

        for (Attribute attr : attributes) {
            if (attr.is(Name.NAME)) {
                String loginWithScimUserName = AttributeUtil.getStringValue(attr);
                // Throw InvalidAttributeValueException if invalid format
                newUser.userName = getUserSCIMUserName(loginWithScimUserName);

            } else if (attr.is(ATTR_SCIM_EMAIL)) {
                SCIMEmail scimEmail = new SCIMEmail();
                scimEmail.value = AttributeUtil.getStringValue(attr);
                newUser.emails = new SCIMEmail[]{scimEmail};

            } else if (attr.is(ATTR_SCIM_GIVEN_NAME)) {
                newUser.name.givenName = AttributeUtil.getStringValue(attr);

            } else if (attr.is(ATTR_SCIM_FAMILY_NAME)) {
                newUser.name.familyName = AttributeUtil.getStringValue(attr);

            } else if (attr.is(ATTR_SCIM_EXTERNAL_ID)) {
                newUser.externalId = AttributeUtil.getStringValue(attr);
            }
        }

        Uid created = client.createUser(schema, newUser);

        // Association can't be constructed here because GitHub login is unknown yet.

        return created;
    }

    @Override
    public Set<AttributeDelta> updateDelta(Uid uid, Set<AttributeDelta> modifications, OperationOptions options) {
        String login = null;
        String scimUserName = null;
        String scimEmail = null;
        String scimGivenName = null;
        String scimFamilyName = null;
        String organizationRole = null;
        Set<String> addTeams = new HashSet<>();
        Set<String> removeTeams = new HashSet<>();
        Set<String> addMaintainerTeams = new HashSet<>();
        Set<String> removeMaintainerTeams = new HashSet<>();

        for (AttributeDelta attr : modifications) {
            if (attr.is(Name.NAME)) {
                // Detected modifying userName (e.g. completed the invitation by full reconciliation, update scimUserName)
                String newLoginWithScimUserName = AttributeDeltaUtil.getStringValue(attr);

                // Detect scimUserName change
                String newScimUserName = getUserSCIMUserName(newLoginWithScimUserName);
                String oldScimUserName = getUserSCIMUserName(uid);
                if (!newScimUserName.equals(oldScimUserName)) {
                    scimUserName = newScimUserName;
                }

                // Detect user login change
                String newLogin = getUserLogin(newLoginWithScimUserName);
                String oldLogin = getUserLogin(uid);
                if (!newLogin.equals(oldLogin)) {
                    login = newLogin;
                }

            } else if (attr.is(ATTR_SCIM_EMAIL)) {
                scimEmail = AttributeDeltaUtil.getStringValue(attr);

            } else if (attr.is(ATTR_SCIM_GIVEN_NAME)) {
                scimGivenName = AttributeDeltaUtil.getStringValue(attr);

            } else if (attr.is(ATTR_SCIM_FAMILY_NAME)) {
                scimFamilyName = AttributeDeltaUtil.getStringValue(attr);

            } else if (attr.is(ATTR_ORGANIZATION_ROLE)) {
                organizationRole = toResourceAttributeValue(AttributeDeltaUtil.getStringValue(attr), "member");

            } else if (attr.is(ATTR_TEAMS)) {
                if (attr.getValuesToAdd() != null) {
                    addTeams = attr.getValuesToAdd().stream().map(v -> v.toString()).collect(Collectors.toSet());
                }
                if (attr.getValuesToRemove() != null) {
                    removeTeams = attr.getValuesToRemove().stream().map(v -> v.toString()).collect(Collectors.toSet());
                }

            } else if (attr.is(ATTR_MAINTAINER_TEAMS)) {
                if (attr.getValuesToAdd() != null) {
                    addMaintainerTeams = attr.getValuesToAdd().stream().map(v -> v.toString()).collect(Collectors.toSet());
                }
                if (attr.getValuesToRemove() != null) {
                    removeMaintainerTeams = attr.getValuesToRemove().stream().map(v -> v.toString()).collect(Collectors.toSet());
                }
            }
        }

        String newNameValue = client.updateUser(schema, uid, scimUserName, scimEmail, scimGivenName, scimFamilyName, login, options);

        String userLogin = resolveUserLogin(uid, newNameValue);

        // Organization role and Association
        if (userLogin != null &&
                (organizationRole != null ||
                        !addTeams.isEmpty() || !removeTeams.isEmpty() ||
                        !addMaintainerTeams.isEmpty() || !removeMaintainerTeams.isEmpty()
                )) {

            // do update organization role
            if (organizationRole != null) {
                // If the user login is stale, it throws UnknownUidException.
                // IDM handle the exception then do discovery process if needed.
                client.assignOrganizationRole(userLogin, organizationRole);
            }

            // assign/unassign the teams
            TeamAssignmentResolver resolver = new TeamAssignmentResolver(addTeams, removeTeams, addMaintainerTeams, removeMaintainerTeams);

            // If the user login is stale, it throws UnknownUidException.
            // IDM handle the exception then do discovery process if needed.
            client.unassignTeams(userLogin, resolver.resolvedRemoveTeams);
            client.assignTeams(userLogin, "member", resolver.resolvedAddTeams);
            client.assignTeams(userLogin, "maintainer", resolver.resolvedAddMaitainerTeams);
        }

        // Detect NAME changing
        if (newNameValue != null) {
            Set<AttributeDelta> sideEffects = new HashSet<>();
            AttributeDelta newName = AttributeDeltaBuilder.build(Name.NAME, newNameValue);
            sideEffects.add(newName);

            return sideEffects;
        }

        return null;
    }

    private String resolveUserLogin(Uid oldUid, String newNameValue) {
        if (newNameValue != null) {
            return getUserLogin(newNameValue);
        }

        String userLogin = getUserLogin(oldUid);
        if (!userLogin.equals(UNKNOWN_USER_NAME)) {
            return userLogin;
        }
        // Can't resolve yet due to not completed invitation
        return null;
    }

    @Override
    public void delete(Uid uid, OperationOptions options) {
        String userLogin = getUserLogin(uid);
        if (!userLogin.equals(UNKNOWN_USER_NAME)) {
            // Fix https://github.com/openstandia/connector-github/issues/6
            // GitHub maintains the user's team association after deletion
            // So, we need to remove the association first
            List<String> teamIds = client.getTeamIdsByUsername(userLogin, configuration.getQueryPageSize());
            client.unassignTeams(userLogin, teamIds);
        }

        // Finally, do delete the user
        client.deleteUser(schema, uid, options);
    }

    @Override
    public void query(GitHubFilter filter, ResultsHandler resultsHandler, OperationOptions options) {
        // Create full attributesToGet by RETURN_DEFAULT_ATTRIBUTES + ATTRIBUTES_TO_GET
        Set<String> attributesToGet = createFullAttributesToGet(schema.userSchema, options);
        boolean allowPartialAttributeValues = shouldAllowPartialAttributeValues(options);

        if (filter == null) {
            client.getUsers(schema,
                    resultsHandler, options, attributesToGet, allowPartialAttributeValues, configuration.getQueryPageSize());
        } else {
            if (filter.isByUid()) {
                client.getUser(schema, filter.uid,
                        resultsHandler, options, attributesToGet, allowPartialAttributeValues, configuration.getQueryPageSize());
            } else {
                client.getUser(schema, filter.name,
                        resultsHandler, options, attributesToGet, allowPartialAttributeValues, configuration.getQueryPageSize());
            }
        }
    }
}
