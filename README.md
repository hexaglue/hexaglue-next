# HexaGlue v7

New reactor for HexaGlue, the architecture compiler for Java applications.
At compile time, it turns a codebase into a semantic graph of architectural
intent: sources are parsed into a code model, classified into an architectural
model with evidence-backed proofs, held to whatever the build states about it,
and handed to the backends a project installed — an architecture audit, a living
documentation, and the persistence the model already describes, written and
handed to the compiler.

## Modules

| Module | Role |
|---|---|
| `hexaglue-model` | The contract: immutable records and sealed interfaces for the code model, the architectural model, classification traces, findings and typed configuration. Zero dependencies, zero logic beyond structural invariants |
| `hexaglue-frontend` | Reads Java sources and their classpath into the code model: type nodes, external stubs for classpath types, typed edges with provenance, typed annotation values and the supertype closure. The only module that sees a parser |
| `hexaglue-knowledge` | What the frameworks mean, as versioned declarative packs: which annotation, supertype or package carries which technical fact. A symbol is always named in full, never by simple name |
| `hexaglue-engine` | The solver: rules derive facts to a fixed point, a deterministic weighing turns the evidences into a verdict, and every verdict carries the proof of how it was reached. It also judges the result — the architectural findings and the validation gates live here — and renders both for a reader. No I/O |
| `hexaglue-spi` | What a backend implements: a manifest — who it is, what it reads, what it will write adapters for — and one pure function from what the run concluded to what it hands over: documents, Java types, and what it declined to write. Backends are ordered by their declared dependencies and isolated from one another, and none of them ever touches a disk |
| `hexaglue-render` | The markup a backend writes in: markdown, tables and Mermaid diagrams, with escaping applied by the builders rather than left to the caller |
| `hexaglue-plugins` | The backends shipped with HexaGlue: [`hexaglue-plugin-jpa`](hexaglue-plugins/hexaglue-plugin-jpa/README.md) (the rows a domain is stored in, the ways to and from them, and the adapter that fills a repository port), [`hexaglue-plugin-audit`](hexaglue-plugins/hexaglue-plugin-audit/README.md) (the architecture report, in markdown and JSON) and [`hexaglue-plugin-living-doc`](hexaglue-plugins/hexaglue-plugin-living-doc/README.md) (the domain and boundary pages, drawn from the model alone) |
| `hexaglue-maven-plugin` | The host: where the sources are, what the configuration document says, which backends are installed, and where what they wrote goes — documents under the build directory, generated types under a source root it hands to the compiler. Everything worth testing without a running build lives beside the goal rather than inside it |
| `hexaglue-testkit` | Published test harness: source fixture helpers, golden-file harness, determinism checks and the reference acceptance corpus |
| `hexaglue-acceptance` | Where the whole chain is exercised end to end: the only module that sees both the frontend and the engine, and the home of the corpus scoreboard and the golden files |

Dependencies point one way: `frontend → model ← engine ← spi ← plugins`. The
boundary between stages is a data model, never a layer of interfaces — a
second frontend, if one ever exists, produces the same code model rather than
implementing an abstraction invented in advance.

A standalone command line stays possible rather than planned — the engine
already renders everything such a host would print, so what remains to build is
the host and nothing else.

## Using it

HexaGlue is used through its Maven plugin. Four goals, and the line between
them is that **one of them judges and the others write**:

```bash
mvn hexaglue:validate         # hold the architecture to the gates the build armed
mvn hexaglue:generate         # write the code the backends make of the model
mvn hexaglue:report           # run the installed backends over one project
mvn hexaglue:reactor-report   # run them over a whole reactor, in one reading
```

`generate` binds to `generate-sources`, writes under
`target/generated-sources/hexaglue` and hands that directory to the compiler.
It never writes among your own sources, and it never fails a build on what it
found: what a backend declined to write is said, and stopping is `validate`'s
business. Everything it writes carries a generated marker, which is how the next
run reads it as what was made of the architecture rather than as the
architecture.

A backend is installed by being declared as a dependency of the plugin — each
one documents what it writes and what it accepts:
[jpa](hexaglue-plugins/hexaglue-plugin-jpa/README.md),
[audit](hexaglue-plugins/hexaglue-plugin-audit/README.md),
[living-doc](hexaglue-plugins/hexaglue-plugin-living-doc/README.md).

What a project asks of the analysis and of each backend goes in a
`hexaglue.yaml` beside the POM — read strictly, so a key nobody reads or a value
nobody can honour fails the build rather than being quietly ignored:

```yaml
analysis:
  basePackage: com.acme.shop

validation:
  failOnUnclassified: true
  findings:
    # only a code the project arms can break its build
    HG-DDD-002: BLOCKER

# a reactor states what each of its modules is; nothing is read from its name
modules:
  shop-domain: DOMAIN
  shop-infra: INFRASTRUCTURE

plugins:
  io.hexaglue.jpa:
    entitySuffix: JpaEntity
  io.hexaglue.audit:
    outputDirectory: audit
  io.hexaglue.living-doc:
    generateDiagrams: false
```

A module of a reactor inherits the document of the reactor above it; the nearest
one wins, whole. Nothing merges two documents, so a gate is never armed by a
file the reader is not looking at.

**A reactor is read in one pass.** Read module by module, a port declared in one
and implemented in another resolves to a stub of itself, and the seam the
analysis exists to find is exactly what it loses. The role of each module is
declared rather than guessed: what a team calls its modules is a habit, not a
fact about its architecture.

**Nothing here decides that an architecture may not compile.** A finding breaks
the build only from the severity a project stated for its code, and a code
nobody armed says what it found and stops there.

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

## What is read, and what is not

The analysis reads the source directory a project declares, and never a root
under the build directory. A build tool knows every root it compiles, generated
ones included, and handing them all over feeds the analysis its own output: an
emitted adapter comes back implementing the port its author wrote, the rules
read that port as a seam rather than a boundary, and a second run over unchanged
sources no longer says the same thing. Code carrying a generated marker is left
out for the same reason, wherever it sits.

This has a consequence worth knowing before reading a report: **on a project
whose adapters were generated into its sources, HexaGlue reports on the part
that was written by hand.** A driven port whose only implementation is generated
reads as a port nothing implements — which is true of the sources, and is not
what the running application does.

Where the generation is HexaGlue's own, the report knows better. A backend
states which family of ports it writes adapters for, before it runs, and a port
of a covered family is not reported as unfilled — and the run says so, naming
the ports it left out and the backend that answers for them. Nothing is inferred
from what a run produced: the declaration is read from the backends the build
installed, so the same sources are judged the same way twice.

Whatever is left out is counted rather than dropped in silence. Every goal says
how many types were not analysed and offers the reasons on request, because a
report written against half a codebase looks exactly like one written against
all of it.

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
make integration  # run the goals against real Maven builds
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

What a goal does is proven by running a build. `make integration` invokes the
goals against real Maven projects and asserts on what they wrote and what they
said, because a goal's parameters, its logs and its exit condition exist nowhere
else — every defect this harness has caught was one no unit test could have
seen.

## License

Mozilla Public License 2.0. Commercial licensing options are available for
organizations wishing to use HexaGlue under different terms: <info@hexaglue.io>.
