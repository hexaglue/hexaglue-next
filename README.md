# HexaGlue v7

New reactor for HexaGlue, the architecture compiler for Java applications.
At compile time, it turns a codebase into a semantic graph of architectural
intent: sources are parsed into a code model, classified into an
architectural model with evidence-backed proofs, then consumed by plugins
(audit, living documentation, code generation).

## Modules

| Module | Role |
|---|---|
| `hexaglue-model` | The contract: immutable records and sealed interfaces for the code model, the architectural model, classification traces, findings and typed configuration. Zero dependencies, zero logic beyond structural invariants |
| `hexaglue-frontend` | Reads Java sources and their classpath into the code model: type nodes, external stubs for classpath types, typed edges with provenance, typed annotation values and the supertype closure. The only module that sees a parser |
| `hexaglue-knowledge` | What the frameworks mean, as versioned declarative packs: which annotation, supertype or package carries which technical fact. A symbol is always named in full, never by simple name |
| `hexaglue-engine` | The solver: rules derive facts to a fixed point, a deterministic weighing turns the evidences into a verdict, and every verdict carries the proof of how it was reached. Also renders that verdict for a reader. No I/O |
| `hexaglue-testkit` | Published test harness: source fixture helpers, golden-file harness, determinism checks and the reference acceptance corpus |
| `hexaglue-acceptance` | Where the whole chain is exercised end to end: the only module that sees both the frontend and the engine, and the home of the corpus scoreboard and the golden files |

Dependencies point one way: `frontend → model ← engine ← spi ← plugins`. The
boundary between stages is a data model, never a layer of interfaces — a
second frontend, if one ever exists, produces the same code model rather than
implementing an abstraction invented in advance.

Further modules arrive as the reactor is built out: `hexaglue-spi`, the
plugins, and the Maven adapter, which will be the host HexaGlue is primarily
used through. A standalone command line stays possible rather than planned —
the engine already renders everything such a host would print, so what remains
to build is the host and nothing else.

## Why a verdict is auditable

A classifier that cannot be argued with is a classifier nobody can trust. Every
conclusion the engine reaches carries the evidence that supports it and the
tree of rules it was derived from, and the engine renders both:

```
com.acme.clinic.owner.Owner: AGGREGATE_ROOT (HIGH, inferred)
  signals, strongest first: declared intent > framework knowledge > graph relation > local structure > topology > naming
  [framework knowledge] com.acme.clinic.owner.Owner is a AGGREGATE_ROOT because a repository stores and retrieves it (org.springframework.data.repository.Repository)
    involving com.acme.clinic.owner.OwnerRepository
  [graph relation] com.acme.clinic.owner.Owner is a AGGREGATE_ROOT because OwnerRepository keeps it and hands it back, which is a lifecycle of its own
    involving com.acme.clinic.owner.OwnerRepository
  derivation:
    [DECISION] AGGREGATE_ROOT(com.acme.clinic.owner.Owner) [decided on 1 signal of framework knowledge, 1 of graph relation]
      [R1] AGGREGATE_ROOT(com.acme.clinic.owner.Owner) [framework knowledge: SPRING_DATA_REPOSITORY(com.acme.clinic.owner.OwnerRepository)]
        SPRING_DATA_REPOSITORY(com.acme.clinic.owner.OwnerRepository) [spring:org.springframework.data.repository.Repository]
      [R1b] AGGREGATE_ROOT(com.acme.clinic.owner.Owner) [graph relation: MANAGED_BY(com.acme.clinic.owner.OwnerRepository)]
  rules cited:
    DECISION: weighs every signal held about a type and commits to one kind
    R1: reads a Spring Data repository declaration for everything it says
    R1b: reads the type a way out stores and retrieves as the aggregate it is
```

Nothing there asks the reader to learn a private notation first. Signals are
named by what they are rather than by an internal code, their pecking order is
stated wherever it decided something, and every rule the derivation cites says
what it concludes. The identifiers remain — they are how a rule is looked up in
the reference and compared by a consumer — but they are never the only thing on
offer.

A type that reached no kind says so with the same detail — its category, the
reason, the candidates that could not be separated, and what would settle the
question. Nothing is ever silently absent from a verdict.

`Explanation` renders one type or a whole run; `Outcome` counts a run. Both
answer in lines a host writes as it pleases — a build plugin logs them one at a
time, a report indents them under a heading — while the structure behind them
stays readable on its own. The rendering is a leaf of the pipeline, never a
stage of it: nothing downstream parses the text back.

## The acceptance corpus

Correctness here is not a matter of unit tests agreeing with the code that was
just written. The reference corpus holds 154 reviewed scenarios across three
populations, each asking a question the others cannot:

| Profile | Population | What it proves |
|---|---|---|
| 1 | Sources written in HexaGlue's own vocabulary | That the engine does not need those names |
| 2 | An enterprise application whose domain is welded to its storage | That roles are read as if the mapping were not there |
| 3 | An application with no naming convention to lean on | That position alone carries the reading |

Every scenario is reviewed by a human before it counts, and the score is held
against a committed floor that fails both below it — a regression — and above
it — a gain nobody recorded. Alongside the floor, golden files pin the whole
model and the whole rendered explanation of every scenario, so a rule that
changes what a verdict rests on shows up as a diff the day it lands.

## Build

```bash
make compile      # compile without tests
make test         # run all tests
make format       # apply Palantir Java Format
make verify       # tests + quality checks (incremental)
make ci           # clean build: the gate that must be green before a release
make mutation     # mutation testing on the production modules
```

Requires JDK 21+ (bytecode target: Java 17) and Maven 3.9+.

Quality gates run on every build: Error Prone and NullAway at compile time,
Checkstyle, PMD and SpotBugs as blocking checks, strict Javadoc linting,
JaCoCo line coverage, ArchUnit rules applied to the reactor itself, and
`bannedDependencies` keeping the parser and the build-tool APIs inside the
single module each belongs to. Use `make ci` rather than `make verify` when
what matters is that nothing warns: an incremental build does not recompile
untouched modules, and their compiler warnings go unreported.

Recording a golden file is a run that declares itself
(`-Dhexaglue.golden.regenerate=true`); a missing golden is a failure, never a
file quietly created from whatever the engine happens to answer today.

## License

Mozilla Public License 2.0. Commercial licensing options are available for
organizations wishing to use HexaGlue under different terms: <info@hexaglue.io>.
