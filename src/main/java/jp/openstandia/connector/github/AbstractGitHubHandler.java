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

import java.util.Set;

/**
 * Base class for GitHub object handlers.
 *
 * @author Hiroyuki Wada
 */
public abstract class AbstractGitHubHandler {

    protected final String instanceName;
    protected final GitHubConfiguration configuration;
    protected final GitHubClient client;
    protected final GitHubSchema schema;

    public AbstractGitHubHandler(String instanceName, GitHubConfiguration configuration, GitHubClient client, GitHubSchema schema) {
        this.instanceName = instanceName;
        this.configuration = configuration;
        this.client = client;
        this.schema = schema;
    }

    abstract Uid create(Set<Attribute> attributes);

    abstract Set<AttributeDelta> updateDelta(Uid uid, Set<AttributeDelta> modifications, OperationOptions options);

    abstract void delete(Uid uid, OperationOptions options);

    abstract void query(GitHubFilter filter, ResultsHandler resultsHandler, OperationOptions options);

}
