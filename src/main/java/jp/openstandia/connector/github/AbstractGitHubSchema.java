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
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Base class for GitHub schema.
 *
 * @author Hiroyuki Wada
 */
public abstract class AbstractGitHubSchema<T extends AbstractGitHubConfiguration> {

    protected final T configuration;
    protected final GitHubClient<? extends AbstractGitHubSchema<T>> client;
    protected Map<String, ObjectHandler> schemaHandlerMap;

    public AbstractGitHubSchema(T configuration, GitHubClient<? extends AbstractGitHubSchema<T>> client) {
        this.configuration = configuration;
        this.client = client;
        this.schemaHandlerMap = new HashMap<>();
    }

    public abstract Schema getSchema();

    protected void buildSchema(SchemaBuilder builder, SchemaDefinition schemaDefinition, Function<SchemaDefinition, ObjectHandler> callback) {
        builder.defineObjectClass(schemaDefinition.getObjectClassInfo());
        ObjectHandler handler = callback.apply(schemaDefinition);
        this.schemaHandlerMap.put(schemaDefinition.getType(), handler);
    }

    protected void buildSchema(SchemaBuilder builder, ObjectClassInfo objectClassInfo, Function<ObjectClassInfo, ObjectHandler> callback) {
        builder.defineObjectClass(objectClassInfo);
        ObjectHandler handler = callback.apply(objectClassInfo);
        this.schemaHandlerMap.put(objectClassInfo.getType(), handler);
    }

    public ObjectHandler getSchemaHandler(ObjectClass objectClass) {
        return schemaHandlerMap.get(objectClass.getObjectClassValue());
    }
}
