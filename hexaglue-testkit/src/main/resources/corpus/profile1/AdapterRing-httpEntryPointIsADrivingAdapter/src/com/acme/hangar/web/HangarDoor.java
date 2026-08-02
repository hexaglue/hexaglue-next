package com.acme.hangar.web;

import com.acme.hangar.core.Assembly;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HangarDoor {

    private final Assembly assembly;

    public HangarDoor(Assembly assembly) {
        this.assembly = assembly;
    }

    public void open(String plan) {
        assembly.assemble(plan);
    }
}
