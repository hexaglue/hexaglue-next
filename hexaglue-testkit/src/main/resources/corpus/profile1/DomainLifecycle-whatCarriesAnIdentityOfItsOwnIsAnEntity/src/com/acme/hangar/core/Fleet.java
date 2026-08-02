package com.acme.hangar.core;

import java.util.List;

public class Fleet {

    private final FleetTag tag;
    private final List<Hull> hulls;

    public Fleet(FleetTag tag, List<Hull> hulls) {
        this.tag = tag;
        this.hulls = hulls;
    }

    public List<Hull> hulls() {
        return hulls;
    }
}
