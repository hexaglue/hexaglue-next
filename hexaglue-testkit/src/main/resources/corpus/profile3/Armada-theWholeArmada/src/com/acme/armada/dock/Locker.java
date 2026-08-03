package com.acme.armada.dock;

import com.acme.armada.yard.Fleet;
import com.acme.armada.yard.FleetTag;
import com.acme.armada.yard.Fleets;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

public class Locker implements Fleets {

    private final JdbcTemplate jdbc;

    public Locker(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Fleet find(FleetTag tag) {
        return jdbc.queryForObject("select * from fleets where tag = ?", null, tag.value());
    }

    @Override
    public void keep(Fleet fleet) {
        jdbc.update("insert into fleets values (?)", fleet.tag().value());
    }

    @Override
    public List<Fleet> all() {
        return jdbc.query("select * from fleets", (java.sql.ResultSet rs, int row) -> null);
    }
}
