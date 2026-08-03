package com.acme.armada.yard;

import java.util.List;

public class Quartermaster {

    private final Fleets fleets;

    public Quartermaster(Fleets fleets) {
        this.fleets = fleets;
    }

    public Fleet at(FleetTag tag) {
        return fleets.find(tag);
    }

    public List<Fleet> everything() {
        return fleets.all();
    }
}
