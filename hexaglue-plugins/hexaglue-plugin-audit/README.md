# hexaglue-plugin-audit

The architecture report: what the analysis concluded, what holds, what does not,
and what it would take.

Every section is read from what the run already concluded. Nothing here
classifies a type, judges an architecture or measures a codebase — the verdicts
come from the engine, the findings come from the engine, the measurements come
from the engine. A backend that recomputed any of them would be a second opinion
free to disagree with the one the build gated on.

## Installing it

A backend is installed by being declared as a dependency of the goal that runs
it:

```xml
<plugin>
  <groupId>io.hexaglue</groupId>
  <artifactId>hexaglue-maven-plugin</artifactId>
  <version>${hexaglue.version}</version>
  <dependencies>
    <dependency>
      <groupId>io.hexaglue</groupId>
      <artifactId>hexaglue-plugin-audit</artifactId>
      <version>${hexaglue.version}</version>
    </dependency>
  </dependencies>
  <executions>
    <execution>
      <goals><goal>report</goal></goals>
    </execution>
  </executions>
</plugin>
```

Its identifier is `io.hexaglue.audit`, and it depends on no other backend.

## What it writes

| Document | Content |
|---|---|
| `architecture-audit.md` | The report, for a reader |
| `architecture-audit.json` | The same conclusions, for a tool. Ordered, so two runs over the same sources produce the same bytes |

Both land under the output directory of the goal, in the subdirectory the plugin
was given.

## Options

Stated under `plugins.io.hexaglue.audit` in `hexaglue.yaml`. An option the
plugin does not declare is refused with the accepted ones named — a typo is
never quietly ignored.

| Option | Default | What it does |
|---|---|---|
| `outputDirectory` | `audit` | Where the documents go, relative to the goal's output directory |
| `writeJson` | `true` | Whether the data form is written alongside the report |
| `generateDiagrams` | `true` | Whether dependency knots are drawn as a diagram |

```yaml
plugins:
  io.hexaglue.audit:
    outputDirectory: audit
    writeJson: false
```

## The report

Eight headings, in the order a reader wants them:

1. **Verdict** — the score, the grade, and how many violations over how many
   analysed types.
2. **What the score is made of** — because a single figure is the worst thing to
   look at alone: a codebase scores badly because nothing could be recognised,
   or because everything was and half of it breaks its own rules, and those call
   for opposite things.
3. **Violations** — every finding, with its code, its severity, its subject and
   what was found.
4. **How far to trust this** — how much of the reading was stated by the sources
   and how much the engine deduced. A kind the sources state is a fact; a kind
   the engine deduced is a reading, and a good one is still a reading.
5. **Quality metrics** — what was counted: types, packages, dependency knots,
   aggregates, ports, adapters.
6. **Inventory** — every analysed type with what its verdict rests on, plus the
   layout of the reactor when there is more than one module.
7. **Package stability** — what the engine measured about each package.
8. **What it would take** — the findings turned into an estimate.

### The score

Four parts, each a proportion of something countable, averaged with no invented
weight, and rendered as a grade from A to E:

| Part | What it counts |
|---|---|
| Read | types the analysis could name |
| Sound | named types no serious violation is about |
| Untangled | packages outside every dependency knot |
| Well-directed | packages whose abstraction matches their use |

An untouched codebase scores full marks rather than a failing grade: nothing was
found against it because there is nothing there yet, and a first commit reported
as a failure teaches nobody anything.

## The codes a report can carry

The checks live in the engine, so `validate` and this report always say the same
thing about the same sources. A code breaks a build only from the severity the
project stated for it under `validation.findings`.

| Code | What it found |
|---|---|
| `HG-DDD-001` | A part of an aggregate is reachable from outside it |
| `HG-DDD-002` | Two aggregates claim the same part |
| `HG-DDD-003` | Aggregates depend on each other in a circle |
| `HG-DDD-004` | An aggregate root has no way in and out of storage |
| `HG-DDD-005` | A domain type names something outside the domain |
| `HG-DDD-006` | An entity has nothing to tell its instances apart |
| `HG-DDD-007` | A value can be changed after it is made |
| `HG-DDD-008` | A part is stored like an entity but reads like a value |
| `HG-HEX-001` | An application type names an adapter instead of the port it implements |
| `HG-HEX-002` | A driven port nothing plugs into |
| `HG-HEX-003` | A driving port nothing in the core answers |
| `HG-HEX-004` | A driven port nothing in the core calls |
| `HG-HEX-005` | A driving port nothing outside drives |
| `HG-HEX-006` | A port that is not an interface |
| `HG-HEX-007` | An adapter reaching another adapter without going through the core |
| `HG-NAME-001` | A type does not follow the naming vocabulary the project opted into |

A port is checked from both sides on purpose. "Nothing plugs into it" and "the
core does not use it" are two different failures, and folding them together
would make the report say half of what it knows.

`HG-NAME-001` reads the vocabulary the **project** declared under
`classification.namingSuffixes`, never one shipped here. A codebase that states
no vocabulary gets no naming finding, on anything.

## Sample

```markdown
## Verdict

**74/100** — grade **C**, 5 violations over 45 analysed types.

## Violations

| Code | Severity | Subject | What was found |
| --- | --- | --- | --- |
| `HG-HEX-002` | MAJOR | `AccountRepository` | AccountRepository is a driven port
nothing implements. It is a hole the core left for the world to fill, and nothing
in these sources fills it. |
```

## License

Mozilla Public License 2.0. Commercial licensing options are available for
organizations wishing to use HexaGlue under different terms: <info@hexaglue.io>.
