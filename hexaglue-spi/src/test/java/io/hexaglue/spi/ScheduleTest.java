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

package io.hexaglue.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ScheduleTest {

    private static PluginManifest manifest(String id, String... dependsOn) {
        return new PluginManifest(id, List.of(dependsOn), Set.of());
    }

    @Nested
    @DisplayName("ordering")
    class Ordering {

        @Test
        @DisplayName("runs a dependency before the plugin that declares it")
        void dependencyFirst() {
            Schedule schedule = Schedule.of(List.of(manifest("b", "a"), manifest("a")));

            assertThat(schedule.order()).containsExactly("a", "b");
        }

        @Test
        @DisplayName("resolves plugins declared in reverse dependency order")
        void reverseDeclarationOrder() {
            Schedule schedule =
                    Schedule.of(List.of(manifest("d", "c"), manifest("c", "b"), manifest("b", "a"), manifest("a")));

            assertThat(schedule.order()).containsExactly("a", "b", "c", "d");
            assertThat(schedule.excluded()).isEmpty();
        }

        @Test
        @DisplayName("orders independent plugins by identifier")
        void independentPluginsInIdentifierOrder() {
            Schedule schedule = Schedule.of(List.of(manifest("zeta"), manifest("alpha"), manifest("mu")));

            assertThat(schedule.order()).containsExactly("alpha", "mu", "zeta");
        }
    }

    @Nested
    @DisplayName("what falls out of the order")
    class Exclusions {

        @Test
        @DisplayName("excludes a plugin whose dependency is nowhere on the classpath")
        void missingDependency() {
            Schedule schedule = Schedule.of(List.of(manifest("a"), manifest("b", "absent")));

            assertThat(schedule.order()).containsExactly("a");
            assertThat(schedule.excluded()).singleElement().satisfies(exclusion -> {
                assertThat(exclusion.pluginId()).isEqualTo("b");
                assertThat(exclusion.reason()).isEqualTo(Schedule.Reason.MISSING_DEPENDENCY);
                assertThat(exclusion.involved()).containsExactly("absent");
            });
        }

        @Test
        @DisplayName("excludes the plugins of a dependency cycle, and only those")
        void cycle() {
            Schedule schedule = Schedule.of(List.of(manifest("a"), manifest("b", "c"), manifest("c", "b")));

            assertThat(schedule.order()).containsExactly("a");
            assertThat(schedule.excluded())
                    .extracting(Schedule.Exclusion::pluginId, Schedule.Exclusion::reason)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("b", Schedule.Reason.CYCLE),
                            org.assertj.core.groups.Tuple.tuple("c", Schedule.Reason.CYCLE));
        }

        @Test
        @DisplayName("excludes a plugin that depends on itself")
        void selfDependency() {
            Schedule schedule = Schedule.of(List.of(manifest("a", "a")));

            assertThat(schedule.order()).isEmpty();
            assertThat(schedule.excluded())
                    .singleElement()
                    .satisfies(exclusion -> assertThat(exclusion.reason()).isEqualTo(Schedule.Reason.CYCLE));
        }

        @Test
        @DisplayName("excludes what depends on an excluded plugin, transitively")
        void blockedByAnExcludedDependency() {
            Schedule schedule = Schedule.of(List.of(manifest("a", "absent"), manifest("b", "a"), manifest("c", "b")));

            assertThat(schedule.order()).isEmpty();
            assertThat(schedule.excluded())
                    .extracting(Schedule.Exclusion::pluginId, Schedule.Exclusion::reason)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("a", Schedule.Reason.MISSING_DEPENDENCY),
                            org.assertj.core.groups.Tuple.tuple("b", Schedule.Reason.BLOCKED),
                            org.assertj.core.groups.Tuple.tuple("c", Schedule.Reason.BLOCKED));
        }

        @Test
        @DisplayName("keeps the first of two plugins claiming the same identifier, and still runs it")
        void duplicateIdentifier() {
            Schedule schedule = Schedule.of(List.of(manifest("a"), manifest("a")));

            assertThat(schedule.order()).containsExactly("a");
            assertThat(schedule.excluded()).isEmpty();
            assertThat(schedule.duplicates()).containsExactly("a");
        }
    }

    @Nested
    @DisplayName("dependents")
    class Dependents {

        @Test
        @DisplayName("names every plugin that would run after a given one, transitively")
        void transitiveDependents() {
            Schedule schedule =
                    Schedule.of(List.of(manifest("a"), manifest("b", "a"), manifest("c", "b"), manifest("d")));

            assertThat(schedule.dependentsOf("a")).containsExactly("b", "c");
            assertThat(schedule.dependentsOf("d")).isEmpty();
        }
    }
}
