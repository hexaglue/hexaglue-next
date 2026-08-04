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

package io.hexaglue.plugin.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.finding.Diagnostic;
import io.hexaglue.model.finding.IssueCode;
import io.hexaglue.spi.HexaGluePlugin;
import io.hexaglue.spi.Measurements;
import io.hexaglue.spi.PluginExecutor;
import io.hexaglue.spi.PluginRun;
import io.hexaglue.spi.SourceFile;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * The persistence a domain gets, and what is said about the parts of it that got none. The plugin
 * is run through the executor rather than called directly: that is the only path a build takes.
 */
class JpaPluginTest {

    private static PluginRun run(Map<String, String> options) {
        return run(ShopFixture.model(), Confidence.HIGH, options);
    }

    private static PluginRun run(ArchModel model, Confidence threshold, Map<String, String> options) {
        List<HexaGluePlugin> plugins = List.of(new JpaPlugin());
        return PluginExecutor.run(
                plugins, model, List.of(), Measurements.none(), threshold, Map.of(JpaPlugin.ID, options));
    }

    private static String source(PluginRun run, String qualifiedName) {
        return run.sources().stream()
                .filter(file -> qualifiedName.equals(file.qualifiedName()))
                .map(SourceFile::content)
                .findFirst()
                .orElseThrow(() -> new AssertionError(qualifiedName + " was not generated, only "
                        + run.sources().stream().map(SourceFile::qualifiedName).toList()));
    }

    /**
     * The generated source with its line breaks flattened. An annotation carrying a member is
     * printed over several lines, and a test that reads code should be asking what it says rather
     * than how it was laid out.
     */
    private static String flat(String source) {
        return source.replaceAll("\\s+", " ");
    }

    private static Optional<Diagnostic> coded(PluginRun run, IssueCode code) {
        return run.diagnostics().stream()
                .filter(diagnostic -> diagnostic.code().equals(code))
                .findFirst();
    }

    @Nested
    @DisplayName("writes an entity for what has a life of its own")
    class WritesAnEntity {

        @Test
        @DisplayName("for the aggregate and for its parts, and nothing for what has no table")
        void forTheAggregateAndItsParts() {
            PluginRun run = run(Map.of());

            assertThat(run.executed()).containsExactly(JpaPlugin.ID);
            assertThat(run.sources())
                    .extracting(SourceFile::qualifiedName)
                    .contains("com.shop.domain.OrderEntity", "com.shop.domain.OrderLineEntity")
                    .doesNotContain("com.shop.domain.OrderIdEntity", "com.shop.domain.CustomerIdEntity");
        }

        @Test
        @DisplayName("and says nothing at all about what the store has no shape for")
        void andSaysNothingAboutWhatItHasNoShapeFor() {
            PluginRun run = run(Map.of());

            assertThat(run.sources())
                    .extracting(SourceFile::qualifiedName)
                    .noneMatch(name -> name.contains("OrderPlaced"));
            assertThat(run.diagnostics())
                    .noneSatisfy(diagnostic -> assertThat(diagnostic.message()).contains("OrderPlaced"));
        }

        @Test
        @DisplayName("under a table named after it, kept away from what SQL reserves")
        void underATableNamedAfterIt() {
            String entity = source(run(Map.of()), "com.shop.domain.OrderEntity");

            assertThat(flat(entity)).contains("@Entity").contains("@Table( name = \"orders\" )");
        }

        @Test
        @DisplayName("storing the identity as the value it wraps, which is what a query matches")
        void storingTheIdentityAsTheValueItWraps() {
            String entity = source(run(Map.of()), "com.shop.domain.OrderEntity");

            assertThat(entity).contains("@Id").contains("private UUID id;").doesNotContain("private OrderId id;");
        }

        @Test
        @DisplayName("and leaving generation to the database only when asked")
        void andLeavingGenerationToTheDatabaseOnlyWhenAsked() {
            assertThat(source(run(Map.of()), "com.shop.domain.OrderEntity")).doesNotContain("@GeneratedValue");
            assertThat(flat(source(run(Map.of("idStrategy", "IDENTITY")), "com.shop.domain.OrderEntity")))
                    .contains("@GeneratedValue( strategy = GenerationType.IDENTITY )");
        }
    }

    @Nested
    @DisplayName("reads what each field holds from the verdict on it")
    class ReadsWhatEachFieldHolds {

        private String order() {
            return source(run(Map.of()), "com.shop.domain.OrderEntity");
        }

        @Test
        @DisplayName("a value goes into the row that holds it")
        void aValueGoesIntoTheRow() {
            assertThat(flat(order())).contains("@Embedded private MoneyEmbeddable total;");
        }

        @Test
        @DisplayName("a collection of parts is a relation, and each part its own row")
        void aCollectionOfPartsIsARelation() {
            assertThat(flat(order())).contains("@OneToMany private List<OrderLineEntity> lines;");
        }

        @Test
        @DisplayName("another aggregate named by its identity stays a column, not a join")
        void anotherAggregateNamedByItsIdentity() {
            assertThat(flat(order()))
                    .contains("@Column( name = \"customer\" ) private UUID customer;")
                    .doesNotContain("private CustomerEntity customer;");
        }

        @Test
        @DisplayName("but a part held whole is joined to, since it has a row of its own")
        void aPartHeldWholeIsJoinedTo() {
            assertThat(flat(order())).contains("@ManyToOne private ShipmentEntity shipment;");
        }

        @Test
        @DisplayName("and so is another aggregate held whole, which is a thing the audit will say something about")
        void andSoIsAnotherAggregateHeldWhole() {
            assertThat(flat(order())).contains("@ManyToOne private InvoiceEntity invoice;");
        }

        @Test
        @DisplayName("and an identity nothing could see inside is stored as itself")
        void anIdentityNothingCouldSeeInsideIsStoredAsItself() {
            assertThat(source(run(Map.of()), "com.shop.domain.OrderLineEntity")).contains("private TagId tag;");
        }

        @Test
        @DisplayName("and a constant belongs to no row at all")
        void andAConstantBelongsToNoRow() {
            assertThat(order()).doesNotContain("PREFIX");
        }

        @Test
        @DisplayName("a column whose name SQL reserves is moved out of the way")
        void aColumnSqlReservesIsMovedOutOfTheWay() {
            assertThat(flat(source(run(Map.of()), "com.shop.domain.OrderLineEntity")))
                    .contains("@Column( name = \"value_col\" )");
        }
    }

    @Nested
    @DisplayName("writes an embeddable for a value")
    class WritesAnEmbeddable {

        @Test
        @DisplayName("with no table and no identity of its own")
        void withNoTableAndNoIdentity() {
            String money = source(run(Map.of()), "com.shop.domain.MoneyEmbeddable");

            assertThat(money).contains("@Embeddable").doesNotContain("@Entity").doesNotContain("@Id");
        }

        @Test
        @DisplayName("unless the build would rather have none")
        void unlessTheBuildWouldRatherHaveNone() {
            PluginRun run = run(Map.of("generateEmbeddables", "false"));

            assertThat(run.sources())
                    .extracting(SourceFile::qualifiedName)
                    .doesNotContain("com.shop.domain.MoneyEmbeddable");
        }
    }

    @Nested
    @DisplayName("declines rather than writing something it is unsure of")
    class DeclinesRatherThanGuessing {

        @Test
        @DisplayName("below the threshold, saying what would make the verdict surer")
        void belowTheThreshold() {
            PluginRun run = run(ShopFixture.model(Confidence.LOW), Confidence.HIGH, Map.of());

            assertThat(run.sources())
                    .extracting(SourceFile::qualifiedName)
                    .doesNotContain("com.shop.domain.OrderEntity");
            Diagnostic refused = coded(run, JpaPlugin.TOO_UNSURE).orElseThrow();
            assertThat(refused.message())
                    .contains("com.shop.domain.Order")
                    .contains("LOW")
                    .contains("HIGH");
            assertThat(refused.remediations()).isNotEmpty();
        }

        @Test
        @DisplayName("and goes on writing everything it is sure of")
        void andGoesOnWritingEverythingElse() {
            PluginRun run = run(ShopFixture.model(Confidence.LOW), Confidence.HIGH, Map.of());

            assertThat(run.executed()).containsExactly(JpaPlugin.ID);
            assertThat(run.sources())
                    .extracting(SourceFile::qualifiedName)
                    .contains("com.shop.domain.OrderLineEntity", "com.shop.domain.MoneyEmbeddable");
        }

        @Test
        @DisplayName("and when nothing names the field carrying an identity")
        void andWhenNothingNamesTheIdentity() {
            PluginRun run = run(Map.of());

            assertThat(run.sources())
                    .extracting(SourceFile::qualifiedName)
                    .doesNotContain("com.shop.domain.CustomerEntity");
            assertThat(coded(run, JpaPlugin.NO_IDENTITY).orElseThrow().message())
                    .contains("com.shop.domain.Customer");
        }
    }

    @Nested
    @DisplayName("serves a repository port from a Spring Data interface")
    class ServesARepositoryPort {

        private String orders() {
            return source(run(Map.of()), "com.shop.domain.OrderJpaRepository");
        }

        @Test
        @DisplayName("keyed by the value the identity is written around")
        void keyedByTheValueTheIdentityIsWrittenAround() {
            assertThat(flat(orders()))
                    .contains("interface OrderJpaRepository extends JpaRepository<OrderEntity, UUID>");
        }

        /**
         * Which method deserves a query is settled by matching its parameters against the fields of
         * the aggregate — never by reading its name, which is what the carrière did.
         */
        @Test
        @DisplayName("asking about a field the aggregate holds, whatever the port called the method")
        void askingAboutAFieldTheAggregateHolds() {
            assertThat(flat(orders())).contains("List<OrderEntity> findByCustomer(UUID customer)");
        }

        @Test
        @DisplayName("and letting the answer say which question it is")
        void andLettingTheAnswerSayWhichQuestionItIs() {
            assertThat(flat(orders()))
                    .contains("boolean existsByCustomer(UUID customer)")
                    .contains("long countByCustomer(UUID customer)");
        }

        @Test
        @DisplayName("answering with one row or with many, as the port asked")
        void answeringWithOneRowOrWithMany() {
            assertThat(flat(orders()))
                    .contains("List<OrderEntity> findByCustomer(UUID customer)")
                    .contains("Optional<OrderEntity> findByShipment(ShipmentEntity shipment)");
        }

        @Test
        @DisplayName("and asking each question once, however many ways the port asks it")
        void andAskingEachQuestionOnce() {
            assertThat(flat(orders()).split("findByCustomer", -1)).hasSize(2);
        }

        @Test
        @DisplayName("and asking nothing about a value the aggregate does not hold")
        void andAskingNothingAboutAValueTheAggregateDoesNotHold() {
            assertThat(orders()).doesNotContain("Instant");
        }

        @Test
        @DisplayName("but writing nothing for what the store already answers")
        void butWritingNothingForWhatTheStoreAlreadyAnswers() {
            assertThat(orders()).doesNotContain("findByIdBy").doesNotContain("findById(");
        }

        @Test
        @DisplayName("and nothing at all for a port that keeps nothing")
        void andNothingAtAllForAPortThatKeepsNothing() {
            PluginRun run = run(Map.of());

            assertThat(run.sources())
                    .extracting(SourceFile::qualifiedName)
                    .doesNotContain("com.shop.domain.AuditingJpaRepository");
            assertThat(coded(run, JpaPlugin.NOTHING_KEPT).orElseThrow().message())
                    .contains("com.shop.domain.Auditing");
        }

        @Test
        @DisplayName("and nothing for a way out that is not a store")
        void andNothingForAWayOutThatIsNotAStore() {
            assertThat(run(Map.of()).sources())
                    .extracting(SourceFile::qualifiedName)
                    .doesNotContain("com.shop.domain.NotifyingJpaRepository");
        }

        @Test
        @DisplayName("and nothing when the verdict on the port falls short")
        void andNothingWhenTheVerdictFallsShort() {
            PluginRun run = run(ShopFixture.model(Confidence.LOW), Confidence.HIGH, Map.of());

            assertThat(run.sources())
                    .extracting(SourceFile::qualifiedName)
                    .doesNotContain("com.shop.domain.OrderJpaRepository");
        }

        @Test
        @DisplayName("and nothing when the aggregate it keeps has no identity to serve rows by")
        void andNothingWhenTheAggregateHasNoIdentity() {
            PluginRun run = run(Map.of());

            assertThat(run.sources())
                    .extracting(SourceFile::qualifiedName)
                    .doesNotContain("com.shop.domain.CustomerJpaRepository");
            assertThat(coded(run, JpaPlugin.NO_IDENTITY).orElseThrow().message())
                    .contains("com.shop.domain.Customer");
        }

        @Test
        @DisplayName("unless the build would rather serve its ports itself")
        void unlessTheBuildWouldRatherServeItsPortsItself() {
            assertThat(run(Map.of("generateRepositories", "false")).sources())
                    .extracting(SourceFile::qualifiedName)
                    .doesNotContain("com.shop.domain.OrderJpaRepository");
        }
    }

    @Nested
    @DisplayName("routes what it writes")
    class RoutesWhatItWrites {

        @Test
        @DisplayName("to the module the build named, and to none when it named none")
        void writesIntoTheModuleTheBuildNamed() {
            assertThat(run(Map.of("targetModule", "shop-persistence")).sources())
                    .allSatisfy(file -> assertThat(file.module()).contains("shop-persistence"));
            assertThat(run(Map.of()).sources())
                    .allSatisfy(file -> assertThat(file.module()).isEmpty());
        }
    }
}
