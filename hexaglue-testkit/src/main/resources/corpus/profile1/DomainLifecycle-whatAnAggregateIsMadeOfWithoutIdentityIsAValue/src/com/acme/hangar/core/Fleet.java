package com.acme.hangar.core;

import java.util.List;

public class Fleet {

    private final FleetTag tag;
    private final List<Hull> hulls;
    private Manifest manifest;

    public Fleet(FleetTag tag, List<Hull> hulls, Manifest manifest) {
        this.tag = tag;
        this.hulls = hulls;
        this.manifest = manifest;
    }

    public List<Hull> hulls() {
        return hulls;
    }

    public Manifest manifest() {
        return manifest;
    }
}
