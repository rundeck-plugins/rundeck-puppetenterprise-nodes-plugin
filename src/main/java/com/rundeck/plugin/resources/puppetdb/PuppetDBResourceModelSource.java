package com.rundeck.plugin.resources.puppetdb;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.resources.ResourceModelSource;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException;
import com.rundeck.plugin.resources.puppetdb.client.PuppetDB;
import com.rundeck.plugin.resources.puppetdb.client.model.CertNodeResource;
import com.rundeck.plugin.resources.puppetdb.client.model.Node;
import com.rundeck.plugin.resources.puppetdb.client.model.NodeFact;
import com.rundeck.plugin.resources.puppetdb.client.model.PuppetDBNode;

/**
 * Created by greg on 3/7/16.
 */
class PuppetDBResourceModelSource implements ResourceModelSource {
    private final Mapper mapper;
    private final Map<String, Object> mapping;
    private final String resourceTag;
    private final String userQuery;
    PuppetDB pdb;

    public PuppetDBResourceModelSource(
            final PuppetDB pdb,
            final Mapper mapper,
            final Map<String, Object> mapping,
            final String resourceTag,
            final String userQuery
    )
    {
        this.pdb = pdb;
        this.mapper = mapper;
        this.mapping = mapping;
        this.resourceTag = resourceTag;
        this.userQuery = userQuery;
    }

    @Override
    public INodeSet getNodes() throws ResourceModelSourceException {
        // get list of nodes without filtering

        final List<Node> nodes = pdb.getPuppetAPI().getNodes(userQuery);
        if (null == nodes || nodes.isEmpty()) {
            return new NodeSetImpl();
        }
        Set<String> factNames = mapper.determineFactNames(mapping);

        List<NodeFact> factSet = pdb.getPuppetAPI().getFactSet(factNames, userQuery);
        final List<CertNodeResource> nodeResources = resourceTag != null 
                                                ? pdb.getPuppetAPI().getResourcesForAllNodes(userQuery, resourceTag)
                                                : Collections.<CertNodeResource>emptyList();
        // build nodes with facts and tags attached
        final List<PuppetDBNode> puppetNodes = pdb.getPuppetDBNodes(nodes, factSet, nodeResources);
        final List<INodeEntry> rundeckNodes = mapper.convertNodes(puppetNodes, mapping);

        final NodeSetImpl nodeSet = new NodeSetImpl();
        nodeSet.putNodes(rundeckNodes);
        return nodeSet;
    }


}
