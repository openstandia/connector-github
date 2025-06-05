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
 * Connector Configuration implementation for GitHub EMU connector.
 * This connector works for EMU (Enterprise Managed Users).
 *
 * @author Hiroyuki Wada
 */
public class GitHubEMUConfiguration extends AbstractGitHubConfiguration {

    private String enterpriseSlug;
    private GuardedString accessToken;
    private String endpointURL;

    @ConfigurationProperty(
            order = 1,
            displayMessageKey = "Enterprise Slug",
            helpMessageKey = "Set GitHub enterprise slug for EMU.",
            required = true,
            confidential = false)
    public String getEnterpriseSlug() {
        return enterpriseSlug;
    }

    public void setEnterpriseSlug(String enterpriseSlug) {
        this.enterpriseSlug = enterpriseSlug;
    }

    @ConfigurationProperty(
            order = 2,
            displayMessageKey = "Access Token",
            helpMessageKey = "Set access token for GitHub SCIM API for EMU.",
            required = true,
            confidential = true)
    public GuardedString getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(GuardedString accessToken) {
        this.accessToken = accessToken;
    }

    @ConfigurationProperty(
            order = 3,
            displayMessageKey = "Endpoint URL",
            helpMessageKey = "Set GitHub endpoint URL (Default: https://api.github.com).",
            required = false,
            confidential = false)
    public String getEndpointURL() {
        return endpointURL;
    }

    public void setEndpointURL(String endpointURL) {
        this.endpointURL = endpointURL;
    }

    @Override
    public void validate() {
    }
}
