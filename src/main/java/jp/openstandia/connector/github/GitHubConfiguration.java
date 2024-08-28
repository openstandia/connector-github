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

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * Connector Configuration implementation for GitHub connector.
 *
 * @author Hiroyuki Wada
 */
public class GitHubConfiguration extends AbstractGitHubConfiguration {

    private GuardedString privateKey;
    private String appId;
    private long installationId;
    private String organizationName;

    @ConfigurationProperty(
            order = 1,
            displayMessageKey = "Private Key (PEM)",
            helpMessageKey = "Set Private Key with PEM format for GitHub API.",
            required = true,
            confidential = true)
    public GuardedString getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(GuardedString privateKey) {
        this.privateKey = privateKey;
    }

    @ConfigurationProperty(
            order = 2,
            displayMessageKey = "App ID",
            helpMessageKey = "Set App ID for GitHub.",
            required = true,
            confidential = false)
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    @ConfigurationProperty(
            order = 3,
            displayMessageKey = "Installation ID",
            helpMessageKey = "Set Installation ID for GitHub.",
            required = true,
            confidential = true)
    public long getInstallationId() {
        return installationId;
    }

    public void setInstallationId(long installationId) {
        this.installationId = installationId;
    }

    @ConfigurationProperty(
            order = 4,
            displayMessageKey = "Organization Name",
            helpMessageKey = "Set GitHub organization name.",
            required = true,
            confidential = false)
    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    @Override
    public void validate() {
    }
}
