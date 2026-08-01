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

package io.hexaglue.model.config;

import io.hexaglue.model.classification.Confidence;
import io.hexaglue.model.finding.IssueCode;
import io.hexaglue.model.finding.Severity;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The gates the validate goal applies to a classified model: statistics-based gates on the
 * classifications and per-code severity thresholds on the findings — one mechanism, the same
 * findings the audit displays.
 *
 * <p>The defaults are permissive: arming a gate is an explicit choice. This record is shape
 * only — evaluating the gates belongs to the engine, binding them from YAML to the loader.</p>
 *
 * @param failOnUnclassified whether an UNCLASSIFIED verdict fails the build
 * @param minConfidence the weakest confidence accepted, ports included; LOW gates nothing
 * @param failOnAmbiguous whether a verdict with kept candidates fails the build
 * @param allowInferred whether INFERRED verdicts are accepted; false demands DECLARED everywhere
 * @param findingThresholds per-code severity overrides, iterated in code order
 * @since 7.0.0
 */
public record ValidationConfig(
        boolean failOnUnclassified,
        Confidence minConfidence,
        boolean failOnAmbiguous,
        boolean allowInferred,
        Map<IssueCode, Severity> findingThresholds) {

    /**
     * Validates the gates and copies the thresholds into a code-ordered view.
     */
    public ValidationConfig {
        Objects.requireNonNull(minConfidence, "minConfidence must not be null");
        Objects.requireNonNull(findingThresholds, "findingThresholds must not be null");
        SortedMap<IssueCode, Severity> ordered = new TreeMap<>();
        findingThresholds.forEach((code, severity) -> {
            Objects.requireNonNull(code, "finding code must not be null");
            Objects.requireNonNull(severity, "finding severity must not be null");
            ordered.put(code, severity);
        });
        findingThresholds = Collections.unmodifiableSortedMap(ordered);
    }

    /**
     * Returns the permissive defaults: no gate armed, LOW confidence floor, inferred verdicts
     * accepted, no per-code threshold.
     *
     * @return the default gates
     */
    public static ValidationConfig defaults() {
        return builder().build();
    }

    /**
     * Creates a builder starting from the permissive defaults.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ValidationConfig} instances.
     *
     * @since 7.0.0
     */
    public static final class Builder {

        private boolean failOnUnclassified;
        private Confidence minConfidence = Confidence.LOW;
        private boolean failOnAmbiguous;
        private boolean allowInferred = true;
        private Map<IssueCode, Severity> findingThresholds = Map.of();

        private Builder() {}

        /**
         * Arms the gate on UNCLASSIFIED verdicts.
         *
         * @param failOnUnclassified whether an UNCLASSIFIED verdict fails the build
         * @return this builder
         */
        public Builder failOnUnclassified(boolean failOnUnclassified) {
            this.failOnUnclassified = failOnUnclassified;
            return this;
        }

        /**
         * Sets the weakest confidence accepted.
         *
         * @param minConfidence the confidence floor, ports included
         * @return this builder
         */
        public Builder minConfidence(Confidence minConfidence) {
            this.minConfidence = minConfidence;
            return this;
        }

        /**
         * Arms the gate on ambiguous verdicts.
         *
         * @param failOnAmbiguous whether a verdict with kept candidates fails the build
         * @return this builder
         */
        public Builder failOnAmbiguous(boolean failOnAmbiguous) {
            this.failOnAmbiguous = failOnAmbiguous;
            return this;
        }

        /**
         * Sets whether inferred verdicts are accepted.
         *
         * @param allowInferred false demands a DECLARED basis everywhere
         * @return this builder
         */
        public Builder allowInferred(boolean allowInferred) {
            this.allowInferred = allowInferred;
            return this;
        }

        /**
         * Sets the per-code severity thresholds.
         *
         * @param findingThresholds the overrides by issue code
         * @return this builder
         */
        public Builder findingThresholds(Map<IssueCode, Severity> findingThresholds) {
            this.findingThresholds = findingThresholds;
            return this;
        }

        /**
         * Builds the gates.
         *
         * @return a new ValidationConfig
         */
        public ValidationConfig build() {
            return new ValidationConfig(
                    failOnUnclassified, minConfidence, failOnAmbiguous, allowInferred, findingThresholds);
        }
    }
}
