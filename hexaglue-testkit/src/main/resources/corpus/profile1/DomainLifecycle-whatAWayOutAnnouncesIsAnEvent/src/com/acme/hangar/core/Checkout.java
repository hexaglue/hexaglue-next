package com.acme.hangar.core;

public class Checkout {

    private final Wire wire;

    public Checkout(Wire wire) {
        this.wire = wire;
    }

    public void settle(String reference) {
        wire.carry(new Sailing(reference, "now"));
    }
}
