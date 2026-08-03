package com.acme.armada.dock;

import java.util.UUID;

public class Harbourmaster {

    private final Convoys convoys;

    public Harbourmaster(Convoys convoys) {
        this.convoys = convoys;
    }

    public Convoy at(UUID tag) {
        return convoys.find(tag);
    }
}
