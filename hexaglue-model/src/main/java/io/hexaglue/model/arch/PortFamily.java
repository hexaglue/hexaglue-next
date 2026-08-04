/*
 * This Source Code Form is part of the HexaGlue project.
 * Copyright (c) 2026 Scalastic
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Commercial licensing options are available for organizations wishing
 * to use HexaGlue under terms different from the MPL 2.0.
 * Contact: info@hexaglue.io
 */

package io.hexaglue.model.arch;

import java.util.Objects;

/**
 * A family of ports, as a backend names the ones it will write adapters for.
 *
 * <p>The grain is the family and not the direction, because a backend fills some of the holes a
 * hexagon leaves and not all of them: one writing persistence covers the ports that keep
 * aggregates, and saying nothing about the port that sends mail is exactly what keeps that one from
 * going unmentioned.</p>
 *
 * @since 7.0.0
 */
public sealed interface PortFamily {

    /**
     * Answers whether the given port belongs to this family.
     *
     * @param port the port to place
     * @return true when the family covers it
     */
    boolean covers(PortType port);

    /**
     * Returns how this family reads in a message.
     *
     * @return the wording
     */
    String toDisplayString();

    /**
     * Names the driven ports of one functional family.
     *
     * @param role the family of driven port
     * @return the port family
     */
    static PortFamily driven(DrivenPortType role) {
        return new Driven(role);
    }

    /**
     * Names the driving ports, which have no families of their own.
     *
     * @return the port family
     */
    static PortFamily driving() {
        return new Driving();
    }

    /**
     * The driven ports of one functional family.
     *
     * @param role the family of driven port
     * @since 7.0.0
     */
    record Driven(DrivenPortType role) implements PortFamily {

        /**
         * Validates the role.
         */
        public Driven {
            Objects.requireNonNull(role, "role must not be null");
        }

        @Override
        public boolean covers(PortType port) {
            Objects.requireNonNull(port, "port must not be null");
            return port instanceof DrivenPort driven && driven.portType() == role;
        }

        @Override
        public String toDisplayString() {
            return "driven ports of role " + role;
        }
    }

    /**
     * Every driving port.
     *
     * @since 7.0.0
     */
    record Driving() implements PortFamily {

        @Override
        public boolean covers(PortType port) {
            Objects.requireNonNull(port, "port must not be null");
            return port instanceof DrivingPort;
        }

        @Override
        public String toDisplayString() {
            return "driving ports";
        }
    }
}
