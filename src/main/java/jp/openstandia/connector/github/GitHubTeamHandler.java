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
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;

import java.util.HashSet;
import java.util.Set;

import static jp.openstandia.connector.github.GitHubUtils.*;

/**
 * Handle GitHub Team object.
 *
 * @author Hiroyuki Wada
 */
public class GitHubTeamHandler extends AbstractGitHubHandler<GitHubConfiguration, GitHubSchema> {

    public static final ObjectClass TEAM_OBJECT_CLASS = new ObjectClass("team");

    private static final Log LOGGER = Log.getLog(GitHubTeamHandler.class);

    // Unique and unchangeable.
    // Don't use "id" here because it conflicts midpoint side.
    // The format is <databaseId>:<nodeId>.
    private static final String ATTR_TEAM_DATABASE_ID_WITH_NODE_ID = "teamId";

    // Unique and changeable.
    private static final String ATTR_TEAM_NAME = "name";

    // Attributes
    public static final String ATTR_DESCRIPTION = "description";
    public static final String ATTR_PRIVACY = "privacy"; // secret, visible(closed in REST API)

    // Readonly
    // Unique and unchangeable (generated).
    public static final String ATTR_TEAM_DATABASE_ID = "databaseId";
    // Unique and changeable (generated from name).
    public static final String ATTR_SLUG = "slug";
    // Unique and unchangeable (generated).
    public static final String ATTR_TEAM_NODE_ID = "nodeId";

    // Association
    public static final String ATTR_PARENT_TEAM_ID = "parentTeamId";

    public GitHubTeamHandler(GitHubConfiguration configuration, GitHubClient<GitHubSchema> client,
                             GitHubSchema schema) {
        super(configuration, client, schema);
    }

    public static ObjectClassInfo getRoleSchema() {
        ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();
        builder.setType(TEAM_OBJECT_CLASS.getObjectClassValue());

        // id (__UID__)
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(Uid.NAME)
                        .setRequired(false) // Must be optional. It is not present for create operations
                        .setCreateable(false)
                        .setUpdateable(false)
                        .setNativeName(ATTR_TEAM_DATABASE_ID_WITH_NODE_ID)
                        .build());

        // name (__NAME__)
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(Name.NAME)
                        .setRequired(true)
                        .setNativeName(ATTR_TEAM_NAME)
                        .setSubtype(AttributeInfo.Subtypes.STRING_CASE_IGNORE)
                        .build());

        // Attributes
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_DESCRIPTION)
                        .setRequired(false)
                        .build());
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_PRIVACY)
                        .setRequired(false)
                        .build());

        // Readonly
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_TEAM_DATABASE_ID)
                        .setRequired(false)
                        .setCreateable(false)
                        .setUpdateable(false)
                        .setType(Long.class)
                        .build());
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_SLUG)
                        .setRequired(false)
                        .setCreateable(false)
                        .setUpdateable(false)
                        .build());
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_TEAM_NODE_ID)
                        .setRequired(false)
                        .setCreateable(false)
                        .setUpdateable(false)
                        .build());

        // Association
        builder.addAttributeInfo(
                AttributeInfoBuilder.define(ATTR_PARENT_TEAM_ID)
                        .setRequired(false)
                        // Association value is expected as string value in midPoint
                        // https://github.com/Evolveum/midpoint/blob/50f01966cfa6c2df458f218c255cc2e0d0631b39/provisioning/provisioning-impl/src/main/java/com/evolveum/midpoint/provisioning/impl/shadowmanager/ShadowManager.java#L554
                        //.setType(Long.class)
                        .build());

        ObjectClassInfo schemaInfo = builder.build();

        LOGGER.ok("The constructed GitHub Team schema: {0}", schemaInfo);

        return schemaInfo;
    }

    @Override
    public Uid create(Set<Attribute> attributes) {
        String name = null;
        String description = null;
        String privacy = null;
        Long parentTeamDatabaseId = null;

        for (Attribute attr : attributes) {
            if (attr.is(Name.NAME)) {
                name = AttributeUtil.getStringValue(attr);

            } else if (attr.is(ATTR_DESCRIPTION)) {
                description = AttributeUtil.getStringValue(attr);

            } else if (attr.is(ATTR_PRIVACY)) {
                privacy = AttributeUtil.getStringValue(attr);

            } else if (attr.is(ATTR_PARENT_TEAM_ID)) {
                String s = AttributeUtil.getStringValue(attr);
                parentTeamDatabaseId = getTeamDatabaseId(s);
            }
        }

        if (name == null) {
            throw new InvalidAttributeValueException("GitHub Team name is required");
        }

        return client.createTeam(schema, name, description, privacy, parentTeamDatabaseId);
    }

    @Override
    public Set<AttributeDelta> updateDelta(Uid uid, Set<AttributeDelta> modifications, OperationOptions options) {
        String name = null;
        String description = null;
        String privacy = null;
        Long parentTeamId = null;
        boolean clearParent = false;

        for (AttributeDelta attr : modifications) {
            if (attr.is(Name.NAME)) {
                name = AttributeDeltaUtil.getStringValue(attr);

            } else if (attr.is(ATTR_DESCRIPTION)) {
                description = toResourceAttributeValue(AttributeDeltaUtil.getStringValue(attr));

            } else if (attr.is(ATTR_PRIVACY)) {
                privacy = AttributeDeltaUtil.getStringValue(attr);

            } else if (attr.is(ATTR_PARENT_TEAM_ID)) {
                String s = AttributeDeltaUtil.getStringValue(attr);
                if (s != null) {
                    parentTeamId = getTeamDatabaseId(s);
                } else {
                    clearParent = true;
                }
            }
        }

        Uid updated = client.updateTeam(schema, uid, name, description, privacy, parentTeamId, clearParent, options);

        // Detected changed NAME(slug)
        if (!uid.getNameHintValue().equals(updated.getNameHintValue())) {
            AttributeDelta newName = AttributeDeltaBuilder.build(Name.NAME, updated.getNameHintValue());
            Set<AttributeDelta> sideEffects = new HashSet<>();
            sideEffects.add(newName);

            return sideEffects;
        }

        return null;
    }

    public void delete(Uid uid, OperationOptions options) {
        client.deleteTeam(schema, uid, options);
    }

    @Override
    public void query(GitHubFilter filter, ResultsHandler resultsHandler, OperationOptions options) {
        // Create full attributesToGet by RETURN_DEFAULT_ATTRIBUTES + ATTRIBUTES_TO_GET
        Set<String> attributesToGet = createFullAttributesToGet(schema.roleSchema, options);
        boolean allowPartialAttributeValues = shouldAllowPartialAttributeValues(options);

        if (filter == null) {
            client.getTeams(schema,
                    resultsHandler, options, attributesToGet, allowPartialAttributeValues, configuration.getQueryPageSize());
        } else {
            if (filter.isByUid()) {
                client.getTeam(schema, filter.uid,
                        resultsHandler, options, attributesToGet, allowPartialAttributeValues, configuration.getQueryPageSize());
            } else {
                client.getTeam(schema, filter.name,
                        resultsHandler, options, attributesToGet, allowPartialAttributeValues, configuration.getQueryPageSize());
            }
        }
    }
}
