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

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.Uid;

/**
 * Filter for GitHub query.
 *
 * @author Hiroyuki Wada
 */
public class GitHubFilter {
    public final Uid uid;
    public final Name name;
    public final FilterType filterType;
    public final String attributeName;
    public final Attribute attributeValue;

    private GitHubFilter(Uid uid) {
        this.uid = uid;
        this.name = null;
        this.filterType = FilterType.EXACT_MATCH;
        this.attributeName = null;
        this.attributeValue = null;
    }

    private GitHubFilter(Name name) {
        this.uid = null;
        this.name = name;
        this.filterType = FilterType.EXACT_MATCH;
        this.attributeName = null;
        this.attributeValue = null;
    }

    public enum FilterType {
        EXACT_MATCH
    }

    private GitHubFilter(String attributeName, FilterType filterType, Attribute attributeValue) {
        this.uid = null;
        this.name = null;
        this.attributeName = attributeName;
        this.filterType = filterType;
        this.attributeValue = attributeValue;
    }

    public static GitHubFilter By(Uid uid) {
        return new GitHubFilter(uid);
    }

    public static GitHubFilter By(Name name) {
        return new GitHubFilter(name);
    }

    public static GitHubFilter ByMember(String attributeName, FilterType filterType, Attribute attributeValue) {
        return new GitHubFilter(attributeName, filterType, attributeValue);
    }

    public boolean isByUid() {
        return uid != null;
    }

    public boolean isByName() {
        return name != null;
    }

    public boolean isByMembers() {
        return attributeName.equals("members.User.value") && filterType == FilterType.EXACT_MATCH;
    }
}
