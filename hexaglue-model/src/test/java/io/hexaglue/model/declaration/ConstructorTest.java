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

package io.hexaglue.model.declaration;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.model.TypeRef;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConstructorTest {

    @Test
    @DisplayName("a no-arg constructor has an empty signature")
    void noArgConstructorHasEmptySignature() {
        Constructor constructor = Constructor.noArg();

        assertThat(constructor.parameters()).isEmpty();
        assertThat(constructor.signature()).isEqualTo("()");
    }

    @Test
    @DisplayName("the signature renders parameter simple names in order")
    void signatureRendersParameterSimpleNames() {
        Constructor constructor = Constructor.of(List.of(
                Parameter.of("id", TypeRef.of("com.a.OrderId")),
                Parameter.of("total", TypeRef.of("java.math.BigDecimal"))));

        assertThat(constructor.signature()).isEqualTo("(OrderId, BigDecimal)");
    }
}
