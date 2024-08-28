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

import org.identityconnectors.framework.common.objects.OperationOptionInfoBuilder;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.spi.operations.SearchOp;

/**
 * Schema for GitHub objects.
 *
 * @author Hiroyuki Wada
 */
public class GitHubEMUSchema extends AbstractGitHubSchema<GitHubEMUConfiguration> {

    public final Schema schema;

    public GitHubEMUSchema(GitHubEMUConfiguration configuration, GitHubClient<GitHubEMUSchema> client) {
        super(configuration, client);

        SchemaBuilder schemaBuilder = new SchemaBuilder(GitHubConnector.class);

        buildSchema(schemaBuilder, GitHubEMUUserHandler.createSchema(configuration, client).build(),
                (schema) -> new GitHubEMUUserHandler(configuration, client, this, schema));
        buildSchema(schemaBuilder, GitHubEMUGroupHandler.createSchema(configuration, client).build(),
                (schema) -> new GitHubEMUGroupHandler(configuration, client, this, schema));

        // Define operation options
        schemaBuilder.defineOperationOption(OperationOptionInfoBuilder.buildAttributesToGet(), SearchOp.class);
        schemaBuilder.defineOperationOption(OperationOptionInfoBuilder.buildReturnDefaultAttributes(), SearchOp.class);
        schemaBuilder.defineOperationOption(OperationOptionInfoBuilder.buildPageSize(), SearchOp.class);
        schemaBuilder.defineOperationOption(OperationOptionInfoBuilder.buildPagedResultsOffset(), SearchOp.class);

        this.schema = schemaBuilder.build();
    }

    @Override
    public Schema getSchema() {
        return schema;
    }

}
