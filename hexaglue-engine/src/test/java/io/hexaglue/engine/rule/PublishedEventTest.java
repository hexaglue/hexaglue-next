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

class PublishedEventTest {

    private static final TypeId PORT = TypeId.of("com.acme.Wire");
    private static final TypeId CALLER = TypeId.of("com.acme.Checkout");
    private static final TypeId AGGREGATE = TypeId.of("com.acme.Fleet");
    private static final TypeId SAILING = TypeId.of("com.acme.Sailing");
    private static final TypeId MANIFEST = TypeId.of("com.acme.Manifest");
    private static final TypeId MARKER = TypeId.of("com.acme.Notice");
    private static final TypeRef TEXT = TypeRef.of("java.lang.String");
    private static final TypeRef VOID = TypeRef.of("void");
    private static final String AGGREGATE_ROOT = "org.jmolecules.ddd.annotation.AggregateRoot";

    /** A way out that only ever goes one way, carrying values: how something is announced. */
    private static TypeNode announcer(TypeId... carried) {
        return TypeNode.builder(PORT, TypeNature.INTERFACE)
                .methods(Stream.of(carried)
                        .map(type -> Method.builder("announce" + type.simpleName(), VOID)
                                .parameters(List.of(Parameter.of("what", TypeRef.of(type.qualifiedName()))))
                                .build())
                        .toList())
                .build();
    }

    private static TypeNode caller() {
        return TypeNode.builder(CALLER, TypeNature.CLASS)
                .fields(List.of(Field.of("wire", TypeRef.of(PORT.qualifiedName()))))
                .build();
    }

    /** An aggregate the author declared, answering with whatever the fixture hands it. */
    private static TypeNode aggregate(List<Field> kept, List<Method> answers) {
        return TypeNode.builder(AGGREGATE, TypeNature.CLASS)
                .annotations(List.of(Annotation.of(AGGREGATE_ROOT)))
                .fields(kept)
                .methods(answers)
                .build();
    }

    /** Something that happened: state, and none of it can change afterwards. */
    private static TypeNode immutable(TypeId id) {
        return TypeNode.builder(id, TypeNature.RECORD)
                .fields(List.of(
                        Field.builder("reference", TEXT)
                                .modifiers(Set.of(Modifier.FINAL))
                                .build(),
                        Field.builder("moment", TEXT)
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

    private static TypeNode mutable(TypeId id) {
        return TypeNode.builder(id, TypeNature.CLASS)
                .fields(List.of(Field.of("reference", TEXT)))
                .build();
    }

    private static Method answers(String name, TypeId with) {
        return Method.of(name, TypeRef.of(with.qualifiedName()));
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

    /** The ties held once the verdicts have settled, which is where the model reads them. */
    private static List<Relation> announcements(TypeNode... types) {
        EngineContext context = context(types);
        return Saturation.saturate(RuleSet.standard(), context.withVerdicts(Classifier.classify(context)))
                .all(Relation.class)
                .stream()
                .filter(relation -> relation.kind() == RelationKind.ANNOUNCES)
                .toList();
    }

    @Nested
    @DisplayName("reads what the domain announces as the event it is")
    class ReadsWhatTheDomainAnnounces {

        @Test
        @DisplayName("when a way out carries it outward and never answers back")
        void whenAWayOutCarriesItOutward() {
            Verdicts settled = verdicts(announcer(SAILING), caller(), immutable(SAILING));

            assertThat(settled.kindOf(PORT)).contains(ArchKind.DRIVEN_PORT);
            assertThat(settled.kindOf(SAILING)).contains(ArchKind.DOMAIN_EVENT);
        }

        @Test
        @DisplayName("when an aggregate answers with it and keeps nothing of the sort")
        void whenAnAggregateAnswersWithIt() {
            // A method handing back something the aggregate does not hold is not a reader of its
            // state: it returns what just happened.
            Verdicts settled = verdicts(
                    aggregate(List.of(Field.of("reference", TEXT)), List.of(answers("sail", SAILING))),
                    immutable(SAILING));

            assertThat(settled.kindOf(SAILING)).contains(ArchKind.DOMAIN_EVENT);
        }
    }

    @Nested
    @DisplayName("says nothing")
    class SaysNothing {

        @Test
        @DisplayName("about what an aggregate answers with and keeps, which is a reader of its state")
        void aboutWhatAnAggregateKeeps() {
            Verdicts settled = verdicts(
                    aggregate(
                            List.of(Field.of("manifest", TypeRef.of(MANIFEST.qualifiedName()))),
                            List.of(answers("manifest", MANIFEST))),
                    immutable(MANIFEST));

            assertThat(settled.kindOf(MANIFEST)).contains(ArchKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("about a value an aggregate answers with and keeps, whatever its shape")
        void aboutAValueAnAggregateAnswersWithAndKeeps() {
            // The most common accessor of all hands back the identity, and nothing about an
            // identity has happened. Whether the answer is part of the state is the whole test:
            // a wrapper kept and handed back keeps both its readings and stays undecided.
            Verdicts settled = verdicts(
                    aggregate(
                            List.of(Field.of("tag", TypeRef.of(MANIFEST.qualifiedName()))),
                            List.of(answers("tag", MANIFEST))),
                    wrapper(MANIFEST));

            assertThat(settled.kindOf(MANIFEST)).contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about a marker interface, which holds no state to have happened")
        void aboutAMarkerInterface() {
            Verdicts settled = verdicts(
                    announcer(MARKER),
                    caller(),
                    TypeNode.builder(MARKER, TypeNature.INTERFACE).build());

            assertThat(settled.kindOf(MARKER)).contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about something handed over that can still change, which is a request")
        void aboutSomethingThatCanStillChange() {
            Verdicts settled = verdicts(announcer(MANIFEST), caller(), mutable(MANIFEST));

            assertThat(settled.kindOf(MANIFEST)).contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about what a way out asking a question carries")
        void aboutWhatAWayOutAskingAQuestionCarries() {
            // A gateway hands values over too, and waits for an answer. Waiting for an answer is
            // what tells a question from an announcement.
            TypeNode gateway = TypeNode.builder(PORT, TypeNature.INTERFACE)
                    .methods(List.of(Method.builder("submit", TEXT)
                            .parameters(List.of(Parameter.of("what", TypeRef.of(SAILING.qualifiedName()))))
                            .build()))
                    .build();

            Verdicts settled = verdicts(gateway, caller(), immutable(SAILING));

            assertThat(settled.kindOf(PORT)).contains(ArchKind.DRIVEN_PORT);
            assertThat(settled.kindOf(SAILING)).contains(ArchKind.VALUE_OBJECT);
        }
    }

    @Nested
    @DisplayName("states what the domain announces as a tie")
    class StatesWhatTheDomainAnnouncesAsATie {

        @Test
        @DisplayName("from the aggregate that answers with it, which is where the event comes from")
        void fromTheAggregateThatAnswersWithIt() {
            List<Relation> ties = announcements(
                    aggregate(List.of(Field.of("reference", TEXT)), List.of(answers("sail", SAILING))),
                    immutable(SAILING));

            assertThat(ties).extracting(Relation::subject, Relation::object).containsExactly(tuple(AGGREGATE, SAILING));
        }

        @Test
        @DisplayName("and from nobody when what the aggregate answers with can still change")
        void andFromNobodyWhenTheAnswerCanStillChange() {
            // Nothing was announced, so nothing announced it: the tie has to fall with the reading
            // rather than outlive it.
            assertThat(announcements(
                            aggregate(List.of(Field.of("reference", TEXT)), List.of(answers("sail", SAILING))),
                            mutable(SAILING)))
                    .isEmpty();
        }

        @Test
        @DisplayName("and from nobody when a way out is the one carrying it outward")
        void andFromNobodyWhenAWayOutCarriesIt() {
            // A port announcing an event says that it leaves, not where it came from, and naming a
            // source the sources do not name would be an invention.
            assertThat(announcements(announcer(SAILING), caller(), immutable(SAILING)))
                    .isEmpty();
        }
    }
}
