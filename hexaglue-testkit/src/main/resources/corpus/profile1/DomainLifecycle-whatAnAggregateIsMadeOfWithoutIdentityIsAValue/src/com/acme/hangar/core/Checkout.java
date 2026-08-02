package com.acme.hangar.core;

public class Checkout {

    private final Ledger ledger;

    public Checkout(Ledger ledger) {
        this.ledger = ledger;
    }

    public void settle(FleetTag tag) {
        ledger.locate(tag);
    }
}
