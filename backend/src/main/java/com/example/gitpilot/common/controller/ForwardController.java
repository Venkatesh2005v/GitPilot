package com.example.gitpilot.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ForwardController {

    @GetMapping(value = {
        "/dashboard",
        "/journey",
        "/knowledge",
        "/dna",
        "/timeline",
        "/onboarding",
        "/recommendations",
        "/analytics",
        "/insights",
        "/settings",
        "/repositories/{id:\\d+}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
