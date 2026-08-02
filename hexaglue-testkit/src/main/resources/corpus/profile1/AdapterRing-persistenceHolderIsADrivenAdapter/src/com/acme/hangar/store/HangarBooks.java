package com.acme.hangar.store;

import com.acme.hangar.core.Fleet;
import jakarta.persistence.EntityManager;

public class HangarBooks {

    private final EntityManager entityManager;

    public HangarBooks(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Fleet read(String reference) {
        return entityManager.find(Fleet.class, reference);
    }
}
