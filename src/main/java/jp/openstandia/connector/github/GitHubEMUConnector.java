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

import jp.openstandia.connector.github.rest.GitHubEMURESTClient;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.spi.ConnectorClass;

import static jp.openstandia.connector.github.GitHubEMUGroupHandler.GROUP_OBJECT_CLASS;
import static jp.openstandia.connector.github.GitHubUserHandler.USER_OBJECT_CLASS;

/**
 * Connector implementation for GitHub EMU connector.
 *
 * @author Hiroyuki Wada
 */
@ConnectorClass(configurationClass = GitHubEMUConfiguration.class, displayNameKey = "NRI OpenStandia GitHub EMU Connector")
public class GitHubEMUConnector extends AbstractGitHubConnector<GitHubEMUConfiguration, GitHubEMUSchema> {

    private static final Log LOG = Log.getLog(GitHubEMUConnector.class);

    @Override
    protected GitHubClient<GitHubEMUSchema> newClient(GitHubEMUConfiguration configuration) {
        return new GitHubEMURESTClient(configuration);
    }

    @Override
    protected GitHubEMUSchema newGitHubSchema(GitHubEMUConfiguration configuration, GitHubClient<GitHubEMUSchema> client) {
        return new GitHubEMUSchema(configuration, client);
    }
}
