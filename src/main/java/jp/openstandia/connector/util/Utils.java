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
package jp.openstandia.connector.util;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeValueCompleteness;
import org.identityconnectors.framework.common.objects.OperationOptions;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Provides utility methods
 *
 * @author Hiroyuki Wada
 */
public class Utils {
    private static final Log LOG = Log.getLog(Utils.class);

    public static ZonedDateTime toZoneDateTime(String yyyymmdd) {
        if (yyyymmdd == null) {
            return null;
        }
        LocalDate date = LocalDate.parse(yyyymmdd);
        return date.atStartOfDay(ZoneId.systemDefault());
    }

    public static ZonedDateTime toZoneDateTime(DateTimeFormatter formatter, String datetimeString) {
        if (datetimeString == null) {
            return null;
        }
        Instant instant = Instant.from(formatter.parse(datetimeString));
        ZoneId zone = ZoneId.systemDefault();
        return ZonedDateTime.ofInstant(instant, zone);
    }

    public static ZonedDateTime toZoneDateTimeForEpochMilli(String epoch) {
        if (epoch == null) {
            return null;
        }
        Instant instant = Instant.ofEpochMilli(Long.parseLong(epoch));
        ZoneId zone = ZoneId.systemDefault();
        return ZonedDateTime.ofInstant(instant, zone);
    }

    public static ZonedDateTime toZoneDateTimeForISO8601OffsetDateTime(String datetimeString) {
        if (datetimeString == null) {
            return null;
        }
        return ZonedDateTime.parse(datetimeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                .withZoneSameInstant(ZoneId.systemDefault());
    }

    public static ZonedDateTime toZoneDateTime(Date date) {
        if (date == null) {
            return null;
        }
        ZoneId zone = ZoneId.systemDefault();
        return ZonedDateTime.ofInstant(date.toInstant(), zone);
    }

    /**
     * Check if attrsToGetSet contains the attribute.
     *
     * @param attrsToGetSet
     * @param attr
     * @param isReturnByDefault
     * @return
     */
    public static boolean shouldReturn(Set<String> attrsToGetSet, String attr, boolean isReturnByDefault) {
        if (attrsToGetSet == null) {
            return isReturnByDefault;
        }
        return attrsToGetSet.contains(attr);
    }

    public static boolean shouldReturn(Set<String> attrsToGetSet, String attr) {
        return attrsToGetSet.contains(attr);
    }

    public static Attribute createIncompleteAttribute(String attr) {
        AttributeBuilder builder = new AttributeBuilder();
        builder.setName(attr).setAttributeValueCompleteness(AttributeValueCompleteness.INCOMPLETE);
        builder.addValue(Collections.EMPTY_LIST);
        return builder.build();
    }

    /**
     * Check if ALLOW_PARTIAL_ATTRIBUTE_VALUES == true.
     *
     * @param options
     * @return
     */
    public static boolean shouldAllowPartialAttributeValues(OperationOptions options) {
        // If the option isn't set from IDM, it may be null.
        return Boolean.TRUE.equals(options.getAllowPartialAttributeValues());
    }

    /**
     * Check if RETURN_DEFAULT_ATTRIBUTES == true.
     *
     * @param options
     * @return
     */
    public static boolean shouldReturnDefaultAttributes(OperationOptions options) {
        // If the option isn't set from IDM, it may be null.
        return Boolean.TRUE.equals(options.getReturnDefaultAttributes());
    }

    /**
     * Create full map of ATTRIBUTES_TO_GET which is composed by RETURN_DEFAULT_ATTRIBUTES + ATTRIBUTES_TO_GET.
     * Key: attribute name of the connector (e.g. __UID__)
     * Value: field name for resource fetching
     *
     * @param schema
     * @param options
     * @return
     */
    public static Map<String, String> createFullAttributesToGet(SchemaDefinition schema, OperationOptions options) {
        Map<String, String> attributesToGet = new HashMap<>();

        if (shouldReturnDefaultAttributes(options)) {
            attributesToGet.putAll(toReturnedByDefaultAttributesSet(schema));
        }

        if (options.getAttributesToGet() != null) {
            for (String a : options.getAttributesToGet()) {
                String fetchField = schema.getFetchField(a);
                if (fetchField == null) {
                    LOG.warn("Requested unknown attribute to get. Ignored it: {0}", a);
                    continue;
                }
                attributesToGet.put(a, fetchField);
            }
        }

        // If ATTRS_TO_GET option is not present (also, RETURN_DEFAULT_ATTRIBUTES option is not present too),
        // then the connector should return only those attributes that the resource returns by default.
        if (options.getAttributesToGet() == null && options.getReturnDefaultAttributes() == null) {
            attributesToGet.putAll(toReturnedByDefaultAttributesSet(schema));
        }

        return attributesToGet;
    }

    private static Map<String, String> toReturnedByDefaultAttributesSet(SchemaDefinition schema) {
        return schema.getReturnedByDefaultAttributesSet();
    }

    public static int resolvePageSize(OperationOptions options, int defaultPageSize) {
        if (options.getPageSize() != null) {
            return options.getPageSize();
        }
        return defaultPageSize;
    }

    public static int resolvePageOffset(OperationOptions options) {
        if (options.getPagedResultsOffset() != null) {
            return options.getPagedResultsOffset();
        }
        return 0;
    }

    public static String handleEmptyAsNull(String s) {
        if (s == null) {
            return null;
        }
        if (s.isEmpty()) {
            return null;
        }
        return s;
    }

    public static String handleNullAsEmpty(String s) {
        if (s == null) {
            return "";
        }
        return s;
    }
}