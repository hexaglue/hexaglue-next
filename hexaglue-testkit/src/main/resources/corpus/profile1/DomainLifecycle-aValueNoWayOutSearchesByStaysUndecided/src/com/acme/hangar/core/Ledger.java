package com.acme.hangar.core;

import java.util.Optional;

public interface Ledger {

    Optional<Fleet> locate(FleetTag tag);

    void keep(Fleet fleet);
}
