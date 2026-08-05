#!/usr/bin/env python3
"""Harvest inline Java source fixtures from the legacy classification test suite.

Scans writeSource("path", <string>) calls in the old reactor's test classes,
groups them by enclosing method, and emits one corpus scenario directory per
group under the new testkit resources:

  corpus/profile1/<ClassName>-<methodName>/
      scenario.properties   basePackage=..., origin=Class#method
      files.txt             one relative source path per line
      src/<relativePath>    the fixture sources

Also emits corpus/profile1/scenarios.txt (sorted index) and copies the legacy
golden JSONs next to the scenarios they correspond to.
"""

import re
import sys
from pathlib import Path

OLD_TEST_ROOT = Path("/Users/jeanjerome/Projets/hexaglue-projects/hexaglue/hexaglue-core/src/test/java")
GOLDEN_SRC = Path("/Users/jeanjerome/Projets/hexaglue-projects/hexaglue/hexaglue-core/src/test/resources/golden")
OUT_ROOT = Path("/Users/jeanjerome/Projets/hexaglue-projects/hexaglue-next/hexaglue-testkit/src/main/resources/corpus/profile1")

SOURCES = sorted(
    list((OLD_TEST_ROOT / "io/hexaglue/core/classification").rglob("*.java"))
    + [OLD_TEST_ROOT / "io/hexaglue/core/GoldenFileTest.java"]
)

LEGACY_GOLDEN_BY_SCENARIO = {
    "GoldenFileTest-createCoffeeshopDomain": "coffeeshop-arch-model.json",
    "GoldenFileTest-createBankingDomain": "banking-arch-model.json",
    "GoldenFileTest-createEcommerceDomain": "ecommerce-arch-model.json",
}

METHOD_RE = re.compile(
    r"^\s{4}(?:private|public|protected)?\s*(?:static\s+)?[\w<>\[\], ?]+\s+(\w+)\s*\([^;]*?\)\s*(?:throws\s+[\w, ]+)?\s*\{",
)
BASEPKG_RE = re.compile(r'(?:analyze|buildGraph|classify)\w*\(\s*"([\w.]+)"')
PKG_RE = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.MULTILINE)


def java_text_block(lines):
    """Strip incidental indentation the way Java text blocks do."""
    non_blank = [l for l in lines if l.strip()]
    if not non_blank:
        return ""
    indent = min(len(l) - len(l.lstrip()) for l in non_blank)
    out = "\n".join(l[indent:].rstrip() for l in lines)
    return out + "\n"


def decode_escapes(s):
    return (
        s.replace("\\\"", "\"")
        .replace("\\n", "\n")
        .replace("\\t", "\t")
        .replace("\\s", " ")
        .replace("\\\\", "\\")
    )


def extract_write_sources(text, class_name, warnings):
    """Yield (method_name, path, content) for each writeSource string literal call."""
    lines = text.split("\n")
    current_method = None
    i = 0
    while i < len(lines):
        line = lines[i]
        m = METHOD_RE.match(line)
        if m:
            current_method = m.group(1)
        idx = line.find("writeSource(")
        if idx >= 0 and not line.strip().startswith("private void writeSource"):
            # capture the path literal, possibly on the same or next line
            call = line[idx:]
            j = i
            while '"' not in call and j + 1 < len(lines):
                j += 1
                call += "\n" + lines[j]
            pm = re.search(r'"((?:[^"\\]|\\.)*)"\s*,', call)
            if not pm:
                warnings.append(f"{class_name}#{current_method}: unparseable writeSource at line {i + 1}")
                i += 1
                continue
            rel_path = decode_escapes(pm.group(1))
            rest = call[pm.end():]
            k = j
            while '"""' not in rest and k + 1 < len(lines):
                k += 1
                rest += "\n" + lines[k]
            if '"""' in rest:
                # text block: content runs from the line after the opening
                # delimiter to the line holding the closing delimiter
                tb_start = k + 1
                block_lines = []
                closing = None
                for n in range(tb_start, len(lines)):
                    if '"""' in lines[n]:
                        closing = n
                        break
                    block_lines.append(lines[n])
                if closing is None:
                    warnings.append(f"{class_name}#{current_method}: unterminated text block for {rel_path}")
                    i += 1
                    continue
                closing_indent = lines[closing][: lines[closing].find('"""')]
                content = java_text_block(block_lines + [closing_indent + "x"])
                # the sentinel line only contributed its indentation; drop it
                content = "\n".join(content.split("\n")[:-2]) + "\n"
                if current_method is None:
                    warnings.append(f"{class_name}: writeSource outside a method for {rel_path}")
                else:
                    yield current_method, rel_path, content
                i = closing + 1
                continue
            sm = re.search(r'"((?:[^"\\]|\\.)*)"\s*\)', rest)
            if sm and current_method is not None:
                yield current_method, rel_path, decode_escapes(sm.group(1)) + "\n"
            else:
                warnings.append(f"{class_name}#{current_method}: non-literal content for {rel_path}")
        i += 1


def base_package(scenario_sources, method_body_pkg):
    if method_body_pkg:
        return method_body_pkg
    pkgs = []
    for _, content in scenario_sources:
        pm = PKG_RE.search(content)
        if pm:
            pkgs.append(pm.group(1).split("."))
    if not pkgs:
        return None
    prefix = pkgs[0]
    for p in pkgs[1:]:
        n = 0
        while n < min(len(prefix), len(p)) and prefix[n] == p[n]:
            n += 1
        prefix = prefix[:n]
    return ".".join(prefix) if prefix else None


def method_base_packages(text):
    """Map method name -> base package literal used in analyze/buildGraph calls."""
    result = {}
    lines = text.split("\n")
    current = None
    for line in lines:
        m = METHOD_RE.match(line)
        if m:
            current = m.group(1)
        bm = BASEPKG_RE.search(line)
        if bm and current and current not in result:
            result[current] = bm.group(1)
    return result


def main():
    if OUT_ROOT.exists():
        import shutil

        shutil.rmtree(OUT_ROOT)
    OUT_ROOT.mkdir(parents=True)

    warnings = []
    scenario_ids = []
    total_files = 0
    skipped_scenarios = []

    for src in SOURCES:
        class_name = src.stem
        text = src.read_text(encoding="utf-8")
        pkg_by_method = method_base_packages(text)
        scenarios = {}
        for method, rel_path, content in extract_write_sources(text, class_name, warnings):
            scenarios.setdefault(method, []).append((rel_path, content))
        for method, files in sorted(scenarios.items()):
            sid = f"{class_name}-{method}"
            bp = base_package(files, pkg_by_method.get(method))
            if bp is None:
                skipped_scenarios.append(f"{sid}: no base package derivable")
                continue
            sdir = OUT_ROOT / sid
            (sdir / "src").mkdir(parents=True)
            seen = {}
            for rel_path, content in files:
                seen[rel_path] = content  # last write wins, mirrors the tests
            for rel_path, content in seen.items():
                out = sdir / "src" / rel_path
                out.parent.mkdir(parents=True, exist_ok=True)
                out.write_text(content, encoding="utf-8")
            (sdir / "files.txt").write_text("\n".join(seen.keys()) + "\n", encoding="utf-8")
            (sdir / "scenario.properties").write_text(
                f"basePackage={bp}\norigin={class_name}#{method}\n", encoding="utf-8"
            )
            golden = LEGACY_GOLDEN_BY_SCENARIO.get(sid)
            if golden:
                (sdir / "legacy-golden.json").write_bytes((GOLDEN_SRC / golden).read_bytes())
            scenario_ids.append(sid)
            total_files += len(seen)

    (OUT_ROOT / "scenarios.txt").write_text("\n".join(sorted(scenario_ids)) + "\n", encoding="utf-8")

    print(f"scenarios: {len(scenario_ids)}")
    print(f"source files: {total_files}")
    if skipped_scenarios:
        print("\nSKIPPED SCENARIOS:")
        for s in skipped_scenarios:
            print(f"  - {s}")
    if warnings:
        print("\nWARNINGS:")
        for w in warnings:
            print(f"  - {w}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
