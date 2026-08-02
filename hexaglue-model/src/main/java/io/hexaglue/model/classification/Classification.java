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

package io.hexaglue.model.classification;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.PortDirection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The complete verdict on a type: the kind, one confidence, the basis, the tiered evidences, the
 * losing candidates when the decision was ambiguous, the proof of the derivation and the
 * remediation hints. This is the trace the whole downstream reads — validation gates on it, the
 * audit reports it, generation thresholds on it.
 *
 * @param kind the decided kind
 * @param direction the side of the hexagon boundary, present only for ports and adapters
 * @param confidence the confidence of the decision
 * @param basis whether the kind was declared or inferred
 * @param evidences the evidences supporting the decision, in tier order
 * @param candidates the losing candidates, kept when the decision was ambiguous
 * @param proof the proof tree of the derivation
 * @param remediations type-specific suggestions to make the classification stronger
 * @since 7.0.0
 */
public record Classification(
        ArchKind kind,
        Optional<PortDirection> direction,
        Confidence confidence,
        Basis basis,
        List<Evidence> evidences,
        List<Candidate> candidates,
        ProofNode proof,
        List<RemediationHint> remediations) {

    /**
     * Validates the coherence of the verdict — a direction only makes sense on the boundary of the
     * hexagon, that is on a port or on an adapter — and copies every collection.
     */
    public Classification {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(confidence, "confidence must not be null");
        Objects.requireNonNull(basis, "basis must not be null");
        Objects.requireNonNull(evidences, "evidences must not be null");
        Objects.requireNonNull(candidates, "candidates must not be null");
        Objects.requireNonNull(proof, "proof must not be null");
        Objects.requireNonNull(remediations, "remediations must not be null");
        if (direction.isPresent() && !kind.isPort() && !kind.isAdapter()) {
            throw new IllegalArgumentException("direction is only meaningful for port and adapter kinds, got " + kind);
        }
        evidences = List.copyOf(evidences);
        candidates = List.copyOf(candidates);
        remediations = List.copyOf(remediations);
    }

    /**
     * Creates a builder for a classification.
     *
     * @param kind the decided kind
     * @param confidence the confidence of the decision
     * @param basis whether the kind was declared or inferred
     * @param proof the proof tree of the derivation
     * @return a new builder
     */
    public static Builder builder(ArchKind kind, Confidence confidence, Basis basis, ProofNode proof) {
        return new Builder(kind, confidence, basis, proof);
    }

    /**
     * Returns whether the decision kept competing candidates.
     *
     * @return true when losing candidates were recorded
     */
    public boolean isAmbiguous() {
        return !candidates.isEmpty();
    }

    /**
     * Builder for {@link Classification} instances.
     *
     * @since 7.0.0
     */
    public static final class Builder {

        private final ArchKind kind;
        private final Confidence confidence;
        private final Basis basis;
        private final ProofNode proof;
        private Optional<PortDirection> direction = Optional.empty();
        private List<Evidence> evidences = List.of();
        private List<Candidate> candidates = List.of();
        private List<RemediationHint> remediations = List.of();

        private Builder(ArchKind kind, Confidence confidence, Basis basis, ProofNode proof) {
            this.kind = Objects.requireNonNull(kind, "kind must not be null");
            this.confidence = Objects.requireNonNull(confidence, "confidence must not be null");
            this.basis = Objects.requireNonNull(basis, "basis must not be null");
            this.proof = Objects.requireNonNull(proof, "proof must not be null");
        }

        /**
         * Sets the port direction.
         *
         * @param direction the direction
         * @return this builder
         */
        public Builder direction(PortDirection direction) {
            this.direction = Optional.of(direction);
            return this;
        }

        /**
         * Sets the supporting evidences.
         *
         * @param evidences the evidences, in tier order
         * @return this builder
         */
        public Builder evidences(List<Evidence> evidences) {
            this.evidences = evidences;
            return this;
        }

        /**
         * Sets the losing candidates of an ambiguous decision.
         *
         * @param candidates the candidates, best first
         * @return this builder
         */
        public Builder candidates(List<Candidate> candidates) {
            this.candidates = candidates;
            return this;
        }

        /**
         * Sets the remediation hints.
         *
         * @param remediations the hints, most effective first
         * @return this builder
         */
        public Builder remediations(List<RemediationHint> remediations) {
            this.remediations = remediations;
            return this;
        }

        /**
         * Builds the classification.
         *
         * @return a new Classification
         */
        public Classification build() {
            return new Classification(kind, direction, confidence, basis, evidences, candidates, proof, remediations);
        }
    }
}
