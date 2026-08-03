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
import java.util.Properties;

/**
 * Loads the reference acceptance corpus shipped as classpath resources of the testkit.
 *
 * <p>Each {@link CorpusProfile} is an index of scenarios; every scenario directory holds a {@code
 * scenario.properties} descriptor ({@code basePackage}, {@code origin}), a {@code files.txt}
 * source listing and the source files themselves under {@code src/}. Scenario order follows the
 * index file, which is kept sorted, so corpus iteration is deterministic.
 *
 * @since 7.0.0
 */
public final class Corpus {

    private Corpus() {}

    /**
     * Loads one profile of the corpus.
     *
     * @param profile the profile to load
     * @return its scenarios, in index order
     * @throws IllegalStateException if the corpus resources are missing or malformed
     */
    public static List<CorpusScenario> of(CorpusProfile profile) {
        Objects.requireNonNull(profile, "profile must not be null");
        String profileRoot = rootOf(profile);
        List<String> ids = readLines(profileRoot + "/scenarios.txt");
        if (ids.isEmpty()) {
            throw new IllegalStateException("Corpus index " + profileRoot + "/scenarios.txt is empty");
        }
        List<CorpusScenario> scenarios = new ArrayList<>(ids.size());
        for (String id : ids) {
            scenarios.add(loadScenario(profile, id));
        }
        return List.copyOf(scenarios);
    }

    /**
     * Returns the classpath root under which a profile ships its scenarios.
     *
     * @param profile the profile
     * @return the resource path, without a trailing slash
     */
    static String rootOf(CorpusProfile profile) {
        return "/corpus/" + profile.directory();
    }

    private static CorpusScenario loadScenario(CorpusProfile profile, String id) {
        String scenarioRoot = rootOf(profile) + "/" + id;
        Properties descriptor = readProperties(scenarioRoot + "/scenario.properties");
        String basePackage = requireProperty(descriptor, "basePackage", scenarioRoot);
        String origin = requireProperty(descriptor, "origin", scenarioRoot);
        List<CorpusScenario.SourceFile> sources = new ArrayList<>();
        for (String relativePath : readLines(scenarioRoot + "/files.txt")) {
            String content = readResource(scenarioRoot + "/src/" + relativePath);
            sources.add(new CorpusScenario.SourceFile(relativePath, content));
        }
        return new CorpusScenario(profile, id, basePackage, origin, sources);
    }

    private static String requireProperty(Properties descriptor, String key, String scenarioRoot) {
        String value = descriptor.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Missing property '" + key + "' in " + scenarioRoot + "/scenario.properties");
        }
        return value.strip();
    }

    private static Properties readProperties(String resourcePath) {
        Properties properties = new Properties();
        try (InputStream stream = open(resourcePath)) {
            properties.load(stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read corpus resource " + resourcePath, e);
        }
        return properties;
    }

    private static List<String> readLines(String resourcePath) {
        return readResource(resourcePath)
                .lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .toList();
    }

    private static String readResource(String resourcePath) {
        try (InputStream stream = open(resourcePath)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read corpus resource " + resourcePath, e);
        }
    }

    private static InputStream open(String resourcePath) {
        InputStream stream = Corpus.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalStateException("Corpus resource not found on classpath: " + resourcePath);
        }
        return stream;
    }
}
