// src/main/java/com/apishield/controller/RootController.java
package com.apishield.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "API Shield Backend");
        response.put("status", "running");
        response.put("timestamp", LocalDateTime.now());
        response.put("version", "1.0.0");
        response.put("docs", "https://fortunate-rebirth-production.up.railway.app/swagger-ui.html");
        return ResponseEntity.ok(response);
    }
}