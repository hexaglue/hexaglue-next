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

import io.hexaglue.engine.Analysis;
import io.hexaglue.engine.EngineContext;
import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.DrivingPort;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExposedAggregateTest {

    private static final TypeId WAY_IN = TypeId.of("com.acme.Booking");
    private static final TypeId DESK = TypeId.of("com.acme.BookingDesk");
    private static final TypeId FLEET = TypeId.of("com.acme.Fleet");
    private static final TypeId FLEET_TAG = TypeId.of("com.acme.FleetTag");
    private static final TypeId FLEETS = TypeId.of("com.acme.Fleets");
    private static final TypeId BERTH = TypeId.of("com.acme.Berth");
    private static final TypeId BERTH_TAG = TypeId.of("com.acme.BerthTag");
    private static final TypeId BERTHS = TypeId.of("com.acme.Berths");

    private static TypeRef ref(TypeId id) {
        return TypeRef.of(id.qualifiedName());
    }

    private static Method takes(String name, TypeRef answer, TypeId argument) {
        return Method.builder(name, answer)
                .parameters(List.of(Parameter.of("what", ref(argument))))
                .build();
    }

    /** An aggregate and its identity, kept by a way out so the domain wave can read them. */
    private static CodeModel.Builder domain(TypeId aggregate, TypeId tag, TypeId store) {
        return CodeModel.builder()
                .addType(TypeNode.builder(aggregate, TypeNature.CLASS)
                        .fields(List.of(Field.of("tag", ref(tag))))
                        .build())
                .addType(TypeNode.builder(tag, TypeNature.RECORD)
                        .fields(List.of(Field.of("value", TypeRef.of("java.lang.String"))))
                        .build())
                .addType(TypeNode.builder(store, TypeNature.INTERFACE)
                        .methods(List.of(takes("find", ref(aggregate), tag), takes("keep", TypeRef.of("void"), aggregate)))
                        .build());
    }

    private static ArchModel modelOf(CodeModel code) {
        return Analysis.analyze(EngineContext.of(code, KnowledgePacks.embedded(), HexaGlueConfig.defaults()))
                .model();
    }

    @Test
    @DisplayName("names the single aggregate a way in speaks of, taken or answered with")
    void namesTheSingleAggregateAWayInSpeaksOf() {
        // The shape a way in actually has: it is handed an identity and answers with the
        // aggregate. Requiring it on both sides — as a way out converges — would find nothing.
        CodeModel code = domain(FLEET, FLEET_TAG, FLEETS)
                .addType(TypeNode.builder(WAY_IN, TypeNature.INTERFACE)
                        .methods(List.of(takes("at", ref(FLEET), FLEET_TAG)))
                        .build())
                .addType(TypeNode.builder(DESK, TypeNature.CLASS)
                        .interfaces(List.of(ref(WAY_IN)))
                        .fields(List.of(Field.of("fleets", ref(FLEETS))))
                        .build())
                .supertypes(DESK, List.of(WAY_IN))
                .build();

        DrivingPort port = (DrivingPort) modelOf(code).type(WAY_IN).orElseThrow();

        assertThat(port.subject()).map(TypeRef::qualifiedName).contains(FLEET.qualifiedName());
    }

    @Test
    @DisplayName("and says nothing of a way in speaking of two, rather than picking one of them")
    void saysNothingOfAWayInSpeakingOfTwo() {
        // Two subjects is not one subject. A resource elected between them would be a guess the
        // sources never made, and the backend that needs one can say so.
        CodeModel code = domain(FLEET, FLEET_TAG, FLEETS)
                .addType(TypeNode.builder(BERTH, TypeNature.CLASS)
                        .fields(List.of(Field.of("tag", ref(BERTH_TAG))))
                        .build())
                .addType(TypeNode.builder(BERTH_TAG, TypeNature.RECORD)
                        .fields(List.of(Field.of("value", TypeRef.of("java.lang.String"))))
                        .build())
                .addType(TypeNode.builder(BERTHS, TypeNature.INTERFACE)
                        .methods(List.of(takes("find", ref(BERTH), BERTH_TAG), takes("keep", TypeRef.of("void"), BERTH)))
                        .build())
                .addType(TypeNode.builder(WAY_IN, TypeNature.INTERFACE)
                        .methods(List.of(takes("at", ref(FLEET), FLEET_TAG), takes("moor", ref(BERTH), BERTH_TAG)))
                        .build())
                .addType(TypeNode.builder(DESK, TypeNature.CLASS)
                        .interfaces(List.of(ref(WAY_IN)))
                        .fields(List.of(Field.of("fleets", ref(FLEETS)), Field.of("berths", ref(BERTHS))))
                        .build())
                .supertypes(DESK, List.of(WAY_IN))
                .build();

        DrivingPort port = (DrivingPort) modelOf(code).type(WAY_IN).orElseThrow();

        assertThat(port.subject()).isEmpty();
    }
}
