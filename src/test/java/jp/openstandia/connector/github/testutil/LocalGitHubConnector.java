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
package jp.openstandia.connector.github.testutil;

import jp.openstandia.connector.github.GitHubClient;
import jp.openstandia.connector.github.GitHubConfiguration;
import jp.openstandia.connector.github.GitHubConnector;

public class LocalGitHubConnector extends GitHubConnector {
    @Override
    protected GitHubClient newClient(GitHubConfiguration configuration) {
        return MockClient.instance();
    }
}