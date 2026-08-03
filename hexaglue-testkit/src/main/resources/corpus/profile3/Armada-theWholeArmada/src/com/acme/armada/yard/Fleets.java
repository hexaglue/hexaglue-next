package com.acme.armada.yard;

import java.util.List;

public interface Fleets {

    Fleet find(FleetTag tag);

    void keep(Fleet fleet);

    List<Fleet> all();
}
