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

import io.hexaglue.knowledge.KnowledgeEntry;
import io.hexaglue.knowledge.KnowledgeFact;
import io.hexaglue.knowledge.KnowledgeFinding;
import io.hexaglue.knowledge.Selector;
import io.hexaglue.model.TypeId;
import io.hexaglue.model.TypeNature;
import io.hexaglue.model.arch.ModuleDescriptor;
import io.hexaglue.model.arch.ModuleRole;
import io.hexaglue.model.arch.ModuleTopology;
import io.hexaglue.model.code.CodeModel;
import io.hexaglue.model.code.Edge;
import io.hexaglue.model.code.EdgeKind;
import io.hexaglue.model.code.ModuleNode;
import io.hexaglue.model.code.TypeNode;
import io.hexaglue.model.config.ModulesConfig;
import io.hexaglue.model.finding.DiagnosticSeverity;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * A reactor is read the way a single project is: from the edges the frontend already recorded, and
 * from what the project declared about its own build layout. Nothing here reads a role out of a
 * module name.
 */
class ModulesTest {

    private final CodeModel.Builder code = CodeModel.builder();
    private final Map<String, ModuleRole> roles = new LinkedHashMap<>();

    private ModulesTest module(String name, ModuleRole role) {
        code.addModule(ModuleNode.of(name));
        roles.put(name, role);
        return this;
    }

    private ModulesTest undeclaredModule(String name) {
        code.addModule(ModuleNode.of(name));
        return this;
    }

    private ModulesTest type(String qualifiedName, String moduleName) {
        TypeNode.Builder node = TypeNode.builder(TypeId.of(qualifiedName), TypeNature.CLASS);
        if (moduleName != null) {
            node.moduleName(moduleName);
        }
        code.addType(node.build());
        return this;
    }

    private ModulesTest edge(String from, String to) {
        code.addEdge(Edge.of(TypeId.of(from), EdgeKind.FIELD_TYPE, TypeId.of(to)));
        return this;
    }

    private Modules read(String... reachingInfrastructure) {
        FactBase facts = new FactBase();
        KnowledgeEntry entry = KnowledgeEntry.of(
                new Selector.Type("org.springframework.jdbc.core.JdbcTemplate"), KnowledgeFact.INFRA_DEPENDENCY);
        for (String subject : reachingInfrastructure) {
            facts.add(new KnowledgeAssertion(TypeId.of(subject), new KnowledgeFinding("spring", entry, Map.of())));
        }
        return Modules.read(code.build(), new ModulesConfig(roles), facts);
    }

    @Nested
    @DisplayName("the layout of a reactor")
    class Layout {

        @Test
        @DisplayName("a module carries the role the project declared, and holds the types read from it")
        void carriesTheDeclaredRoleAndItsTypes() {
            ModuleTopology topology = module("shop-domain", ModuleRole.DOMAIN)
                    .type("com.shop.Order", "shop-domain")
                    .read()
                    .topology();

            assertThat(topology.module("shop-domain"))
                    .map(ModuleDescriptor::role)
                    .contains(ModuleRole.DOMAIN);
            assertThat(topology.typesInModule("shop-domain")).containsExactly(TypeId.of("com.shop.Order"));
        }

        @Test
        @DisplayName("a module depends on another when one of its types names one of the other's")
        void dependsOnWhatItsTypesName() {
            ModuleTopology topology = module("shop-domain", ModuleRole.DOMAIN)
                    .module("shop-infra", ModuleRole.INFRASTRUCTURE)
                    .type("com.shop.Order", "shop-domain")
                    .type("com.shop.jpa.OrderRecord", "shop-infra")
                    .edge("com.shop.jpa.OrderRecord", "com.shop.Order")
                    .read()
                    .topology();

            assertThat(topology.dependenciesOf("shop-infra")).containsExactly("shop-domain");
            assertThat(topology.dependenciesOf("shop-domain")).isEmpty();
        }

        @Test
        @DisplayName("a coupling between two types of the same module is no dependency")
        void ignoresCouplingsInsideAModule() {
            ModuleTopology topology = module("shop-domain", ModuleRole.DOMAIN)
                    .type("com.shop.Order", "shop-domain")
                    .type("com.shop.OrderLine", "shop-domain")
                    .edge("com.shop.Order", "com.shop.OrderLine")
                    .read()
                    .topology();

            assertThat(topology.dependenciesOf("shop-domain")).isEmpty();
        }

        @Test
        @DisplayName("a coupling to a type the classpath supplied is no dependency of the reactor")
        void ignoresCouplingsOutsideTheReactor() {
            ModuleTopology topology = module("shop-domain", ModuleRole.DOMAIN)
                    .type("com.shop.Order", "shop-domain")
                    .type("java.util.List", null)
                    .edge("com.shop.Order", "java.util.List")
                    .read()
                    .topology();

            assertThat(topology.dependenciesOf("shop-domain")).isEmpty();
        }

        @Test
        @DisplayName("a single-module project has no layout to describe")
        void aSingleModuleProjectHasNoLayout() {
            ModuleTopology topology = type("com.shop.Order", null).read().topology();

            assertThat(topology.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("what reads as holding a domain")
    class DomainCandidates {

        @Test
        @DisplayName("a module depending on nothing and reaching no infrastructure")
        void dependsOnNothingAndReachesNoInfrastructure() {
            ModuleTopology topology = module("shop-domain", ModuleRole.DOMAIN)
                    .module("shop-infra", ModuleRole.INFRASTRUCTURE)
                    .type("com.shop.Order", "shop-domain")
                    .type("com.shop.jpa.OrderRecord", "shop-infra")
                    .edge("com.shop.jpa.OrderRecord", "com.shop.Order")
                    .read()
                    .topology();

            assertThat(topology.domainCandidates())
                    .extracting(ModuleDescriptor::name)
                    .containsExactly("shop-domain");
        }

        @Test
        @DisplayName("not a module whose types reach an infrastructure tool, whatever it depends on")
        void notAModuleReachingInfrastructure() {
            ModuleTopology topology = module("shop-domain", ModuleRole.DOMAIN)
                    .type("com.shop.Order", "shop-domain")
                    .read("com.shop.Order")
                    .topology();

            assertThat(topology.domainCandidates()).isEmpty();
        }

        @Test
        @DisplayName("not a module depending on one the project declared nothing about")
        void notAModuleDependingOnAnUndeclaredOne() {
            ModuleTopology topology = module("shop-domain", ModuleRole.DOMAIN)
                    .undeclaredModule("shop-legacy")
                    .type("com.shop.Order", "shop-domain")
                    .type("com.shop.legacy.Ledger", "shop-legacy")
                    .edge("com.shop.Order", "com.shop.legacy.Ledger")
                    .read()
                    .topology();

            assertThat(topology.domainCandidates()).isEmpty();
        }
    }

    @Nested
    @DisplayName("what the reading leaves out")
    class LeftOut {

        @Test
        @DisplayName("a module read without a declared role is named, and stays out of the layout")
        void namesAModuleWithoutADeclaredRole() {
            Modules modules = module("shop-domain", ModuleRole.DOMAIN)
                    .undeclaredModule("shop-legacy")
                    .type("com.shop.legacy.Ledger", "shop-legacy")
                    .read();

            assertThat(modules.topology().module("shop-legacy")).isEmpty();
            assertThat(modules.diagnostics()).singleElement().satisfies(diagnostic -> {
                assertThat(diagnostic.code().value()).isEqualTo("HG-ENGINE-004");
                assertThat(diagnostic.severity()).isEqualTo(DiagnosticSeverity.WARNING);
                assertThat(diagnostic.message()).contains("shop-legacy").contains("modules");
            });
        }

        @Test
        @DisplayName("a project that declared every module it read has nothing to report")
        void saysNothingWhenEveryModuleIsDeclared() {
            Modules modules = module("shop-domain", ModuleRole.DOMAIN)
                    .type("com.shop.Order", "shop-domain")
                    .read();

            assertThat(modules.diagnostics()).isEmpty();
        }

        @Test
        @DisplayName("a single-module project is silent, having no module to declare")
        void silentOnASingleModuleProject() {
            Modules modules = read();

            assertThat(modules.diagnostics()).isEmpty();
            assertThat(modules.topology().isEmpty()).isTrue();
        }

        @Test
        @DisplayName("a type of an undeclared module is assigned to nothing rather than to a role nobody stated")
        void leavesTypesOfUndeclaredModulesUnassigned() {
            Modules modules = undeclaredModule("shop-legacy")
                    .type("com.shop.legacy.Ledger", "shop-legacy")
                    .read();

            assertThat(modules.topology().moduleOf(TypeId.of("com.shop.legacy.Ledger")))
                    .isEmpty();
        }
    }
}
