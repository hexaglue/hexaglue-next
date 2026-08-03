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

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.TypeRef;
import io.hexaglue.model.arch.AdapterType;
import io.hexaglue.model.arch.ApplicationType;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.arch.DrivenAdapter;
import io.hexaglue.model.arch.DrivenPort;
import io.hexaglue.model.arch.DrivingAdapter;
import io.hexaglue.model.arch.PortType;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.classification.Evidence;
import io.hexaglue.model.classification.EvidenceTier;
import io.hexaglue.model.finding.Finding;
import io.hexaglue.model.finding.IssueCode;
import io.hexaglue.model.finding.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Whether the hexagon is one: the core written against contracts, and the world plugged into them.
 *
 * <p>The carrière checked this with four overlapping rules — application purity, dependency
 * direction, dependency inversion, layer isolation — that condemned the same reference up to three
 * times over, so a report said one thing three ways. Here each fact is stated once, by the check
 * that owns it: the domain naming the outside is a domain finding, the application naming an
 * adapter is one finding rather than two, and an adapter reaching another adapter is its own.</p>
 *
 * <p>A port is checked from both sides, because both can be missing and they mean different
 * things: nobody plugged into it, and nobody in the core uses it.</p>
 *
 * @since 7.0.0
 */
public final class HexagonalFindings {

    /** An application type names an adapter instead of the port it implements. */
    static final IssueCode APPLICATION_NAMES_ADAPTER = IssueCode.of("HG-HEX-001");

    /** A driven port nothing plugs into. */
    static final IssueCode DRIVEN_PORT_UNPLUGGED = IssueCode.of("HG-HEX-002");

    /** A driving port nothing in the core answers. */
    static final IssueCode DRIVING_PORT_UNANSWERED = IssueCode.of("HG-HEX-003");

    /** A driven port nothing in the core calls. */
    static final IssueCode DRIVEN_PORT_UNUSED = IssueCode.of("HG-HEX-004");

    /** A driving port nothing outside drives. */
    static final IssueCode DRIVING_PORT_UNDRIVEN = IssueCode.of("HG-HEX-005");

    /** A port that is not an interface. */
    static final IssueCode PORT_NOT_AN_INTERFACE = IssueCode.of("HG-HEX-006");

    /** An adapter reaching another adapter without going through the core. */
    static final IssueCode ADAPTER_TO_ADAPTER = IssueCode.of("HG-HEX-007");

    private HexagonalFindings() {}

    /**
     * Runs every hexagonal check.
     *
     * @param judgement what the checks may read
     * @return the findings, in the order the checks are stated
     */
    static List<Finding> of(Judgement judgement) {
        List<Finding> findings = new ArrayList<>();
        findings.addAll(applicationTypesNamingAdapters(judgement));
        findings.addAll(drivenPortsNothingImplements(judgement));
        findings.addAll(drivingPortsNothingAnswers(judgement));
        findings.addAll(drivenPortsNobodyCalls(judgement));
        findings.addAll(drivingPortsNobodyDrives(judgement));
        findings.addAll(portsThatAreNotInterfaces(judgement));
        findings.addAll(adaptersReachingAdapters(judgement));
        return findings;
    }

    /**
     * The application is the layer that decides; the adapter is the one that knows how. An
     * application type naming an adapter has both, which means the decision cannot be tested,
     * reused or re-plugged without the how coming along.
     *
     * <p>The fix is specific rather than general: the adapter usually declares which port it
     * implements, and that port is the thing to name instead.</p>
     */
    private static List<Finding> applicationTypesNamingAdapters(Judgement judgement) {
        List<Finding> findings = new ArrayList<>();
        for (ArchType type : judgement.model().types()) {
            if (!(type instanceof ApplicationType)) {
                continue;
            }
            List<TypeId> adapters = judgement.dependencies().usedBy(type.id()).stream()
                    .filter(used -> judgement
                            .model()
                            .type(used)
                            .map(AdapterType.class::isInstance)
                            .orElse(false))
                    .toList();
            if (adapters.isEmpty()) {
                continue;
            }
            findings.add(finding(
                            APPLICATION_NAMES_ADAPTER,
                            Severity.CRITICAL,
                            type.id(),
                            type.id().simpleName() + " names " + names(adapters) + ". " + instead(judgement, adapters)
                                    + " The application decides, the adapter knows how; a service holding both "
                                    + "cannot be run against anything else.",
                            evidence("names-an-adapter", "an adapter of the outside", adapters))
                    .relatedTypes(adapters)
                    .build());
        }
        return findings;
    }

    /**
     * Names the ports the adapters declare, so the message says what to depend on rather than only
     * what not to.
     */
    private static String instead(Judgement judgement, List<TypeId> adapters) {
        List<String> ports = adapters.stream()
                .flatMap(adapter -> judgement.model().type(adapter).stream())
                .filter(DrivenAdapter.class::isInstance)
                .map(DrivenAdapter.class::cast)
                .flatMap(adapter -> adapter.implementedPorts().stream())
                .map(port -> TypeId.of(port.qualifiedName()).simpleName())
                .distinct()
                .sorted()
                .toList();
        return ports.isEmpty() ? "" : "Name " + String.join(", ", ports) + " instead.";
    }

    /**
     * A driven port is a hole the core leaves for the world to fill. Unfilled, the core cannot run
     * — and nothing in the sources says what was supposed to fill it.
     */
    private static List<Finding> drivenPortsNothingImplements(Judgement judgement) {
        List<TypeId> implemented = judgement
                .model()
                .all(DrivenAdapter.class)
                .flatMap(adapter -> adapter.implementedPorts().stream())
                .map(port -> TypeId.of(port.qualifiedName()))
                .toList();
        return judgement
                .model()
                .portIndex()
                .drivenPorts()
                .filter(port -> !implemented.contains(port.id()))
                .filter(port -> implementorsOf(judgement, port.id()).isEmpty())
                .map(port -> finding(
                                DRIVEN_PORT_UNPLUGGED,
                                Severity.MAJOR,
                                port.id(),
                                port.id().simpleName()
                                        + " is a driven port nothing implements. It is a hole the core left for the "
                                        + "world to fill, and nothing in these sources fills it.",
                                evidence("no-adapter", "no adapter implements it", List.of()))
                        .build())
                .toList();
    }

    /**
     * A driving port is what the core promises to answer. Unanswered, the promise is a name.
     */
    private static List<Finding> drivingPortsNothingAnswers(Judgement judgement) {
        return judgement
                .model()
                .portIndex()
                .drivingPorts()
                .filter(port -> implementorsOf(judgement, port.id()).stream()
                        .noneMatch(implementor -> implementor instanceof ApplicationType))
                .map(port -> finding(
                                DRIVING_PORT_UNANSWERED,
                                Severity.MAJOR,
                                port.id(),
                                port.id().simpleName()
                                        + " is a driving port that nothing in the application answers. What the core "
                                        + "promises here, no type of the core delivers.",
                                evidence(
                                        "no-implementation-in-the-core",
                                        "no application type implements it",
                                        List.of()))
                        .build())
                .toList();
    }

    /**
     * A driven port the core never calls is a contract written for nobody: whatever implements it
     * runs, or does not, without the core ever noticing.
     */
    private static List<Finding> drivenPortsNobodyCalls(Judgement judgement) {
        List<Finding> findings = new ArrayList<>();
        for (DrivenPort port : judgement.model().portIndex().drivenPorts().toList()) {
            boolean calledByTheCore = judgement.dependencies().usersOf(port.id()).stream()
                    .flatMap(user -> judgement.model().type(user).stream())
                    .anyMatch(HexagonalFindings::isCore);
            if (!calledByTheCore) {
                findings.add(finding(
                                DRIVEN_PORT_UNUSED,
                                Severity.MINOR,
                                port.id(),
                                port.id().simpleName()
                                        + " is a driven port nothing in the core calls. A contract the core never "
                                        + "uses is a contract nobody is holding anyone to.",
                                evidence("unused-by-the-core", "no domain or application type names it", List.of()))
                        .build());
            }
        }
        return findings;
    }

    /**
     * A driving port nothing drives is a way in that nothing came in through.
     */
    private static List<Finding> drivingPortsNobodyDrives(Judgement judgement) {
        List<TypeId> driven = judgement
                .model()
                .all(DrivingAdapter.class)
                .flatMap(adapter -> adapter.drivingPorts().stream())
                .map(port -> TypeId.of(port.qualifiedName()))
                .toList();
        return judgement
                .model()
                .portIndex()
                .drivingPorts()
                .filter(port -> !driven.contains(port.id()))
                .filter(port -> judgement.dependencies().usersOf(port.id()).stream()
                        .flatMap(user -> judgement.model().type(user).stream())
                        .noneMatch(AdapterType.class::isInstance))
                .map(port -> finding(
                                DRIVING_PORT_UNDRIVEN,
                                Severity.MINOR,
                                port.id(),
                                port.id().simpleName()
                                        + " is a driving port nothing outside drives. It is a way in that nothing "
                                        + "in these sources comes in through.",
                                evidence("no-driving-adapter", "no adapter drives it", List.of()))
                        .build())
                .toList();
    }

    /**
     * A port is a contract, and a contract that is a class is a contract with an implementation
     * already chosen. Whatever plugs in afterwards inherits that choice.
     */
    private static List<Finding> portsThatAreNotInterfaces(Judgement judgement) {
        List<Finding> findings = new ArrayList<>();
        for (ArchType type : judgement.model().types()) {
            if (!(type instanceof PortType) || type.structure().nature() == TypeNature.INTERFACE) {
                continue;
            }
            findings.add(finding(
                            PORT_NOT_AN_INTERFACE,
                            Severity.MAJOR,
                            type.id(),
                            type.id().simpleName() + " is a port declared as a "
                                    + type.structure().nature().name().toLowerCase(java.util.Locale.ROOT)
                                    + ". A port is a contract; one that carries an implementation makes everything "
                                    + "plugging into it inherit that implementation.",
                            evidence(
                                    "not-an-interface",
                                    "declared as " + type.structure().nature(),
                                    List.of()))
                    .build());
        }
        return findings;
    }

    /**
     * Two adapters wired to each other is the world talking to itself around the core: the
     * behaviour that joins them lives in neither the domain nor the application, so nothing can
     * test it and nothing can replace either side.
     */
    private static List<Finding> adaptersReachingAdapters(Judgement judgement) {
        List<Finding> findings = new ArrayList<>();
        for (ArchType type : judgement.model().types()) {
            if (!(type instanceof AdapterType)) {
                continue;
            }
            List<TypeId> others = judgement.dependencies().usedBy(type.id()).stream()
                    .filter(used -> judgement
                            .model()
                            .type(used)
                            .map(AdapterType.class::isInstance)
                            .orElse(false))
                    .toList();
            if (!others.isEmpty()) {
                findings.add(finding(
                                ADAPTER_TO_ADAPTER,
                                Severity.MAJOR,
                                type.id(),
                                type.id().simpleName() + " reaches " + names(others)
                                        + " without going through the core. What joins two adapters lives in "
                                        + "neither the domain nor the application, so nothing can test it and "
                                        + "neither side can be replaced.",
                                evidence("adapter-to-adapter", "another adapter", others))
                        .relatedTypes(others)
                        .build());
            }
        }
        return findings;
    }

    /**
     * The types declaring a port among their supertypes. Read from the structure the frontend
     * recorded, never from a name.
     */
    private static List<ArchType> implementorsOf(Judgement judgement, TypeId port) {
        return judgement.model().types().stream()
                .filter(type -> type.structure().interfaces().stream()
                        .map(TypeRef::qualifiedName)
                        .anyMatch(name -> name.equals(port.toString())))
                .toList();
    }

    private static boolean isCore(ArchType type) {
        return type.kind() != ArchKind.DRIVING_ADAPTER
                && type.kind() != ArchKind.DRIVEN_ADAPTER
                && type.kind() != ArchKind.UNCLASSIFIED
                && !(type instanceof PortType);
    }

    private static String names(List<TypeId> types) {
        return types.stream().map(TypeId::simpleName).collect(Collectors.joining(", "));
    }

    private static Evidence evidence(String fact, String justification, List<TypeId> related) {
        return new Evidence(
                EvidenceTier.GRAPH_RELATION, Confidence.HIGH, fact, justification, Optional.empty(), related);
    }

    private static Finding.Builder finding(
            IssueCode code, Severity severity, TypeId subject, String message, Evidence evidence) {
        return Finding.builder(code, severity, message, subject).evidences(List.of(evidence));
    }
}
