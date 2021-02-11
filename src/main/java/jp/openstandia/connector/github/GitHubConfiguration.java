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
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * Connector Configuration implementation for GitHub connector.
 *
 * @author Hiroyuki Wada
 */
public class GitHubConfiguration extends AbstractConfiguration {

    private GuardedString privateKey;
    private String appId;
    private long installationId;
    private String organizationName;

    private String httpProxyHost;
    private int httpProxyPort;
    private String httpProxyUser;
    private GuardedString httpProxyPassword;
    private int queryPageSize = 30;
    private int connectionTimeoutInMilliseconds = 10000; // 10s
    private int readTimeoutInMilliseconds = 10000; // 10s
    private int writeTimeoutInMilliseconds = 10000; // 10s

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

    @ConfigurationProperty(
            order = 5,
            displayMessageKey = "Query page size",
            helpMessageKey = "Set page size for GitHub paging API (Default: 30, Max: 100)",
            required = false,
            confidential = false)
    public int getQueryPageSize() {
        return queryPageSize;
    }

    public void setQueryPageSize(int queryPageSize) {
        this.queryPageSize = queryPageSize;
    }


    @ConfigurationProperty(
            order = 6,
            displayMessageKey = "Connection Timeout",
            helpMessageKey = "Connection timeout in milliseconds. (Default: 10000)",
            required = false,
            confidential = false)
    public int getConnectionTimeoutInMilliseconds() {
        return connectionTimeoutInMilliseconds;
    }

    public void setConnectionTimeoutInMilliseconds(int connectionTimeoutInMilliseconds) {
        this.connectionTimeoutInMilliseconds = connectionTimeoutInMilliseconds;
    }

    @ConfigurationProperty(
            order = 7,
            displayMessageKey = "Read Timeout",
            helpMessageKey = "Read timeout in milliseconds. (Default: 30000)",
            required = false,
            confidential = false)
    public int getReadTimeoutInMilliseconds() {
        return readTimeoutInMilliseconds;
    }

    public void setReadTimeoutInMilliseconds(int readTimeoutInMilliseconds) {
        this.readTimeoutInMilliseconds = readTimeoutInMilliseconds;
    }

    @ConfigurationProperty(
            order = 8,
            displayMessageKey = "Write Timeout",
            helpMessageKey = "Write timeout in milliseconds. (Default: 30000)",
            required = false,
            confidential = false)
    public int getWriteTimeoutInMilliseconds() {
        return writeTimeoutInMilliseconds;
    }

    public void setWriteTimeoutInMilliseconds(int writeTimeoutInMilliseconds) {
        this.writeTimeoutInMilliseconds = writeTimeoutInMilliseconds;
    }

    @ConfigurationProperty(
            order = 9,
            displayMessageKey = "HTTP Proxy Host",
            helpMessageKey = "Hostname for the HTTP Proxy",
            required = false,
            confidential = false)
    public String getHttpProxyHost() {
        return httpProxyHost;
    }

    public void setHttpProxyHost(String httpProxyHost) {
        this.httpProxyHost = httpProxyHost;
    }

    @ConfigurationProperty(
            order = 10,
            displayMessageKey = "HTTP Proxy Port",
            helpMessageKey = "Port for the HTTP Proxy",
            required = false,
            confidential = false)
    public int getHttpProxyPort() {
        return httpProxyPort;
    }

    public void setHttpProxyPort(int httpProxyPort) {
        this.httpProxyPort = httpProxyPort;
    }

    @ConfigurationProperty(
            order = 11,
            displayMessageKey = "HTTP Proxy User",
            helpMessageKey = "Username for the HTTP Proxy Authentication",
            required = false,
            confidential = false)
    public String getHttpProxyUser() {
        return httpProxyUser;
    }

    public void setHttpProxyUser(String httpProxyUser) {
        this.httpProxyUser = httpProxyUser;
    }

    @ConfigurationProperty(
            order = 12,
            displayMessageKey = "HTTP Proxy Password",
            helpMessageKey = "Password for the HTTP Proxy Authentication",
            required = false,
            confidential = true)
    public GuardedString getHttpProxyPassword() {
        return httpProxyPassword;
    }

    public void setHttpProxyPassword(GuardedString httpProxyPassword) {
        this.httpProxyPassword = httpProxyPassword;
    }

    @Override
    public void validate() {
    }
}
