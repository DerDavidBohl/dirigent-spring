//package org.davidbohl.dirigent.ui;
//
//import jakarta.servlet.http.HttpServletRequest;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.servlet.HandlerMapping;
//import org.springframework.web.util.UrlPathHelper;
//
//@Controller
//public class SpaController {
//
//    @RequestMapping(path = { "/ui/", "/ui/**"})
//    public String forward(HttpServletRequest request) {
//        UrlPathHelper pathHelper = new UrlPathHelper();
//        String path = pathHelper.getPathWithinApplication(request);
//
//        if (path.contains(".")) {
//            return null;
//        }
//
//        return "forward:/ui/index.html";
//    }
//}