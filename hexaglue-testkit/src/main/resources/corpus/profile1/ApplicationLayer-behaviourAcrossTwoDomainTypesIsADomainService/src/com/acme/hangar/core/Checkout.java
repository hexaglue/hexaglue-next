package com.acme.hangar.core;

public class Checkout {

    private final Dispatch dispatch;

    public Checkout(Dispatch dispatch) {
        this.dispatch = dispatch;
    }

    public void settle(Fleet fleet, Manifest manifest) {
        dispatch.plan(fleet, manifest);
    }
}
