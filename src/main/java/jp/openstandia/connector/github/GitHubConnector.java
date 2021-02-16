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

import jp.openstandia.connector.github.rest.GitHubRESTClient;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.InstanceNameAware;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.*;

import java.util.Set;

import static jp.openstandia.connector.github.GitHubTeamHandler.TEAM_OBJECT_CLASS;
import static jp.openstandia.connector.github.GitHubUserHandler.USER_OBJECT_CLASS;

/**
 * Connector implementation for GitHub connector.
 *
 * @author Hiroyuki Wada
 */
@ConnectorClass(configurationClass = GitHubConfiguration.class, displayNameKey = "NRI OpenStandia GitHub Connector")
public class GitHubConnector implements PoolableConnector, CreateOp, UpdateDeltaOp, DeleteOp, SchemaOp, TestOp, SearchOp<GitHubFilter>, InstanceNameAware {

    private static final Log LOG = Log.getLog(GitHubConnector.class);

    protected GitHubConfiguration configuration;
    protected GitHubClient client;

    private GitHubSchema schema;
    private String instanceName;

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void init(Configuration configuration) {
        this.configuration = (GitHubConfiguration) configuration;

        try {
            initClient();
            getSchema();
        } catch (RuntimeException e) {
            throw processRuntimeException(e);
        }

        LOG.ok("Connector {0} successfully initialized", getClass().getName());
    }

    protected void initClient() {
        client = new GitHubRESTClient(instanceName, configuration);
    }

    @Override
    public Schema schema() {
        try {
            schema = new GitHubSchema(configuration, client);
            return schema.schema;

        } catch (RuntimeException e) {
            throw processRuntimeException(e);
        }
    }

    private GitHubSchema getSchema() {
        // Load schema map if it's not loaded yet
        if (schema == null) {
            schema();
        }
        return schema;
    }

    protected AbstractGitHubHandler createHandler(ObjectClass objectClass) {
        if (objectClass == null) {
            throw new InvalidAttributeValueException("ObjectClass value not provided");
        }

        if (objectClass.equals(USER_OBJECT_CLASS)) {
            return new GitHubUserHandler(instanceName, configuration, client, schema);

        } else if (objectClass.equals(TEAM_OBJECT_CLASS)) {
            return new GitHubTeamHandler(instanceName, configuration, client, schema);

        } else {
            throw new InvalidAttributeValueException("Unsupported object class " + objectClass);
        }
    }

    @Override
    public Uid create(ObjectClass objectClass, Set<Attribute> createAttributes, OperationOptions options) {
        if (createAttributes == null || createAttributes.isEmpty()) {
            throw new InvalidAttributeValueException("Attributes not provided or empty");
        }

        try {
            return createHandler(objectClass).create(createAttributes);

        } catch (RuntimeException e) {
            throw processRuntimeException(e);
        }
    }

    @Override
    public Set<AttributeDelta> updateDelta(ObjectClass objectClass, Uid uid, Set<AttributeDelta> modifications, OperationOptions options) {
        if (uid == null) {
            throw new InvalidAttributeValueException("uid not provided");
        }
        if (modifications == null || modifications.isEmpty()) {
            throw new InvalidAttributeValueException("modifications not provided or empty");
        }

        try {
            return createHandler(objectClass).updateDelta(uid, modifications, options);

        } catch (RuntimeException e) {
            throw processRuntimeException(e);
        }
    }

    @Override
    public void delete(ObjectClass objectClass, Uid uid, OperationOptions options) {
        if (uid == null) {
            throw new InvalidAttributeValueException("uid not provided");
        }

        try {
            createHandler(objectClass).delete(uid, options);

        } catch (RuntimeException e) {
            throw processRuntimeException(e);
        }
    }

    @Override
    public FilterTranslator<GitHubFilter> createFilterTranslator(ObjectClass objectClass, OperationOptions options) {
        return new GitHubFilterTranslator(objectClass, options);
    }

    @Override
    public void executeQuery(ObjectClass objectClass, GitHubFilter filter, ResultsHandler resultsHandler, OperationOptions options) {
        try {
            createHandler(objectClass).query(filter, resultsHandler, options);

        } catch (RuntimeException e) {
            throw processRuntimeException(e);
        }
    }

    @Override
    public void test() {
        try {
            dispose();
            initClient();
            client.test();
        } catch (RuntimeException e) {
            throw processRuntimeException(e);
        }
    }

    @Override
    public void dispose() {
        client.close();
        this.client = null;
    }

    @Override
    public void checkAlive() {
        // Do nothing
    }

    @Override
    public void setInstanceName(String instanceName) {
        // Called after initialized
        this.instanceName = instanceName;
    }

    protected ConnectorException processRuntimeException(RuntimeException e) {
        if (e instanceof ConnectorException) {
            return (ConnectorException) e;
        }
        return new ConnectorException(e);
    }
}
