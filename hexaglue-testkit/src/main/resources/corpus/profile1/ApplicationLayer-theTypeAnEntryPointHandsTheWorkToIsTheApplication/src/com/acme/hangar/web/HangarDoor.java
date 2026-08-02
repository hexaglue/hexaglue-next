package com.acme.hangar.web;

import com.acme.hangar.core.Dispatch;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HangarDoor {

    private final Dispatch target;

    public HangarDoor(Dispatch target) {
        this.target = target;
    }

    public void open(String plan) {
        target.plan(plan);
    }
}
