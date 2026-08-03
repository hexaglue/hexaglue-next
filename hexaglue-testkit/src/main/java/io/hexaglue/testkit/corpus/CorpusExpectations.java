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

package io.hexaglue.testkit.corpus;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * What a corpus scenario is held to mean.
 *
 * <p>The scenarios were harvested from the legacy suite, whose engine carries confirmed bugs and
 * whose vocabulary the v7 model does not share. What the old engine answered is therefore an
 * observation, never an oracle: an expectation counts only once a human has read the legacy
 * assertions, decided what is actually true, and marked the file reviewed. A draft is carried
 * along and ignored, so the reference grows by review rather than by import.</p>
 *
 * <p>The format is deliberately plain — comments, one status line, one claim per line:</p>
 *
 * <pre>{@code
 * status: reviewed
 * expect: com.example.Order = AGGREGATE_ROOT
 * reject: com.example.Order = VALUE_OBJECT
 * expect: com.example.OrderEntity = NO VERDICT
 * }</pre>
 *
 * <p>A claim usually names an {@code ArchKind}. {@code NO VERDICT} is the one exception: it is
 * what the harness renders when a type reached no verdict at all, and claiming it is how a
 * scenario states that a type is absent from the analyzed model — generated code, which the
 * frontend keeps out, rather than a type the engine considered and could not name.</p>
 *
 * @param scenarioId the scenario these expectations belong to
 * @param reviewed whether a human has vouched for them
 * @param claims what the scenario is held to mean, in file order
 * @since 7.0.0
 */
public record CorpusExpectations(String scenarioId, boolean reviewed, List<Claim> claims) {

    /**
     * What a claim names when a type reached no verdict at all, and the harness has no kind to
     * render. Stated once here, next to the format it belongs to, so that everything comparing a
     * model against a claim spells the absence the same way.
     *
     * @since 7.0.0
     */
    public static final String NO_VERDICT = "NO VERDICT";

    private static final String REVIEWED = "status: reviewed";
    private static final String EXPECT = "expect:";
    private static final String REJECT = "reject:";

    /**
     * Validates the components and copies the claims.
     */
    public CorpusExpectations {
        Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        Objects.requireNonNull(claims, "claims must not be null");
        claims = List.copyOf(claims);
    }

    /**
     * Reads the expectations recorded next to a scenario.
     *
     * @param scenario the scenario
     * @return its expectations, empty and unreviewed when the scenario has no file yet
     */
    public static CorpusExpectations of(CorpusScenario scenario) {
        Objects.requireNonNull(scenario, "scenario must not be null");
        return of(scenario.profile(), scenario.id());
    }

    /**
     * Reads the expectations recorded next to a scenario named by its profile and id.
     *
     * @param profile the profile the scenario belongs to
     * @param scenarioId the scenario id
     * @return its expectations, empty and unreviewed when the scenario has no file yet
     */
    public static CorpusExpectations of(CorpusProfile profile, String scenarioId) {
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(scenarioId, "scenarioId must not be null");
        String resource = Corpus.rootOf(profile) + "/" + scenarioId + "/expectations.txt";
        try (InputStream stream = CorpusExpectations.class.getResourceAsStream(resource)) {
            if (stream == null) {
                return new CorpusExpectations(scenarioId, false, List.of());
            }
            return parse(scenarioId, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read corpus expectations " + resource, e);
        }
    }

    private static CorpusExpectations parse(String scenarioId, String content) {
        boolean reviewed = false;
        List<Claim> claims = new ArrayList<>();
        for (String raw : content.lines().map(String::strip).toList()) {
            if (raw.isEmpty() || raw.startsWith("#")) {
                continue;
            }
            if (REVIEWED.equals(raw)) {
                reviewed = true;
            } else if (raw.startsWith(EXPECT)) {
                claims.add(claim(scenarioId, raw.substring(EXPECT.length()), false));
            } else if (raw.startsWith(REJECT)) {
                claims.add(claim(scenarioId, raw.substring(REJECT.length()), true));
            }
        }
        return new CorpusExpectations(scenarioId, reviewed, claims);
    }

    private static Claim claim(String scenarioId, String body, boolean rejected) {
        int separator = body.indexOf('=');
        if (separator < 0) {
            throw new IllegalStateException(
                    "Malformed expectation in " + scenarioId + ": expected 'type = KIND', got '" + body.strip() + "'");
        }
        return new Claim(
                body.substring(0, separator).strip(),
                body.substring(separator + 1).strip(),
                rejected);
    }

    /**
     * Returns whether these expectations can be scored: a draft states nothing anyone vouched for.
     *
     * @return true when reviewed and not empty
     */
    public boolean isScorable() {
        return reviewed && !claims.isEmpty();
    }

    /**
     * One thing a scenario is held to mean about one type.
     *
     * @param qualifiedName the type the claim speaks about
     * @param kind the kind named, as an {@code ArchKind} constant
     * @param rejected true when the kind is the one the type must <em>not</em> receive
     * @since 7.0.0
     */
    public record Claim(String qualifiedName, String kind, boolean rejected) {

        /**
         * Validates that the claim names both a type and a kind.
         */
        public Claim {
            Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
            Objects.requireNonNull(kind, "kind must not be null");
            if (qualifiedName.isBlank() || kind.isBlank()) {
                throw new IllegalArgumentException("a claim names a type and a kind");
            }
        }

        /**
         * Returns whether the given kind satisfies this claim.
         *
         * @param actual the kind the engine decided, as an {@code ArchKind} name
         * @return true when the claim holds
         */
        public boolean isSatisfiedBy(String actual) {
            return rejected != kind.equals(actual);
        }

        @Override
        public String toString() {
            return (rejected ? "reject " : "expect ") + qualifiedName + " = " + kind;
        }
    }
}
