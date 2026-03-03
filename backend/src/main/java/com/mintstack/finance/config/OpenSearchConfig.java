package com.mintstack.finance.config;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;

@Configuration
public class OpenSearchConfig {

    @Value("${opensearch.host:localhost}")
    private String host;

    @Value("${opensearch.port:19200}")
    private int port;

    @Value("${opensearch.scheme:http}")
    private String scheme;

    @Value("${opensearch.username:}")
    private String username;

    @Value("${opensearch.password:}")
    private String password;

    @Bean
    public RestHighLevelClient openSearchClient() {
        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme))
            .setRequestConfigCallback(requestConfigBuilder ->
                requestConfigBuilder
                    .setConnectTimeout(5000)
                    .setSocketTimeout(60000));

        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));

            builder.setHttpClientConfigCallback(httpClientBuilder -> {
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);

                if ("https".equalsIgnoreCase(scheme)) {
                    try {
                        TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
                        SSLContext sslContext = SSLContextBuilder.create()
                            .loadTrustMaterial(null, acceptingTrustStrategy)
                            .build();
                        httpClientBuilder.setSSLContext(sslContext);
                        httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
                    } catch (Exception exception) {
                        throw new IllegalStateException("Failed to initialize OpenSearch SSL context", exception);
                    }
                }

                return httpClientBuilder;
            });
        }

        return new RestHighLevelClient(builder);
    }
}
