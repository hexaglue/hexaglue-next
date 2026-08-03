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

package io.hexaglue.engine.finding;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.model.TypeId;
import io.hexaglue.model.config.ClassificationConfig;
import io.hexaglue.model.finding.Finding;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The tool holds no opinion about names. It reads the vocabulary a project opted into and holds
 * the project to that — which is why the same sources produce a finding with a vocabulary and
 * nothing at all without one.
 */
class NamingFindingsTest {

    private static List<Finding> naming(List<Finding> findings) {
        return findings.stream()
                .filter(finding -> finding.code().equals(NamingFindings.OFF_VOCABULARY))
                .toList();
    }

    @Test
    @DisplayName("says nothing on a codebase that stated no convention")
    void staysSilentWithoutAVocabulary() {
        List<Finding> findings =
                ShopJudgements.shop().domainEvent("com.shop.OrderPlaced").judge();

        assertThat(naming(findings)).isEmpty();
    }

    @Test
    @DisplayName("holds a codebase to the convention it opted into")
    void reportsATypeOffTheStatedVocabulary() {
        List<Finding> findings = ShopJudgements.shop()
                .domainEvent("com.shop.OrderPlaced")
                .judgeWith(ClassificationConfig.conventional());

        assertThat(naming(findings)).singleElement().satisfies(finding -> {
            assertThat(finding.subject()).isEqualTo(TypeId.of("com.shop.OrderPlaced"));
            assertThat(finding.message()).contains("DOMAIN_EVENT").contains("Event");
        });
    }

    @Test
    @DisplayName("says nothing about a type that follows it")
    void staysSilentOnAConformingName() {
        List<Finding> findings = ShopJudgements.shop()
                .domainEvent("com.shop.OrderPlacedEvent")
                .judgeWith(ClassificationConfig.conventional());

        assertThat(naming(findings)).isEmpty();
    }
}
