package com.acme.hangar;

import org.springframework.context.annotation.Configuration;

@Configuration
public class Wiring {

    public String hangarName() {
        return "north";
    }
}
