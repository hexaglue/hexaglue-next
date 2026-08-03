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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A backend is installed by being on the classpath. What is checked here is that a run finds what
 * is there and hands it over in a stated order, so two builds of the same project run the same
 * plugins in the same sequence.
 */
class PluginDiscoveryTest {

    /**
     * A loader that answers the service lookup with the declaration a test hands it, without any
     * file being written anywhere.
     */
    // The parent has to be the loader that actually holds these test classes, so that the services
    // it declares can be found; the context loader is not that, and would defeat the fixture.
    @SuppressWarnings("PMD.UseProperClassLoader")
    private static ClassLoader declaring(String services) {
        return new ClassLoader(PluginDiscoveryTest.class.getClassLoader()) {
            @Override
            public Enumeration<URL> getResources(String name) {
                if (!("META-INF/services/" + HexaGluePlugin.class.getName()).equals(name)) {
                    return Collections.emptyEnumeration();
                }
                return Collections.enumeration(List.of(stating(services)));
            }
        };
    }

    private static URL stating(String content) {
        try {
            return new URL("hexaglue", null, 0, "services", new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL url) {
                    return new URLConnection(url) {
                        @Override
                        public void connect() {
                            // Nothing to connect to: the content is held in memory.
                        }

                        @Override
                        public InputStream getInputStream() {
                            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
                        }
                    };
                }
            });
        } catch (java.net.MalformedURLException impossible) {
            throw new IllegalStateException("the in-memory service declaration could not be built", impossible);
        }
    }

    /** A backend that answers to a stated identifier. */
    public static class First implements HexaGluePlugin {

        @Override
        public PluginManifest manifest() {
            return PluginManifest.of("io.hexaglue.alpha");
        }

        @Override
        public void contribute(Contribution contribution) {
            // Nothing: discovery is what this test is about.
        }
    }

    /** A second backend, declared after the first and named before it. */
    public static class Second implements HexaGluePlugin {

        @Override
        public PluginManifest manifest() {
            return PluginManifest.of("io.hexaglue.aardvark");
        }

        @Override
        public void contribute(Contribution contribution) {
            // Nothing: discovery is what this test is about.
        }
    }

    @Test
    @DisplayName("finds the backends a classpath declares")
    void findsWhatIsDeclared() {
        List<HexaGluePlugin> found = PluginDiscovery.on(declaring(First.class.getName()));

        assertThat(found)
                .singleElement()
                .satisfies(plugin -> assertThat(plugin.manifest().id()).isEqualTo("io.hexaglue.alpha"));
    }

    @Test
    @DisplayName("hands them over in identifier order, not in the order they were declared")
    void ordersByIdentifier() {
        List<HexaGluePlugin> found =
                PluginDiscovery.on(declaring(First.class.getName() + "\n" + Second.class.getName() + "\n"));

        assertThat(found)
                .extracting(plugin -> plugin.manifest().id())
                .containsExactly("io.hexaglue.aardvark", "io.hexaglue.alpha");
    }

    @Test
    @DisplayName("finds nothing on a classpath that installed nothing")
    void findsNothingWhenNothingIsInstalled() {
        assertThat(PluginDiscovery.on(declaring(""))).isEmpty();
    }
}
