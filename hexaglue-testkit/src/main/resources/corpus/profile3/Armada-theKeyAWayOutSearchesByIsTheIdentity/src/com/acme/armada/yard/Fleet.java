package com.acme.armada.yard;

import java.util.List;

public record Fleet(FleetTag tag, String yard, List<Hull> hulls) {}
