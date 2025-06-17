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
import org.kohsuke.github.SCIMEMUGroup;
import org.kohsuke.github.SCIMMember;
import org.kohsuke.github.SCIMPatchOperations;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jp.openstandia.connector.util.Utils.toZoneDateTimeForISO8601OffsetDateTime;
import static org.identityconnectors.framework.common.objects.AttributeInfo.Flags.*;

/**
 * Handle GitHub EMU Group object.
 *
 * @author Hiroyuki Wada
 */
public class GitHubEMUGroupHandler extends AbstractGitHubEMUHandler {

    public static final ObjectClass GROUP_OBJECT_CLASS = new ObjectClass("EMUGroup");

    private static final Log LOGGER = Log.getLog(GitHubEMUGroupHandler.class);

    public GitHubEMUGroupHandler(GitHubEMUConfiguration configuration, GitHubClient<GitHubEMUSchema> client,
                                 GitHubEMUSchema schema, SchemaDefinition schemaDefinition) {
        super(configuration, client, schema, schemaDefinition);
    }

    public static SchemaDefinition.Builder createSchema(AbstractGitHubConfiguration configuration, GitHubClient<GitHubEMUSchema> client) {
        SchemaDefinition.Builder<SCIMEMUGroup, SCIMPatchOperations, SCIMEMUGroup> sb
                = SchemaDefinition.newBuilder(GROUP_OBJECT_CLASS, SCIMEMUGroup.class, SCIMPatchOperations.class, SCIMEMUGroup.class);

        // GitHub EMU supports SCIM v2.0 partially.
        // Spec: https://docs.github.com/en/enterprise-cloud@latest/rest/enterprise-admin/scim?apiVersion=2022-11-28#supported-scim-group-attributes

        // __UID__
        // The id for the user. Must be unique and unchangeable.
        sb.addUid("groupId",
                SchemaDefinition.Types.UUID,
                null,
                (source) -> source.id,
                "id",
                NOT_CREATABLE, NOT_UPDATEABLE
        );

        // code (__NAME__)
        // The name for the group. Must be unique and changeable.
        sb.addName("displayName",
                SchemaDefinition.Types.STRING_CASE_IGNORE,
                (source, dest) -> dest.displayName = source,
                (source, dest) -> dest.replace("displayName", source),
                (source) -> source.displayName,
                null,
                REQUIRED
        );

        // Attributes
        sb.add("externalId",
                SchemaDefinition.Types.STRING,
                (source, dest) -> {
                    dest.externalId = source;
                },
                (source, dest) -> dest.replace("externalId", source),
                (source) -> source.externalId,
                null
        );

        // Association
        sb.addAsMultiple("members.User.value",
                SchemaDefinition.Types.UUID,
                (source, dest) -> {
                    dest.members = source != null ? source.stream().map(x -> {
                        SCIMMember scimMember = new SCIMMember();
                        scimMember.value = x;
                        return scimMember;
                    }).collect(Collectors.toList()) : null;
                },
                (add, dest) -> dest.addMembers(add),
                (remove, dest) -> dest.removeMembers(remove),
                (source) -> source.members != null ? source.members.stream().filter(x -> x.ref.contains("/Users/")).map(x -> x.value) : Stream.empty(),
                null
        );

        // Metadata (readonly)
        sb.add("meta.created",
                SchemaDefinition.Types.DATETIME,
                null,
                (source) -> source.meta != null ? toZoneDateTimeForISO8601OffsetDateTime(source.meta.created) : null,
                null,
                NOT_CREATABLE, NOT_UPDATEABLE
        );
        sb.add("meta.lastModified",
                SchemaDefinition.Types.DATETIME,
                null,
                (source) -> source.meta != null ? toZoneDateTimeForISO8601OffsetDateTime(source.meta.lastModified) : null,
                null,
                NOT_CREATABLE, NOT_UPDATEABLE
        );

        LOGGER.ok("The constructed GitHub EMU User schema");

        return sb;
    }

    @Override
    public Uid create(Set<Attribute> attributes) {
        SCIMEMUGroup user = new SCIMEMUGroup();
        SCIMEMUGroup mapped = schemaDefinition.apply(attributes, user);

        Uid created = client.createEMUGroup(schema, mapped);

        return created;
    }

    @Override
    public Set<AttributeDelta> updateDelta(Uid uid, Set<AttributeDelta> modifications, OperationOptions options) {
        SCIMPatchOperations dest = new SCIMPatchOperations();

        schemaDefinition.applyDelta(modifications, dest);

        if (dest.hasAttributesChange()) {
            client.patchEMUGroup(uid, dest);
        }

        return null;
    }

    @Override
    public void delete(Uid uid, OperationOptions options) {
        client.deleteEMUGroup(uid, options);
    }

    @Override
    public int getByUid(Uid uid, ResultsHandler resultsHandler, OperationOptions options,
                        Set<String> returnAttributesSet, Set<String> fetchFieldsSet,
                        boolean allowPartialAttributeValues, int pageSize, int pageOffset) {
        SCIMEMUGroup user = client.getEMUGroup(uid, options, fetchFieldsSet);

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
        SCIMEMUGroup group = client.getEMUGroup(name, options, fetchFieldsSet);

        if (group != null) {
            resultsHandler.handle(toConnectorObject(schemaDefinition, group, returnAttributesSet, allowPartialAttributeValues));
            return 1;
        }
        return 0;
    }

    @Override
    public int getAll(ResultsHandler resultsHandler, OperationOptions options,
                      Set<String> returnAttributesSet, Set<String> fetchFieldsSet,
                      boolean allowPartialAttributeValues, int pageSize, int pageOffset) {
        return client.getEMUGroups((g) -> resultsHandler.handle(toConnectorObject(schemaDefinition, g, returnAttributesSet, allowPartialAttributeValues)),
                options, fetchFieldsSet, pageSize, pageOffset);
    }

    @Override
    public int getByMembers(Attribute attribute, ResultsHandler resultsHandler, OperationOptions options, Set<String> returnAttributesSet, Set<String> fetchFieldSet, boolean allowPartialAttributeValues, int pageSize, int pageOffset) {
        // Unfortunately, GitHub EMU doesn't support filter by members.value (It supports displayName, id and displayName filter).
        // So, we need to fetch all groups.
        Set<Object> memberIds = new HashSet<>(attribute.getValue());
        return client.getEMUGroups((g) -> {
            // Filter by member's value
            boolean contains = g.members.stream()
                    .map(m -> m.value)
                    .collect(Collectors.toSet())
                    .containsAll(memberIds);
            if (contains) {
                return resultsHandler.handle(toConnectorObject(schemaDefinition, g, returnAttributesSet, allowPartialAttributeValues));
            }

            return true;
        }, options, fetchFieldSet, pageSize, pageOffset);
    }
}
