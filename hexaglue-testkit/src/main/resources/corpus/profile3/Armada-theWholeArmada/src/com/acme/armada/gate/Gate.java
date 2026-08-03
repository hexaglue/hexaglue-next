package com.acme.armada.gate;

import com.acme.armada.yard.Boarding;
import com.acme.armada.yard.Fleet;
import com.acme.armada.yard.FleetTag;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Gate {

    private final Boarding boarding;

    public Gate(Boarding boarding) {
        this.boarding = boarding;
    }

    public Fleet open(String yard, List<com.acme.armada.yard.Hull> hulls) {
        return boarding.assemble(yard, hulls);
    }

    public Fleet look(FleetTag tag) {
        return boarding.at(tag);
    }
}
