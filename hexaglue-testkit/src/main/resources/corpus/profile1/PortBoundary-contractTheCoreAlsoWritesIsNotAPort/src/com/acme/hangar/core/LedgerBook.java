package com.acme.hangar.core;

public class LedgerBook implements Ledger {

    @Override
    public String locate(String reference) {
        return reference;
    }
}
