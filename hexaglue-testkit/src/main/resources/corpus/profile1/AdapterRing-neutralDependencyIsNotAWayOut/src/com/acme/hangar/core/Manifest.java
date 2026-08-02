package com.acme.hangar.core;

import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Manifest {

    private static final Logger LOG = LoggerFactory.getLogger(Manifest.class);

    @NotNull
    private String reference;

    public void record() {
        LOG.info("manifest recorded");
    }
}
