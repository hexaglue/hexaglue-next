package com.acme.armada.log;

public class FleetRepository {

    private final String label;

    public FleetRepository(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
