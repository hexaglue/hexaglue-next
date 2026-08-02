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
import static org.assertj.core.api.Assertions.tuple;

import io.hexaglue.engine.Classifier;
import io.hexaglue.engine.EngineContext;
import io.hexaglue.engine.FactBase;
import io.hexaglue.engine.KindEvidence;
import io.hexaglue.engine.Relation;
import io.hexaglue.engine.RelationKind;
import io.hexaglue.engine.RuleSet;
import io.hexaglue.engine.Saturation;
import io.hexaglue.engine.Verdicts;
import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.Modifier;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.Candidate;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.declaration.Annotation;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LookupIdentityTest {

    private static final TypeId PORT = TypeId.of("com.acme.Ledger");
    private static final TypeId AGGREGATE = TypeId.of("com.acme.Fleet");
    private static final TypeId TAG = TypeId.of("com.acme.FleetTag");
    private static final TypeId BERTH = TypeId.of("com.acme.Berth");
    private static final TypeId MANIFEST = TypeId.of("com.acme.Manifest");
    private static final TypeId CALLER = TypeId.of("com.acme.Checkout");
    private static final TypeRef AGGREGATE_REF = TypeRef.of(AGGREGATE.qualifiedName());
    private static final TypeRef TAG_REF = TypeRef.of(TAG.qualifiedName());
    private static final TypeRef BERTH_REF = TypeRef.of(BERTH.qualifiedName());
    private static final TypeRef MANIFEST_REF = TypeRef.of(MANIFEST.qualifiedName());
    private static final TypeRef TEXT = TypeRef.of("java.lang.String");
    private static final TypeRef VOID = TypeRef.of("void");

    /** A way out whose signatures converge on the aggregate, searched by the given keys. */
    private static TypeNode storage(TypeRef... keys) {
        List<Method> methods = Stream.concat(
                        Stream.of(keys)
                                .map(key -> Method.builder(
                                                "locateBy" + key.simpleName(),
                                                TypeRef.parameterized("java.util.Optional", AGGREGATE_REF))
                                        .parameters(List.of(Parameter.of("key", key)))
                                        .build()),
                        Stream.of(Method.builder("keep", VOID)
                                .parameters(List.of(Parameter.of("subject", AGGREGATE_REF)))
                                .build()))
                .toList();
        return TypeNode.builder(PORT, TypeNature.INTERFACE).methods(methods).build();
    }

    /** The aggregate, keeping whichever values the fixture hands it, plus state that can change. */
    private static TypeNode aggregate(TypeRef... kept) {
        List<Field> fields = Stream.concat(
                        Stream.of(Field.of("reference", TEXT)),
                        Stream.of(kept)
                                .map(type -> Field.builder("kept" + type.simpleName(), type)
                                        .modifiers(Set.of(Modifier.FINAL))
                                        .build()))
                .toList();
        return TypeNode.builder(AGGREGATE, TypeNature.CLASS).fields(fields).build();
    }

    /** The same subject, with the author saying it is a value whatever the way out does with it. */
    private static TypeNode declaredValue(TypeRef... kept) {
        return TypeNode.builder(AGGREGATE, TypeNature.CLASS)
                .annotations(List.of(Annotation.of("org.jmolecules.ddd.annotation.ValueObject")))
                .fields(aggregate(kept).fields())
                .build();
    }

    /** A way out that takes one value without ever answering with the aggregate, and searches by another. */
    private static TypeNode forgetful() {
        return TypeNode.builder(PORT, TypeNature.INTERFACE)
                .methods(List.of(
                        Method.builder("forget", VOID)
                                .parameters(List.of(Parameter.of("key", TAG_REF)))
                                .build(),
                        Method.builder("locateByBerth", TypeRef.parameterized("java.util.Optional", AGGREGATE_REF))
                                .parameters(List.of(Parameter.of("key", BERTH_REF)))
                                .build(),
                        Method.builder("keep", VOID)
                                .parameters(List.of(Parameter.of("subject", AGGREGATE_REF)))
                                .build()))
                .build();
    }

    /** Two values in one declaration: immutable, and nothing like the shape of an identity. */
    private static TypeNode pair(TypeId id) {
        return TypeNode.builder(id, TypeNature.RECORD)
                .fields(List.of(
                        Field.builder("code", TEXT)
                                .modifiers(Set.of(Modifier.FINAL))
                                .build(),
                        Field.builder("weight", TEXT)
                                .modifiers(Set.of(Modifier.FINAL))
                                .build()))
                .build();
    }

    /** A value wrapped around exactly one thing: the shape an identity is written in. */
    private static TypeNode wrapper(TypeId id) {
        return TypeNode.builder(id, TypeNature.RECORD)
                .fields(List.of(Field.builder("value", TEXT)
                        .modifiers(Set.of(Modifier.FINAL))
                        .build()))
                .build();
    }

    private static TypeNode caller() {
        return TypeNode.builder(CALLER, TypeNature.CLASS)
                .fields(List.of(Field.of("ledger", TypeRef.of(PORT.qualifiedName()))))
                .build();
    }

    private static EngineContext context(TypeNode... types) {
        CodeModel.Builder code = CodeModel.builder();
        for (TypeNode type : types) {
            code.addType(type);
        }
        return EngineContext.of(code.build(), KnowledgePacks.embedded(), HexaGlueConfig.defaults());
    }

    private static Verdicts verdicts(TypeNode... types) {
        return Classifier.classify(context(types));
    }

    /** The facts as they stand once the verdicts have settled, which is what a consumer reads. */
    private static FactBase settled(TypeNode... types) {
        EngineContext context = context(types);
        return Saturation.saturate(RuleSet.standard(), context.withVerdicts(Classifier.classify(context)));
    }

    @Nested
    @DisplayName("reads the key a way out searches an aggregate by as its identity")
    class ReadsTheKey {

        @Test
        @DisplayName("when the aggregate keeps it and the way out takes it to answer with the aggregate")
        void whenTheAggregateKeepsItAndTheWayOutTakesIt() {
            Verdicts settled = verdicts(storage(TAG_REF), caller(), aggregate(TAG_REF), wrapper(TAG));

            assertThat(settled.kindOf(AGGREGATE)).contains(ArchKind.AGGREGATE_ROOT);
            assertThat(settled.kindOf(TAG)).contains(ArchKind.IDENTIFIER);
        }

        @Test
        @DisplayName("and states the tie, so nothing downstream has to guess which type carries it")
        void andStatesTheTie() {
            FactBase facts = settled(storage(TAG_REF), caller(), aggregate(TAG_REF), wrapper(TAG));

            assertThat(facts.about(AGGREGATE, Relation.class))
                    .extracting(Relation::kind, Relation::object)
                    .contains(tuple(RelationKind.IDENTIFIED_BY, TAG));
        }

        @Test
        @DisplayName("without that tie becoming a lifecycle of its own")
        void withoutThatTieBecomingALifecycle() {
            // Saying that an aggregate is identified by a value is not saying that a way out keeps
            // the value: only the tie stating storage gives anything a lifecycle.
            FactBase facts = settled(storage(TAG_REF), caller(), aggregate(TAG_REF), wrapper(TAG));

            assertThat(facts.about(TAG, KindEvidence.class))
                    .extracting(KindEvidence::kind)
                    .doesNotContain(ArchKind.AGGREGATE_ROOT);
        }

        @Test
        @DisplayName("settling the duel the shape alone cannot, and leaving the other wrapper undecided")
        void settlingTheDuelTheShapeAloneCannot() {
            // Both records wrap one value, so the local shape reads each of them as an identity and
            // as a value at once. Only the one the way out searches by is settled here; the other
            // keeps both readings, which is the honest answer.
            Verdicts settled =
                    verdicts(storage(TAG_REF), caller(), aggregate(TAG_REF, BERTH_REF), wrapper(TAG), wrapper(BERTH));

            assertThat(settled.kindOf(TAG)).contains(ArchKind.IDENTIFIER);
            assertThat(settled.kindOf(BERTH)).contains(ArchKind.UNCLASSIFIED);
            assertThat(settled.verdict(BERTH).orElseThrow().candidates())
                    .extracting(Candidate::kind)
                    .containsExactlyInAnyOrder(ArchKind.IDENTIFIER, ArchKind.VALUE_OBJECT);
        }
    }

    @Nested
    @DisplayName("says nothing")
    class SaysNothing {

        @Test
        @DisplayName("about a key the aggregate does not keep")
        void aboutAKeyTheAggregateDoesNotKeep() {
            // Something the way out is handed but the aggregate never holds is a search criterion,
            // not the identity of anything.
            Verdicts settled = verdicts(storage(BERTH_REF), caller(), aggregate(TAG_REF), wrapper(TAG), wrapper(BERTH));

            assertThat(settled.kindOf(AGGREGATE)).contains(ArchKind.AGGREGATE_ROOT);
            assertThat(settled.kindOf(BERTH)).contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("when two keys the aggregate keeps both answer with it")
        void whenTwoKeysBothAnswerWithIt() {
            // Electing one of them would be a coin flip, and an aggregate with two identities is a
            // question for the report rather than an answer for the model.
            Verdicts settled = verdicts(
                    storage(TAG_REF, BERTH_REF), caller(), aggregate(TAG_REF, BERTH_REF), wrapper(TAG), wrapper(BERTH));

            assertThat(settled.kindOf(TAG)).contains(ArchKind.UNCLASSIFIED);
            assertThat(settled.kindOf(BERTH)).contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about an attribute a way out searches by that is not written as an identity")
        void aboutAnAttributeThatIsNotWrittenAsAnIdentity() {
            // A way out may well search by a name or a date the aggregate holds. Being searched by
            // is half the reading; the other half is being written the way an identity is written,
            // and without it the aggregate would look as though it had two identities.
            Verdicts settled = verdicts(
                    storage(TAG_REF, MANIFEST_REF),
                    caller(),
                    aggregate(TAG_REF, MANIFEST_REF),
                    wrapper(TAG),
                    pair(MANIFEST));

            assertThat(settled.kindOf(TAG)).contains(ArchKind.IDENTIFIER);
            assertThat(settled.kindOf(MANIFEST)).contains(ArchKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("about a key handed to a method that answers with something else")
        void aboutAKeyHandedToAMethodThatAnswersWithSomethingElse() {
            // Handing a value over is not searching by it. Only the methods that answer with the
            // aggregate are searches for it, so a method that merely takes the value says nothing.
            Verdicts settled =
                    verdicts(forgetful(), caller(), aggregate(TAG_REF, BERTH_REF), wrapper(TAG), wrapper(BERTH));

            assertThat(settled.kindOf(BERTH)).contains(ArchKind.IDENTIFIER);
            assertThat(settled.kindOf(TAG)).contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about the key of a type no way out established as an aggregate")
        void aboutTheKeyOfATypeThatIsNotAnAggregate() {
            Verdicts settled = verdicts(storage(TAG_REF), aggregate(TAG_REF), wrapper(TAG));

            assertThat(settled.kindOf(AGGREGATE)).contains(ArchKind.UNCLASSIFIED);
            assertThat(settled.kindOf(TAG)).contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about the key of a subject the author declared to be something else")
        void aboutTheKeyOfASubjectDeclaredToBeSomethingElse() {
            // A way out keeping a type is a reason to read it as an aggregate, and the author
            // saying otherwise outranks it. What the identity rule needs is the aggregate settled,
            // not merely a way out that stores.
            Verdicts settled = verdicts(storage(TAG_REF), caller(), declaredValue(TAG_REF), wrapper(TAG));

            assertThat(settled.kindOf(AGGREGATE)).contains(ArchKind.VALUE_OBJECT);
            assertThat(settled.kindOf(TAG)).contains(ArchKind.UNCLASSIFIED);
        }
    }
}
