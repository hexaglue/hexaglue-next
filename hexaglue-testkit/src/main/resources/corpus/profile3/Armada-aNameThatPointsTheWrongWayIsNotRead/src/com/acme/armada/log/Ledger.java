package com.acme.armada.log;

public class Ledger {

    private final Bookkeeping bookkeeping;

    public Ledger(Bookkeeping bookkeeping) {
        this.bookkeeping = bookkeeping;
    }

    public void note(String line) {
        bookkeeping.write(line);
    }
}
