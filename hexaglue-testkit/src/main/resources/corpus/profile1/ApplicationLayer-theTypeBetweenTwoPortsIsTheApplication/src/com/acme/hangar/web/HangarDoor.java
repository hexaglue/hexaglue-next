package com.acme.hangar.web;

import com.acme.hangar.core.Boarding;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HangarDoor {

    private final Boarding target;

    public HangarDoor(Boarding target) {
        this.target = target;
    }

    public void open(String plan) {
        target.board(plan);
    }
}
