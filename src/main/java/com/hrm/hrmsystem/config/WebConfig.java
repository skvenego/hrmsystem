package com.hrm.hrmsystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Handle static resources
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Forward all frontend routes to index.html
        registry.addViewController("/login").setViewName("forward:/index.html");
        registry.addViewController("/register").setViewName("forward:/index.html");
        registry.addViewController("/dashboard").setViewName("forward:/index.html");
        registry.addViewController("/dashboard/employees").setViewName("forward:/index.html");
        registry.addViewController("/dashboard/attendance").setViewName("forward:/index.html");
        registry.addViewController("/dashboard/leave").setViewName("forward:/index.html");
        registry.addViewController("/dashboard/payroll").setViewName("forward:/index.html");
        registry.addViewController("/dashboard/payslips").setViewName("forward:/index.html");
        registry.addViewController("/dashboard/profile").setViewName("forward:/index.html");
        registry.addViewController("/dashboard/settings").setViewName("forward:/index.html");
    }
}
