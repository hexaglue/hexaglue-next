package com.acme.hangar.store;

import com.acme.hangar.core.Ledger;

public class LedgerBook implements Ledger {

    @Override
    public String locate(String reference) {
        return reference;
    }
}
