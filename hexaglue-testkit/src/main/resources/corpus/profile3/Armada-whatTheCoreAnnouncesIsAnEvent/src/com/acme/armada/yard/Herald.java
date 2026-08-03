package com.acme.armada.yard;

import java.time.Instant;

public class Herald {

    private final Trumpets trumpets;

    public Herald(Trumpets trumpets) {
        this.trumpets = trumpets;
    }

    public void launched(String designation) {
        trumpets.sound(new FleetLaunched(designation, Instant.now()));
    }
}
