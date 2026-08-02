package com.acme.hangar.core;

public class Manifest {

    private final Dispatch dispatch;

    public Manifest(Dispatch dispatch) {
        this.dispatch = dispatch;
    }

    public void record(String reference) {
        dispatch.plan(reference);
    }
}
