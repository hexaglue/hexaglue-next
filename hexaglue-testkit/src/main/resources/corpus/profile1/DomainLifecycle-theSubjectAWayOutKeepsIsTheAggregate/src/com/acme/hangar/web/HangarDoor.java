package com.acme.hangar.web;

import com.acme.hangar.core.Boarding;
import com.acme.hangar.core.FleetTag;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HangarDoor {

    private final Boarding target;

    public HangarDoor(Boarding target) {
        this.target = target;
    }

    public void open(FleetTag tag) {
        target.board(tag);
    }
}
