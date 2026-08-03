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

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.ArchType;
import io.hexaglue.model.classification.Basis;
import io.hexaglue.model.classification.Candidate;
import io.hexaglue.model.classification.Classification;
import io.hexaglue.model.config.ValidationConfig;
import io.hexaglue.model.finding.Finding;
import io.hexaglue.model.finding.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * What the configured gates make of a classified model: the refusals, type by type and gate by
 * gate, or nothing at all when everything passed.
 *
 * <p>Validation is a policy of the engine and not a plugin, because it produces a verdict and no
 * content: the same model a report would describe is the one held to the gates here. It reads the
 * classification of every analyzed type, ports included — a boundary the analysis is unsure about
 * is exactly the kind of uncertainty a build wants to hear about.</p>
 *
 * <p>Findings are held to the same place. A code the configuration arms is a code that refuses the
 * build once a finding of it reaches the stated severity; a code nobody armed says nothing,
 * whatever it found. That is one mechanism, not two: the same judgement the report displays is the
 * one that fails a build, so a build cannot pass while its report condemns it.</p>
 *
 * <p>A refusal carries the type it is about rather than a copy of what to do next, so a host that
 * wants to say more can explain that very type — and the remediation it prints is the one the
 * engine wrote for it, never a generic suggestion.</p>
 *
 * @param refusals what failed, in model order and, within a type, in gate order
 * @since 7.0.0
 */
public record Validation(List<Refusal> refusals) {

    /**
     * Validates and copies the refusals.
     */
    public Validation {
        Objects.requireNonNull(refusals, "refusals must not be null");
        refusals = List.copyOf(refusals);
    }

    /**
     * Holds a classified model to the configured gates.
     *
     * @param model the classified model
     * @param gates the conditions the model is held to
     * @return the refusals, empty when the model passed
     */
    public static Validation of(ArchModel model, ValidationConfig gates) {
        return of(model, List.of(), gates);
    }

    /**
     * Holds a classified model and what the checks made of it to the configured gates.
     *
     * @param model the classified model
     * @param findings what the checks found
     * @param gates the conditions the model is held to
     * @return the refusals, empty when the model passed
     */
    public static Validation of(ArchModel model, List<Finding> findings, ValidationConfig gates) {
        Objects.requireNonNull(model, "model must not be null");
        Objects.requireNonNull(findings, "findings must not be null");
        Objects.requireNonNull(gates, "gates must not be null");
        List<Refusal> refusals = new ArrayList<>();
        for (ArchType type : model.types()) {
            Classification verdict = type.classification();
            if (gates.failOnUnclassified() && verdict.kind() == ArchKind.UNCLASSIFIED) {
                refusals.add(new Refusal(type, Gate.UNCLASSIFIED, "no kind could be decided"));
            }
            if (!verdict.confidence().isAtLeast(gates.minConfidence())) {
                refusals.add(new Refusal(
                        type,
                        Gate.CONFIDENCE,
                        verdict.confidence() + " is below the required " + gates.minConfidence()));
            }
            if (gates.failOnAmbiguous() && verdict.isAmbiguous()) {
                refusals.add(new Refusal(type, Gate.AMBIGUOUS, "the decision kept " + candidates(verdict)));
            }
            if (!gates.allowInferred() && verdict.basis() == Basis.INFERRED) {
                refusals.add(new Refusal(type, Gate.INFERRED, "the kind was deduced, not stated by the sources"));
            }
        }
        refusals.addAll(refused(model, findings, gates));
        return new Validation(refusals);
    }

    /**
     * A finding refuses a build only where the configuration armed its code, and only once it
     * reaches the severity stated there. Everything else it found is for the report to show.
     *
     * <p>The reason carries the finding's own words rather than a sentence about findings in
     * general: a build that stops has to say what it stopped on, and the check that found it
     * already wrote that down.</p>
     */
    private static List<Refusal> refused(ArchModel model, List<Finding> findings, ValidationConfig gates) {
        List<Refusal> refusals = new ArrayList<>();
        for (Finding finding : findings) {
            Severity threshold = gates.findingThresholds().get(finding.code());
            if (threshold == null || !finding.severity().isAtLeast(threshold)) {
                continue;
            }
            model.type(finding.subject())
                    .ifPresent(subject -> refusals.add(new Refusal(
                            subject,
                            Gate.FINDING,
                            finding.code().value() + " (" + finding.severity() + "): " + finding.message())));
        }
        return refusals;
    }

    /**
     * Returns whether the model met every armed gate.
     *
     * @return true when nothing was refused
     */
    public boolean passed() {
        return refusals.isEmpty();
    }

    private static String candidates(Classification verdict) {
        List<String> kinds = verdict.candidates().stream()
                .map(Candidate::kind)
                .map(Enum::name)
                .toList();
        return kinds.size() + " candidates: " + String.join(", ", kinds);
    }

    /**
     * One condition one type failed to meet.
     *
     * @param subject the type the gate refused
     * @param gate the condition it failed
     * @param reason what to say about this type under this gate
     * @since 7.0.0
     */
    public record Refusal(ArchType subject, Gate gate, String reason) {

        /**
         * Validates that a refusal names what it is about.
         */
        public Refusal {
            Objects.requireNonNull(subject, "subject must not be null");
            Objects.requireNonNull(gate, "gate must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
            if (reason.isBlank()) {
                throw new IllegalArgumentException("reason must not be blank");
            }
        }
    }
}
