package com.ossradar.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**").allowedOriginPatterns("*").allowedMethods("*").allowedHeaders("*");
            }
        };
    }

    @Bean
    public RestTemplate githubRestTemplate(
            RestTemplateBuilder builder,
            @Value("${ossradar.github.token:}") String token) {
        return builder.additionalInterceptors((request, body, execution) -> {
            request.getHeaders().set(HttpHeaders.ACCEPT, "application/vnd.github+json");
            request.getHeaders().set("X-GitHub-Api-Version", "2022-11-28");
            request.getHeaders().set(HttpHeaders.USER_AGENT, "OSS-Radar/0.1");
            if (token != null && !token.isBlank()) {
                request.getHeaders().setBearerAuth(token);
            }
            return execution.execute(request, body);
        }).build();
    }
}
