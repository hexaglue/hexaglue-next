package com.acme.armada.yard;

import java.util.List;

public interface Boarding {

    Fleet assemble(String yard, List<Hull> hulls);

    Fleet at(FleetTag tag);
}
