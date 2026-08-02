package com.acme.hangar.core;

public class Dispatch {

    private Manifest last;

    public Manifest plan(Fleet fleet, Manifest manifest) {
        last = manifest;
        return last;
    }
}
