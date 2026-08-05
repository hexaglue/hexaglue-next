#!/usr/bin/env python3
"""Draft an expectation file for each profile 1 corpus scenario.

The scenarios were extracted from the legacy classification suite with their
sources but without what those tests asserted. This script goes back to the
method each scenario came from (recorded as origin=Class#method), copies its
assertions verbatim, and proposes an expectation from them.

What it emits is a DRAFT, never a truth. The legacy engine carries confirmed
bugs (B3, B4, B12, B13) and the legacy tests assert legacy vocabulary — port
kinds, criteria priorities, confidence levels — that the v7 model does not
share. Every file is written with `status: draft`; the ratchet counts only the
scenarios a human has read and promoted to `status: reviewed`.

  corpus/profile1/<scenario>/expectations.txt
      # verbatim legacy assertions, for the reviewer
      status: draft
      expect: com.example.Order = AGGREGATE_ROOT
      reject: com.example.Order = VALUE_OBJECT

Re-running never overwrites a reviewed file.
"""

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OLD_TEST_ROOT = ROOT / "hexaglue/hexaglue-core/src/test/java"
CORPUS = ROOT / "hexaglue-next/hexaglue-testkit/src/main/resources/corpus/profile1"

# Legacy ElementKind names that survive unchanged into ArchKind.
CARRIED_OVER_KINDS = {
    "AGGREGATE_ROOT",
    "ENTITY",
    "VALUE_OBJECT",
    "IDENTIFIER",
    "DOMAIN_EVENT",
    "DOMAIN_SERVICE",
    "DRIVING_PORT",
    "DRIVEN_PORT",
    "APPLICATION_SERVICE",
    "COMMAND_HANDLER",
    "QUERY_HANDLER",
    "UNCLASSIFIED",
}

# The legacy port classifier answered a port family where v7 answers a kind and
# a direction; the family lands on DrivenPort.portType, not on the kind.
PORT_FAMILIES = {"REPOSITORY", "GATEWAY", "EVENT_PUBLISHER", "NOTIFICATION", "GENERIC", "OTHER"}

UNCLASSIFIED_TRUE = re.compile(r"isUnclassified\(\)\)\.isTrue\(\)")
CLASSIFIED_FALSE = re.compile(r"isClassified\(\)\)\.isFalse\(\)")
TARGET_DIRECTION = re.compile(r"targetDirection\(\)\)\.isEqualTo\(PortDirection\.(\w+)\)")
TARGET_KIND = re.compile(r"targetKind\(\)\)\.isEqualTo\(ElementKind\.(\w+)\)")
KIND_STRING = re.compile(r"\.kind\(\)\)\.isEqualTo\(\"(\w+)\"\)")
KIND_ENUM = re.compile(r"\.kind\(\)\)\.isEqualTo\(ElementKind\.(\w+)\)")
MATCHED_TRUE = re.compile(r"\.matched\(\)\)\.isTrue\(\)")
MATCHED_FALSE = re.compile(r"\.matched\(\)\)\.isFalse\(\)")
PORT_DIRECTION = re.compile(r"portDirection\(\)\)\.isEqualTo\(PortDirection\.(\w+)\)")
NODE_ID = re.compile(r"NodeId\.type\(\"([\w.$]+)\"\)")
LOCAL_BINDING = re.compile(r"\b(\w+)\s*=\s*[\w.]*\(?\"([\w.$]+)\"\)")


def assertions_of(body: str) -> list[str]:
    """The assertions of a method, each on one line.

    Palantir wraps a long AssertJ chain over several lines, so an assertion is a
    statement rather than a line: reading it line by line loses exactly the part
    that names the expected kind.
    """
    statements = []
    for statement in body.split(";"):
        collapsed = " ".join(statement.split())
        if "assertThat(" in collapsed:
            statements.append(collapsed[collapsed.index("assertThat(") :])
    return statements


def method_body(source: str, method: str) -> str | None:
    """Returns the text of one test method, braces balanced."""
    start = source.find(f" {method}(")
    if start < 0:
        return None
    opening = source.find("{", start)
    if opening < 0:
        return None
    depth = 0
    for position in range(opening, len(source)):
        if source[position] == "{":
            depth += 1
        elif source[position] == "}":
            depth -= 1
            if depth == 0:
                return source[opening : position + 1]
    return None


def subject_of(body: str, scenario_types: list[str]) -> str | None:
    """Names the type the assertions speak about.

    A classifier test names it outright through NodeId.type("..."); a criteria
    test evaluates a local variable bound earlier to a qualified name. When the
    scenario declares exactly one type, that one is the subject by default.
    """
    named = NODE_ID.findall(body)
    if named:
        return named[0]
    evaluated = re.search(r"evaluate\((\w+)", body)
    if evaluated:
        variable = evaluated.group(1)
        for candidate, qualified_name in LOCAL_BINDING.findall(body):
            if candidate == variable and qualified_name in scenario_types:
                return qualified_name
    return scenario_types[0] if len(scenario_types) == 1 else None


def proposal(body: str, subject: str) -> list[str]:
    """Translates the legacy assertions into v7 expectations, as far as it can."""
    negated = bool(MATCHED_FALSE.search(body)) or bool(CLASSIFIED_FALSE.search(body))
    kinds = TARGET_KIND.findall(body) + KIND_ENUM.findall(body) + KIND_STRING.findall(body)
    lines = []
    if UNCLASSIFIED_TRUE.search(body) and not kinds:
        return [f"expect: {subject} = UNCLASSIFIED"]
    if not kinds and ".isEmpty()" in body and "NodeId" in body:
        return [
            "# the legacy engine returned no verdict at all on this type. v7 owes a verdict to",
            "# every type of the perimeter (A5), so the counterpart of absent is unclassified —",
            "# unless the type is out of scope, which is a different statement.",
            f"expect: {subject} = UNCLASSIFIED",
        ]
    for kind in dict.fromkeys(kinds):
        if kind in CARRIED_OVER_KINDS:
            lines.append(f"{'reject' if negated else 'expect'}: {subject} = {kind}")
        elif kind in PORT_FAMILIES:
            direction = PORT_DIRECTION.search(body) or TARGET_DIRECTION.search(body)
            side = f"{direction.group(1)}_PORT" if direction else "DRIVEN_PORT"
            lines.append(f"# legacy port family {kind}: v7 states the kind, the family is a port attribute")
            lines.append(f"{'reject' if negated else 'expect'}: {subject} = {side}")
        else:
            lines.append(f"# legacy kind {kind} has no v7 counterpart; decide by hand")
    if not lines and MATCHED_TRUE.search(body):
        lines.append("# the legacy test asserted a match without naming a kind; decide by hand")
    if not lines:
        lines.append("# no expectation could be drafted; read the assertions above and decide by hand")
    return lines


def scenario_types(scenario: Path) -> list[str]:
    """The qualified names the scenario's own sources declare."""
    paths = (scenario / "files.txt").read_text(encoding="utf-8").split()
    return [path.removesuffix(".java").replace("/", ".") for path in paths]


def harvest(scenario: Path, sources: dict[str, str]) -> str:
    descriptor = dict(
        line.split("=", 1)
        for line in (scenario / "scenario.properties").read_text(encoding="utf-8").splitlines()
        if "=" in line
    )
    class_name, method = descriptor["origin"].split("#", 1)
    body = method_body(sources.get(class_name, ""), method)

    header = [
        f"# {descriptor['origin']}",
        "#",
        "# Drafted from the legacy assertions below. The legacy engine is an observation,",
        "# never an oracle: it carries confirmed bugs, and v7 doctrine (D7, D8) deliberately",
        "# changes some verdicts. Read, correct, then set status to reviewed.",
        "#",
    ]
    if body is None:
        header.append("# The origin method could not be located in the carriere.")
        return "\n".join(header + ["status: draft", ""]) + "\n"

    assertions = assertions_of(body)
    header += [f"#   {assertion}" for assertion in assertions] or ["#   (no assertion found)"]

    claims = " ; ".join(assertions)
    subject = subject_of(body, scenario_types(scenario))
    if subject is None:
        proposed = ["# the assertions name no single subject; decide by hand"]
    else:
        proposed = proposal(claims, subject)
    if "ClassificationConfig" in body:
        proposed = [
            "# CONDITIONAL: the legacy test configured the classifier, and the corpus does not",
            "# carry that configuration yet. The expectation below holds only under it.",
            *proposed,
        ]
    return "\n".join(header + ["", "status: draft"] + proposed) + "\n"


def main() -> int:
    sources = {path.stem: path.read_text(encoding="utf-8") for path in OLD_TEST_ROOT.rglob("*.java")}
    written = kept = 0
    for scenario in sorted(path for path in CORPUS.iterdir() if path.is_dir()):
        target = scenario / "expectations.txt"
        if target.exists() and "status: reviewed" in target.read_text(encoding="utf-8"):
            kept += 1
            continue
        target.write_text(harvest(scenario, sources), encoding="utf-8")
        written += 1
    print(f"{written} draft(s) written, {kept} reviewed file(s) left untouched")
    return 0


if __name__ == "__main__":
    sys.exit(main())
