package com.acme.clinic.billing;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

public class InvoiceLedger {

    private final JdbcTemplate jdbc;

    public InvoiceLedger(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<InvoiceRow> unpaid() {
        return jdbc.query("select * from invoices where paid = false", null);
    }
}
