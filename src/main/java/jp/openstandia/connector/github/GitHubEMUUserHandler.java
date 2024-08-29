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

import jp.openstandia.connector.util.SchemaDefinition;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.*;
import org.kohsuke.github.*;

import java.util.ArrayList;
import java.util.Set;

import static jp.openstandia.connector.util.Utils.toZoneDateTimeForISO8601OffsetDateTime;
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.*;

/**
 * Handle GitHub EMU User object.
 *
 * @author Hiroyuki Wada
 */
public class GitHubEMUUserHandler extends AbstractGitHubEMUHandler {

    public static final ObjectClass USER_OBJECT_CLASS = new ObjectClass("EMUUser");

    private static final Log LOGGER = Log.getLog(GitHubEMUUserHandler.class);

    public GitHubEMUUserHandler(GitHubEMUConfiguration configuration, GitHubClient<GitHubEMUSchema> client, GitHubEMUSchema schema,
                                SchemaDefinition schemaDefinition) {
        super(configuration, client, schema, schemaDefinition);
    }

    public static SchemaDefinition.Builder createSchema(AbstractGitHubConfiguration configuration, GitHubClient<GitHubEMUSchema> client) {
        SchemaDefinition.Builder<SCIMEMUUser, SCIMPatchOperations, SCIMEMUUser> sb
                = SchemaDefinition.newBuilder(USER_OBJECT_CLASS, SCIMEMUUser.class, SCIMPatchOperations.class, SCIMEMUUser.class);

        // GitHub EMU supports SCIM v2.0 partially.
        // Spec: https://docs.github.com/ja/enterprise-cloud@latest/rest/enterprise-admin/scim?apiVersion=2022-11-28#supported-scim-user-attributes

        // __UID__
        // The id for the user. Must be unique and unchangeable.
        sb.addUid("userId",
                SchemaDefinition.Types.UUID,
                null,
                (source) -> source.id,
                "id",
                NOT_CREATABLE, NOT_UPDATEABLE
        );

        // code (__NAME__)
        // The name for the user. Must be unique and changeable.
        sb.addName("userName",
                SchemaDefinition.Types.STRING_CASE_IGNORE,
                (source, dest) -> dest.userName = source,
                (source, dest) -> dest.replace("userName", source),
                (source) -> source.userName,
                null,
                REQUIRED
        );

        // __ENABLE__ attribute
        sb.add(OperationalAttributes.ENABLE_NAME,
                SchemaDefinition.Types.BOOLEAN,
                (source, dest) -> dest.active = source,
                (source, dest) -> dest.replace("active", source),
                (source) -> source.active,
                "active"
        );

        // Attributes
        sb.add("externalId",
                SchemaDefinition.Types.STRING,
                (source, dest) -> {
                    dest.externalId = source;
                },
                (source, dest) -> dest.replace("externalId", source),
                (source) -> source.externalId,
                null,
                REQUIRED
        );
        sb.add("displayName",
                SchemaDefinition.Types.STRING,
                (source, dest) -> {
                    dest.displayName = source;
                },
                (source, dest) -> dest.replace("displayName", source),
                (source) -> source.displayName,
                null
        );
        sb.add("name.formatted",
                SchemaDefinition.Types.STRING,
                (source, dest) -> {
                    if (dest.name == null) {
                        dest.name = new SCIMName();
                    }
                    dest.name.formatted = source;
                },
                (source, dest) -> dest.replace("name.formatted", source),
                (source) -> source.name != null ? source.name.formatted : null,
                null
        );
        sb.add("name.givenName",
                SchemaDefinition.Types.STRING,
                (source, dest) -> {
                    if (dest.name == null) {
                        dest.name = new SCIMName();
                    }
                    dest.name.givenName = source;
                },
                (source, dest) -> dest.replace("name.givenName", source),
                (source) -> source.name != null ? source.name.givenName : null,
                null
        );
        sb.add("name.familyName",
                SchemaDefinition.Types.STRING,
                (source, dest) -> {
                    if (dest.name == null) {
                        dest.name = new SCIMName();
                    }
                    dest.name.familyName = source;
                },
                (source, dest) -> dest.replace("name.familyName", source),
                (source) -> source.name != null ? source.name.familyName : null,
                null
        );
        // SCIM schema has "emails", but we define "primaryEmail" as single value here for easy mapping in IDM
        // Also, it seems that GitHub doesn't support remove operation for "email", so we define single value only
        sb.add("primaryEmail",
                SchemaDefinition.Types.STRING_CASE_IGNORE,
                (source, dest) -> {
                    if (source == null) {
                        return;
                    }
                    SCIMEmail scimEmail = new SCIMEmail();
                    scimEmail.value = source;
                    scimEmail.primary = true;

                    dest.emails = new ArrayList<>();
                    dest.emails.add(scimEmail);
                },
                (source, dest) -> {
                    if (source == null) {
                        dest.replace((SCIMEmail) null);
                        return;
                    }
                    SCIMEmail newEmail = new SCIMEmail();
                    newEmail.value = source;
                    newEmail.primary = true;
                    dest.replace(newEmail);
                },
                (source) -> source.emails != null && !source.emails.isEmpty() ? source.emails.get(0).value : null,
                null,
                REQUIRED
        );
        // SCIM schema has "roles", but we define "primaryRole" as single value here for easy mapping in IDM
        // Also, it seems that GitHub doesn't support add/remove operation for "roles", so we define single value only
        sb.add("primaryRole",
                SchemaDefinition.Types.STRING,
                (source, dest) -> {
                    if (source == null) {
                        return;
                    }
                    SCIMRole scimRole = new SCIMRole();
                    scimRole.value = source;
                    scimRole.primary = true;

                    dest.roles = new ArrayList<>();
                    dest.roles.add(scimRole);
                },
                (source, dest) -> {
                    if (source == null) {
                        dest.replace((SCIMRole) null);
                        return;
                    }
                    SCIMRole newRole = new SCIMRole();
                    newRole.value = source;
                    newRole.primary = true;
                    dest.replace(newRole);
                },
                (source) -> source.roles != null && !source.roles.isEmpty() ? source.roles.get(0).value : null,
                null
        );

        // Association
        // GitHub EMU SCIM supports "groups" attributes, although no document
        sb.addAsMultiple("groups",
                SchemaDefinition.Types.UUID,
                null,
                null,
                null,
                (source) -> source.groups.stream().filter(x -> x.ref.contains("/Groups/")).map(x -> x.value),
                null,
                NOT_CREATABLE, NOT_UPDATEABLE, NOT_RETURNED_BY_DEFAULT
        );

        // Metadata (readonly)
        sb.add("meta.created",
                SchemaDefinition.Types.DATETIME,
                null,
                (source) -> toZoneDateTimeForISO8601OffsetDateTime(source.meta.created),
                null,
                NOT_CREATABLE, NOT_UPDATEABLE
        );
        sb.add("meta.lastModified",
                SchemaDefinition.Types.DATETIME,
                null,
                (source) -> toZoneDateTimeForISO8601OffsetDateTime(source.meta.lastModified),
                null,
                NOT_CREATABLE, NOT_UPDATEABLE
        );

        LOGGER.ok("The constructed GitHub EMU User schema");

        return sb;
    }

    @Override
    public Uid create(Set<Attribute> attributes) {
        SCIMEMUUser user = new SCIMEMUUser();
        SCIMEMUUser mapped = schemaDefinition.apply(attributes, user);

        Uid created = client.createEMUUser(mapped);

        return created;
    }

    @Override
    public Set<AttributeDelta> updateDelta(Uid uid, Set<AttributeDelta> modifications, OperationOptions options) {
        SCIMPatchOperations dest = new SCIMPatchOperations();

        schemaDefinition.applyDelta(modifications, dest);

        if (dest.hasAttributesChange()) {
            client.patchEMUUser(uid, dest);
        }

        return null;
    }

    @Override
    public void delete(Uid uid, OperationOptions options) {
        client.deleteEMUUser(uid, options);
    }

    @Override
    public int getByUid(Uid uid, ResultsHandler resultsHandler, OperationOptions options,
                        Set<String> returnAttributesSet, Set<String> fetchFieldsSet,
                        boolean allowPartialAttributeValues, int pageSize, int pageOffset) {
        SCIMEMUUser user = client.getEMUUser(uid, options, fetchFieldsSet);

        if (user != null) {
            resultsHandler.handle(toConnectorObject(schemaDefinition, user, returnAttributesSet, allowPartialAttributeValues));
            return 1;
        }
        return 0;
    }

    @Override
    public int getByName(Name name, ResultsHandler resultsHandler, OperationOptions options,
                         Set<String> returnAttributesSet, Set<String> fetchFieldsSet,
                         boolean allowPartialAttributeValues, int pageSize, int pageOffset) {
        SCIMEMUUser user = client.getEMUUser(name, options, fetchFieldsSet);

        if (user != null) {
            resultsHandler.handle(toConnectorObject(schemaDefinition, user, returnAttributesSet, allowPartialAttributeValues));
            return 1;
        }
        return 0;
    }

    @Override
    public int getAll(ResultsHandler resultsHandler, OperationOptions options,
                      Set<String> returnAttributesSet, Set<String> fetchFieldsSet,
                      boolean allowPartialAttributeValues, int pageSize, int pageOffset) {
        return client.getEMUUsers((u) -> resultsHandler.handle(toConnectorObject(schemaDefinition, u, returnAttributesSet, allowPartialAttributeValues)),
                options, fetchFieldsSet, pageSize, pageOffset);
    }
}
