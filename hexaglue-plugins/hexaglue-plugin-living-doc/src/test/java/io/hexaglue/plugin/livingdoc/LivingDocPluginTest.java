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

package io.hexaglue.plugin.livingdoc;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.spi.Document;
import io.hexaglue.spi.HexaGluePlugin;
import io.hexaglue.spi.PluginExecutor;
import io.hexaglue.spi.PluginRun;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The plugin runs the way a host runs it — through the executor, on a model and a set of stated
 * options — because that is the only path a build ever takes.
 */
class LivingDocPluginTest {

    private static PluginRun run(Map<String, String> options) {
        List<HexaGluePlugin> plugins = List.of(new LivingDocPlugin());
        return PluginExecutor.run(plugins, ShopFixture.model(), Map.of(LivingDocPlugin.ID, options));
    }

    private static String document(PluginRun run, String name) {
        return run.documents().stream()
                .filter(document -> document.path().endsWith(name))
                .map(Document::content)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no document named " + name + " in " + run.documents()));
    }

    @Nested
    @DisplayName("what a run produces")
    class Produced {

        @Test
        @DisplayName("emits the three pages under the directory it was given")
        void emitsThreePages() {
            PluginRun run = run(Map.of("outputDirectory", "docs/architecture"));

            assertThat(run.executed()).containsExactly(LivingDocPlugin.ID);
            assertThat(run.diagnostics()).isEmpty();
            assertThat(run.documents())
                    .extracting(Document::path)
                    .containsExactly(
                            "docs/architecture/README.md", "docs/architecture/domain.md", "docs/architecture/ports.md");
        }

        @Test
        @DisplayName("writes under living-doc when nobody says where")
        void defaultsToLivingDoc() {
            assertThat(run(Map.of()).documents())
                    .extracting(Document::path)
                    .allSatisfy(path -> assertThat(path).startsWith("living-doc/"));
        }
    }

    @Nested
    @DisplayName("the way in")
    class Overview {

        @Test
        @DisplayName("counts what the model holds, kind by kind")
        void countsKinds() {
            String overview = document(run(Map.of()), OverviewDocument.NAME);

            assertThat(overview).contains("| AGGREGATE_ROOT | 1 |").contains("| DRIVEN_PORT | 1 |");
        }

        @Test
        @DisplayName("says where the types live")
        void listsPackages() {
            String overview = document(run(Map.of()), OverviewDocument.NAME);

            assertThat(overview).contains("`com.shop.domain`").contains("`com.shop.application`");
        }

        @Test
        @DisplayName("indexes a type towards the page that documents it")
        void indexesTowardsThePages() {
            String overview = document(run(Map.of()), OverviewDocument.NAME);

            assertThat(overview)
                    .contains("[Order](domain.md#com-shop-domain-order)")
                    .contains("[Orders](ports.md#com-shop-domain-orders)");
        }

        @Test
        @DisplayName("lists what could not be read rather than dropping it")
        void listsWhatCouldNotBeRead() {
            String overview = document(run(Map.of()), OverviewDocument.NAME);

            assertThat(overview)
                    .contains("`com.shop.util.StringUtils`")
                    .contains("UTILITY")
                    .contains("nothing of the perimeter uses it");
        }
    }

    @Nested
    @DisplayName("the domain page")
    class Domain {

        @Test
        @DisplayName("says what identifies an aggregate and what it is made of")
        void describesAnAggregate() {
            String domain = document(run(Map.of()), DomainDocument.NAME);

            assertThat(domain)
                    .contains("<a id=\"com-shop-domain-order\"></a>")
                    .contains("### Order")
                    .contains("**Identified by**: `OrderId`, on `id`")
                    .contains("**Entities**: [OrderLine](#com-shop-domain-orderline)")
                    .contains("**Values**: [Money](#com-shop-domain-money)")
                    .contains("**Events**: [OrderPlaced](#com-shop-domain-orderplaced)")
                    .contains("**Persisted through**: [Orders](ports.md#com-shop-domain-orders)");
        }

        @Test
        @DisplayName("says what an identifier wraps and what it identifies")
        void describesAnIdentifier() {
            String domain = document(run(Map.of()), DomainDocument.NAME);

            assertThat(domain).contains("| OrderId | `UUID` |");
        }

        @Test
        @DisplayName("draws the aggregate when asked, and stays silent when refused")
        void drawsOnlyWhenAsked() {
            assertThat(document(run(Map.of()), DomainDocument.NAME))
                    .contains("```mermaid")
                    .contains("<<AggregateRoot>>")
                    .contains("identified by OrderId");
            assertThat(document(run(Map.of("generateDiagrams", "false")), DomainDocument.NAME))
                    .doesNotContain("```mermaid");
        }

        @Test
        @DisplayName("shows no more properties in a diagram than it was told to")
        void capsDiagramProperties() {
            String domain = document(run(Map.of("propertiesPerDiagram", "1")), DomainDocument.NAME);

            String diagram = domain.substring(domain.indexOf("```mermaid"), domain.indexOf("## Aggregates"));
            assertThat(diagram).contains("+OrderId id").doesNotContain("+Money total");
        }
    }

    @Nested
    @DisplayName("the boundary page")
    class Ports {

        @Test
        @DisplayName("says which way a port faces and what a driven one manages")
        void describesPorts() {
            String ports = document(run(Map.of()), PortsDocument.NAME);

            assertThat(ports)
                    .contains("## Driving ports")
                    .contains("### PlaceOrder")
                    .contains("## Driven ports")
                    .contains("**Family**: REPOSITORY")
                    .contains("**Manages**: [Order](domain.md#com-shop-domain-order)")
                    .contains("`void save(Order order)`");
        }

        @Test
        @DisplayName("draws the boundary with the aggregate its port manages")
        void drawsTheBoundary() {
            String ports = document(run(Map.of()), PortsDocument.NAME);

            assertThat(ports)
                    .contains("subgraph driving[\"Driving\"]")
                    .contains("subgraph driven[\"Driven\"]")
                    .contains("com_shop_domain_Orders -->|\"manages\"| com_shop_domain_Order");
        }
    }

    @Nested
    @DisplayName("what a verdict rests on")
    class Provenance {

        @Test
        @DisplayName("folds the evidence of every verdict into the page")
        void showsProvenance() {
            String domain = document(run(Map.of()), DomainDocument.NAME);

            assertThat(domain)
                    .contains("<summary>What this verdict rests on</summary>")
                    .contains("**Confidence**: HIGH — **basis**: INFERRED")
                    .contains("| S3 | `managed-by(AGGREGATE_ROOT)` | a repository names it |");
        }

        @Test
        @DisplayName("leaves it out when the author does not want it")
        void omitsProvenanceOnRequest() {
            String domain = document(run(Map.of("includeProvenance", "false")), DomainDocument.NAME);

            assertThat(domain).doesNotContain("What this verdict rests on");
        }
    }

    @Nested
    @DisplayName("options")
    class Options {

        @Test
        @DisplayName("declares every option it reads, so a typo is refused rather than ignored")
        void declaresWhatItReads() {
            PluginRun refused = run(Map.of("outputDirectorie", "typo"));

            assertThat(refused.executed()).isEmpty();
            assertThat(refused.diagnostics())
                    .singleElement()
                    .satisfies(diagnostic -> assertThat(diagnostic.message()).contains("outputDirectorie"));
        }

        @Test
        @DisplayName("refuses a diagram budget that makes no sense")
        void refusesANegativeBudget() {
            PluginRun refused = run(Map.of("propertiesPerDiagram", "-1"));

            assertThat(refused.executed()).isEmpty();
            assertThat(refused.diagnostics()).isNotEmpty();
        }
    }
}
