package com.acme.armada.yard;

import java.util.List;
import java.util.UUID;

public class Quartermaster implements Boarding {

    private final Fleets fleets;

    private final Trumpets trumpets;

    public Quartermaster(Fleets fleets, Trumpets trumpets) {
        this.fleets = fleets;
        this.trumpets = trumpets;
    }

    @Override
    public Fleet assemble(String yard, List<Hull> hulls) {
        Fleet fleet = new Fleet(new FleetTag(UUID.randomUUID()), yard, hulls);
        fleets.keep(fleet);
        trumpets.sound(new FleetLaunched(fleet.tag(), yard));
        return fleet;
    }

    @Override
    public Fleet at(FleetTag tag) {
        return fleets.find(tag);
    }
}
