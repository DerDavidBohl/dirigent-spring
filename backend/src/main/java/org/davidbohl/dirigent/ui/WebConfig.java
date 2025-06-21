//package org.davidbohl.dirigent.ui;
//
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
//import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class WebConfig implements WebMvcConfigurer {
//    @Override
//    public void addViewControllers(ViewControllerRegistry registry) {
//        // Forward only if it's NOT a request for a resource with a file extension (like .js, .css, .png)
//        registry.addViewController("/ui/{path:[^\\.]*}/**") // Match "/ui/something" or "/ui/something/else", excluding URLs with dots.
//                .setViewName("forward:/ui/index.html");
//    }
//}
