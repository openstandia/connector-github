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

import jp.openstandia.connector.util.ObjectHandler;
import jp.openstandia.connector.util.SchemaDefinition;
import org.identityconnectors.framework.common.objects.*;

import java.util.Set;

/**
 * Base class for GitHub object handlers.
 *
 * @author Hiroyuki Wada
 */
public abstract class AbstractGitHubHandler<T extends AbstractGitHubConfiguration, U extends AbstractGitHubSchema<T>> implements ObjectHandler {

    protected String instanceName;
    protected final T configuration;
    protected final GitHubClient<U> client;
    protected final U schema;

    public AbstractGitHubHandler(T configuration, GitHubClient<U> client, U schema) {
        this.configuration = configuration;
        this.client = client;
        this.schema = schema;
    }

    public ObjectHandler setInstanceName(String instanceName) {
        this.instanceName = instanceName;
        return this;
    }

    @Override
    public SchemaDefinition getSchemaDefinition() {
        throw new UnsupportedOperationException();
    }
}
