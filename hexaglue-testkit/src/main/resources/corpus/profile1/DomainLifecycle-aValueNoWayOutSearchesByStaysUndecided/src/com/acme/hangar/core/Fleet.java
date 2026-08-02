package com.acme.hangar.core;

public class Fleet {

    private final FleetTag tag;
    private final Berth berth;
    private String reference;

    public Fleet(FleetTag tag, Berth berth, String reference) {
        this.tag = tag;
        this.berth = berth;
        this.reference = reference;
    }

    public FleetTag tag() {
        return tag;
    }

    public Berth berth() {
        return berth;
    }
}
