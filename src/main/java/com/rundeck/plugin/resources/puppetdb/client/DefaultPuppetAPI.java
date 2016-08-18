package com.rundeck.plugin.resources.puppetdb.client;

import static java.lang.String.format;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.rundeck.plugin.resources.puppetdb.Constants;
import com.rundeck.plugin.resources.puppetdb.client.model.*;
import org.apache.log4j.Logger;

public class DefaultPuppetAPI implements PuppetAPI, Constants {

    private static final String UTF8 = StandardCharsets.UTF_8.toString();
    
    private static final Logger LOG = Logger.getLogger(DefaultPuppetAPI.class);


    private final HTTP http;
    private static final Gson GSON = new Gson();

    public DefaultPuppetAPI(final HTTP http) {
        this.http = http;
    }


    @Override
    public List<Node> getNodes(final String userQuery) {
        final String path = "pdb/query/v4/nodes" + getUserQuery(userQuery);
        return http.makeRequest(path, Node.listParser(GSON), Collections.<Node>emptyList(), "getNodes()");
    }

    Function<String, List<NodeFact>> singleFact(final String userQuery) {
        return new Function<String, List<NodeFact>>() {
            @Override
            public List<NodeFact> apply(final String input) {
                return getFactForAllNodes(input, userQuery);
            }
        };
    }

    public List<NodeFact> getFactSet(Set<String> facts, final String userQuery) {
        ImmutableList<List<NodeFact>> lists = FluentIterable.from(facts).transform(singleFact(userQuery)).toList();
        List<NodeFact> nodeFacts = new ArrayList<>();
        for (List<NodeFact> list : lists) {
            nodeFacts.addAll(list);
        }
        return nodeFacts;
    }

    public List<NodeFact> getFactForAllNodes(String fact, final String userQuery) {
        final String path = format("pdb/query/v4/facts/%s", fact) + getUserQuery(userQuery);
        return http.makeRequest(path, NodeFact.parser(GSON), Collections.<NodeFact>emptyList(), "getFactForAllNodes()");
    }

    private String getUserQuery(String puppetNodeQuery) {
        return (puppetNodeQuery != null && !puppetNodeQuery.trim().isEmpty())
               ? ("?query=") + urlencode(puppetNodeQuery)
               : "";
    }

    @Override
    public List<NodeResource> getResourcesForNode(final Node node, final String resourceTag) {
        final String path = format("pdb/query/v4/nodes/%s/resources/%s", node.getCertname(), resourceTag);

        return http.makeRequest(
                path,
                NodeResource.parser(GSON),
                Collections.<NodeResource>emptyList(),
                "getClassesForNode()"
        );
    }

    @Override
    public List<CertNodeResource> getResourcesForAllNodes(final String userQuery, final String resourceTag) {
        final String path = format("pdb/query/v4/resources/%s%s", resourceTag, getUserQuery(userQuery));
        return http.makeRequest(
                path,
                CertNodeResource.listParser(GSON), Collections.<CertNodeResource>emptyList(), "getClassesForAllNodes()"
        );
    }

    private String urlencode(final String query) {
        if (null == query) {
            return null;
        }
        try {
            return URLEncoder.encode(query, UTF8);
        } catch (UnsupportedEncodingException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    @Override
    public List<Fact> getFactsForNode(final Node node) {
        final String url = format("pdb/query/v4/nodes/%s/facts", node.getCertname());
        return http.makeRequest(url, Fact.listParser(GSON), Collections.<Fact>emptyList(), "getFactsForNode()");
    }

}
