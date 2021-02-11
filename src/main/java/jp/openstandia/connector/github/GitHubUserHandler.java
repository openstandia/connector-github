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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static jp.openstandia.connector.github.GitHubUtils.*;

/**
 * Handle GitHub user object.
 *
 * @author Hiroyuki Wada
 */
public class GitHubUserHandler extends AbstractGitHubHandler {

    public static final ObjectClass USER_OBJECT_CLASS = new ObjectClass("user");

    private static final Log LOGGER = Log.getLog(GitHubUserHandler.class);

    // Unique and unchangeable. This is SCIM user id.
    // Don't use "id" here because it conflicts midpoint side.
    private static final String ATTR_USER_ID = "scimUserId";

    // Unique and changeable. This is GitHub login(username)
    public static final String ATTR_USER_LOGIN = "login";

    // Attributes
    public static final String ATTR_SCIM_USER_NAME = "scimUserName";
    public static final String ATTR_SCIM_EMAIL = "scimEmail";
    public static final String ATTR_SCIM_GIVEN_NAME = "scimGivenName";
    public static final String ATTR_SCIM_FAMILY_NAME = "scimFamilyName";
    public static final String ATTR_SCIM_EXTERNAL_ID = "scimExternalId";
    public static final String ATTR_ORGANIZATION_ROLE = "organizationRole";

    // Association
    public static final String ATTR_TEAMS = "teams"; // team(databaseId)

    public GitHubUserHandler(String instanceName, GitHubConfiguration configuration, GitHubClient client,
                             GitHubSchema schema) {
        super(instanceName, configuration, client, schema);
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

        // login (__NAME__)
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(Name.NAME)
                        .setRequired(false)
                        .setNativeName(ATTR_USER_LOGIN)
                        .setSubtype(AttributeInfo.Subtypes.STRING_CASE_IGNORE)
                        .build());

        // attributes
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_SCIM_USER_NAME)
                        .setRequired(true)
                        .setSubtype(AttributeInfo.Subtypes.STRING_CASE_IGNORE)
                        .build());
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
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_ORGANIZATION_ROLE)
                        .setRequired(false)
                        .build());

        // Association
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_TEAMS)
                        .setRequired(false)
                        .setMultiValued(true)
                        // We define the team's UID as string with <teamId>:<nodeId> format
                        // .setType(Integer.class)
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

        String login = null;
        List<String> teams = null;

        for (Attribute attr : attributes) {
            if (attr.is(ATTR_SCIM_USER_NAME)) {
                newUser.userName = AttributeUtil.getStringValue(attr);

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

            } else if (attr.is(Name.NAME)) {
                login = AttributeUtil.getStringValue(attr);

            } else if (attr.is(ATTR_TEAMS)) {
                teams = attr.getValue().stream().map(v -> v.toString()).collect(Collectors.toList());
            }
        }

        Uid created = client.createUser(schema, newUser);

        // Association
        if (login != null && teams != null && !teams.isEmpty()) {
            // Check the login is valid first using check organization membership REST API
            if (client.isOrganizationMember(login)) {
                // If it's valid userLogin, do assign the teams
                client.assignTeams(login, teams);
            }
        }

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
        List<String> addTeams = new ArrayList<>();
        List<String> removeTeams = new ArrayList<>();

        for (AttributeDelta attr : modifications) {
            if (attr.is(Name.NAME)) {
                login = AttributeDeltaUtil.getStringValue(attr);

            } else if (attr.is(ATTR_SCIM_USER_NAME)) {
                scimUserName = AttributeDeltaUtil.getStringValue(attr);

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
                    addTeams = attr.getValuesToAdd().stream().map(v -> v.toString()).collect(Collectors.toList());
                }
                if (attr.getValuesToRemove() != null) {
                    removeTeams = attr.getValuesToRemove().stream().map(v -> v.toString()).collect(Collectors.toList());
                }
            }
        }

        client.updateUser(schema, uid, scimUserName, scimEmail, scimGivenName, scimFamilyName, options);

        String userLogin = resolveUserLogin(uid, login);

        // Organization role and Association
        if (userLogin != null && (!addTeams.isEmpty() || !removeTeams.isEmpty())) {
            // Check the userLogin is valid first using check organization membership REST API
            if (client.isOrganizationMember(userLogin)) {
                // If it's valid userLogin, do update organization role and assign/unassign the teams
                if (organizationRole != null) {
                    client.assignOrganizationRole(userLogin, organizationRole);
                }

                if (!addTeams.isEmpty()) {
                    client.assignTeams(userLogin, addTeams);
                }
                if (!removeTeams.isEmpty()) {
                    client.unassignTeams(userLogin, removeTeams);
                }
            }
        }

        return null;
    }

    private String resolveUserLogin(Uid uid, String login) {
        if (login != null) {
            return login;
        }

        return uid.getNameHintValue();
    }

    @Override
    public void delete(Uid uid, OperationOptions options) {
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
