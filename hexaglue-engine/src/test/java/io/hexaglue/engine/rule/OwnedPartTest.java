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
import io.hexaglue.model.classification.Candidate;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.declaration.Annotation;
import io.hexaglue.model.declaration.Field;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * What an aggregate is made of, read from the two rules that share the reading: a part carrying an
 * identity of its own and a part carrying none are one composition seen twice.
 */
class OwnedPartTest {

    private static final TypeId OWNER = TypeId.of("com.acme.Fleet");
    private static final TypeId TAG = TypeId.of("com.acme.FleetTag");
    private static final TypeId MANIFEST = TypeId.of("com.acme.Manifest");
    private static final TypeId HULL = TypeId.of("com.acme.Hull");
    private static final TypeId HULL_TAG = TypeId.of("com.acme.HullTag");
    private static final TypeId BERTH = TypeId.of("com.acme.Berth");
    private static final TypeId CONTRACT = TypeId.of("com.acme.Ledger");
    private static final TypeRef TEXT = TypeRef.of("java.lang.String");
    private static final String AGGREGATE_ROOT = "org.jmolecules.ddd.annotation.AggregateRoot";
    private static final String IDENTIFIER = "org.jmolecules.ddd.types.Identifier";

    /** An aggregate the author declared, so composition is read without a way out in the fixture. */
    private static TypeNode owner(Field... kept) {
        return TypeNode.builder(OWNER, TypeNature.CLASS)
                .annotations(List.of(Annotation.of(AGGREGATE_ROOT)))
                .fields(List.of(kept))
                .build();
    }

    /** The same declaration without the annotation: nothing places it, so it owns nothing. */
    private static TypeNode unplacedOwner(Field... kept) {
        return TypeNode.builder(OWNER, TypeNature.CLASS).fields(List.of(kept)).build();
    }

    private static Field keeps(String name, TypeId type) {
        return Field.of(name, TypeRef.of(type.qualifiedName()));
    }

    private static Field keepsMany(String name, TypeId element) {
        return Field.of(name, TypeRef.parameterized("java.util.List", TypeRef.of(element.qualifiedName())));
    }

    /** State that can change, which Q2 may object to and Q1 reads as a value all the same. */
    private static TypeNode mutable(TypeId id) {
        return TypeNode.builder(id, TypeNature.CLASS)
                .fields(List.of(Field.of("code", TEXT)))
                .build();
    }

    /** A part keeping something the author declared an identity, plus whatever else it keeps. */
    private static TypeNode identified(Field alsoKept) {
        return TypeNode.builder(HULL, TypeNature.CLASS)
                .fields(List.of(keeps("tag", HULL_TAG), alsoKept))
                .build();
    }

    /** A value wrapped around exactly one thing: the shape an identity is written in. */
    private static TypeNode wrapper(TypeId id, TypeRef... implemented) {
        return TypeNode.builder(id, TypeNature.RECORD)
                .interfaces(List.of(implemented))
                .fields(List.of(Field.builder("value", TEXT)
                        .modifiers(Set.of(Modifier.FINAL))
                        .build()))
                .build();
    }

    private static EngineContext context(TypeNode... types) {
        CodeModel.Builder code = CodeModel.builder();
        for (TypeNode type : types) {
            code.addType(type);
        }
        code.addType(wrapper(HULL_TAG, TypeRef.of(IDENTIFIER)));
        code.addType(TypeNode.externalStub(TypeId.of(IDENTIFIER), TypeNature.INTERFACE));
        code.supertypes(HULL_TAG, List.of(TypeId.of(IDENTIFIER)));
        return EngineContext.of(code.build(), KnowledgePacks.embedded(), HexaGlueConfig.defaults());
    }

    private static Verdicts verdicts(TypeNode... types) {
        return Classifier.classify(context(types));
    }

    /** The ties held once the verdicts have settled, which is where the model reads them. */
    private static List<Relation> ownerships(TypeNode... types) {
        EngineContext context = context(types);
        return Saturation.saturate(RuleSet.standard(), context.withVerdicts(Classifier.classify(context)))
                .all(Relation.class)
                .stream()
                .filter(relation -> relation.kind() == RelationKind.OWNS)
                .toList();
    }

    @Nested
    @DisplayName("reads what an aggregate is made of")
    class ReadsWhatAnAggregateIsMadeOf {

        @Test
        @DisplayName("as an entity, when the part carries an identity of its own")
        void anEntityWhenThePartCarriesAnIdentity() {
            Verdicts settled = verdicts(owner(keepsMany("hulls", HULL)), identified(Field.of("code", TEXT)));

            assertThat(settled.kindOf(HULL_TAG)).contains(ArchKind.IDENTIFIER);
            assertThat(settled.kindOf(HULL)).contains(ArchKind.ENTITY);
        }

        @Test
        @DisplayName("as a value, when it carries none — even when its state can change")
        void aValueWhenItCarriesNone() {
            // Mutability is what Q2 objects to and never what Q1 refuses to read: an aggregate part
            // with setters everywhere is still a part, and hiding it would leave nothing to audit.
            Verdicts settled = verdicts(owner(keeps("manifest", MANIFEST)), mutable(MANIFEST));

            assertThat(settled.kindOf(MANIFEST)).contains(ArchKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("through a collection, which is how one aggregate keeps many of a part")
        void throughACollection() {
            Verdicts settled = verdicts(owner(keepsMany("manifests", MANIFEST)), mutable(MANIFEST));

            assertThat(settled.kindOf(MANIFEST)).contains(ArchKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("and on down, from a part that is an entity to what that entity keeps")
        void andOnDownFromAPartThatIsAnEntity() {
            Verdicts settled = verdicts(
                    owner(keepsMany("hulls", HULL)), identified(keeps("manifest", MANIFEST)), mutable(MANIFEST));

            assertThat(settled.kindOf(HULL)).contains(ArchKind.ENTITY);
            assertThat(settled.kindOf(MANIFEST)).contains(ArchKind.VALUE_OBJECT);
        }
    }

    @Nested
    @DisplayName("says nothing")
    class SaysNothing {

        @Test
        @DisplayName("about a value shaped like an identity, whose duel belongs to the way out")
        void aboutAValueShapedLikeAnIdentity() {
            // Composition cannot tell the identity of an aggregate from a plain value it keeps —
            // both are fields. Settling that by position would decide the duel with no evidence.
            Verdicts settled = verdicts(owner(keeps("tag", TAG)), wrapper(TAG));

            assertThat(settled.kindOf(TAG)).contains(ArchKind.UNCLASSIFIED);
            assertThat(settled.verdict(TAG).orElseThrow().candidates())
                    .extracting(Candidate::kind)
                    .containsExactlyInAnyOrder(ArchKind.IDENTIFIER, ArchKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("about a type no aggregate keeps, whatever the same declaration reads as when kept")
        void aboutATypeNoAggregateKeeps() {
            Verdicts settled = verdicts(owner(keeps("manifest", MANIFEST)), mutable(MANIFEST), mutable(BERTH));

            assertThat(settled.kindOf(MANIFEST)).contains(ArchKind.VALUE_OBJECT);
            assertThat(settled.kindOf(BERTH)).contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about what a type nothing has placed keeps")
        void aboutWhatAnUnplacedTypeKeeps() {
            Verdicts settled = verdicts(unplacedOwner(keeps("manifest", MANIFEST)), mutable(MANIFEST));

            assertThat(settled.kindOf(OWNER)).contains(ArchKind.UNCLASSIFIED);
            assertThat(settled.kindOf(MANIFEST)).contains(ArchKind.UNCLASSIFIED);
        }

        @Test
        @DisplayName("about a type keeping another of its own kind, which is not made of itself")
        void aboutATypeKeepingAnotherOfItsOwnKind() {
            // A part pointing at the next one of its kind is an ordinary shape, and reading it as
            // composition would have the entity be a value inside itself.
            Verdicts settled = verdicts(owner(keepsMany("hulls", HULL)), identified(keeps("next", HULL)));

            assertThat(settled.kindOf(HULL)).contains(ArchKind.ENTITY);
        }

        @Test
        @DisplayName("about a contract an aggregate keeps, because a contract is nobody's part")
        void aboutAContractAnAggregateKeeps() {
            // Holding a way out is a layering question, and answering it by calling the contract a
            // value would erase the boundary instead of reporting it.
            Verdicts settled = verdicts(
                    owner(keeps("ledger", CONTRACT)),
                    TypeNode.builder(CONTRACT, TypeNature.INTERFACE).build());

            assertThat(settled.kindOf(CONTRACT)).contains(ArchKind.DRIVEN_PORT);
        }
    }

    @Nested
    @DisplayName("states the composition as a tie")
    class StatesTheCompositionAsATie {

        @Test
        @DisplayName("from the owner to each of its parts, one carrying an identity and one not")
        void fromTheOwnerToEachOfItsParts() {
            // Saying that a part is a value is not saying whose part it is, and a generator writing
            // a mapping needs the owner, not the kind alone.
            List<Relation> ties = ownerships(
                    owner(keepsMany("hulls", HULL), keeps("manifest", MANIFEST)),
                    identified(Field.of("code", TEXT)),
                    mutable(MANIFEST));

            assertThat(ties)
                    .extracting(Relation::subject, Relation::object)
                    .containsExactlyInAnyOrder(tuple(OWNER, HULL), tuple(OWNER, MANIFEST));
        }

        @Test
        @DisplayName("and on down, from a part that owns parts of its own")
        void andOnDownFromAPartThatOwnsPartsOfItsOwn() {
            List<Relation> ties = ownerships(
                    owner(keepsMany("hulls", HULL)), identified(keeps("manifest", MANIFEST)), mutable(MANIFEST));

            assertThat(ties)
                    .extracting(Relation::subject, Relation::object)
                    .containsExactlyInAnyOrder(tuple(OWNER, HULL), tuple(HULL, MANIFEST));
        }

        @Test
        @DisplayName("never about what the composition leaves alone")
        void neverAboutWhatTheCompositionLeavesAlone() {
            List<Relation> ties = ownerships(
                    owner(keeps("tag", TAG), keeps("ledger", CONTRACT)),
                    wrapper(TAG),
                    TypeNode.builder(CONTRACT, TypeNature.INTERFACE).build());

            assertThat(ties).isEmpty();
        }
    }
}
