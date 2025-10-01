# Deprecated

This repository is no longer maintained and is only avaiable for historical example purposes.  It will be deleted at some point in the future.



Rundeck Puppet Enterprise Nodes Plugin
========================

Version: 0.9.x

[![Build Status](https://travis-ci.org/rundeck-plugins/rundeck-puppetenterprise-nodes-plugin.svg?branch=master)](https://travis-ci.org/rundeck-plugins/rundeck-puppetenterprise-nodes-plugin)

This is a Resource Model Source plugin for [Rundeck][] 1.5+ that provides
Puppet Enterprise Nodes as nodes for the RunDeck server.

[Rundeck]: http://rundeck.org

This is based on the original <https://github.com/latamdevs/rundeck-puppetenterprise-nodes-plugin>.

See [Release Notes](https://github.com/rundeck-plugins/rundeck-puppetenterprise-nodes-plugin/blob/master/Release-Notes.md) for version changes.

Previous Check
------------

You'll need to connect to a puppet HOST and PORT (default 8080), please test your configuration with this curl:
curl http://HOST:PORT/pdb/query/v4/nodes

or in the case of SSL (default port 8081):
curl 'https://HOST:PORT/pdb/query/v4/nodes' \
  --tlsv1 \
  --cacert /etc/puppet/ssl/certs/ca.pem \
  --cert /etc/puppet/ssl/certs/<HOST>.pem \
  --key /etc/puppet/ssl/private_keys/<HOST>.pem \

Mock API
------------

Run a mock api server at <http://localhost:5050>:

  ./gradlew -p mock-puppetdb-api-server

This uses [ratpack](http://ratpack.io) to respond to the `/nodes`, `/resources/Class` and `/facts/*` queries
with mocked json data.


Installation
------------

Download from the [releases page](https://github.com/rundeck-plugins/rundeck-puppetenterprise-nodes-plugin/releases).

Put the `rundeck-puppetenterprise-nodes-plugin-x.y.jar` into your `$RDECK_BASE/libext` dir.

Usage
-----

You can configure the Resource Model Sources for a project either via the
RunDeck GUI, under the "Admin" page, or you can modify the `project.properties`
file to configure the sources.

See: [Resource Model Source Configuration](http://rundeck.org/docs/manual/plugins.html#resource-model-source-configuration)

The provider name is: `puppet-enterprise`

Here are the configuration properties for Simple Configuration:

* `host`: Puppet Enterprise Master host
* `port`: Puppet Enterprise Master port
* `sslDir`: name of the ssl dir containing puppet certificates, if null plugin will use HTTP instead of HTTPS, this dir should contain:
 * <ssldir>/private_keys/<host>.pem
 * <ssldir>/certs/<host>.pem
 * <ssldir>/ca/ca_crt.pem
* `mappingFile`: Path to a java properties-formatted mapping definition file.
* `nodeQuery`: Puppet Query to filter nodes, if null then the plugin will return all, see more information on [Puppet API Query reference(https://docs.puppetlabs.com/puppetdb/latest/api/query/v4/nodes.html)
* `default tag`: default tag to add to nodes
* `include classes`: if selected, queries the classes to add them as tags, otherwise does not do it

Configuration keys vary if you use them directly in the project file or in Rundeck using Edit Configuration File, for example node 1:

resources.source.1.config.PROPERTY_PUPPETDB_HOST=host
resources.source.1.config.PROPERTY_PUPPETDB_PORT=port


Mapping Definition
----------

RunDeck Node attributes are configured by mapping Puppet Enterprise Node facts via a
mapping configuration.

The mapping declares the node attributes that will be set, and what their values
will be set to using a "selector" on properties of the Puppet Enterprise Node object.

the default mapping file is src/main/resources/defaultMapping.json

Conditional Mapping
-----------
Conditional mapping allows for node attributes to be set when the value of a property in the Puppet Enterprise Node object matches a specified regular expression.

This is useful for specifying a different executor than the default one for specific nodes (i.e. if Rundeck is connecting to both Windows and Linux nodes in the same project).

An example can be found in the file:

`src/test/resources/conditional_mapping/one_matching_mapping.json`

Mapping format
---------------

The mapping consists of defining either a selector or a default for
the desired Node fields.  The "nodename" field is required, and will 
automatically be set to the instance ID if no other value is defined.

For purposes of the mapping definition, a `field selector` is either:

* An Puppet Enterprise fieldname, or dot-separated field names

Selectors use the Apache [BeanUtils](http://commons.apache.org/beanutils/) to extract a property value from the 
Puppet json resources.

Mapping Puppet Enterprise Nodes to Rundeck Nodes
=================

Rundeck Nodes can have arbitrary attribute values, this Plugin turns Puppet Facts (key/value) into Rundeck Attributes

Puppet Enterprise Nodes can also have "Classes" which are values attached to the Node, this Plugin associate them to Rundeck tags as they are simple string labels.
