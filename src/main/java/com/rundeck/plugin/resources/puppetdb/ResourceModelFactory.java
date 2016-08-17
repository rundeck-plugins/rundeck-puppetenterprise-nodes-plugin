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
* PuppetEnterpriseResourceModelSource.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 9/1/11 4:27 PM
* 
*/
package com.rundeck.plugin.resources.puppetdb;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.core.plugins.configuration.Property;
import com.dtolabs.rundeck.core.resources.ResourceModelSource;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceFactory;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rundeck.plugin.resources.puppetdb.client.DefaultHTTP;
import com.rundeck.plugin.resources.puppetdb.client.PuppetDB;

@Plugin(name = "puppet-enterprise", service = "ResourceModelSource")
public class ResourceModelFactory implements ResourceModelSourceFactory, Describable, Constants {

    private static MetricRegistry METRICS = new MetricRegistry();
    private static Logger log = Logger.getLogger(ResourceModelFactory.class);

    private final Gson gson;
    private final MetricRegistry metrics;

    public ResourceModelFactory(final Framework framework) {
        this.gson = new Gson();
        this.metrics = METRICS;
    }

    private void attachConsoleReporter(final MetricRegistry metrics, final Properties properties) {
        if (properties.containsKey(PROPERTY_METRICS_INTERVAL)) {
            final ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
                                                            .convertRatesTo(TimeUnit.SECONDS)
                                                            .convertDurationsTo(TimeUnit.MILLISECONDS)
                                                            .build();
            reporter.start(Integer.parseInt(properties.getProperty(PROPERTY_METRICS_INTERVAL)), TimeUnit.MINUTES);
        }
    }

    @Override
    public ResourceModelSource createResourceModelSource(final Properties properties) throws ConfigurationException {
        attachConsoleReporter(metrics, properties);
        validate(properties);

        final Mapper mapper = new Mapper(PropertyHandling.readDefaultNodeTag(properties));
        final Map<String, Object> mapping = getMapping(properties);
        DefaultHTTP defaultHTTP = createHTTP(properties, metrics);
        PuppetDB pdb = new PuppetDB(defaultHTTP);
        return new PuppetDBResourceModelSource(
                pdb,
                mapper,
                mapping,
                PropertyHandling.readBoolean(properties, PROPERTY_INCLUDE_CLASSES, false),
                PropertyHandling.readPuppetDbQuery(properties).orNull()
        );
    }


    @Override
    public Description getDescription() {
        return PLUGIN_DESCRIPTION;
    }

    public static void validate(final Properties properties) throws ConfigurationException {
        final List<Property> missingProperties = PropertyHandling.getMissingProperties(properties);
        if (isNotEmpty(missingProperties)) {
            final String missingPropertiesNames = PropertyHandling.joinPropertyNames(missingProperties);

            final String template = "Can't start %s plugin, Missing properties: '%s'";
            final String message = format(template, PROVIDER_NAME, missingPropertiesNames);
            throw new ConfigurationException(message);
        }
    }

    public static DefaultHTTP createHTTP(final Properties properties, final MetricRegistry metrics) {
        String puppetSslDir = properties.getProperty(PROPERTY_PUPPETDB_SSL_DIR);
        String certificatName = properties.getProperty(PROPERTY_PUPPETDB_CERTIFICATE_NAME);
        String puppetHost = properties.getProperty(PROPERTY_PUPPETDB_HOST);
        String puppetPort = properties.getProperty(PROPERTY_PUPPETDB_PORT);
        return new DefaultHTTP(puppetHost, puppetPort, certificatName, puppetSslDir, metrics);
    }

    public Map<String, Object> getMapping(final Properties properties) {
        final Type mappingType = new TypeToken<Map<String, Object>>() {
        }.getType();
        final String mappingFilePath = properties.getProperty(PROPERTY_MAPPING_FILE);

        if (isNotBlank(mappingFilePath)) {
            final File mappingFile = new File(mappingFilePath);
            final boolean validFile = mappingFile.exists() && mappingFile.canRead();

            if (validFile) {
                final String mappingFileContents = readFromFile(mappingFile);
                try {
                    return gson.fromJson(mappingFileContents, mappingType);
                } catch (Exception ex) {
                    log.warn("while reading mapping file: " + mappingFilePath, ex);
                }
            } else {
                log.warn("can't access mapping file : " + mappingFilePath);
            }
        }

        try {
            final String mappingFileContents = readFromClasspath("defaultMapping.json");
            return gson.fromJson(mappingFileContents, mappingType);
        } catch (Exception ex) {
            log.warn("while reading default mapping", ex);
        }

        return emptyMap();
    }

    private String readFromFile(final File file) {
        try (
                final InputStream inputStream = new FileInputStream(file);
                final Scanner scanner = new Scanner(inputStream)
        ) {
            scanner.useDelimiter("\\Z");
            return scanner.next();
        } catch (Exception ex) {
            log.warn("can't read file: " + file.getAbsolutePath(), ex);
        }

        return "";
    }

    private String readFromClasspath(final String name) {
        try (
                final InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(name);
                final Scanner scanner = new Scanner(inputStream)
        ) {
            scanner.useDelimiter("\\Z");
            return scanner.next();
        } catch (Exception ex) {
            log.warn("can't read " + name + " form classpath", ex);
        }

        return "";
    }

}
