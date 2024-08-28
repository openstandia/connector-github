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
import jp.openstandia.connector.util.Utils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.InstanceNameAware;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.identityconnectors.framework.spi.operations.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Connector super class for GitHub connectors.
 *
 * @author Hiroyuki Wada
 */
public abstract class AbstractGitHubConnector<T extends AbstractGitHubConfiguration, U extends AbstractGitHubSchema<T>> implements PoolableConnector, CreateOp, UpdateDeltaOp, DeleteOp, SchemaOp, TestOp, SearchOp<GitHubFilter>, InstanceNameAware {

    private static final Log LOG = Log.getLog(AbstractGitHubConnector.class);

    protected T configuration;
    protected GitHubClient<U> client;
    protected String instanceName;
    protected U schema;

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

    @Override
    public void init(Configuration configuration) {
        this.configuration = (T) configuration;

        try {
            this.client = newClient(this.configuration);
            getSchema();
        } catch (RuntimeException e) {
            throw processRuntimeException(e);
        }

        LOG.ok("Connector {0} successfully initialized", getClass().getName());
    }

    protected abstract GitHubClient<U> newClient(T configuration);

    protected abstract U newGitHubSchema(T configuration, GitHubClient<U> client);

    @Override
    public Schema schema() {
        try {
            schema = newGitHubSchema(configuration, client);
            return schema.getSchema();

        } catch (RuntimeException e) {
            throw processRuntimeException(e);
        }
    }

    private U getSchema() {
        // Load schema map if it's not loaded yet
        if (schema == null) {
            schema();
        }
        return schema;
    }

    private ObjectHandler getSchemaHandler(ObjectClass objectClass) {
        if (objectClass == null) {
            throw new InvalidAttributeValueException("ObjectClass value not provided");
        }

        // Load schema map if it's not loaded yet
        if (schema == null) {
            schema();
        }

        ObjectHandler handler = schema.getSchemaHandler(objectClass);

        if (handler == null) {
            throw new InvalidAttributeValueException("Unsupported object class " + objectClass);
        }
       
        handler.setInstanceName(instanceName);

        return handler;
    }

    @Override
    public Uid create(ObjectClass objectClass, Set<Attribute> createAttributes, OperationOptions options) {
        if (createAttributes == null || createAttributes.isEmpty()) {
            throw new InvalidAttributeValueException("Attributes not provided or empty");
        }

        try {
            return getSchemaHandler(objectClass).create(createAttributes);

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
            return getSchemaHandler(objectClass).updateDelta(uid, modifications, options);

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
            getSchemaHandler(objectClass).delete(uid, options);

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
        if (this instanceof GitHubEMUConnector) {
            executeQueryWithSearchResult(objectClass, filter, resultsHandler, options);
        } else {
            try {
                getSchemaHandler(objectClass).query(filter, resultsHandler, options);

            } catch (RuntimeException e) {
                throw processRuntimeException(e);
            }
        }
    }

    protected void executeQueryWithSearchResult(ObjectClass objectClass, GitHubFilter filter, ResultsHandler resultsHandler, OperationOptions options) {
        try {
            ObjectHandler schemaHandler = getSchemaHandler(objectClass);
            SchemaDefinition schema = schemaHandler.getSchemaDefinition();

            int pageSize = Utils.resolvePageSize(options, configuration.getQueryPageSize());
            int pageOffset = Utils.resolvePageOffset(options);

            // Create full attributesToGet by RETURN_DEFAULT_ATTRIBUTES + ATTRIBUTES_TO_GET
            Map<String, String> attributesToGet = Utils.createFullAttributesToGet(schema, options);
            Set<String> returnAttributesSet = attributesToGet.keySet();
            // Collect actual resource fields for fetching (We can them for filtering attributes if the resource supports it)
            Set<String> fetchFieldSet = new HashSet<>(attributesToGet.values());

            boolean allowPartialAttributeValues = Utils.shouldAllowPartialAttributeValues(options);

            int total = 0;
            AtomicInteger fetchedCount = new AtomicInteger();
            ResultsHandler countableResultHandler = (connectorObject) -> {
                fetchedCount.getAndIncrement();
                return resultsHandler.handle(connectorObject);
            };

            if (filter != null) {
                if (filter.isByUid()) {
                    total = schemaHandler.getByUid(filter.uid, countableResultHandler, options,
                            returnAttributesSet, fetchFieldSet,
                            allowPartialAttributeValues, pageSize, pageOffset);
                } else if (filter.isByName()) {
                    total = schemaHandler.getByName(filter.name, countableResultHandler, options,
                            returnAttributesSet, fetchFieldSet,
                            allowPartialAttributeValues, pageSize, pageOffset);
                } else if (filter.isByMembers()) {
                    total = schemaHandler.getByMembers(filter.attributeValue, countableResultHandler, options,
                            returnAttributesSet, fetchFieldSet,
                            allowPartialAttributeValues, pageSize, pageOffset);
                }
                // No result
            } else {
                total = schemaHandler.getAll(countableResultHandler, options,
                        returnAttributesSet, fetchFieldSet,
                        allowPartialAttributeValues, pageSize, pageOffset);
            }

            if (resultsHandler instanceof SearchResultsHandler &&
                    pageOffset > 0) {

                int remaining = total - (pageOffset - 1) - fetchedCount.get();

                SearchResultsHandler searchResultsHandler = (SearchResultsHandler) resultsHandler;
                SearchResult searchResult = new SearchResult(null, remaining);
                searchResultsHandler.handleResult(searchResult);
            }

        } catch (RuntimeException e) {
            throw processRuntimeException(e);
        }
    }

    @Override
    public void test() {
        try {
            dispose();
            client = newClient(this.configuration);
            if (instanceName != null) {
                client.setInstanceName(instanceName);
            }
            client.test();
        } catch (RuntimeException e) {
            throw processRuntimeException(e);
        }
    }

    @Override
    public void dispose() {
        if (client != null) {
            client.close();
            this.client = null;
        }
    }

    @Override
    public void checkAlive() {
        // Do nothing
    }

    @Override
    public void setInstanceName(String instanceName) {
        // Called after initialized
        this.instanceName = instanceName;
        this.client.setInstanceName(instanceName);
    }

    protected ConnectorException processRuntimeException(RuntimeException e) {
        if (e instanceof ConnectorException) {
            return (ConnectorException) e;
        }
        return new ConnectorException(e);
    }
}
