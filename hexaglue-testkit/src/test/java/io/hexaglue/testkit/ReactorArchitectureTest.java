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

package io.hexaglue.testkit;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Self-applied architecture rules of the reactor: the module boundaries HexaGlue preaches are
 * enforced on its own code. Rules allow an empty match so they hold from the first commit and
 * bite as modules are added.
 */
@AnalyzeClasses(packages = "io.hexaglue", importOptions = ImportOption.DoNotIncludeTests.class)
@SuppressWarnings("PMD.TestClassWithoutTestCases") // @ArchTest fields run through the ArchUnit JUnit engine
class ReactorArchitectureTest {

    @ArchTest
    static final ArchRule SPOON_CONFINED_TO_FRONTEND = noClasses()
            .that()
            .resideOutsideOfPackage("io.hexaglue.frontend..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("spoon..")
            .because("Spoon is an implementation detail of hexaglue-frontend; the boundary is the code model")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule MAVEN_CONFINED_TO_MAVEN_PLUGIN = noClasses()
            .that()
            .resideOutsideOfPackage("io.hexaglue.maven..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("org.apache.maven..")
            .because("Maven APIs stay in the thin hexaglue-maven-plugin adapter")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule TESTKIT_STAYS_ENGINE_AGNOSTIC = noClasses()
            .that()
            .resideInAPackage("io.hexaglue.testkit..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("io.hexaglue.engine..", "io.hexaglue.frontend..", "io.hexaglue.knowledge..")
            .because("the testkit reaches the engine only through the AnalysisRunner service interface")
            .allowEmptyShould(true);
}
