package com.event.eventservice.config;

import org.springframework.http.HttpHeaders;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableConfigurationProperties(InternalApiProperties.class)
public class ClientConfig {

    private final InternalApiProperties internalApiProperties;

    public ClientConfig(InternalApiProperties internalApiProperties) {
        this.internalApiProperties = internalApiProperties;
    }

    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        ClientHttpRequestInterceptor authorizationForwardingInterceptor = (request, body, execution) -> {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                HttpServletRequest incomingRequest = servletAttributes.getRequest();
                String authorization = incomingRequest.getHeader(HttpHeaders.AUTHORIZATION);
                if (authorization != null && !authorization.isBlank()) {
                    request.getHeaders().set(HttpHeaders.AUTHORIZATION, authorization);
                }
            }
            if (internalApiProperties.getKey() != null && !internalApiProperties.getKey().isBlank()) {
                request.getHeaders().set(internalApiProperties.getHeaderName(), internalApiProperties.getKey());
            }
            return execution.execute(request, body);
        };

        return RestClient.builder()
                .requestInterceptor(authorizationForwardingInterceptor);
    }
}
