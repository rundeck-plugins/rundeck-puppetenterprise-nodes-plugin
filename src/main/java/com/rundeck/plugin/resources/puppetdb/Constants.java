package com.rundeck.plugin.resources.puppetdb;

import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyUtil;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;

public interface Constants {

    String PROVIDER_NAME = "puppet-enterprise";

    String PROPERTY_PUPPETDB_HOST = "PROPERTY_PUPPETDB_HOST";
    String PROPERTY_PUPPETDB_CERTIFICATE_NAME = "PROPERTY_PUPPETDB_CERTIFICATE_NAME";
    String PROPERTY_PUPPETDB_PORT = "PROPERTY_PUPPETDB_PORT";
    String PROPERTY_PUPPETDB_SSL_DIR = "PROPERTY_PUPPETDB_SSL_DIR";
    String PROPERTY_MAPPING_FILE = "PROPERTY_MAPPING_FILE";
    String PROPERTY_DEFAULT_NODE_TAG = "PROPERTY_DEFAULT_NODE_TAG";
    String PROPERTY_NODE_QUERY = "PROPERTY_NODE_QUERY";
    String PROPERTY_METRICS_INTERVAL = "PROPERTY_METRICS_INTERVAL";
    String PROPERTY_TAGS_SOURCE = "PROPERTY_TAGS_SOURCE";

    boolean PROPERTY_IS_REQUIRED = true;
    boolean PROPERTY_IS_OPTIONAL = false;

    Description PLUGIN_DESCRIPTION = DescriptionBuilder.builder()
            .name(PROVIDER_NAME)
            .title("PuppetDB Nodes Plugin")
            .description("Produces Nodes from PuppetDB")
            .property(PropertyUtil.string(PROPERTY_PUPPETDB_HOST, "PuppetDB Host", "Puppet DB hostname (ie localhost)", PROPERTY_IS_REQUIRED, null))
            .property(PropertyUtil.integer(PROPERTY_PUPPETDB_PORT, "PuppetDB Port", "Puppet DB port (defaults to 8081)", PROPERTY_IS_REQUIRED, "8081"))
            .property(PropertyUtil.string(PROPERTY_PUPPETDB_CERTIFICATE_NAME, "Client certificate name", "The name of the client certificate", PROPERTY_IS_OPTIONAL, null))
            .property(PropertyUtil.string(PROPERTY_PUPPETDB_SSL_DIR, "PuppetDB SSL Directory", "local directory for SSL, if null it'll use http, "
                    + "it should contain <ssl directory>/private_keys/<client certificate name>.pem "
                    + ", <ssl directory>/certs/<client certificate name>.pem "
                    + "and <ssl directory>/ca/ca_crt.pem", false, null))
            .property(PropertyUtil.string(PROPERTY_NODE_QUERY, "Optional Node Query", "i.e. this will just get one node of named example.local [\"=\", \"certname\", \"example.local\"]", false, null))
            .property(PropertyUtil.string(PROPERTY_MAPPING_FILE, "Mapping JSON", "JSON File describing mapping between PuppetDB Data and Rundeck Data", PROPERTY_IS_OPTIONAL, ""))
            .property(PropertyUtil.string(PROPERTY_DEFAULT_NODE_TAG, "Default tag for imported nodes", "Automatically adds this tag to nodes", PROPERTY_IS_OPTIONAL, ""))
            .property(PropertyUtil.integer(PROPERTY_METRICS_INTERVAL, "Metrics logging interval", "Log the codahale metrics to the service.log file at the specified minute interval (no logging if unset).", false, "15"))
            .property(PropertyUtil.string(PROPERTY_TAGS_SOURCE, "Tags ressource",
                                        "In what puppet resource to look for tags.",
                                        false,
                                        null))
            .build();

}
