package com.example.patent;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class PatentController {

    @GetMapping("/api/info")
    public Map<String, String> info() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("pod", hostname());
        data.put("app", "Patent App");
        data.put("project", "SWE304 Project 4");
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        return data;
    }

    @GetMapping("/api/hello")
    public String hello() {
        return "Hello from Patent App (SWE304 Project 4) - served by pod: " + hostname();
    }

    private String hostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
