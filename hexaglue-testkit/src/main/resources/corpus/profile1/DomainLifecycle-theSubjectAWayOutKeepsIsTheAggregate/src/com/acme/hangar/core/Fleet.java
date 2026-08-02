package com.acme.hangar.core;

public class Fleet {

    private final FleetTag tag;
    private final String reference;

    public Fleet(FleetTag tag, String reference) {
        this.tag = tag;
        this.reference = reference;
    }

    public FleetTag tag() {
        return tag;
    }

    public String reference() {
        return reference;
    }
}
