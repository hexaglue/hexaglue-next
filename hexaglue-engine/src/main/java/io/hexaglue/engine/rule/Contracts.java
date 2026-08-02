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

import io.hexaglue.engine.Derivation;
import io.hexaglue.engine.KindEvidence;
import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.classification.RuleId;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.declaration.Constructor;
import io.hexaglue.model.declaration.Field;
import io.hexaglue.model.declaration.Parameter;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * How the boundary of the hexagon is read: not from what an interface is called, but from who
 * writes it and who calls it.
 *
 * <p>An interface becomes a port through its position. The core calling a contract nobody inside
 * fulfils is reaching outward, and that contract is a driven port; the core fulfilling a contract
 * the outer ring calls is being reached, and that contract is a driving port. Neither reading needs
 * a suffix or a package, which is the whole point: the previous engine could only tell the two
 * apart by their names, and got them wrong whenever the names disagreed with the wiring.</p>
 *
 * <p><strong>Not every interface is a port.</strong> A contract the core both fulfils and calls is
 * an agreement it has with itself — a seam between two of its own pieces, not a hole in the wall.
 * Reading it as a boundary would invent a frontier the code does not have, so nothing is emitted
 * and the type keeps whatever else speaks for it. The same silence covers the interface nobody in
 * the perimeter touches: with no consumer and no implementer in sight, its position is simply not
 * observable, and saying so is the honest answer.</p>
 *
 * <p>Whether a type belongs to the core is read from the round before: a type the outer ring
 * already claims is not the core reaching outward, it is the outside reaching in. On the first
 * round nothing is placed yet, so everything counts as core and the readings are broad; the rounds
 * that follow narrow them as the ring fills, and a reading whose ground has gone disappears with
 * it.</p>
 *
 * @since 7.0.0
 */
final class Contracts {

    private static final Set<TypeNature> IMPLEMENTATIONS = Set.of(TypeNature.CLASS, TypeNature.RECORD);

    private Contracts() {}

    /**
     * Returns the types of the core that fulfil the given contract. An interface refining it is
     * not one of them: a contract is fulfilled by an implementation, and an interface never is.
     *
     * @param derivation the derivation the rule is running under
     * @param contract the contract to look up
     * @return the implementers, in perimeter order
     */
    static List<TypeNode> implementersInTheCore(Derivation derivation, TypeId contract) {
        return implementersOf(derivation, contract).stream()
                .filter(type -> !onTheRing(derivation, type.id()))
                .toList();
    }

    /**
     * Returns every type of the perimeter that fulfils the given contract, wherever it sits.
     *
     * @param derivation the derivation the rule is running under
     * @param contract the contract to look up
     * @return the implementers, in perimeter order
     */
    static List<TypeNode> implementersOf(Derivation derivation, TypeId contract) {
        return derivation.perimeter().types().stream()
                .filter(type -> IMPLEMENTATIONS.contains(type.nature()))
                .filter(type -> fulfils(derivation.code(), type, contract))
                .toList();
    }

    /**
     * Returns the types of the core keeping the given contract as a collaborator.
     *
     * @param derivation the derivation the rule is running under
     * @param contract the contract to look up
     * @return the holders, in perimeter order
     */
    static List<TypeNode> holdersInTheCore(Derivation derivation, TypeId contract) {
        return holdersOf(derivation, contract).stream()
                .filter(type -> !onTheRing(derivation, type.id()))
                .toList();
    }

    /**
     * Returns every type of the perimeter keeping the given contract as a collaborator.
     *
     * @param derivation the derivation the rule is running under
     * @param contract the contract to look up
     * @return the holders, in perimeter order
     */
    static List<TypeNode> holdersOf(Derivation derivation, TypeId contract) {
        return derivation.perimeter().types().stream()
                .filter(type -> holds(type, contract))
                .toList();
    }

    /**
     * Returns whether the outer ring already claims the given type.
     *
     * @param derivation the derivation the rule is running under
     * @param id the type to place
     * @return true when the previous round read it as an adapter
     */
    static boolean onTheRing(Derivation derivation, TypeId id) {
        return derivation.kindOf(id).filter(ArchKind::isAdapter).isPresent();
    }

    /**
     * Returns whether the core already claims the given type. Such a type may well stand at a
     * frontier that is not one — a domain type fulfilling a way out — but that is a conformity
     * question, and answering it by moving the type to the ring would hide it.
     *
     * @param derivation the derivation the rule is running under
     * @param id the type to place
     * @return true when the previous round read it as a domain, application or port type
     */
    static boolean claimedByTheCore(Derivation derivation, TypeId id) {
        return derivation
                .kindOf(id)
                .filter(kind -> kind != ArchKind.UNCLASSIFIED && !kind.isAdapter())
                .isPresent();
    }

    /**
     * States a reading of the boundary, at the tier such readings belong to: a relation between
     * types, never a property of one declaration.
     *
     * @param derivation the derivation the rule is running under
     * @param subject the type being placed
     * @param kind the position read for it
     * @param fact the relation that produced the reading
     * @param why the reason, phrased for a reader of the report
     * @param related the types the relation runs through, in relevance order
     * @param rule the rule concluding it
     */
    static void speak(
            Derivation derivation,
            TypeId subject,
            ArchKind kind,
            String fact,
            String why,
            List<TypeId> related,
            RuleId rule) {
        Evidence evidence = new Evidence(
                EvidenceTier.GRAPH_RELATION,
                EvidenceTier.GRAPH_RELATION.maxConfidence(),
                fact,
                subject.qualifiedName() + " is a " + kind + " because " + why,
                derivation.code().type(subject).flatMap(TypeNode::sourceLocation),
                related);
        derivation.derive(KindEvidence.derived(subject, kind, evidence, 0, rule));
    }

    /**
     * Answers whether a type fulfils a contract, directly or through anything it inherits from.
     */
    private static boolean fulfils(CodeModel code, TypeNode type, TypeId contract) {
        return Stream.concat(type.superClass().stream(), type.interfaces().stream())
                        .anyMatch(reference -> contract.qualifiedName().equals(reference.qualifiedName()))
                || code.supertypesOf(type.id()).contains(contract);
    }

    /**
     * Answers whether a type keeps another one, wherever the declaration puts it: an instance
     * field, a constructor parameter, or a type argument of either. Holding a contract in a list is
     * still holding it; naming it in a constant is not, because a constant belongs to the type
     * rather than to anything it collaborates with — which is also why an interface, having no
     * instance state and no constructor, never holds anything.
     */
    private static boolean holds(TypeNode type, TypeId contract) {
        return Stream.concat(
                        Shapes.state(type).stream().map(Field::type),
                        type.constructors().stream()
                                .map(Constructor::parameters)
                                .flatMap(List::stream)
                                .map(Parameter::type))
                .flatMap(Contracts::named)
                .anyMatch(contract::equals);
    }

    /**
     * Returns the types a reference names: the reference itself and, recursively, its arguments.
     */
    private static Stream<TypeId> named(TypeRef reference) {
        Stream<TypeId> arguments = reference.typeArguments().stream().flatMap(Contracts::named);
        return reference instanceof TypeRef.Named
                ? Stream.concat(Stream.of(TypeId.of(reference.qualifiedName())), arguments)
                : arguments;
    }
}
