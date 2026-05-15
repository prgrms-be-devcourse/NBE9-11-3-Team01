package com.team01.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 정적 리소스 핸들러 설정 클래스
 * 클라이언트의 /static/images/ 요청을 resources/static/images/ 폴더와 매핑합니다.
 */
@Configuration
class WebConfig : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/static/images/**")
            .addResourceLocations("classpath:/static/images/")
    }
}