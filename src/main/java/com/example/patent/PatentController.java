package com.example.patent;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.InetAddress;
import java.net.UnknownHostException;

@RestController
public class PatentController {

    @GetMapping("/")
    public String hello() {
        String host;
        try {
            host = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            host = "unknown";
        }
        return "Hello from Patent App (SWE304 Project 4) - served by pod: " + host;
    }
}
