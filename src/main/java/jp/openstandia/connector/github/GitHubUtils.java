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

import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.kohsuke.github.*;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides utility methods.
 *
 * @author Hiroyuki Wada
 */
public class GitHubUtils {

    public static ZonedDateTime toZoneDateTime(OffsetDateTime dateTime) {
        return dateTime.toZonedDateTime();
    }

    public static String toResourceAttributeValue(String s) {
        // To support deleting value, return empty string
        if (s == null) {
            return "";
        }

        return s;
    }

    public static String toResourceAttributeValue(String s, String defaultValue) {
        if (s == null) {
            return defaultValue;
        }

        return s;
    }

    public static boolean shouldReturn(Set<String> attrsToGetSet, String attr) {
        if (attrsToGetSet == null) {
            return true;
        }
        return attrsToGetSet.contains(attr);
    }

    /**
     * Check if ALLOW_PARTIAL_ATTRIBUTE_VALUES == true.
     *
     * @param options operation options
     * @return true: allow partial attribute values, false: not allow
     */
    public static boolean shouldAllowPartialAttributeValues(OperationOptions options) {
        // If the option isn't set from IDM, it may be null.
        return Boolean.TRUE.equals(options.getAllowPartialAttributeValues());
    }

    /**
     * Check if RETURN_DEFAULT_ATTRIBUTES == true.
     *
     * @param options operation options
     * @return true: return default attributes, false: not return
     */
    public static boolean shouldReturnDefaultAttributes(OperationOptions options) {
        // If the option isn't set from IDM, it may be null.
        return Boolean.TRUE.equals(options.getReturnDefaultAttributes());
    }

    /**
     * Create full set of ATTRIBUTES_TO_GET which is composed by RETURN_DEFAULT_ATTRIBUTES + ATTRIBUTES_TO_GET.
     *
     * @param schema  schema map
     * @param options operation options
     * @return set of the attributes to get
     */
    public static Set<String> createFullAttributesToGet(Map<String, AttributeInfo> schema, OperationOptions options) {
        Set<String> attributesToGet = null;
        if (shouldReturnDefaultAttributes(options)) {
            attributesToGet = new HashSet<>();
            attributesToGet.addAll(toReturnedByDefaultAttributesSet(schema));
        }
        if (options.getAttributesToGet() != null) {
            if (attributesToGet == null) {
                attributesToGet = new HashSet<>();
            }
            for (String a : options.getAttributesToGet()) {
                attributesToGet.add(a);
            }
        }
        return attributesToGet;
    }

    private static Set<String> toReturnedByDefaultAttributesSet(Map<String, AttributeInfo> schema) {
        return schema.entrySet().stream()
                .filter(entry -> entry.getValue().isReturnedByDefault())
                .map(entry -> entry.getKey())
                .collect(Collectors.toSet());
    }

    public static Throwable getRootCause(final Throwable t) {
        final List<Throwable> list = getThrowableList(t);
        return list.size() < 2 ? null : list.get(list.size() - 1);
    }

    private static List<Throwable> getThrowableList(Throwable t) {
        final List<Throwable> list = new ArrayList<>();
        while (t != null && !list.contains(t)) {
            list.add(t);
            t = t.getCause();
        }
        return list;
    }

    public static Uid toUserUid(SCIMUser user) {
        return new Uid(user.id, new Name(toUserName(user)));
    }

    public static String toUserName(SCIMUser user) {
        return toUserName(null, user.userName);
    }

    public static String toUserName(String login, String scimUserName) {
        if (login == null) {
            // Need to return the format with <login>:<scimUserName>
            // GitHub username policy is:
            // Username may only contain alphanumeric characters or single hyphens, and cannot begin or end with a hyphen.
            // So, return special "_unknown_" tag here because we can't determine the user login name yet
            return UNKNOWN_USER_NAME + ":" + scimUserName;
        }
        return login + ":" + scimUserName;
    }

    public static final String UNKNOWN_USER_NAME = "_unknown_";

    public static String getUserLogin(Uid uid) throws InvalidAttributeValueException {
        return getUserLogin(uid.getNameHintValue());
    }

    public static String getUserSCIMUserName(Uid uid) throws InvalidAttributeValueException {
        return getUserSCIMUserName(uid.getNameHintValue());
    }

    public static String getUserSCIMUserName(Name name) throws InvalidAttributeValueException {
        return getUserSCIMUserName(name.getNameValue());
    }

    public static String getUserSCIMUserName(String nameValue) throws InvalidAttributeValueException {
        return parseUserNameValue(nameValue)[1];
    }

    public static String getUserLogin(Name name) throws InvalidAttributeValueException {
        return getUserLogin(name.getNameValue());
    }

    public static String getUserLogin(String nameValue) throws InvalidAttributeValueException {
        return parseUserNameValue(nameValue)[0];
    }

    private static String[] parseUserNameValue(String nameValue) throws InvalidAttributeValueException {
        String[] split = nameValue.split(":");
        if (split.length != 2) {
            throw new InvalidAttributeValueException("GitHub userName must be \"login:scimUserName\" format. value: " + nameValue);
        }
        return split;
    }

    public static String toTeamUid(GHTeam team) {
        return toTeamUid(String.valueOf(team.getId()), team.getNodeId());
    }

    public static String toTeamUid(GraphQLTeamEdge teamEdge) {
        return toTeamUid(teamEdge.node);
    }

    public static String toTeamUid(GraphQLTeam team) {
        return toTeamUid(team.databaseId.toString(), team.id);
    }

    private static String toTeamUid(String databaseId, String nodeId) {
        return databaseId + ":" + nodeId;
    }

    public static long getTeamDatabaseId(Uid uid) {
        return getTeamDatabaseId(uid.getUidValue());
    }

    public static long getTeamDatabaseId(String uid) throws InvalidAttributeValueException {
        String databaseId = parseTeamUidValue(uid)[0];

        try {
            return Long.parseLong(databaseId);
        } catch (NumberFormatException e) {
            throw new InvalidAttributeValueException("Unexpected teamId: " + uid);
        }
    }

    public static String getTeamNodeId(Uid uid) throws InvalidAttributeValueException {
        return parseTeamUidValue(uid.getUidValue())[1];
    }

    private static String[] parseTeamUidValue(String uidValue) throws InvalidAttributeValueException {
        String[] split = uidValue.split(":");
        if (split.length != 2) {
            throw new InvalidAttributeValueException("GitHub teamId must be \"databaseId:nodeId\" format. value: " + uidValue);
        }
        return split;
    }

    public static GHTeam.Privacy toGHTeamPrivacy(String privacy) throws InvalidAttributeValueException {
        try {
            // Validation
            GraphQLTeamPrivacy gp = GraphQLTeamPrivacy.valueOf(privacy.toUpperCase());

            // Need to convert
            GHTeam.Privacy ghp = null;
            if (gp == GraphQLTeamPrivacy.SECRET) {
                ghp = GHTeam.Privacy.SECRET;
            } else {
                ghp = GHTeam.Privacy.CLOSED;
            }
            return ghp;

        } catch (IllegalArgumentException e) {
            throw new InvalidAttributeValueException("GitHub Team privacy must be \"visible\" or \"secret\": " + privacy);
        }
    }
}
