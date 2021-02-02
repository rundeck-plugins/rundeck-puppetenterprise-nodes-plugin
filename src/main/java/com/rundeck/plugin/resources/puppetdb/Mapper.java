/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * NodeGenerator.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: Oct 18, 2010 7:03:37 PM
 * 
 */
package com.rundeck.plugin.resources.puppetdb;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtilsBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.rundeck.plugin.resources.puppetdb.client.model.PuppetDBNode;

/**
 *
 */
public class Mapper implements Constants {

    public static final Predicate<Object> IS_MAP_PREDICATE = new Predicate<Object>() {
        @Override
        public boolean apply(final Object input) {
            return input instanceof Map;
        }
    };
    private static final Logger log = LoggerFactory.getLogger(ResourceModelFactory.class);

    private final PropertyUtilsBean propertyUtilsBean;
    private final Optional<String> defaultNodeTag;
    public Mapper(final Optional<String> defaultNodeTag) {
        this.defaultNodeTag=defaultNodeTag;
        this.propertyUtilsBean = new PropertyUtilsBean();
    }

    public Optional<INodeEntry> apply(final PuppetDBNode puppetNode,
            final Map<String, Object> mappings) {

        // create a new instance
        final NodeEntryImpl result = newNodeTreeImpl();

        // parse every property BUT tags and attributes
        try {
            final Set<String> especialProperties = setOf("tags", "attributes");
            final Map<String, String> readProperties = assembleMapOmitingKeys(puppetNode, mappings, especialProperties);
            propertyUtilsBean.copyProperties(result, readProperties);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            log.warn("while trying to assemble Rundeck node from PuppetDB Node", e);
        }

        // parse attributes
        final boolean hasAttributes = mappings.containsKey("attributes");
        if (hasAttributes) {
            final Object attributesMapping = mappings.get("attributes");

            final boolean isValidMapping = null != attributesMapping && attributesMapping instanceof Map;
            if (isValidMapping) {
                final Map<String, String> newAttributes = assembleMap(puppetNode, (Map<String, Object>) attributesMapping);
                result.getAttributes().putAll(newAttributes);
            }
        }

        // parse conditional attributes
        final boolean hasConditionalAttributes = mappings.containsKey("conditionalAttributes");
        if (hasConditionalAttributes) {
            final Object conditionalAttributesMapping = mappings.get("conditionalAttributes");

            final boolean isValidMapping = null != conditionalAttributesMapping && conditionalAttributesMapping instanceof Map;
            if (isValidMapping) {
                final Map<String, String> newAttributes = assembleMapWithConditions(puppetNode, (Map<String, Object>) conditionalAttributesMapping);
                result.getAttributes().putAll(newAttributes);
            }
        }


        // parse tags
        final boolean hasTags = mappings.containsKey("tags");
        if (hasTags) {
            // TODO: for now, tags is every tag we found.
            result.getTags().addAll(puppetNode.getClasses());

            // add default tag if any
            if (defaultNodeTag.isPresent()) {
                result.getTags().add(defaultNodeTag.get());
            }

            /*
             final Object tagsMapping = mappings.getOrDefault("tags", emptyMap());
             final boolean isValidMapping = tagsMapping instanceof List;
             if (isValidMapping) {
             final Set<String> newTags = assembleSet(puppetNode, (List<Map<String, String>>) tagsMapping);
             result.getTags().addAll(newTags);
             }
             */
        }


        // check if valid
        return isNodeInValidState(result) ? Optional.<INodeEntry> of(result) : Optional.<INodeEntry> absent();
    }


    public <T> Set<T> setOf(T... ts) {
        return new LinkedHashSet<>(asList(ts));
    }

    private Map<String, String> assembleMap(final PuppetDBNode puppetNode,
            final Map<String, Object> mappings) {
        final Map<String, String> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : mappings.entrySet()) {
            final String key = entry.getKey();

            final boolean isValidMapping = entry.getValue() instanceof Map;
            if (!isValidMapping) {
                log.warn("wrong mapping for property: 'key', please specify a json object with properties 'path' and/or 'default'");
                continue;
            }

            final Map<String, String> mapping = (Map<String, String>) entry.getValue();
            final String value = getPuppetNodeProperty(puppetNode, mapping);
            if (null != value) { 
                result.put(key, value);
            }
        }

        return result;
    }

    private Map<String, String> assembleMapWithConditions(final PuppetDBNode puppetNode,
                                                          final Map<String, Object> mappings) {
        final Map<String, String> result = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : mappings.entrySet()) {
            final String key = entry.getKey();

            final boolean isValidMapping = entry.getValue() instanceof Map;
            if (!isValidMapping) {
                log.warn("wrong mapping for property: 'key', please specify a json object with properties 'path' and/or 'default'");
                continue;
            }


            final Map<String, String> mapping = (Map<String, String>) entry.getValue();
            if (!mapping.containsKey("conditionPath") || !mapping.containsKey("conditionRegex")) {
                log.warn("wrong mapping for property: 'conditionalAttributes', please specify a json object with properties 'conditionPath' and 'conditionRegex'");
                continue;
            }

            final String conditionPath = mapping.get("conditionPath");
            final String conditionRegex = mapping.get("conditionRegex");
            final String conditionValue = getPuppetNodeProperty(puppetNode, conditionPath);

            if (!regExValid(conditionRegex)) {
                log.warn("invalid regex on conditional attribute: " + conditionRegex);
                continue;
            }

            if (conditionValue.matches(conditionRegex)) {
                final String value = getPuppetNodeProperty(puppetNode, mapping);
                if (null != value) {
                    result.put(key, value);
                }
            }
        }

        return result;
    }

    private boolean regExValid(String regEx) {
        try {
            Pattern.compile(regEx);
            return true;
        } catch (PatternSyntaxException exception) {
            return false;
        }

    }

    private Map<String, String> assembleMapOmitingKeys(final PuppetDBNode puppetNode,
            final Map<String, Object> mappings,
            final Set<String> omitKeys) {
        final Map<String, Object> newMappings = new LinkedHashMap<>(mappings);

        for (final String key : omitKeys) {
            newMappings.remove(key);
        }

        return assembleMap(puppetNode, newMappings);
    }

    private String getPuppetNodeProperty(final PuppetDBNode puppetNode,
            final Map<String, String> propertyMapping) {
        if (null == propertyMapping || propertyMapping.isEmpty()) {
            return "";
        }

        final boolean isPath = propertyMapping.containsKey("path");
        if (isPath) {
            final String value = getPuppetNodeProperty(puppetNode, propertyMapping.get("path"));
            if (isNotBlank(value)) {
                return value;
            }
        }

        final boolean isDefault = propertyMapping.containsKey("default");
        if (isDefault) {
            final String value = propertyMapping.get("default");
            if (isNotBlank(value)) {
                return value;
            }
        }

        return "";
    }

    private String getPuppetNodeProperty(final PuppetDBNode puppetNode,
            final String propertyPath) {
        if (isBlank(propertyPath) || null == puppetNode) {
            return "";
        }

        try {
            final Object value = propertyUtilsBean.getProperty(puppetNode, propertyPath);
            return null == value ? "" : value.toString();
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | NestedNullException e) {
            final String template = "can't parse propertyPath: '%s' for node '%s'";
            final String message = format(template, propertyPath,puppetNode.getCertname());
            log.warn(message);
            log.debug(message, e);
        }

        return "";
    }

    private NodeEntryImpl newNodeTreeImpl() {
        final NodeEntryImpl result = new NodeEntryImpl();

        if (null == result.getTags()) {
            result.setTags(new LinkedHashSet<>());
        }

        if (null == result.getAttributes()) {
            result.setAttributes(new LinkedHashMap<String, String>());
        }

        return result;
    }

    protected final boolean isNodeInValidState(final INodeEntry nodeEntry) {
        if (null == nodeEntry) {
            log.error("invalid node state, node is null");
            return false;
        }

        final String nodename = nodeEntry.getNodename();
        if (isBlank(nodename)) {
            log.warn("invalid node state, nodename is blank");
            return false;
        }

        final String hostname = nodeEntry.getHostname();
        if (isBlank(hostname)) {
            log.warn("invalid node state, hostname is blank");
            return false;
        }

        final String username = nodeEntry.getUsername();
        if (isBlank(username)) {
            log.warn("ignoring parsed node, username is blank");
            return false;
        }

        return true;
    }


    List<INodeEntry> convertNodes(final List<PuppetDBNode> puppetNodes, final Map<String, Object> mapping) {

        return FluentIterable.from(puppetNodes)
                             .transform(withFixedMapping(mapping))
                             .filter(new Predicate<Optional<INodeEntry>>() {
                                 @Override
                                 public boolean apply(final Optional<INodeEntry> input) {
                                     return input.isPresent();
                                 }
                             })
                             .transform(new Function<Optional<INodeEntry>, INodeEntry>() {

                                 @Override
                                 public INodeEntry apply(
                                         final Optional<INodeEntry>
                                                 input
                                 )
                                 {
                                     return input.get();
                                 }
                             })
                             .toList();
    }

    public Function<PuppetDBNode, Optional<INodeEntry>> withFixedMapping(final Map<String, Object> mapping) {
        final Mapper mapper = this;
        return new Function<PuppetDBNode, Optional<INodeEntry>>() {
            @Override
            public Optional<INodeEntry> apply(final PuppetDBNode input) {
                return mapper.apply(input, mapping);
            }
        };
    }

    public Set<String> determineFactNames(final Map<String, Object> mapping) {
        Set<String> results = new HashSet<>();
        ImmutableList<Object> nonAttributes = FluentIterable.from(mapping.entrySet())
                                                                         .filter(new Predicate<Map.Entry<String,
                                                                                 Object>>() {
                                                                                     @Override
                                                                                     public boolean apply(
                                                                                             final Map.Entry<String,
                                                                                                     Object> input
                                                                                     )
                                                                                     {
                                                                                         return !input.getKey().equals(
                                                                                                 "attributes") &&
                                                                                                !input.getKey().equals
                                                                                                        ("tags");
                                                                                     }
                                                                                 }
                                                                         ).
                        transform(
                                new Function<Map.Entry<String,Object>, Object>() {
                                    @Override
                                    public Object apply(final Map.Entry<String, Object> input) {
                                        return input.getValue();
                                    }
                                }
                        ).toList();
        results.addAll(mappingFactNames(nonAttributes));

        Object attributes = mapping.get("attributes");
        if (attributes != null && attributes instanceof Map) {
            Map<String, Object> attrs = (Map<String, Object>) attributes;
            results.addAll(mappingFactNames(FluentIterable.from(attrs.values()).toList()));
        }
        //TODO: mapping of facts to tags is not yet used
        /*
        Object tags = mapping.get("tags");
        if (tags != null && tags instanceof Map) {
            Map<String, Object> attrs = (Map<String, Object>) tags;
            results.addAll(mappingFactNames(FluentIterable.from(attrs.values()).toList()));
        }
        */

        return results;
    }
    public Set<String> mappingFactNames(List<Object> input){
        return FluentIterable.from(input)
                      .filter(IS_MAP_PREDICATE)
                             .transform(castToMap())
                             .filter(containsKeyPredicate("path"))
                             .transform(mapValue("path"))
                             .filter(startsWithPredicate("facts."))
                             .transform(substring("facts.".length()))
                             .transform(firstPart("."))
                             .toSet();
    }

    private Function<String, String> firstPart(final String separator) {
        return new Function<String, String>() {
            @Override
            public String apply(final String input) {
                if(input.contains(separator)){
                    return input.substring(0,input.indexOf(separator));
                }else{
                    return input;
                }
            }
        };
    }

    private Function<Object, Map<String, String>> castToMap() {
        return new Function<Object, Map<String, String>>() {
            @Override
            public Map<String, String> apply(final Object input) {
                return (Map<String, String>) input;
            }
        };
    }

    private Function<String, String> substring(final int length) {
        return new Function<String, String>() {
            @Override
            public String apply(final String input) {
                return input.substring(length);
            }
        };
    }

    private Predicate<String> startsWithPredicate(final String string) {
        return new Predicate<String>() {
            @Override
            public boolean apply(final String input) {
                return input.startsWith(string);
            }
        };
    }

    private Function<Map<String, String>, String> mapValue(final String key) {
        return new Function<Map<String, String>, String>() {
            @Override
            public String apply(final Map<String, String> input) {
                return input.get(key);
            }
        };
    }

    private Predicate<Map<String, String>> containsKeyPredicate(final String path) {
        return new Predicate<Map<String, String>>() {
            @Override
            public boolean apply(final Map<String, String> input) {
                return input.containsKey(path);
            }
        };
    }
}
