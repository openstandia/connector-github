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

import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.spi.operations.SearchOp;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Schema for GitHub objects.
 *
 * @author Hiroyuki Wada
 */
public class GitHubSchema {

    private final GitHubConfiguration configuration;
    private final GitHubClient client;

    public final Schema schema;
    public final Map<String, AttributeInfo> userSchema;
    public final Map<String, AttributeInfo> roleSchema;

    public GitHubSchema(GitHubConfiguration configuration, GitHubClient client) {
        this.configuration = configuration;
        this.client = client;

        SchemaBuilder schemaBuilder = new SchemaBuilder(GitHubConnector.class);

        ObjectClassInfo userSchemaInfo = GitHubUserHandler.getUserSchema();
        schemaBuilder.defineObjectClass(userSchemaInfo);

        ObjectClassInfo roleSchemaInfo = GitHubTeamHandler.getRoleSchema();
        schemaBuilder.defineObjectClass(roleSchemaInfo);

        schemaBuilder.defineOperationOption(OperationOptionInfoBuilder.buildAttributesToGet(), SearchOp.class);
        schemaBuilder.defineOperationOption(OperationOptionInfoBuilder.buildReturnDefaultAttributes(), SearchOp.class);

        schema = schemaBuilder.build();

        Map<String, AttributeInfo> userSchemaMap = new HashMap<>();
        for (AttributeInfo info : userSchemaInfo.getAttributeInfo()) {
            userSchemaMap.put(info.getName(), info);
        }

        Map<String, AttributeInfo> roleSchemaMp = new HashMap<>();
        for (AttributeInfo info : roleSchemaInfo.getAttributeInfo()) {
            roleSchemaMp.put(info.getName(), info);
        }

        this.userSchema = Collections.unmodifiableMap(userSchemaMap);
        this.roleSchema = Collections.unmodifiableMap(roleSchemaMp);
    }
}
