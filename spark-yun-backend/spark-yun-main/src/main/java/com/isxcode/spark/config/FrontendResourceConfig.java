package com.isxcode.spark.config;

import java.time.Duration;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class FrontendResourceConfig implements WebMvcConfigurer {

    private static final CacheControl CACHE_CONTROL =
        CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable();

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/assets/**").addResourceLocations("classpath:/assets/")
            .setCacheControl(CACHE_CONTROL);

        registry.addResourceHandler("/favicon.ico", "/manifest.json", "/robots.txt").addResourceLocations("classpath:/")
            .setCacheControl(CACHE_CONTROL);
    }
}
