package com.example.shop.right;

import com.example.shop.left.Ledger;

/** The other half of the knot. */
public class Journal {

    private Ledger ledger;

    /**
     * Returns the ledger it belongs to.
     *
     * @return the ledger
     */
    public Ledger ledger() {
        return ledger;
    }
}
