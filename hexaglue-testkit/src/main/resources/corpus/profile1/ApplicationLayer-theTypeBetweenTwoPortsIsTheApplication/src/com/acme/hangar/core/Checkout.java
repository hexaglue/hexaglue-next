package com.acme.hangar.core;

public class Checkout implements Boarding {

    private final Ledger ledger;

    public Checkout(Ledger ledger) {
        this.ledger = ledger;
    }

    @Override
    public void board(String plan) {
        ledger.locate(plan);
    }
}
