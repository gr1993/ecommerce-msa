package com.example.catalogservice.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class ElasticsearchTestContainerConfig {

    private static final String ELASTICSEARCH_IMAGE = "elasticsearch:9.2.3";

    @Bean
    @ServiceConnection
    public ElasticsearchContainer elasticsearchContainer() {
        return new ElasticsearchContainer(DockerImageName.parse(ELASTICSEARCH_IMAGE))
                .withEnv("xpack.security.enabled", "false")
                .withEnv("discovery.type", "single-node")
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                // Nori 플러그인 설치를 위한 커맨드
                .withCommand("sh", "-c",
                        "bin/elasticsearch-plugin install analysis-nori --batch && " +
                        "bin/elasticsearch");
    }
}
