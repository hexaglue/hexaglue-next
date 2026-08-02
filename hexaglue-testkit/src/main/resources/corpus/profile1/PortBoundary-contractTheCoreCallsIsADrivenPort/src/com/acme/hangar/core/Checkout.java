package com.acme.hangar.core;

public class Checkout {

    private final Ledger ledger;

    public Checkout(Ledger ledger) {
        this.ledger = ledger;
    }

    public String settle(String reference) {
        return ledger.locate(reference);
    }
}
