package com.acme.hangar.core;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

@SecondaryPort
public interface Ledger {
    String locate(String reference);
}
