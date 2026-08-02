package com.acme.hangar.store;

import com.acme.hangar.core.Ledger;
import jakarta.persistence.EntityManager;

public class HangarBooks {

    private final EntityManager entityManager;
    private final Ledger ledger;

    public HangarBooks(EntityManager entityManager, Ledger ledger) {
        this.entityManager = entityManager;
        this.ledger = ledger;
    }

    public String read(String reference) {
        return entityManager.find(String.class, ledger.locate(reference));
    }
}
