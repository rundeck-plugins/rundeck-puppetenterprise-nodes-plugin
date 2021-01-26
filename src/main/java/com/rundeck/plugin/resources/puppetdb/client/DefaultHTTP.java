package com.rundeck.plugin.resources.puppetdb.client;

import static java.lang.String.format;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Function;

/**
 * Use apache http client
 */
public class DefaultHTTP implements HTTP {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultHTTP.class);
    private static final String HTTPS = "https";
    public static final String HTTP = "http";
    private static final String USERAGENT = "Rundeck Node Plugin";
    
    private final String puppetProtocol;
    private final String puppetHost;
    private final String puppetPort;
    private final String puppetCertificatName;
    private final String puppetSslDir;
    private final MetricRegistry metrics;

    public DefaultHTTP(
            final String puppetHost,
            final String puppetPort,
            final String puppetCertificatName,
            final String puppetSslDir,
            final MetricRegistry metrics
            ) {
        this.puppetProtocol = puppetSslDir == null ? HTTP : HTTPS;
        this.puppetHost = puppetHost;
        this.puppetPort = puppetPort;
        this.puppetCertificatName = puppetCertificatName;
        this.puppetSslDir = puppetSslDir;
        this.metrics = metrics;
    }

    public String getBaseUrl(final String path) {
        final String baseUrl = format("%s://%s:%s/%s", puppetProtocol, puppetHost, puppetPort, path);
        LOG.info(format("baseUrl is: %s", baseUrl));
        return baseUrl;
    }

    private HttpGet mkGet(final String path) {
        return new HttpGet(getBaseUrl(path));
    }

    @Override
    public <T> T makeRequest(final String path, Function<String, T> parser, T errResult, final String name)
    {
        HttpGet get = mkGet(path);
        final CloseableHttpClient httpclient = getClient();

        LOG.debug(format("GET %s", get.getURI()));
        requestCounter();
        try (final CloseableHttpResponse response = httpclient.execute(get)) {
            final int statusCode = response.getStatusLine().getStatusCode();
            final boolean ok = statusCode == HttpStatus.SC_OK;

            if (!ok) {
                errorsCounter();
                LOG.warn(format(name + " ended with status code: %d msg: %s", statusCode,
                        EntityUtils.toString(response.getEntity())
                        ));
                return errResult;
            }

            final String responseBody = EntityUtils.toString(response.getEntity());

            return parser.apply(responseBody);
        } catch (final IOException ex) {
            errorsCounter();
            LOG.warn(name + " exception while trying to request PuppetDB API: " + ex.getMessage(), ex);
        }
        return errResult;
    }

    CloseableHttpClient getClient() {
        return puppetProtocol.equalsIgnoreCase(HTTPS)
                ? getHttpsClient()
                        : HttpClients.custom().setUserAgent(USERAGENT).build();
    }

    private CloseableHttpClient getHttpsClient() {
        try {
            SSLContext sslContext = PemSslContext.getContext(puppetCertificatName, puppetSslDir);

            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
            Registry<ConnectionSocketFactory> r = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register(HTTPS, sslsf)
                    .build();

            HttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(r);

            return HttpClients.custom()
                    .setConnectionManager(cm)
                    .setUserAgent(USERAGENT)
                    .build();
        } catch (IOException | GeneralSecurityException ex) {
            LOG.error("Can't configure https with puppetdb: " + ex.getMessage(), ex);
            return null;
        }
    }

    private void errorsCounter() {
        metrics.counter(MetricRegistry.name(DefaultPuppetAPI.class, "http.errors.count")).inc();
    }

    private void requestCounter() {
        metrics.counter(MetricRegistry.name(DefaultPuppetAPI.class, "http.request.count")).inc();
    }

}
