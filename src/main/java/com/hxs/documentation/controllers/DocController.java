package com.hxs.documentation.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Re-direct to documentation controller
 * @author HSteidel
 */
@Controller
public class DocController {

    @RequestMapping("/")
    public String home() {
        return "redirect:swagger-ui.html";
    }

}
