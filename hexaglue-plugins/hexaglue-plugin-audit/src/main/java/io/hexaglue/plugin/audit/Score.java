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

package io.hexaglue.plugin.audit;

import io.hexaglue.model.ArchKind;
import io.hexaglue.model.arch.ArchModel;
import io.hexaglue.model.arch.Stability;
import io.hexaglue.model.finding.Finding;
import io.hexaglue.model.finding.Severity;
import io.hexaglue.spi.Measurements;
import java.util.List;
import java.util.Objects;

/**
 * One number, and the four it is made of.
 *
 * <p>A single figure is what gets looked at and the worst thing to look at alone, so it never
 * appears without its parts: a codebase can score badly because nothing could be recognised, or
 * because everything was recognised and half of it breaks its own rules, and those call for
 * opposite things. Each part is a share of something countable — no weights invented to make a
 * number come out well.</p>
 *
 * @param readable how much of the codebase the analysis could name, out of a hundred
 * @param sound how much of what it named holds together, out of a hundred
 * @param untangled how many packages sit outside every dependency knot, out of a hundred
 * @param wellDirected how many dependencies run towards what is harder to change, out of a hundred
 * @param overall the mean of the four, out of a hundred
 * @param grade the letter the overall figure falls into
 * @since 7.0.0
 */
public record Score(int readable, int sound, int untangled, int wellDirected, int overall, String grade) {

    /**
     * Validates the grade.
     */
    public Score {
        Objects.requireNonNull(grade, "grade must not be null");
    }

    /**
     * Scores an analysed codebase.
     *
     * @param model the classified model
     * @param findings what the checks made of it
     * @param measurements what was measured about its shape
     * @return the score and its parts
     */
    static Score of(ArchModel model, List<Finding> findings, Measurements measurements) {
        int types = model.types().size();
        int named = (int) model.types().stream()
                .filter(type -> type.kind() != ArchKind.UNCLASSIFIED)
                .count();
        int condemned = (int) findings.stream()
                .filter(finding -> finding.severity().isAtLeast(Severity.MAJOR))
                .map(Finding::subject)
                .distinct()
                .count();
        int packages = measurements.packages().size();
        int tangled = (int) measurements.cycles().stream().mapToLong(List::size).sum();
        int wrongWay = (int) measurements.packages().stream()
                .filter(Score::pointsTheWrongWay)
                .count();

        int readable = share(named, types);
        int sound = share(named - Math.min(condemned, named), named);
        int untangled = share(packages - tangled, packages);
        int wellDirected = share(packages - wrongWay, packages);
        int overall = (readable + sound + untangled + wellDirected) / 4;
        return new Score(readable, sound, untangled, wellDirected, overall, gradeOf(overall));
    }

    /**
     * A package that is depended upon and concrete is one nothing can change away from; the measure
     * of that distance is what the reading calls out.
     */
    private static boolean pointsTheWrongWay(Stability stability) {
        return stability.distance() > 0.5;
    }

    /**
     * An empty codebase scores full marks on everything: there is nothing wrong with it, and
     * pretending otherwise would make the first commit of a project look like a failure.
     */
    private static int share(int part, int whole) {
        return whole == 0 ? 100 : Math.max(0, Math.min(100, part * 100 / whole));
    }

    private static String gradeOf(int overall) {
        if (overall >= 90) {
            return "A";
        }
        if (overall >= 75) {
            return "B";
        }
        if (overall >= 60) {
            return "C";
        }
        return overall >= 40 ? "D" : "E";
    }
}
