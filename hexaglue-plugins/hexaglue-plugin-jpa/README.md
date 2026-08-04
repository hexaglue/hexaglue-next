# hexaglue-plugin-jpa

The persistence side of a hexagon, written from the classified model: the rows
a domain is stored in, the ways between a domain object and its row, the store
a repository port is served by, and the adapter that plugs the port into it.

Nothing here decides what a type is. What gets stored is what the analysis said
has a life of its own — an aggregate and its parts become tables, a value
becomes part of the row that holds it — and that question was answered before
this backend was handed anything.

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
      <artifactId>hexaglue-plugin-jpa</artifactId>
      <version>${hexaglue.version}</version>
    </dependency>
  </dependencies>
  <executions>
    <execution>
      <goals><goal>generate</goal></goals>
    </execution>
  </executions>
</plugin>
```

Its identifier is `io.hexaglue.jpa`, and it depends on no other backend. The
generated code is written against Jakarta Persistence and Spring Data JPA, which
the project provides — this backend depends on neither.

## What it writes

| For | It writes |
|---|---|
| An aggregate or one of its parts | an `@Entity` with a table of its own, keyed by the value its identity is written around |
| A value the domain holds | an `@Embeddable`, stored in the row that holds it |
| Any of the above | a mapper, both ways, calling the type's own accessors and its own constructor |
| A repository port | a Spring Data interface extending `JpaRepository`, declaring the questions the inherited ones do not answer |
| A repository port | an adapter implementing it, `@Component`, over that interface and those mappers |
| A value that is one of a closed set | **nothing** — an enum is kept as itself, in the column of whatever holds it |

Everything it writes carries `@Generated("io.hexaglue.jpa")`, which is how the
next reading leaves it out of the architecture instead of reading it as one.

An identity is stored as the value it wraps: an aggregate identified by an
`OrderId` is found by the `UUID` inside it, because a column holding the wrapper
is a column no query can match. A reference to another aggregate stays a column
and never becomes a join.

A value with no state of its own is one the provider already has a shape for, so
nothing is written for it: the column carries `@Enumerated(EnumType.STRING)` and
the mapper hands it over untouched. **By name and never by rank** — the
provider's own default is the rank, and the day a constant is inserted in the
middle, every row already written means something else.

## What it reads, and what it refuses to read

Which store operation answers a port method is settled by the **shape** of that
method — what it takes and what it answers with — never by how the project spelt
it. A port asking `List<Order> of(CustomerId customer)` gets
`findByCustomer(UUID)`, and one asking the same thing under any other name gets
the same. The name written into the generated interface is Spring Data's own
requirement, derived from the fields of the entity this backend just wrote.

One shape is shared by two operations: taking the whole aggregate and answering
nothing is either storing it or erasing it. There, and only there, the port's own
word decides — and only a word the store itself uses counts:

```java
void save(Order order);      // stored
void delete(Order order);    // erased
void archive(Order order);   // neither: nothing is written, and it is said
```

Erasing is never concluded from shape alone. A query that reads the wrong rows
can be run again; a deletion that erases them cannot.

## What it declines, and says

A backend that guessed would write something that looks right and stores the
wrong rows. This one stops and says what would unblock it:

| Code | What happened |
|---|---|
| `HG-JPA-001` | the verdict on the type is less sure than this build generates from; the remediation the engine wrote for that very type travels with the refusal |
| `HG-JPA-002` | an aggregate or a part whose identity nothing in the analysis names — a row cannot be found without a key |
| `HG-JPA-003` | a repository port the analysis did not reach an aggregate for: a store serves one thing rather than anything |
| `HG-JPA-004` | a type no mapper can be written for, naming the field that stopped it — half a mapper loses what it skips |
| `HG-JPA-005` | a port asking something the store has no answer for, naming every such method — a class implementing a port implements all of it |

In every case the backend goes on with the rest.

<!-- GENERATED:CONFIG:START -->

## What it accepts

| Key | Default | What it does |
|---|---|---|
| `entitySuffix` | `Entity` | what a generated entity is called after the type it stores |
| `embeddableSuffix` | `Embeddable` | what a generated embeddable is called after the value it stores |
| `repositorySuffix` | `JpaRepository` | what the generated Spring Data interface is called after the aggregate it serves |
| `mapperSuffix` | `Mapper` | what a generated mapper is called after the type it carries across |
| `adapterSuffix` | `JpaAdapter` | what a generated adapter is called after the port it answers |
| `tablePrefix` | *(none)* | what every table of this project is prefixed with |
| `generateEmbeddables` | `true` | whether values are stored as embeddables at all |
| `generateRepositories` | `true` | whether repository ports are served by a generated interface |
| `generateMappers` | `true` | whether the ways between a domain type and its row are written |
| `generateAdapters` | `true` | whether repository ports are answered by a generated adapter |
| `idStrategy` | `ASSIGNED` | who decides an identity the domain does not: `ASSIGNED`, `IDENTITY`, `SEQUENCE`, `AUTO`, `UUID` |
| `targetModule` | *(none)* | the module generated types are addressed to |

<!-- GENERATED:CONFIG:END -->

```yaml
plugins:
  io.hexaglue.jpa:
    entitySuffix: Row
    tablePrefix: shop_
    idStrategy: IDENTITY
```

A key nobody reads fails the build rather than being quietly ignored, and so
does a value nobody can honour.

## What it declares before it runs

The manifest states that this backend writes the adapters of **driven ports of
role REPOSITORY**. The checks read that declaration: a port of that family that
nothing in the sources implements is not reported as a hole, because this build
writes what fills it — and the run says which ports it left out and on whose
word. A hexagon reaching the outside for anything else — sending mail, calling a
service — leaves holes this backend says nothing about, and those go on being
reported as the holes they are.
