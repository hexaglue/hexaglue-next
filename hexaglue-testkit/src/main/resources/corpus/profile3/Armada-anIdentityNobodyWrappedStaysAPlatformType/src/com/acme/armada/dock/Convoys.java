package com.acme.armada.dock;

import java.util.List;
import java.util.UUID;

public interface Convoys {

    Convoy find(UUID tag);

    void keep(Convoy convoy);

    List<Convoy> all();
}
