# HexaGlue v7

New reactor for HexaGlue, the architecture compiler for Java applications.
At compile time, it turns a codebase into a semantic graph of architectural
intent: sources are parsed into a code model, classified into an
architectural model with evidence-backed proofs, then consumed by plugins
(audit, living documentation, code generation).

## Modules

| Module | Role |
|---|---|
| `hexaglue-testkit` | Published test harness: fixture DSL, golden-file harness, reference corpus |

Further modules (`hexaglue-model`, `hexaglue-frontend`, `hexaglue-knowledge`,
`hexaglue-engine`, `hexaglue-spi`, plugins, Maven adapter, CLI) arrive as the
reactor is built out.

## Build

```bash
make compile      # compile without tests
make test         # run all tests
make format       # apply Palantir Java Format
make verify       # tests + quality checks
```

Requires JDK 21+ (bytecode target: Java 17) and Maven 3.9+.
