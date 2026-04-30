package com.event.registrationservice.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@EnableConfigurationProperties(InternalApiProperties.class)
public class FeignAuthForwardingConfig {

    private final InternalApiProperties internalApiProperties;

    public FeignAuthForwardingConfig(InternalApiProperties internalApiProperties) {
        this.internalApiProperties = internalApiProperties;
    }

    @Bean
    public RequestInterceptor bearerTokenForwardingInterceptor() {
        return requestTemplate -> {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
                return;
            }
            HttpServletRequest request = servletAttributes.getRequest();
            String authorization = request.getHeader("Authorization");
            if (authorization != null && !authorization.isBlank()) {
                requestTemplate.header("Authorization", authorization);
            }
            if (internalApiProperties.getKey() != null && !internalApiProperties.getKey().isBlank()) {
                requestTemplate.header(internalApiProperties.getHeaderName(), internalApiProperties.getKey());
            }
        };
    }
}
