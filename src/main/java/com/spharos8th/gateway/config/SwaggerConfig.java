package com.spharos8th.gateway.config;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties.SwaggerUrl;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.discovery.event.HeartbeatEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Eureka에 등록된 서비스 목록을 읽어 Swagger UI 상단의 서비스 선택 드롭다운을 구성한다.
 * 각 문서는 gateway 라우트(/{serviceId}/v3/api-docs)를 통해 해당 서비스로 전달된다.
 */
@Configuration
public class SwaggerConfig {

    private static final String API_DOCS_PATH = "/v3/api-docs";

    private final DiscoveryClient discoveryClient;
    private final SwaggerUiConfigProperties swaggerUiConfigProperties;
    private final Set<String> excludedServices;

    public SwaggerConfig(
            DiscoveryClient discoveryClient,
            SwaggerUiConfigProperties swaggerUiConfigProperties,
            @Value("${spring.application.name}") String applicationName,
            @Value("${swagger.excluded-services:discovery}") List<String> excludedServices
    ) {
        this.discoveryClient = discoveryClient;
        this.swaggerUiConfigProperties = swaggerUiConfigProperties;
        this.excludedServices = new LinkedHashSet<>();
        this.excludedServices.add(applicationName.toLowerCase());
        excludedServices.forEach(service -> this.excludedServices.add(service.toLowerCase()));
    }

    // 기동 직후 + Eureka 레지스트리 갱신(HeartbeatEvent, 기본 30초)마다 서비스 목록을 다시 반영한다.
    @EventListener({ApplicationReadyEvent.class, HeartbeatEvent.class})
    public void refreshSwaggerUrls() {
        Set<SwaggerUrl> urls = new LinkedHashSet<>();

        discoveryClient.getServices().stream()
                .map(String::toLowerCase)
                .filter(serviceId -> !excludedServices.contains(serviceId))
                .sorted()
                .forEach(serviceId -> urls.add(
                        new SwaggerUrl(serviceId, "/" + serviceId + API_DOCS_PATH, serviceId)
                ));

        swaggerUiConfigProperties.setUrls(urls);
    }
}
