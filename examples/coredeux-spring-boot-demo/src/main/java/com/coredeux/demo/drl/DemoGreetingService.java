package com.coredeux.demo.drl;

import org.springframework.stereotype.Service;

@Service("demoGreetingService")
public class DemoGreetingService {

    public String message() {
        return "Hello from the Coredeux DRL spring demo";
    }
}
