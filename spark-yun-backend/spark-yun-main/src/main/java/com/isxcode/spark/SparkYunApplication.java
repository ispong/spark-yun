package com.isxcode.spark;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(value = "/")
@SpringBootApplication
public class SparkYunApplication {

    public static void main(String[] args) {

        SpringApplication.run(SparkYunApplication.class, args);
    }

    @GetMapping(
        value = {"/", "/auth", "/ssoauth", "/home/**", "/platform/**", "/admin/**", "/workspace/**",
            "/personal-info", "/403", "/no-tenant", "/share/**", "/dashboard/**"})
    public String index() {

        return "index";
    }
}
