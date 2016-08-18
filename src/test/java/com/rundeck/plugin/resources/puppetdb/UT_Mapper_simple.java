package com.rundeck.plugin.resources.puppetdb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.*;
import java.util.Map.Entry;

import com.rundeck.plugin.resources.puppetdb.client.PuppetDB;
import com.rundeck.plugin.resources.puppetdb.client.model.*;
import org.junit.Before;
import org.junit.Test;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rundeck.plugin.resources.puppetdb.client.PuppetAPI;

public class UT_Mapper_simple {

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
    public void test_known_mapping() {
        this.mapping = getMapping("simple/known_mapping.json");
        PuppetDB db = new PuppetDB(testApi);
        final ImmutableList<PuppetDBNode> nodesWithFacts = FluentIterable
                .from(testApi.getNodes(null))
                .transform(db.queryNode("Class"))
                .toList();

        final PuppetDBNode puppetDBNode = nodesWithFacts.get(0);

        final Optional<INodeEntry> maybeNode = mapper.apply(puppetDBNode, mapping);
        final INodeEntry nodeEntry = maybeNode.orNull();

        assertTrue("maybeNode should be present", maybeNode.isPresent());
        assertEquals("nodeEntry.hostname should be 100.112.162.79", "100.112.162.79", nodeEntry.getHostname());
        assertEquals("nodeEntry.username should be username", "username", nodeEntry.getUsername());
    }

    @Test
    public void test_known_mapping_with_missing_property() {
        this.mapping = getMapping("simple/known_mapping_with_missing_property.json");
        PuppetDB db = new PuppetDB(testApi);
        final ImmutableList<PuppetDBNode> nodesWithFacts = FluentIterable.from(testApi.getNodes(null))
                .transform(db.queryNode("Class"))
                .toList();

        final PuppetDBNode puppetDBNode = nodesWithFacts.get(0);

        final Optional<INodeEntry> maybeNode = mapper.apply(puppetDBNode, mapping);
        final INodeEntry nodeEntry = maybeNode.orNull();

        assertTrue("maybeNode should be present", maybeNode.isPresent());
        assertEquals("nodeEntry.hostname should be 100.112.162.79", "100.112.162.79", nodeEntry.getHostname());
        assertEquals("nodeEntry.username should be username", "username", nodeEntry.getUsername());
        for (Entry<String, String> e : nodeEntry.getAttributes().entrySet()) {
            assertTrue(e != null);
            assertTrue(e.getKey() != null);
            assertTrue(e.getValue() != null);
        }
        for (Object o : nodeEntry.getTags()) {
            assertTrue(o != null);
        }
        
        assertTrue(nodeEntry.getDescription() != null);
        //assertTrue(nodeEntry.getFrameworkProject() != null);
        assertTrue(nodeEntry.getNodename() != null);
        assertTrue(nodeEntry.getOsArch() != null);
        //assertTrue(nodeEntry.getOsFamily() != null);
        assertTrue(nodeEntry.getOsName() != null);
        assertTrue(nodeEntry.getOsVersion() != null);
        assertTrue(nodeEntry.getUsername() != null);
        assertTrue(nodeEntry.extractHostname() != null);
        //assertTrue(nodeEntry.extractPort() != null);
        assertTrue(nodeEntry.extractUserName() != null);
    }

    public Map<String, Object> getMapping(final String fileName) {
        final Type mappingType = new TypeToken<Map<String, Object>>() {
        }.getType();
        return gson.fromJson(readFile(fileName), mappingType);
    }

    public PuppetAPI testApi() {
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
                return gson.fromJson(readFile("simple/nodes.json"), Node.LIST);
            }

            @Override
            public List<Fact> getFactsForNode(final Node node) {
                return gson.fromJson(readFile("simple/facts.json"), Fact.LIST);
            }

            @Override
            public List<NodeResource> getResourcesForNode(final Node node, final String resourceTag) {
                return gson.fromJson(readFile("simple/classes.json"), NodeResource.LIST);
            }
        };
    }

    public String readFile(final String name) {
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
