package com.rundeck.plugin.resources.puppetdb.client;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.rundeck.plugin.resources.puppetdb.client.model.*;

import java.util.List;

/**
 * Facade for puppet api
 */
public class PuppetDB {

    private final PuppetAPI puppetAPI;

    public PuppetDB(final HTTP http) {
        this.puppetAPI = new DefaultPuppetAPI(http);
    }

    public PuppetDB(final PuppetAPI puppetAPI) {
        this.puppetAPI = puppetAPI;
    }

    public ImmutableList<PuppetDBNode> getPuppetDBNodes(
            final List<Node> nodes,
            final List<NodeFact> factSet,
            final List<CertNodeClass> nodeClasses
    )
    {
        return FluentIterable.from(nodes)
                             .transform(queryNodeWithFacts(factSet, nodeClasses))
                             .toList();
    }

    /**
     * Create a puppet DB Node by filtering the facts
     *
     * @param node
     * @param facts
     * @param nodeClasses1
     *
     * @return
     */
    public PuppetDBNode getNodeWithFacts(
            final Node node,
            final List<NodeFact> facts,
            final List<CertNodeClass> nodeClasses1
    )
    {
        final List<NodeFact> newfacts = FluentIterable.from(facts).filter(certNameEqualsPredicate(node)).toList();
        final List<CertNodeClass> classes = FluentIterable.from(nodeClasses1)
                                                          .filter(certNameEqualsPredicate(node))
                                                          .toList();


        return new PuppetDBNode(node, newfacts, classes);
    }

    private Predicate<Certname> certNameEqualsPredicate(final Certname node) {
        return new Predicate<Certname>() {
            @Override
            public boolean apply(final Certname input) {
                if (input == null ||
                    input.getCertname() == null ||
                    node == null ||
                    node.getCertname() == null) {
                    return false;
                }
                return node.getCertname().equals(input.getCertname());
            }
        };
    }


    public PuppetDBNode getNodeWithFacts(final Node node) {
        final List<Fact> facts = puppetAPI.getFactsForNode(node);
        final List<NodeClass> nodeClasses = puppetAPI.getClassesForNode(node);
        return new PuppetDBNode(node, facts, nodeClasses);
    }

    public Function<Node, PuppetDBNode> queryNodeWithFacts(
            final List<NodeFact> facts,
            final List<CertNodeClass> nodeClasses
    )
    {
        return new Function<Node, PuppetDBNode>() {
            @Override
            public PuppetDBNode apply(final Node node) {
                return getNodeWithFacts(node, facts, nodeClasses);
            }
        };
    }

    public Function<Node, PuppetDBNode> queryNode() {
        return new Function<Node, PuppetDBNode>() {
            @Override
            public PuppetDBNode apply(final Node node) {
                return getNodeWithFacts(node);
            }
        };
    }

    public PuppetAPI getPuppetAPI() {
        return puppetAPI;
    }
}
