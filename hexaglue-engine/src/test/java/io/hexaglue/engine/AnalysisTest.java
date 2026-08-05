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

package io.hexaglue.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import io.hexaglue.knowledge.KnowledgePacks;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.Modifier;
import io.hexaglue.model.SourceLocation;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.AggregateRoot;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.DomainEvent;
import io.hexaglue.model.arch.DomainService;
import io.hexaglue.model.arch.DrivenAdapter;
import io.hexaglue.model.arch.DrivenPort;
import io.hexaglue.model.arch.DrivenPortType;
import io.hexaglue.model.arch.DrivingAdapter;
import io.hexaglue.model.arch.DrivingPort;
import io.hexaglue.model.arch.Entity;
import io.hexaglue.model.arch.Identifier;
import io.hexaglue.model.arch.ModuleTopology;
import io.hexaglue.model.arch.UnclassifiedType;
import io.hexaglue.model.arch.UnclassifiedType.UnclassifiedCategory;
import io.hexaglue.model.arch.UseCase;
import io.hexaglue.model.arch.ValueObject;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.classification.RemediationAction;
import io.hexaglue.model.classification.RemediationHint;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.CodeModelCapability;
import io.hexaglue.model.code.MethodBodyFacts;
import io.hexaglue.model.code.MethodBodyFacts.Instantiation;
import io.hexaglue.model.code.MethodBodyFacts.Invocation;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.HexaGlueConfig;
import io.hexaglue.model.declaration.Annotation;
import io.hexaglue.model.declaration.Constructor;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.FieldRole;
import io.hexaglue.model.declaration.Method;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * What the model says once the verdicts have settled: every type of the perimeter present, each in
 * the record its kind calls for, filled from the ties the rules stated and from the declaration
 * itself.
 */
class AnalysisTest {

    private static final TypeId FLEET = TypeId.of("com.acme.hangar.core.Fleet");
    private static final TypeId FLEET_TAG = TypeId.of("com.acme.hangar.core.FleetTag");
    private static final TypeId HULL = TypeId.of("com.acme.hangar.core.Hull");
    private static final TypeId HULL_TAG = TypeId.of("com.acme.hangar.core.HullTag");
    private static final TypeId DECK = TypeId.of("com.acme.hangar.core.Deck");
    private static final TypeId DECK_TAG = TypeId.of("com.acme.hangar.core.DeckTag");
    private static final TypeId MANIFEST = TypeId.of("com.acme.hangar.core.Manifest");
    private static final TypeId SAILING = TypeId.of("com.acme.hangar.core.Sailing");
    private static final TypeId BOOKS = TypeId.of("com.acme.hangar.core.HangarBooks");
    private static final TypeId BERTHING = TypeId.of("com.acme.hangar.core.Berthing");
    private static final TypeId DISPATCH = TypeId.of("com.acme.hangar.core.Dispatch");
    private static final TypeId FERRYING = TypeId.of("com.acme.hangar.core.Ferrying");
    private static final TypeId CHARTERING = TypeId.of("com.acme.hangar.core.Chartering");
    private static final TypeId MOORING = TypeId.of("com.acme.hangar.core.Mooring");
    private static final TypeId DESK = TypeId.of("com.acme.hangar.core.MooringDesk");
    private static final TypeId GATE = TypeId.of("com.acme.hangar.web.HangarGate");
    private static final TypeId LEDGER = TypeId.of("com.acme.hangar.store.Ledger");
    private static final TypeId DOOR = TypeId.of("com.acme.hangar.web.HangarDoor");
    private static final TypeId RECORDING = TypeId.of("com.acme.hangar.store.Recording");
    private static final TypeId OUTER = TypeId.of("com.acme.hangar.core.Berth");
    private static final TypeId INNER = TypeId.of("com.acme.hangar.core.Berth$Slot");
    private static final TypeId BASE = TypeId.of("com.acme.hangar.core.Moorage");

    private static final TypeRef TEXT = TypeRef.of("java.lang.String");
    private static final TypeRef VOID = TypeRef.of("void");
    private static final TypeId JMOLECULES_IDENTIFIER = TypeId.of("org.jmolecules.ddd.types.Identifier");
    private static final String JMOLECULES_SERVICE = "org.jmolecules.ddd.annotation.Service";
    private static final String REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController";
    private static final TypeId ENTITY_MANAGER = TypeId.of("jakarta.persistence.EntityManager");

    private static TypeRef ref(TypeId id) {
        return TypeRef.of(id.qualifiedName());
    }

    private static Field keeps(String name, TypeId type) {
        return Field.of(name, ref(type));
    }

    private static Field keepsMany(String name, TypeId element) {
        return Field.of(name, TypeRef.parameterized("java.util.List", ref(element)));
    }

    private static Field frozen(String name, TypeRef type) {
        return Field.builder(name, type).modifiers(Set.of(Modifier.FINAL)).build();
    }

    private static Method takes(String name, TypeRef answer, TypeId argument) {
        return Method.builder(name, answer)
                .parameters(List.of(Parameter.of("what", ref(argument))))
                .build();
    }

    /**
     * A whole hexagon: an entry point on one side, storage on the other, and between them a domain
     * nothing declares — every verdict here is reached from a relation.
     */
    private static CodeModel hexagon() {
        return hangar().build();
    }

    private static CodeModel.Builder hangar() {
        return CodeModel.builder()
                // The identity comes last on purpose: what carries it is read from the tie, never
                // from the position of a field among its neighbours.
                .addType(TypeNode.builder(FLEET, TypeNature.CLASS)
                        .fields(List.of(keepsMany("hulls", HULL), keeps("manifest", MANIFEST), keeps("tag", FLEET_TAG)))
                        .methods(List.of(Method.of("sail", ref(SAILING))))
                        .build())
                // The constant belongs to the class, not to any instance of it, so it is not part
                // of what the identity is written around.
                .addType(TypeNode.builder(FLEET_TAG, TypeNature.RECORD)
                        .fields(List.of(
                                Field.builder("NONE", TEXT)
                                        .modifiers(Set.of(Modifier.STATIC, Modifier.FINAL))
                                        .build(),
                                frozen("value", TEXT)))
                        .build())
                .addType(TypeNode.builder(HULL, TypeNature.CLASS)
                        .fields(List.of(Field.of("code", TEXT), keeps("tag", HULL_TAG), keeps("deck", DECK)))
                        .build())
                .addType(TypeNode.builder(HULL_TAG, TypeNature.RECORD)
                        .interfaces(List.of(ref(JMOLECULES_IDENTIFIER)))
                        .fields(List.of(frozen("value", TEXT)))
                        .build())
                .addType(TypeNode.builder(DECK, TypeNature.CLASS)
                        .fields(List.of(keeps("tag", DECK_TAG)))
                        .build())
                .addType(TypeNode.builder(DECK_TAG, TypeNature.RECORD)
                        .interfaces(List.of(ref(JMOLECULES_IDENTIFIER)))
                        .fields(List.of(frozen("hull", TEXT), frozen("level", TEXT)))
                        .build())
                .addType(TypeNode.builder(MANIFEST, TypeNature.CLASS)
                        .fields(List.of(Field.of("note", TEXT)))
                        .build())
                .addType(TypeNode.builder(SAILING, TypeNature.RECORD)
                        .fields(List.of(frozen("reference", TEXT), frozen("moment", TEXT)))
                        .build())
                .addType(TypeNode.builder(BOOKS, TypeNature.INTERFACE)
                        .methods(List.of(takes("find", ref(FLEET), FLEET_TAG), takes("keep", VOID, FLEET)))
                        .build())
                .addType(TypeNode.builder(BERTHING, TypeNature.INTERFACE)
                        .methods(List.of(takes("berth", VOID, FLEET_TAG), takes("sail", ref(SAILING), FLEET_TAG)))
                        .build())
                .addType(TypeNode.builder(DISPATCH, TypeNature.CLASS)
                        .interfaces(List.of(ref(BERTHING)))
                        .fields(List.of(keeps("books", BOOKS), keeps("ferrying", FERRYING)))
                        .build())
                .addType(TypeNode.builder(FERRYING, TypeNature.CLASS)
                        .methods(List.of(Method.builder("plan", ref(SAILING))
                                .parameters(
                                        List.of(Parameter.of("fleet", ref(FLEET)), Parameter.of("of", ref(MANIFEST))))
                                .build()))
                        .build())
                // Each of the three below keeps or answers one thing that is a way out and one that
                // is not, so what is listed is what was filtered rather than what was there.
                .addType(TypeNode.builder(CHARTERING, TypeNature.CLASS)
                        .annotations(List.of(Annotation.of(JMOLECULES_SERVICE)))
                        .fields(List.of(keeps("books", BOOKS), keeps("manifest", MANIFEST)))
                        .methods(List.of(takes("charter", VOID, FLEET)))
                        .build())
                .addType(TypeNode.builder(LEDGER, TypeNature.CLASS)
                        .interfaces(List.of(ref(BOOKS), ref(RECORDING)))
                        .constructors(List.of(Constructor.of(List.of(Parameter.of("entities", ref(ENTITY_MANAGER))))))
                        .build())
                .addType(TypeNode.builder(RECORDING, TypeNature.INTERFACE).build())
                .addType(TypeNode.builder(DOOR, TypeNature.CLASS)
                        .annotations(List.of(Annotation.of(REST_CONTROLLER)))
                        .fields(List.of(keeps("berthing", BERTHING), keeps("manifest", MANIFEST)))
                        .build())
                .addType(TypeNode.externalStub(JMOLECULES_IDENTIFIER, TypeNature.INTERFACE))
                .addType(TypeNode.externalStub(ENTITY_MANAGER, TypeNature.INTERFACE))
                .supertypes(HULL_TAG, List.of(JMOLECULES_IDENTIFIER))
                .supertypes(DECK_TAG, List.of(JMOLECULES_IDENTIFIER))
                .supertypes(DISPATCH, List.of(BERTHING))
                .supertypes(LEDGER, List.of(BOOKS, RECORDING));
    }

    /**
     * The same hangar with a second way in, whose four ways of asking about a fleet by its tag are
     * written to be indistinguishable from one another by shape alone: each takes the tag, each
     * answers with the fleet. What tells them apart is only in the bodies of the desk answering
     * them, and the bodies are read here.
     */
    private static CodeModel hangarThatChanges() {
        return hangar().capability(CodeModelCapability.METHOD_BODIES)
                .addType(TypeNode.builder(MOORING, TypeNature.INTERFACE)
                        .methods(List.of(
                                takes("look", ref(FLEET), FLEET_TAG),
                                takes("moor", ref(FLEET), FLEET_TAG),
                                takes("stow", ref(FLEET), FLEET_TAG),
                                takes("draft", ref(FLEET), FLEET_TAG)))
                        .build())
                .addType(TypeNode.builder(DESK, TypeNature.CLASS)
                        .interfaces(List.of(ref(MOORING)))
                        .fields(List.of(keeps("books", BOOKS)))
                        .build())
                .addType(TypeNode.builder(GATE, TypeNature.CLASS)
                        .annotations(List.of(Annotation.of(REST_CONTROLLER)))
                        .fields(List.of(keeps("mooring", MOORING)))
                        .build())
                .supertypes(DESK, List.of(MOORING))
                // Asks the way out for a fleet, and hands it nothing back.
                .addBodyFacts(new MethodBodyFacts(DESK, "look", List.of(new Invocation(BOOKS, "find")), List.of()))
                // Asks, then hands the fleet over: the hangar is not what it was.
                .addBodyFacts(new MethodBodyFacts(
                        DESK, "moor", List.of(new Invocation(BOOKS, "find"), new Invocation(BOOKS, "keep")), List.of()))
                // Hands the fleet over too, but through a hand of its own.
                .addBodyFacts(new MethodBodyFacts(DESK, "stow", List.of(new Invocation(DESK, "put")), List.of()))
                .addBodyFacts(new MethodBodyFacts(DESK, "put", List.of(new Invocation(BOOKS, "keep")), List.of()))
                // Builds a fleet of its own and keeps it: making one is not changing the hangar.
                .addBodyFacts(new MethodBodyFacts(
                        DESK, "draft", List.of(new Invocation(BOOKS, "find")), List.of(new Instantiation(FLEET))))
                .build();
    }

    /** A declaration written inside another one, with everything the sources say around it. */
    private static CodeModel nesting() {
        return CodeModel.builder()
                .addType(TypeNode.builder(OUTER, TypeNature.CLASS)
                        .superClass(ref(BASE))
                        .documentation("A berth, and what it holds.")
                        .sourceLocation(new SourceLocation("Berth.java", 3, 12))
                        .fields(List.of(Field.of("note", TEXT)))
                        .build())
                .addType(TypeNode.builder(INNER, TypeNature.RECORD)
                        .enclosingType(OUTER)
                        .fields(List.of(frozen("value", TEXT)))
                        .build())
                .addType(TypeNode.externalStub(BASE, TypeNature.CLASS))
                .build();
    }

    private static ArchModel modelOf(CodeModel code) {
        return Analysis.analyze(EngineContext.of(code, KnowledgePacks.embedded(), HexaGlueConfig.defaults()))
                .model();
    }

    private static <T extends ArchType> T read(ArchModel model, TypeId id, Class<T> shape) {
        return model.type(id).filter(shape::isInstance).map(shape::cast).orElseThrow();
    }

    private static List<String> names(List<TypeRef> references) {
        return references.stream().map(TypeRef::qualifiedName).toList();
    }

    @Nested
    @DisplayName("gives every type of the perimeter a place")
    class GivesEveryTypeAPlace {

        @Test
        @DisplayName("and leaves none of them out, whatever the verdict was")
        void andLeavesNoneOfThemOut() {
            ArchModel model = modelOf(hexagon());

            assertThat(model.types()).extracting(ArchType::id).contains(FLEET, BOOKS, DOOR, LEDGER);
        }

        @Test
        @DisplayName("without the stubs standing in for what the sources only reference")
        void withoutTheExternalStubs() {
            ArchModel model = modelOf(hexagon());

            assertThat(model.type(ENTITY_MANAGER)).isEmpty();
            assertThat(model.type(JMOLECULES_IDENTIFIER)).isEmpty();
        }

        @Test
        @DisplayName("carrying the verdict it was given, proof and all")
        void carryingTheVerdictItWasGiven() {
            ArchModel model = modelOf(hexagon());

            assertThat(model.classificationOf(FLEET)).map(Classification::kind).contains(ArchKind.AGGREGATE_ROOT);
            assertThat(model.explain(FLEET)).isPresent();
        }

        @Test
        @DisplayName("described as its declaration is written")
        void describedAsItsDeclarationIsWritten() {
            ArchType fleet = read(modelOf(hexagon()), FLEET, ArchType.class);

            assertThat(fleet.structure().nature()).isEqualTo(TypeNature.CLASS);
            assertThat(fleet.structure().fields()).extracting(Field::name).containsExactly("hulls", "manifest", "tag");
            assertThat(fleet.structure().methods()).extracting(Method::name).containsExactly("sail");
        }

        @Test
        @DisplayName("with what it inherits, what it documents and where it was written")
        void withWhatItInheritsAndWhereItWasWritten() {
            ArchType berth = read(modelOf(nesting()), OUTER, ArchType.class);

            assertThat(berth.structure().superClass())
                    .map(TypeRef::qualifiedName)
                    .contains(BASE.qualifiedName());
            assertThat(berth.structure().documentation()).contains("A berth, and what it holds.");
            assertThat(berth.structure().sourceLocation()).isPresent();
        }

        @Test
        @DisplayName("and with the types it encloses, which the sources only state the other way round")
        void andWithTheTypesItEncloses() {
            ArchType berth = read(modelOf(nesting()), OUTER, ArchType.class);

            assertThat(berth.structure().nestedTypes())
                    .extracting(TypeRef::qualifiedName)
                    .containsExactly(INNER.qualifiedName());
            assertThat(read(modelOf(nesting()), INNER, ArchType.class)
                            .structure()
                            .nestedTypes())
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("says of each field what the analysis reached about it")
    class SaysOfEachFieldWhatWasReached {

        private Field fieldOf(TypeId owner, String name) {
            return read(modelOf(hexagon()), owner, ArchType.class).structure().fields().stream()
                    .filter(field -> name.equals(field.name()))
                    .findFirst()
                    .orElseThrow();
        }

        @Test
        @DisplayName("unwrapping a container down to what it holds")
        void unwrappingAContainer() {
            assertThat(fieldOf(FLEET, "hulls").elementType())
                    .map(TypeRef::qualifiedName)
                    .contains(HULL.qualifiedName());
            assertThat(fieldOf(FLEET, "hulls").hasRole(FieldRole.COLLECTION)).isTrue();
        }

        @Test
        @DisplayName("and leaving what holds nothing alone")
        void leavingWhatHoldsNothingAlone() {
            assertThat(fieldOf(FLEET, "manifest").elementType()).isEmpty();
            assertThat(fieldOf(FLEET, "manifest").hasRole(FieldRole.COLLECTION)).isFalse();
        }

        @Test
        @DisplayName("naming the identity from the tie, on the aggregate a way out searches")
        void namingTheIdentityFromTheTie() {
            assertThat(fieldOf(FLEET, "tag").hasRole(FieldRole.IDENTITY)).isTrue();
            assertThat(fieldOf(FLEET, "hulls").hasRole(FieldRole.IDENTITY)).isFalse();
            assertThat(fieldOf(FLEET, "manifest").hasRole(FieldRole.IDENTITY)).isFalse();
        }

        @Test
        @DisplayName("and from the verdict on its type, on a part that carries one")
        void andFromTheVerdictOnAPart() {
            assertThat(fieldOf(HULL, "tag").hasRole(FieldRole.IDENTITY)).isTrue();
            assertThat(fieldOf(HULL, "code").hasRole(FieldRole.IDENTITY)).isFalse();
        }

        @Test
        @DisplayName("but never on something no owner of an identity declared")
        void butNeverOnSomethingNoOwnerDeclared() {
            assertThat(fieldOf(DOOR, "berthing").hasRole(FieldRole.IDENTITY)).isFalse();
            assertThat(fieldOf(DOOR, "manifest").hasRole(FieldRole.IDENTITY)).isFalse();
        }

        @Test
        @DisplayName("marking as embedded what its type was read to be a value of")
        void markingAsEmbeddedWhatItsTypeWasReadToBe() {
            assertThat(fieldOf(FLEET, "manifest").hasRole(FieldRole.EMBEDDED)).isTrue();
            assertThat(fieldOf(FLEET, "tag").hasRole(FieldRole.EMBEDDED)).isTrue();
            assertThat(fieldOf(FLEET, "hulls").hasRole(FieldRole.EMBEDDED)).isFalse();
        }

        @Test
        @DisplayName("naming the single value an identity is written around")
        void namingTheSingleValueAnIdentityIsWrittenAround() {
            assertThat(fieldOf(FLEET, "tag").wrappedType())
                    .map(TypeRef::qualifiedName)
                    .contains("java.lang.String");
        }

        @Test
        @DisplayName("and saying nothing when it is written around several")
        void andSayingNothingWhenWrittenAroundSeveral() {
            assertThat(fieldOf(DECK, "tag").hasRole(FieldRole.IDENTITY)).isTrue();
            assertThat(fieldOf(DECK, "tag").wrappedType()).isEmpty();
        }

        /**
         * Two aggregates, one holding the other whole and the other's identity beside it. Nothing
         * in the corpus does either — well-drawn domains reference one another by identity, and
         * none of the fixtures crosses two aggregates — so both readings would otherwise ship
         * covered by nothing.
         */
        private static CodeModel twoAggregates() {
            return CodeModel.builder()
                    .addType(TypeNode.builder(FLEET, TypeNature.CLASS)
                            .fields(List.of(
                                    Field.builder("NONE", ref(FLEET_TAG))
                                            .modifiers(Set.of(Modifier.STATIC, Modifier.FINAL))
                                            .build(),
                                    Field.builder("tag", ref(FLEET_TAG))
                                            .documentation("What this fleet is searched by.")
                                            .sourceLocation(new SourceLocation("Fleet.java", 7, 20))
                                            .build(),
                                    keeps("berth", DECK_TAG)))
                            .methods(List.of(Method.of("sail", ref(SAILING))))
                            .build())
                    .addType(TypeNode.builder(FLEET_TAG, TypeNature.RECORD)
                            .fields(List.of(frozen("value", TEXT)))
                            .build())
                    .addType(TypeNode.builder(DECK, TypeNature.CLASS)
                            .fields(List.of(keeps("tag", DECK_TAG)))
                            .build())
                    .addType(TypeNode.builder(DECK_TAG, TypeNature.RECORD)
                            .fields(List.of(frozen("value", TEXT)))
                            .build())
                    .addType(TypeNode.builder(SAILING, TypeNature.RECORD)
                            .fields(List.of(frozen("reference", TEXT)))
                            .build())
                    .addType(TypeNode.builder(BOOKS, TypeNature.INTERFACE)
                            .methods(List.of(takes("find", ref(FLEET), FLEET_TAG), takes("keep", VOID, FLEET)))
                            .build())
                    .addType(TypeNode.builder(BERTHING, TypeNature.INTERFACE)
                            .methods(List.of(takes("find", ref(DECK), DECK_TAG), takes("keep", VOID, DECK)))
                            .build())
                    .addType(TypeNode.builder(DISPATCH, TypeNature.CLASS)
                            .fields(List.of(
                                    keeps("books", BOOKS),
                                    keeps("berthing", BERTHING),
                                    keeps("flagship", FLEET),
                                    keeps("berth", DECK)))
                            .build())
                    .build();
        }

        @Test
        @DisplayName("marking as a reference a field holding a whole other aggregate")
        void markingAsAReferenceAFieldHoldingAWholeAggregate() {
            ArchType dispatch = read(modelOf(twoAggregates()), DISPATCH, ArchType.class);

            assertThat(dispatch.structure().fields())
                    .filteredOn(field -> field.hasRole(FieldRole.AGGREGATE_REFERENCE))
                    .extracting(Field::name)
                    .containsExactly("flagship", "berth");
        }

        /**
         * {@code berth} holds an aggregate written around a single field, which is the shape an
         * identity has: what keeps the two apart is the verdict on the type, never its shape.
         */
        @Test
        @DisplayName("and saying nothing of what a whole aggregate is written around")
        void andSayingNothingOfWhatAWholeAggregateIsWrittenAround() {
            ArchType dispatch = read(modelOf(twoAggregates()), DISPATCH, ArchType.class);

            assertThat(dispatch.structure().fields())
                    .filteredOn(field -> field.hasRole(FieldRole.AGGREGATE_REFERENCE))
                    .allSatisfy(field -> assertThat(field.wrappedType()).isEmpty());
        }

        @Test
        @DisplayName("keeping the identity to the one a way out searches by, not to any it holds")
        void keepingTheIdentityToTheOneAWayOutSearchesBy() {
            ArchType fleet = read(modelOf(twoAggregates()), FLEET, ArchType.class);

            assertThat(fleet.structure().fields())
                    .filteredOn(field -> field.hasRole(FieldRole.IDENTITY))
                    .extracting(Field::name)
                    .containsExactly("tag");
        }

        @Test
        @DisplayName("and never to what belongs to the class rather than to one of its instances")
        void andNeverToWhatBelongsToTheClass() {
            ArchType fleet = read(modelOf(twoAggregates()), FLEET, ArchType.class);

            assertThat(fleet.structure().fields())
                    .filteredOn(field -> "NONE".equals(field.name()))
                    .allSatisfy(field ->
                            assertThat(field.hasRole(FieldRole.IDENTITY)).isFalse());
        }

        @Test
        @DisplayName("without losing what the sources wrote around the field")
        void withoutLosingWhatTheSourcesWroteAroundTheField() {
            ArchType fleet = read(modelOf(twoAggregates()), FLEET, ArchType.class);

            assertThat(fleet.structure().fields())
                    .filteredOn(field -> "tag".equals(field.name()))
                    .singleElement()
                    .satisfies(field -> {
                        assertThat(field.documentation()).contains("What this fleet is searched by.");
                        assertThat(field.sourceLocation())
                                .map(SourceLocation::lineStart)
                                .contains(7);
                    });
        }

        @Test
        @DisplayName("leaving audit and plumbing unsaid, which no pack can name on a member")
        void leavingAuditAndPlumbingUnsaid() {
            assertThat(read(modelOf(hexagon()), FLEET, ArchType.class)
                            .structure()
                            .fields())
                    .flatExtracting(Field::roles)
                    .doesNotContain(FieldRole.AUDIT, FieldRole.TECHNICAL);
        }

        @Test
        @DisplayName("and telling the same field the same way wherever the model holds it")
        void andTellingTheSameFieldTheSameWayEverywhere() {
            AggregateRoot fleet = read(modelOf(hexagon()), FLEET, AggregateRoot.class);

            assertThat(fleet.identityField()).contains(fieldOf(FLEET, "tag"));
        }
    }

    @Nested
    @DisplayName("fills the domain from the ties the rules stated")
    class FillsTheDomain {

        @Test
        @DisplayName("naming the identity a way out searches the aggregate by")
        void namingTheIdentity() {
            AggregateRoot fleet = read(modelOf(hexagon()), FLEET, AggregateRoot.class);

            assertThat(fleet.identityField()).map(Field::name).contains("tag");
            assertThat(fleet.effectiveIdentityType())
                    .map(TypeRef::qualifiedName)
                    .contains("java.lang.String");
        }

        @Test
        @DisplayName("naming what the aggregate is made of, entities apart from values")
        void namingWhatTheAggregateIsMadeOf() {
            AggregateRoot fleet = read(modelOf(hexagon()), FLEET, AggregateRoot.class);

            assertThat(names(fleet.entities())).containsExactly(HULL.qualifiedName());
            assertThat(names(fleet.valueObjects())).containsExactly(MANIFEST.qualifiedName());
        }

        @Test
        @DisplayName("naming what it announces and the way out that keeps it")
        void namingWhatItAnnouncesAndTheWayOut() {
            AggregateRoot fleet = read(modelOf(hexagon()), FLEET, AggregateRoot.class);

            assertThat(names(fleet.domainEvents())).containsExactly(SAILING.qualifiedName());
            assertThat(fleet.drivenPort()).map(TypeRef::qualifiedName).contains(BOOKS.qualifiedName());
        }

        @Test
        @DisplayName("naming the aggregate an entity belongs to, and the identity of its own it carries")
        void namingTheAggregateAnEntityBelongsTo() {
            Entity hull = read(modelOf(hexagon()), HULL, Entity.class);

            assertThat(hull.owningAggregate()).map(TypeRef::qualifiedName).contains(FLEET.qualifiedName());
            assertThat(hull.identityField()).map(Field::name).contains("tag");
        }

        @Test
        @DisplayName("naming the value an identity is written around")
        void namingTheValueAnIdentityIsWrittenAround() {
            Identifier tag = read(modelOf(hexagon()), FLEET_TAG, Identifier.class);

            assertThat(tag.wrappedType()).map(TypeRef::qualifiedName).contains("java.lang.String");
        }

        @Test
        @DisplayName("and naming none when the identity is written around several things")
        void andNamingNoneWhenTheIdentityIsWrittenAroundSeveralThings() {
            // An identity spread over two values is stored under neither of them on its own, and
            // electing one would hand a generator the wrong column.
            Identifier tag = read(modelOf(hexagon()), DECK_TAG, Identifier.class);

            assertThat(tag.wrappedType()).isEmpty();
        }

        @Test
        @DisplayName("and naming no aggregate for a part owned by a part")
        void andNamingNoAggregateForAPartOwnedByAPart() {
            // Deck belongs to Hull, and Hull to Fleet. Naming Hull as the aggregate would be wrong
            // and naming Fleet would be a step the analysis never took.
            assertThat(read(modelOf(hexagon()), DECK, Entity.class).owningAggregate())
                    .isEmpty();
        }

        @Test
        @DisplayName("naming the aggregate an event came from")
        void namingTheAggregateAnEventCameFrom() {
            DomainEvent sailing = read(modelOf(hexagon()), SAILING, DomainEvent.class);

            assertThat(sailing.sourceAggregate()).map(TypeRef::qualifiedName).contains(FLEET.qualifiedName());
        }

        @Test
        @DisplayName("and a value with nothing to add stays a value")
        void andAValueWithNothingToAddStaysAValue() {
            assertThat(read(modelOf(hexagon()), MANIFEST, ValueObject.class).kind())
                    .isEqualTo(ArchKind.VALUE_OBJECT);
        }

        @Test
        @DisplayName("naming the ways out a declared domain service is handed")
        void namingTheWaysOutADomainServiceIsHanded() {
            DomainService chartering = read(modelOf(hexagon()), CHARTERING, DomainService.class);

            assertThat(names(chartering.injectedPorts())).containsExactly(BOOKS.qualifiedName());
            assertThat(chartering.operations()).extracting(Method::name).containsExactly("charter");
        }

        @Test
        @DisplayName("and listing none for a service read from the domain types it works across")
        void andListingNoneForAServiceReadFromItsCollaboration() {
            // A service read that way holds no way out by construction: holding one is what tells a
            // collaboration inside the domain from a call to the outside.
            assertThat(read(modelOf(hexagon()), FERRYING, DomainService.class).injectedPorts())
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("fills the frontier from the position each side holds")
    class FillsTheFrontier {

        @Test
        @DisplayName("giving a way out the trade its signatures ply and the aggregate it keeps")
        void givingAWayOutItsTradeAndSubject() {
            DrivenPort books = read(modelOf(hexagon()), BOOKS, DrivenPort.class);

            assertThat(books.portType()).isEqualTo(DrivenPortType.REPOSITORY);
            assertThat(books.managedAggregate()).map(TypeRef::qualifiedName).contains(FLEET.qualifiedName());
        }

        @Test
        @DisplayName("giving a way in one use case per method, asking apart from telling")
        void givingAWayInOneUseCasePerMethod() {
            DrivingPort berthing = read(modelOf(hexagon()), BERTHING, DrivingPort.class);

            assertThat(berthing.useCases())
                    .extracting(useCase -> useCase.method().name(), UseCase::type)
                    .containsExactly(
                            tuple("berth", UseCase.UseCaseType.COMMAND), tuple("sail", UseCase.UseCaseType.QUERY));
        }

        @Test
        @DisplayName("telling a way in that changes something from one that only asks, by the bodies")
        void tellingAWayInThatChangesSomething() {
            DrivingPort mooring = read(modelOf(hangarThatChanges()), MOORING, DrivingPort.class);

            assertThat(mooring.useCases())
                    .extracting(useCase -> useCase.method().name(), UseCase::type)
                    .containsExactly(
                            tuple("look", UseCase.UseCaseType.QUERY),
                            tuple("moor", UseCase.UseCaseType.COMMAND),
                            tuple("stow", UseCase.UseCaseType.COMMAND),
                            tuple("draft", UseCase.UseCaseType.QUERY));
        }

        @Test
        @DisplayName("giving a way in the types of the perimeter it takes and answers with")
        void givingAWayInItsTypes() {
            DrivingPort berthing = read(modelOf(hexagon()), BERTHING, DrivingPort.class);

            assertThat(names(berthing.inputTypes())).containsExactly(FLEET_TAG.qualifiedName());
            assertThat(names(berthing.outputTypes())).containsExactly(SAILING.qualifiedName());
        }

        @Test
        @DisplayName("giving an entry point the way in it reaches the application through")
        void givingAnEntryPointItsWayIn() {
            DrivingAdapter door = read(modelOf(hexagon()), DOOR, DrivingAdapter.class);

            assertThat(names(door.ports())).containsExactly(BERTHING.qualifiedName());
        }

        @Test
        @DisplayName("giving a piece of plumbing the way out it answers")
        void givingPlumbingTheWayOutItAnswers() {
            DrivenAdapter ledger = read(modelOf(hexagon()), LEDGER, DrivenAdapter.class);

            assertThat(names(ledger.ports())).containsExactly(BOOKS.qualifiedName());
        }
    }

    @Nested
    @DisplayName("says why a type it could not place stayed there")
    class SaysWhyATypeStayedUnplaced {

        private static final TypeId PLUMBING = TypeId.of("com.acme.hangar.Wiring");
        private static final TypeId ALONE = TypeId.of("com.acme.hangar.core.Berth");
        private static final String CONFIGURATION = "org.springframework.context.annotation.Configuration";

        private static UnclassifiedType unplaced(TypeId id, TypeNode... types) {
            CodeModel.Builder code = CodeModel.builder();
            for (TypeNode type : types) {
                code.addType(type);
            }
            return read(modelOf(code.build()), id, UnclassifiedType.class);
        }

        private static TypeNode alone() {
            return TypeNode.builder(ALONE, TypeNature.CLASS)
                    .fields(List.of(Field.of("code", TEXT)))
                    .build();
        }

        @Test
        @DisplayName("as plumbing, when a framework says it belongs to no ring")
        void plumbingIsTechnical() {
            TypeNode wiring = TypeNode.builder(PLUMBING, TypeNature.CLASS)
                    .annotations(List.of(Annotation.of(CONFIGURATION)))
                    .build();

            assertThat(unplaced(PLUMBING, wiring).category()).isEqualTo(UnclassifiedCategory.TECHNICAL);
        }

        @Test
        @DisplayName("as a tie, when two readings were left standing with nothing between them")
        void twoReadingsLeftStandingAreATie() {
            // A value wrapped around one thing reads as an identity and as a value at once, and
            // with no way out searching by it, nothing decides which.
            TypeNode wrapper = TypeNode.builder(FLEET_TAG, TypeNature.RECORD)
                    .fields(List.of(frozen("value", TEXT)))
                    .build();
            UnclassifiedType tag = unplaced(FLEET_TAG, wrapper);

            assertThat(tag.category()).isEqualTo(UnclassifiedCategory.AMBIGUOUS);
            assertThat(tag.classification().candidates()).hasSize(2);
            assertThat(tag.reason().orElseThrow()).contains("IDENTIFIER", "VALUE_OBJECT");
        }

        @Test
        @DisplayName("and leaves plumbing alone, which is categorized and not a gap to close")
        void andLeavesPlumbingAlone() {
            TypeNode wiring = TypeNode.builder(PLUMBING, TypeNature.CLASS)
                    .annotations(List.of(Annotation.of(CONFIGURATION)))
                    .build();

            assertThat(unplaced(PLUMBING, wiring).classification().remediations())
                    .isEmpty();
        }

        @Test
        @DisplayName("as nothing known, when no neighbour ever used it")
        void nothingKnownWhenNobodyUsesIt() {
            UnclassifiedType berth = unplaced(ALONE, alone());

            assertThat(berth.category()).isEqualTo(UnclassifiedCategory.UNKNOWN);
            assertThat(berth.reason().orElseThrow()).contains("nothing in the perimeter uses it");
        }

        @Test
        @DisplayName("and what would settle it, which is to declare the kind or give it a context")
        void andWhatWouldSettleIt() {
            assertThat(unplaced(ALONE, alone()).classification().remediations())
                    .extracting(RemediationHint::action)
                    .containsExactly(RemediationAction.CONFIGURE_EXPLICIT);
        }

        @Test
        @DisplayName("and a mapping to a store is not plumbing, whatever else it fails to say")
        void andAMappingToAStoreIsNotPlumbing() {
            // Being mapped to a store says where a type is written, never what it is, so the type
            // is left exactly as unread as it was — and a gap, not a ring nobody belongs to.
            TypeNode mapped = TypeNode.builder(ALONE, TypeNature.CLASS)
                    .annotations(List.of(Annotation.of("jakarta.persistence.Entity")))
                    .fields(List.of(Field.of("code", TEXT)))
                    .build();

            assertThat(unplaced(ALONE, mapped).category()).isEqualTo(UnclassifiedCategory.UNKNOWN);
        }
    }

    @Nested
    @DisplayName("reads the links for what they say")
    class ReadsTheLinksForWhatTheySay {

        private static ArchModel modelWith(Relation extra) {
            EngineContext context = EngineContext.of(hexagon(), KnowledgePacks.embedded(), HexaGlueConfig.defaults());
            Verdicts verdicts = Classifier.classify(context);
            FactBase facts = Saturation.saturate(RuleSet.standard(), context.withVerdicts(verdicts));
            facts.add(extra);
            return Assembly.assemble(context, facts, verdicts, ModuleTopology.empty());
        }

        @Test
        @DisplayName("never taking a link of one shape for a link of another")
        void neverTakingALinkOfOneShapeForAnother() {
            // Something announcing the entity is not something the entity belongs to, and reading
            // both as one would leave the entity with two owners and therefore none.
            ArchModel model = modelWith(Relation.derived(SAILING, RelationKind.ANNOUNCES, HULL, RuleId.of("TEST")));

            assertThat(read(model, HULL, Entity.class).owningAggregate())
                    .map(TypeRef::qualifiedName)
                    .contains(FLEET.qualifiedName());
        }

        @Test
        @DisplayName("and failing loudly when a type of the perimeter was never ruled on")
        void andFailingLoudlyWhenATypeWasNeverRuledOn() {
            EngineContext context = EngineContext.of(hexagon(), KnowledgePacks.embedded(), HexaGlueConfig.defaults());
            FactBase facts = Saturation.saturate(RuleSet.standard(), context);

            assertThatThrownBy(() -> Assembly.assemble(context, facts, Verdicts.none(), ModuleTopology.empty()))
                    .isInstanceOf(EngineException.class)
                    .hasMessageContaining("no verdict was reached on");
        }
    }
}
