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
| `hexaglue-engine` | The solver: rules derive facts to a fixed point, a deterministic weighing turns the evidences into a verdict, and every verdict carries the proof of how it was reached. No I/O |
| `hexaglue-testkit` | Published test harness: source fixture helpers, golden-file harness, determinism checks and the reference acceptance corpus |

Dependencies point one way: `frontend → model ← engine ← spi ← plugins`. The
boundary between stages is a data model, never a layer of interfaces — a
second frontend, if one ever exists, produces the same code model rather than
implementing an abstraction invented in advance.

Further modules (`hexaglue-spi`, plugins, Maven adapter, CLI) arrive as the
reactor is built out.

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
