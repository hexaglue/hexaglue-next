package com.example.shop.left;

import com.example.shop.right.Journal;

/** Half of a knot: two packages that depend on each other in a circle. */
public class Ledger {

    private Journal journal;

    /**
     * Returns the journal it writes to.
     *
     * @return the journal
     */
    public Journal journal() {
        return journal;
    }
}
