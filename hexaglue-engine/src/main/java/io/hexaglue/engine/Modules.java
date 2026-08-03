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

import io.hexaglue.knowledge.KnowledgeFact;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.arch.ModuleDescriptor;
import io.hexaglue.model.arch.ModuleRole;
import io.hexaglue.model.arch.ModuleTopology;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.Edge;
import io.hexaglue.model.code.ModuleNode;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.ModulesConfig;
import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.DiagnosticSeverity;
import io.hexaglue.model.finding.IssueCode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Reads the build layout of a reactor: which module holds which type, which module depends on
 * which, and which of them read as holding a domain.
 *
 * <p>The dependencies are folded from the edges the frontend already recorded, exactly as
 * {@link Dependencies} folds them into packages — deriving a second set by walking type structures
 * again would be a second answer to the same question.</p>
 *
 * <p>A module reads as holding a domain when it depends on no other module of the reactor and none
 * of its types reaches an infrastructure tool. Both halves matter: a module coupled to nothing
 * internal but built on a persistence framework is a library, not a domain. The reading is made on
 * the whole reactor, including the modules whose role nobody declared — a domain module depending
 * on an undeclared one depends on something, and hiding that would turn a gap in the configuration
 * into a compliment.</p>
 *
 * <p>Only a module whose role the project declared enters the topology. There is no neutral role to
 * fall back on, and giving one would put a reading in the model nobody stated; what is left out is
 * named instead.</p>
 *
 * @since 7.0.0
 */
final class Modules {

    /** A module was read, and the configuration declares no role for it. */
    private static final IssueCode ROLE_NOT_DECLARED = IssueCode.of("HG-ENGINE-004");

    private final ModuleTopology topology;
    private final List<Diagnostic> diagnostics;

    private Modules(ModuleTopology topology, List<Diagnostic> diagnostics) {
        this.topology = topology;
        this.diagnostics = List.copyOf(diagnostics);
    }

    /**
     * Reads the layout of what was analyzed.
     *
     * @param code the analyzed sources, carrying the module each type was read from
     * @param roles the role the project declares for each of its modules
     * @param facts what the analysis holds, the packs' assertions included
     * @return the layout, and what it could not place
     */
    static Modules read(CodeModel code, ModulesConfig roles, FactBase facts) {
        Objects.requireNonNull(code, "code must not be null");
        Objects.requireNonNull(roles, "roles must not be null");
        Objects.requireNonNull(facts, "facts must not be null");
        if (code.modules().isEmpty()) {
            return new Modules(ModuleTopology.empty(), List.of());
        }
        Map<TypeId, String> byType = assignments(code);
        Map<String, Set<String>> dependencies = dependencies(code, byType);
        Set<String> reachingInfrastructure = reachingInfrastructure(facts, byType);

        ModuleTopology.Builder topology = ModuleTopology.builder();
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (ModuleNode module : code.modules()) {
            Optional<ModuleRole> role = roles.roleOf(module.name());
            if (role.isEmpty()) {
                diagnostics.add(undeclared(module));
                continue;
            }
            topology.addModule(new ModuleDescriptor(module.name(), role.orElseThrow(), module.basePackage()));
            if (dependencies.getOrDefault(module.name(), Set.of()).isEmpty()
                    && !reachingInfrastructure.contains(module.name())) {
                topology.domainCandidate(module.name());
            }
        }
        Set<String> declared = declaredNames(code, roles);
        byType.forEach((type, module) -> {
            if (declared.contains(module)) {
                topology.assign(type, module);
            }
        });
        dependencies.forEach((from, targets) -> {
            if (declared.contains(from)) {
                targets.stream().filter(declared::contains).forEach(target -> topology.dependency(from, target));
            }
        });
        return new Modules(topology.build(), diagnostics);
    }

    /**
     * Returns the layout the reading built.
     *
     * @return the topology, empty on a single-module project
     */
    ModuleTopology topology() {
        return topology;
    }

    /**
     * Returns what the reading could not place, one entry per module read without a declared role.
     *
     * @return the diagnostics, in the order the modules were read
     */
    List<Diagnostic> diagnostics() {
        return diagnostics;
    }

    private static Set<String> declaredNames(CodeModel code, ModulesConfig roles) {
        Set<String> declared = new TreeSet<>();
        code.modules().stream()
                .map(ModuleNode::name)
                .filter(name -> roles.roleOf(name).isPresent())
                .forEach(declared::add);
        return declared;
    }

    private static Map<TypeId, String> assignments(CodeModel code) {
        Map<TypeId, String> byType = new TreeMap<>();
        for (TypeNode type : code.types()) {
            type.moduleName().ifPresent(module -> byType.put(type.id(), module));
        }
        return byType;
    }

    /**
     * Folds the couplings between types into couplings between the modules holding them. A type the
     * classpath supplied carries no module, so nothing outside the reactor can weigh on its shape.
     */
    private static Map<String, Set<String>> dependencies(CodeModel code, Map<TypeId, String> byType) {
        Map<String, Set<String>> dependencies = new LinkedHashMap<>();
        for (Edge edge : code.edges()) {
            if (!Dependencies.coupling().contains(edge.kind())) {
                continue;
            }
            String from = byType.get(edge.source());
            String to = byType.get(edge.target());
            if (from == null || to == null || from.equals(to)) {
                continue;
            }
            dependencies.computeIfAbsent(from, name -> new TreeSet<>()).add(to);
        }
        return dependencies;
    }

    private static Set<String> reachingInfrastructure(FactBase facts, Map<TypeId, String> byType) {
        Set<String> modules = new TreeSet<>();
        for (KnowledgeAssertion assertion : facts.all(KnowledgeAssertion.class)) {
            if (assertion.finding().fact() == KnowledgeFact.INFRA_DEPENDENCY) {
                Optional.ofNullable(byType.get(assertion.subject())).ifPresent(modules::add);
            }
        }
        return modules;
    }

    private static Diagnostic undeclared(ModuleNode module) {
        return Diagnostic.builder(
                        ROLE_NOT_DECLARED,
                        DiagnosticSeverity.WARNING,
                        module.name()
                                + " was read but its role is not declared, so it is absent from the layout of the"
                                + " reactor; state it under 'modules' in the configuration")
                .build();
    }
}
