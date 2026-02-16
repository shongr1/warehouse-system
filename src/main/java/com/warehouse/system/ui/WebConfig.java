package com.warehouse.system.ui;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UiAuthInterceptor uiAuthInterceptor;

    public WebConfig(UiAuthInterceptor uiAuthInterceptor) {
        this.uiAuthInterceptor = uiAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(uiAuthInterceptor)
                .addPathPatterns("/ui/**");
    }
}
