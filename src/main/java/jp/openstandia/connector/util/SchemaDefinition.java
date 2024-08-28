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

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jp.openstandia.connector.util.Utils.createIncompleteAttribute;
import static jp.openstandia.connector.util.Utils.shouldReturn;

/**
 * Provides generic schema builder.
 *
 * @author Hiroyuki Wada
 */
public class SchemaDefinition {

    public static <C, R> Builder<C, C, R> newBuilder(ObjectClass objectClass, Class<C> createOrUpdateClass, Class<R> readClass) {
        return newBuilder(objectClass, createOrUpdateClass, createOrUpdateClass, readClass);
    }

    public static <C, U, R> Builder<C, U, R> newBuilder(ObjectClass objectClass, Class<C> createClass, Class<U> updateClass, Class<R> readClass) {
        Builder<C, U, R> schemaBuilder = new Builder<C, U, R>(objectClass, createClass, updateClass, readClass);
        return schemaBuilder;
    }

    public static class Builder<C, U, R> {
        private final ObjectClass objectClass;
        private final List<AttributeMapper> attributes = new ArrayList<>();

        public <C, U, R> Builder(ObjectClass objectClass, Class<C> createClass, Class<U> updateClass, Class<R> readClass) {
            this.objectClass = objectClass;
        }

        public <T> void addUid(String name,
                               Types<T> typeClass,

                               BiConsumer<T, C> create,
                               BiConsumer<T, U> update,
                               Function<R, T> read,

                               String fetchField,

                               AttributeInfo.Flags... options
        ) {
            AttributeMapper attr = new AttributeMapper(Uid.NAME, name, typeClass, create, update, read, fetchField, options);
            this.attributes.add(attr);
        }

        public <T> void addUid(String name,
                               Types<T> typeClass,

                               BiConsumer<T, C> createOrUpdate,
                               Function<R, T> read,

                               String fetchField,

                               AttributeInfo.Flags... options
        ) {
            AttributeMapper attr = new AttributeMapper(Uid.NAME, name, typeClass, createOrUpdate, createOrUpdate, read, fetchField, options);
            this.attributes.add(attr);
        }

        public <T> void addName(String name,
                                Types<T> typeClass,

                                BiConsumer<T, C> create,
                                BiConsumer<T, U> update,
                                Function<R, T> read,

                                String fetchField,

                                AttributeInfo.Flags... options
        ) {
            AttributeMapper attr = new AttributeMapper(Name.NAME, name, typeClass, create, update, read, fetchField, options);
            this.attributes.add(attr);
        }

        public <T> void addName(String name,
                                Types<T> typeClass,

                                BiConsumer<T, C> createOrUpdate,
                                Function<R, T> read,

                                String fetchField,

                                AttributeInfo.Flags... options
        ) {
            AttributeMapper attr = new AttributeMapper(Name.NAME, name, typeClass, createOrUpdate, createOrUpdate, read, fetchField, options);
            this.attributes.add(attr);
        }

        public <T> void addEnable(String name,
                                  Types<T> typeClass,

                                  BiConsumer<T, C> create,
                                  BiConsumer<T, U> update,
                                  Function<R, T> read,

                                  String fetchField,

                                  AttributeInfo.Flags... options
        ) {
            AttributeMapper attr = new AttributeMapper(OperationalAttributes.ENABLE_NAME, name, typeClass, create, update, read, fetchField, options);
            this.attributes.add(attr);
        }

        public <T> void add(String name,
                            Types<T> typeClass,

                            BiConsumer<T, C> create,
                            BiConsumer<T, U> update,
                            Function<R, T> read,

                            String fetchField,

                            AttributeInfo.Flags... options
        ) {
            AttributeMapper attr = new AttributeMapper(name, typeClass, create, update, read, fetchField, options);
            this.attributes.add(attr);
        }

        public <T> void add(String name,
                            Types<T> typeClass,

                            BiConsumer<T, C> createOrUpdate,
                            Function<R, T> read,

                            String fetchField,

                            AttributeInfo.Flags... options
        ) {
            AttributeMapper attr = new AttributeMapper(name, typeClass, createOrUpdate, createOrUpdate, read, fetchField, options);
            this.attributes.add(attr);
        }

        public <T> void addAsMultiple(String name,
                                      Types<T> typeClass,

                                      BiConsumer<List<T>, C> create,
                                      BiConsumer<List<T>, U> updateAdd,
                                      BiConsumer<List<T>, U> updateRemove,
                                      Function<R, Stream<T>> read,

                                      String fetchField,

                                      AttributeInfo.Flags... options
        ) {
            AttributeMapper attr = new AttributeMapper(name, typeClass, create, updateAdd, updateRemove, read, fetchField, options);
            this.attributes.add(attr);
        }

        public SchemaDefinition build() {
            SchemaDefinition schemaDefinition = new SchemaDefinition(objectClass, buildSchemaInfo(), buildAttributeMap());
            return schemaDefinition;
        }

        private ObjectClassInfo buildSchemaInfo() {
            List<AttributeInfo> list = attributes.stream()
                    .map(attr -> {
                        AttributeInfoBuilder define = AttributeInfoBuilder.define(attr.connectorName);

                        define.setType(attr.type.typeClass);
                        define.setMultiValued(attr.isMultiple);
                        define.setNativeName(attr.name);

                        if (attr.type == Types.UUID) {
                            define.setSubtype(AttributeInfo.Subtypes.STRING_UUID);

                        } else if (attr.type == Types.STRING_CASE_IGNORE) {
                            define.setSubtype(AttributeInfo.Subtypes.STRING_CASE_IGNORE);

                        } else if (attr.type == Types.STRING_URI) {
                            define.setSubtype(AttributeInfo.Subtypes.STRING_URI);

                        } else if (attr.type == Types.STRING_LDAP_DN) {
                            define.setSubtype(AttributeInfo.Subtypes.STRING_LDAP_DN);

                        } else if (attr.type == Types.XML) {
                            define.setSubtype(AttributeInfo.Subtypes.STRING_XML);

                        } else if (attr.type == Types.JSON) {
                            define.setSubtype(AttributeInfo.Subtypes.STRING_JSON);
                        }

                        for (AttributeInfo.Flags option : attr.options) {
                            switch (option) {
                                case REQUIRED: {
                                    define.setRequired(true);
                                    break;
                                }
                                case NOT_CREATABLE: {
                                    define.setCreateable(false);
                                    break;
                                }
                                case NOT_UPDATEABLE: {
                                    define.setUpdateable(false);
                                    break;
                                }
                                case NOT_READABLE: {
                                    define.setReadable(false);
                                    break;
                                }
                                case NOT_RETURNED_BY_DEFAULT: {
                                    define.setReturnedByDefault(false);
                                    break;
                                }
                            }
                        }

                        return define.build();
                    })
                    .collect(Collectors.toList());

            ObjectClassInfoBuilder builder = new ObjectClassInfoBuilder();
            builder.setType(objectClass.getObjectClassValue());
            builder.addAllAttributeInfo(list);

            return builder.build();
        }

        private Map<String, AttributeMapper> buildAttributeMap() {
            Map<String, AttributeMapper> map = attributes.stream()
                    // Use connectorName for the key (to lookup by special name like __UID__
                    .collect(Collectors.toMap(a -> a.connectorName, a -> a));
            return map;
        }

        public void addEnable() {

        }
    }

    private final ObjectClass objectClass;
    private final ObjectClassInfo objectClassInfo;
    private final Map<String, AttributeMapper> attributeMap;
    // Key: attribute name (for connector. e.g. __NAME__)
    // Value: field name for resource fetching
    private final Map<String, String> returnedByDefaultAttributesSet;
    private final Map<String, String> notReadableAttributesSet;

    public SchemaDefinition(ObjectClass objectClass, ObjectClassInfo objectClassInfo, Map<String, AttributeMapper> attributeMap) {
        this.objectClass = objectClass;
        this.objectClassInfo = objectClassInfo;
        this.attributeMap = attributeMap;
        this.returnedByDefaultAttributesSet = getObjectClassInfo().getAttributeInfo().stream()
                .filter(i -> i.isReturnedByDefault())
                .map(i -> i.getName())
                .collect(Collectors.toMap(n -> n, n -> attributeMap.get(n).fetchField));
        this.notReadableAttributesSet = getObjectClassInfo().getAttributeInfo().stream()
                .filter(i -> !i.isReadable())
                .map(i -> i.getName())
                .collect(Collectors.toMap(n -> n, n -> attributeMap.get(n).fetchField));
    }

    public ObjectClassInfo getObjectClassInfo() {
        return objectClassInfo;
    }

    public Map<String, String> getReturnedByDefaultAttributesSet() {
        return returnedByDefaultAttributesSet;
    }

    public boolean isReturnedByDefaultAttribute(String attrName) {
        return returnedByDefaultAttributesSet.containsKey(attrName);
    }

    public boolean isReadableAttributes(String attrName) {
        return !notReadableAttributesSet.containsKey(attrName);
    }

    public String getFetchField(String name) {
        AttributeMapper attributeMapper = attributeMap.get(name);
        if (attributeMapper != null) {
            return attributeMapper.fetchField;
        }
        return null;
    }

    public <T> T apply(Set<Attribute> attrs, T dest) {
        for (Attribute attr : attrs) {
            AttributeMapper attributeMapper = attributeMap.get(attr.getName());
            if (attributeMapper == null) {
                throw new InvalidAttributeValueException("Invalid attribute: " + attr.getName());
            }

            attributeMapper.apply(attr, dest);
        }
        return dest;
    }

    public <U> boolean applyDelta(Set<AttributeDelta> deltas, U dest) {
        boolean changed = false;
        for (AttributeDelta delta : deltas) {
            AttributeMapper attributeMapper = attributeMap.get(delta.getName());
            if (attributeMapper == null) {
                throw new InvalidAttributeValueException("Invalid attribute: " + delta.getName());
            }

            attributeMapper.apply(delta, dest);
            changed = true;
        }
        return changed;
    }

    public <R> ConnectorObjectBuilder toConnectorObjectBuilder(R source, Set<String> attributesToGet, boolean allowPartialAttributeValues) {
        final ConnectorObjectBuilder builder = new ConnectorObjectBuilder()
                .setObjectClass(objectClass);

        AttributeMapper uid = attributeMap.get(Uid.NAME);
        addAttribute(builder, uid.apply(source));

        // Need to set __NAME__ because it throws IllegalArgumentException
        AttributeMapper name = attributeMap.get(Name.NAME);
        addAttribute(builder, name.apply(source));

        for (Map.Entry<String, AttributeMapper> entry : attributeMap.entrySet()) {
            // When requested partial attribute values, return incomplete attribute if the attribute is not returned by default and readable
            if (allowPartialAttributeValues) {
                if (!isReturnedByDefaultAttribute(entry.getKey()) && isReadableAttributes(entry.getKey())
                        && attributesToGet.contains(entry.getKey())) {
                    addAttribute(builder, createIncompleteAttribute(entry.getKey()));
                    continue;
                }
            }
            if (shouldReturn(attributesToGet, entry.getKey())) {
                Attribute value = entry.getValue().apply(source);
                addAttribute(builder, value);
            }
        }

        return builder;
    }

    protected void addAttribute(ConnectorObjectBuilder builder, Attribute attribute) {
        if (attribute == null) {
            return;
        }
        // Don't set null because it causes NPE
        builder.addAttribute(attribute);
    }

    public String getType() {
        return objectClassInfo.getType();
    }

    public static class Types<TC> {
        public static final Types<String> STRING = new Types(String.class);
        public static final Types<String> STRING_CASE_IGNORE = new Types(String.class);
        public static final Types<String> STRING_URI = new Types(String.class);
        public static final Types<String> STRING_LDAP_DN = new Types(String.class);
        public static final Types<String> XML = new Types(String.class);
        public static final Types<String> JSON = new Types(String.class);
        public static final Types<String> UUID = new Types(String.class);
        public static final Types<Integer> INTEGER = new Types(Integer.class);
        public static final Types<Integer> LONG = new Types(Long.class);
        public static final Types<Integer> FLOAT = new Types(Float.class);
        public static final Types<Integer> DOUBLE = new Types(Double.class);
        public static final Types<Boolean> BOOLEAN = new Types(Boolean.class);
        public static final Types<BigDecimal> BIG_DECIMAL = new Types(BigDecimal.class);
        public static final Types<String> DATE_STRING = new Types(ZonedDateTime.class);
        public static final Types<String> DATETIME_STRING = new Types(ZonedDateTime.class);
        public static final Types<ZonedDateTime> DATE = new Types(ZonedDateTime.class);
        public static final Types<ZonedDateTime> DATETIME = new Types(ZonedDateTime.class);
        public static final Types<GuardedString> GUARDED_STRING = new Types(GuardedString.class);

        private final Class<TC> typeClass;

        private Types(Class<TC> typeClass) {
            this.typeClass = typeClass;
        }
    }

    static class AttributeMapper<T, C, U, R> {
        private final String connectorName;
        private final String name;
        private final Types<T> type;
        boolean isMultiple;

        private final BiConsumer<T, C> create;
        private final BiConsumer<T, U> replace;
        private final BiConsumer<List<T>, U> add;
        private final BiConsumer<List<T>, U> remove;
        private final Function<R, Object> read;

        private final String fetchField;

        private final AttributeInfo.Flags[] options;

        private DateTimeFormatter dateFormat;
        private DateTimeFormatter dateTimeFormat;

        private static final DateTimeFormatter DEFAULT_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
        private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

        public AttributeMapper(String connectorName, String name, Types<T> typeClass,
                               BiConsumer<T, C> create,
                               BiConsumer<T, U> replace,
                               Function<R, Object> read,
                               String fetchField,
                               AttributeInfo.Flags... options
        ) {
            this(connectorName, name, typeClass, create, replace, null, null, read, fetchField, false, options);
        }

        public AttributeMapper(String name, Types<T> typeClass,
                               BiConsumer<T, C> create,
                               BiConsumer<T, U> replace,
                               Function<R, Object> read,
                               String fetchField,
                               AttributeInfo.Flags... options
        ) {
            this(name, name, typeClass, create, replace, null, null, read, fetchField, false, options);
        }

        public AttributeMapper(String name, Types<T> typeClass,
                               BiConsumer<T, C> create,
                               BiConsumer<List<T>, U> add,
                               BiConsumer<List<T>, U> remove,
                               Function<R, Object> read,
                               String fetchField,
                               AttributeInfo.Flags... options
        ) {
            this(name, name, typeClass, create, null, add, remove, read, fetchField, true, options);
        }

        public AttributeMapper(String connectorName, String name, Types<T> typeClass,
                               BiConsumer<T, C> create,
                               BiConsumer<T, U> replace,
                               BiConsumer<List<T>, U> add,
                               BiConsumer<List<T>, U> remove,
                               Function<R, Object> read,
                               String fetchField,
                               boolean isMultiple,
                               AttributeInfo.Flags... options
        ) {
            this.connectorName = connectorName;
            this.name = name;
            this.type = typeClass;
            this.create = create;
            this.replace = replace;
            this.add = add;
            this.remove = remove;
            this.read = read;
            this.fetchField = fetchField != null ? fetchField : name;
            this.options = options;
            this.isMultiple = isMultiple;
        }

        public boolean isStringType() {
            return type == Types.STRING || type == Types.STRING_URI || type == Types.STRING_LDAP_DN ||
                    type == Types.STRING_LDAP_DN || type == Types.STRING_CASE_IGNORE || type == Types.XML ||
                    type == Types.JSON || type == Types.UUID;
        }

        public AttributeMapper dateFormat(DateTimeFormatter dateFormat) {
            this.dateFormat = dateFormat;
            return this;
        }

        public AttributeMapper datetimeFormat(DateTimeFormatter datetimeFormat) {
            this.dateTimeFormat = datetimeFormat;
            return this;
        }

        private String formatDate(ZonedDateTime zonedDateTime) {
            if (zonedDateTime == null) {
                return null;
            }
            if (this.dateFormat == null) {
                return zonedDateTime.format(DEFAULT_DATE_FORMAT);
            }
            return zonedDateTime.format(this.dateFormat);
        }

        private String formatDateTime(ZonedDateTime zonedDateTime) {
            if (zonedDateTime == null) {
                return null;
            }
            if (this.dateTimeFormat == null) {
                return zonedDateTime.format(DEFAULT_DATE_TIME_FORMAT);
            }
            return zonedDateTime.format(this.dateFormat);
        }

        private ZonedDateTime toDate(String dateString) {
            LocalDate date;
            if (this.dateFormat == null) {
                date = LocalDate.parse(dateString, DEFAULT_DATE_FORMAT);
            } else {
                date = LocalDate.parse(dateString, this.dateFormat);
            }
            return date.atStartOfDay(ZoneId.systemDefault());
        }

        private ZonedDateTime toDateTime(String dateTimeString) {
            ZonedDateTime dateTime;
            if (this.dateTimeFormat == null) {
                dateTime = ZonedDateTime.parse(dateTimeString, DEFAULT_DATE_TIME_FORMAT);
            } else {
                dateTime = ZonedDateTime.parse(dateTimeString, this.dateTimeFormat);
            }
            return dateTime;
        }

        public void apply(Attribute source, C dest) {
            if (create == null) {
                return;
            }

            if (isMultiple) {
                if (type == Types.DATE_STRING) {
                    List<T> values = source.getValue().stream()
                            .map(v -> (ZonedDateTime) v)
                            .map(v -> (T) formatDate(v))
                            .collect(Collectors.toList());
                    create.accept((T) values, dest);

                } else if (type == Types.DATETIME_STRING) {
                    List<T> values = source.getValue().stream()
                            .map(v -> (ZonedDateTime) v)
                            .map(v -> (T) formatDateTime(v))
                            .collect(Collectors.toList());
                    create.accept((T) values, dest);

                } else {
                    List<T> values = source.getValue().stream().map(v -> (T) v).collect(Collectors.toList());
                    create.accept((T) values, dest);
                }

            } else {
                if (isStringType()) {
                    String value = AttributeUtil.getAsStringValue(source);
                    create.accept((T) value, dest);

                } else if (type == Types.INTEGER) {
                    Integer value = AttributeUtil.getIntegerValue(source);
                    create.accept((T) value, dest);

                } else if (type == Types.LONG) {
                    Long value = AttributeUtil.getLongValue(source);
                    create.accept((T) value, dest);

                } else if (type == Types.FLOAT) {
                    Float value = AttributeUtil.getFloatValue(source);
                    create.accept((T) value, dest);

                } else if (type == Types.DOUBLE) {
                    Double value = AttributeUtil.getDoubleValue(source);
                    create.accept((T) value, dest);

                } else if (type == Types.BOOLEAN) {
                    Boolean value = AttributeUtil.getBooleanValue(source);
                    create.accept((T) value, dest);

                } else if (type == Types.BIG_DECIMAL) {
                    BigDecimal value = AttributeUtil.getBigDecimalValue(source);
                    create.accept((T) value, dest);

                } else if (type == Types.DATE || type == Types.DATETIME) {
                    ZonedDateTime date = (ZonedDateTime) AttributeUtil.getSingleValue(source);
                    String formatted = formatDate(date);
                    create.accept((T) formatted, dest);

                } else if (type == Types.DATE_STRING) {
                    ZonedDateTime date = (ZonedDateTime) AttributeUtil.getSingleValue(source);
                    String formatted = formatDate(date);
                    create.accept((T) formatted, dest);

                } else if (type == Types.DATETIME_STRING) {
                    ZonedDateTime date = (ZonedDateTime) AttributeUtil.getSingleValue(source);
                    String formatted = formatDateTime(date);
                    create.accept((T) formatted, dest);

                } else if (type == Types.GUARDED_STRING) {
                    GuardedString guardedString = AttributeUtil.getGuardedStringValue(source);
                    create.accept((T) guardedString, dest);

                } else {
                    T value = (T) AttributeUtil.getSingleValue(source);
                    create.accept(value, dest);
                }
            }
        }

        public void apply(AttributeDelta source, U dest) {
            if (isMultiple) {
                if (add == null || remove == null) {
                    return;
                }

                if (type == Types.DATE_STRING) {
                    List<T> valuesToAdd = safeStream(source.getValuesToAdd())
                            .map(v -> (ZonedDateTime) v)
                            .map(v -> (T) formatDate(v))
                            .collect(Collectors.toList());
                    List<T> valuesToRemove = safeStream(source.getValuesToRemove())
                            .map(v -> (ZonedDateTime) v)
                            .map(v -> (T) formatDate(v))
                            .collect(Collectors.toList());

                    if (!valuesToAdd.isEmpty()) {
                        add.accept(valuesToAdd, dest);
                    }
                    if (!valuesToRemove.isEmpty()) {
                        remove.accept(valuesToRemove, dest);
                    }

                } else if (type == Types.DATETIME_STRING) {
                    List<T> valuesToAdd = safeStream(source.getValuesToAdd())
                            .map(v -> (ZonedDateTime) v)
                            .map(v -> (T) formatDateTime(v))
                            .collect(Collectors.toList());
                    List<T> valuesToRemove = safeStream(source.getValuesToRemove())
                            .map(v -> (ZonedDateTime) v)
                            .map(v -> (T) formatDateTime(v))
                            .collect(Collectors.toList());

                    if (!valuesToAdd.isEmpty()) {
                        add.accept(valuesToAdd, dest);
                    }
                    if (!valuesToRemove.isEmpty()) {
                        remove.accept(valuesToRemove, dest);
                    }

                } else {
                    List<T> valuesToAdd = safeStream(source.getValuesToAdd()).map(v -> (T) v).collect(Collectors.toList());
                    List<T> valuesToRemove = safeStream(source.getValuesToRemove()).map(v -> (T) v).collect(Collectors.toList());

                    if (!valuesToAdd.isEmpty()) {
                        add.accept(valuesToAdd, dest);
                    }
                    if (!valuesToRemove.isEmpty()) {
                        remove.accept(valuesToRemove, dest);
                    }
                }

            } else {
                if (replace == null) {
                    return;
                }

                if (isStringType()) {
                    String value = AttributeDeltaUtil.getAsStringValue(source);
                    replace.accept((T) value, dest);

                } else if (type == Types.INTEGER) {
                    Integer value = AttributeDeltaUtil.getIntegerValue(source);
                    replace.accept((T) value, dest);

                } else if (type == Types.LONG) {
                    Long value = AttributeDeltaUtil.getLongValue(source);
                    replace.accept((T) value, dest);

                } else if (type == Types.FLOAT) {
                    Float value = AttributeDeltaUtil.getFloatValue(source);
                    replace.accept((T) value, dest);

                } else if (type == Types.DOUBLE) {
                    Double value = AttributeDeltaUtil.getDoubleValue(source);
                    replace.accept((T) value, dest);

                } else if (type == Types.BOOLEAN) {
                    Boolean value = AttributeDeltaUtil.getBooleanValue(source);
                    replace.accept((T) value, dest);

                } else if (type == Types.BIG_DECIMAL) {
                    BigDecimal value = AttributeDeltaUtil.getBigDecimalValue(source);
                    replace.accept((T) value, dest);

                } else if (type == Types.DATE || type == Types.DATETIME) {
                    ZonedDateTime date = (ZonedDateTime) AttributeDeltaUtil.getSingleValue(source);
                    replace.accept((T) date, dest);

                } else if (type == Types.DATE_STRING) {
                    ZonedDateTime date = (ZonedDateTime) AttributeDeltaUtil.getSingleValue(source);
                    String formatted = formatDate(date);
                    replace.accept((T) formatted, dest);

                } else if (type == Types.DATETIME_STRING) {
                    ZonedDateTime date = (ZonedDateTime) AttributeDeltaUtil.getSingleValue(source);
                    String formatted = formatDateTime(date);
                    replace.accept((T) formatted, dest);

                } else if (type == Types.GUARDED_STRING) {
                    GuardedString guardedString = AttributeDeltaUtil.getGuardedStringValue(source);
                    replace.accept((T) guardedString, dest);

                } else {
                    T value = (T) AttributeDeltaUtil.getSingleValue(source);
                    replace.accept(value, dest);
                }
            }
        }

        public Attribute apply(R source) {
            if (read == null) {
                return null;
            }

            Object value = read.apply(source);
            if (value == null) {
                // Don't make attribute if no value
                return null;
            }

            if (isMultiple) {
                Stream<?> multipleValues = (Stream<?>) value;

                if (type == Types.DATE_STRING) {
                    List<ZonedDateTime> values = multipleValues
                            .map(v -> (String) v)
                            .map(v -> toDate(v))
                            .collect(Collectors.toList());
                    return safeBuildAttribute(values);

                } else if (type == Types.DATETIME_STRING) {
                    List<ZonedDateTime> values = multipleValues
                            .map(v -> (String) v)
                            .map(v -> toDateTime(v))
                            .collect(Collectors.toList());
                    return safeBuildAttribute(values);

                } else {
                    List<?> values = multipleValues.collect(Collectors.toList());
                    return safeBuildAttribute(values);
                }

            } else {
                if (type == Types.DATE_STRING) {
                    ZonedDateTime date = toDate(value.toString());
                    return AttributeBuilder.build(connectorName, date);

                } else if (type == Types.DATETIME_STRING) {
                    ZonedDateTime dateTime = toDateTime(value.toString());
                    return AttributeBuilder.build(connectorName, dateTime);
                }
                return AttributeBuilder.build(connectorName, value);
            }
        }

        private Stream<Object> safeStream(List<Object> list) {
            if (list == null) {
                return Collections.emptyList().stream();
            }
            return list.stream();
        }

        private Attribute safeBuildAttribute(List<?> values) {
            if (values.isEmpty()) {
                // Don't make attribute if no values
                return null;
            }
            return AttributeBuilder.build(connectorName, values);
        }
    }
}