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

package io.hexaglue.engine.rule;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.engine.Classifier;
import io.hexaglue.engine.EngineContext;
import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.PortDirection;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.declaration.Annotation;
import io.hexaglue.model.declaration.Method;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FrameworkEntryPointTest {

    private static final TypeId SUBJECT = TypeId.of("com.acme.Anything");

    private static Classification verdictOf(String... annotations) {
        CodeModel code = CodeModel.builder()
                .addType(TypeNode.builder(SUBJECT, TypeNature.CLASS)
                        .annotations(List.of(annotations).stream()
                                .map(Annotation::of)
                                .toList())
                        .build())
                .build();
        return Classifier.classify(EngineContext.of(code, KnowledgePacks.embedded(), HexaGlueConfig.defaults()))
                .verdict(SUBJECT)
                .orElseThrow();
    }

    /**
     * The shape a message-driven adapter actually has: a stereotype on the class, and the broker
     * named on the method that receives from it.
     */
    private static Classification verdictOfListeningComponent(String listener) {
        CodeModel code = CodeModel.builder()
                .addType(TypeNode.builder(SUBJECT, TypeNature.CLASS)
                        .annotations(List.of(Annotation.of("org.springframework.stereotype.Component")))
                        .methods(List.of(Method.builder("receive", TypeRef.of("void"))
                                .annotations(List.of(Annotation.of(listener)))
                                .build()))
                        .build())
                .build();
        return Classifier.classify(EngineContext.of(code, KnowledgePacks.embedded(), HexaGlueConfig.defaults()))
                .verdict(SUBJECT)
                .orElseThrow();
    }

    @Nested
    @DisplayName("reads what the framework calls from outside")
    class ReadsWhatTheFrameworkCallsFromOutside {

        @Test
        @DisplayName("as the driving adapter it is, when the type answers HTTP")
        void aControllerIsADrivingAdapter() {
            Classification verdict = verdictOf("org.springframework.web.bind.annotation.RestController");

            assertThat(verdict.kind()).isEqualTo(ArchKind.DRIVING_ADAPTER);
            assertThat(verdict.direction()).contains(PortDirection.DRIVING);
            assertThat(verdict.confidence()).isEqualTo(Confidence.HIGH);
            assertThat(verdict.basis()).isEqualTo(Basis.INFERRED);
        }

        @Test
        @DisplayName("as the driving adapter it is, when the type answers a broker")
        void aListenerIsADrivingAdapter() {
            assertThat(verdictOf("org.springframework.kafka.annotation.KafkaListener")
                            .kind())
                    .isEqualTo(ArchKind.DRIVING_ADAPTER);
        }

        @Test
        @DisplayName("whatever vendor names the entry point")
        void whateverVendorNamesTheEntryPoint() {
            assertThat(verdictOf("jakarta.ws.rs.Path").kind()).isEqualTo(ArchKind.DRIVING_ADAPTER);
        }

        @Test
        @DisplayName("when the broker is named on the receiving method, under a stereotype alone")
        void whenTheBrokerIsNamedOnTheReceivingMethod() {
            Classification verdict = verdictOfListeningComponent("org.springframework.jms.annotation.JmsListener");

            assertThat(verdict.kind()).isEqualTo(ArchKind.DRIVING_ADAPTER);
            assertThat(verdict.confidence()).isEqualTo(Confidence.HIGH);
        }

        @Test
        @DisplayName("reading one entry point per type, however many annotations spell it")
        void oneEntryPointPerTypeHoweverManyAnnotations() {
            // Two entry-point annotations state one fact: this type is called from outside.
            assertThat(verdictOf(
                                    "org.springframework.stereotype.Controller",
                                    "org.springframework.web.bind.annotation.ControllerAdvice")
                            .kind())
                    .isEqualTo(ArchKind.DRIVING_ADAPTER);
        }
    }

    @Nested
    @DisplayName("says nothing")
    class SaysNothing {

        @Test
        @DisplayName("about a stereotype, which an application service wears just as often")
        void aboutAStereotype() {
            assertThat(verdictOf("org.springframework.stereotype.Service").kind())
                    .isEqualTo(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about plumbing, which belongs to no ring of the hexagon")
        void aboutPlumbing() {
            assertThat(verdictOf("org.springframework.context.annotation.Configuration")
                            .kind())
                    .isEqualTo(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about a type no pack recognizes")
        void aboutATypeNoPackRecognizes() {
            assertThat(verdictOf("com.acme.Homemade").kind()).isEqualTo(ArchKind.UNCLASSIFIED);
        }
    }
}
