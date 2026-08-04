# hexaglue-plugin-living-doc

Documentation that cannot drift from the code it describes.

Three pages, all derived from the classified model: the way in, the domain, the
boundary. Nothing is read from anywhere else — no convention over the file tree,
no second reading of the source — so a page is wrong only if the model is, and
then it is the model that gets fixed.

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
      <artifactId>hexaglue-plugin-living-doc</artifactId>
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

Its identifier is `io.hexaglue.living-doc`, and it depends on no other backend.

## What it writes

| Document | Content |
|---|---|
| `README.md` | The way in: counts by kind, where the types live, an index towards the page documenting each one, and **what the engine could not read** |
| `domain.md` | The domain: what identifies an aggregate, what it is made of, its values, its events, and a class diagram |
| `ports.md` | The boundary: which way each port faces, the family and managed aggregate of a driven one, and a graph of the whole boundary |

The way in lists what could not be read rather than dropping it. A page that
showed only what was understood would read as a codebase that was understood.

## Options

Stated under `plugins.io.hexaglue.living-doc` in `hexaglue.yaml`. An option the
plugin does not declare is refused with the accepted ones named — a typo is
never quietly ignored.

| Option | Default | What it does |
|---|---|---|
| `outputDirectory` | `living-doc` | Where the pages go, relative to the goal's output directory |
| `generateDiagrams` | `true` | Whether the aggregate and boundary diagrams are drawn |
| `propertiesPerDiagram` | `5` | How many properties a class shows in a diagram before it stops. A negative budget is refused |
| `includeProvenance` | `true` | Whether each verdict unfolds what it rests on |

```yaml
plugins:
  io.hexaglue.living-doc:
    outputDirectory: docs
    propertiesPerDiagram: 3
```

## Provenance

Every verdict on a page can be unfolded to show what it rests on: its confidence,
its basis, and the evidences by tier.

```markdown
<details>
<summary>What this verdict rests on</summary>

**Confidence**: HIGH — **basis**: INFERRED

| Tier | Fact | Why |
| --- | --- | --- |
| S3 | `managed-by(AGGREGATE_ROOT)` | a repository names it |
</details>
```

A verdict that could be made certain says how, on the same fold: **To make it
certain**, followed by the declaration that would settle it.

This is not debugging output, which is why the option that governs it is not
called one. Documentation showing only its conclusions would let a reader take a
kind the engine inferred for a kind the author declared, and those are not worth
the same.

## Anchors and names

Two decisions a reader of the output will notice:

- **Anchors are written, not guessed.** Markdown derives an anchor from a
  heading, so two types with the same simple name get anchors differing by a
  number no generator can predict. Each section writes its own `<a id="…">` on
  the **qualified** name, so a link stays right whatever happens to the titles.
- **Names are shortened for display** and kept in full where they disambiguate —
  under the type's own heading, and in the anchor. A page paved with qualified
  names does not get read.

## Sample

```markdown
### Order

`com.example.shop.Order`

- **Identified by**: `UUID`, on `id`
- **Persisted through**: [Orders](ports.md#com-example-shop-orders)
- **Values**: [Money](#com-example-shop-money)
```

## License

Mozilla Public License 2.0. Commercial licensing options are available for
organizations wishing to use HexaGlue under different terms: <info@hexaglue.io>.
