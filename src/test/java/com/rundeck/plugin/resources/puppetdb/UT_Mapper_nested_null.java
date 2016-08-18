package com.rundeck.plugin.resources.puppetdb;

import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.*;

import com.rundeck.plugin.resources.puppetdb.client.PuppetDB;
import com.rundeck.plugin.resources.puppetdb.client.model.*;
import org.junit.Before;
import org.junit.Test;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rundeck.plugin.resources.puppetdb.client.PuppetAPI;

public class UT_Mapper_nested_null {

    Mapper mapper;

    Gson gson;
    PuppetAPI testApi;
    Map<String, Object> mapping;

    @Before
    public void before() {
        this.mapper = new Mapper(Optional.<String>absent());
        this.gson = new Gson();
        this.testApi = testApi();
    }

    @Test
    public void missing_nested_property_should_be_parsed_as_empty() {
        // this mapping has a invalid mapping for one of the attributes
        PuppetDB db = new PuppetDB(testApi);
        this.mapping = getMapping("nested_null/nested_null_mapping.json"); 
        final List<Node> nodes = testApi.getNodes(null);

        final List<PuppetDBNode> nodesWithFacts = FluentIterable.from(nodes)
            .transform(db.queryNode("Class"))
            .toList();

        for (int i = 0; i < nodesWithFacts.size(); i++) {
            final PuppetDBNode puppetDBNode = nodesWithFacts.get(i);

            final Optional<INodeEntry> maybeNode = mapper.apply(puppetDBNode, mapping);
            assertTrue(format("maybeNode[%d] should be present", i), maybeNode.isPresent());

            final INodeEntry node = maybeNode.get();
            // "nestedNull" was set to trigger the exception. to check, just remove it from catch clause at Mapper.java 
            final String nestedNull = node.getAttributes().get("nestedNull");
            assertEquals("nestedNull property should be empty", "", nestedNull);
        }
    }

    private Map<String, Object> getMapping(final String fileName) {
        final Type mappingType = new TypeToken<Map<String, Object>>() {
        }.getType();
        final String mapping = readFile(fileName);
        return gson.fromJson(readFile(fileName), mappingType);
    }

    public PuppetAPI testApi() {
        final String prefix = "nested_null";
        final List<Node> nodes = gson.fromJson(readFile(prefix + "/nodes.json"), Node.LIST);
        final List<Fact> facts = gson.fromJson(readFile(prefix + "/facts.json"), Fact.LIST);
        final List<NodeResource> classes = gson.fromJson(readFile(prefix + "/classes.json"), NodeResource.LIST);

        return new PuppetAPI() {
            @Override
            public List<CertNodeResource> getResourcesForAllNodes(final String userQuery, final String resourceTag) {
                return null;
            }

            @Override
            public List<NodeFact> getFactSet(final Set<String> facts, final String userQuery) {
                return null;
            }

            @Override
            public List<Node> getNodes(final String userQuery) {
                return nodes;
            }

            @Override
            public List<Fact> getFactsForNode(final Node node) {
                return facts;
            }

            @Override
            public List<NodeResource> getResourcesForNode(final Node node, final String resourceTag) {
                return classes;
            }
        };
    }

    private String readFile(final String name) {
        try (final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(name);
                final Scanner scanner = new Scanner(inputStream)) {
            scanner.useDelimiter("\\Z");
            return scanner.next();
        } catch (Exception ex) {
            System.err.println("can't read file: " + name);
            ex.printStackTrace(System.err);
        }

        return "";
    }

}
