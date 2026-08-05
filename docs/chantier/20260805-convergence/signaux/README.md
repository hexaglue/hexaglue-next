# `signaux` — voir les signaux que le moteur retient sur un type

Le rapport d'audit publie un **verdict** ; les évidences qui l'ont porté ne
sortent nulle part. Ce harnais les affiche, pour qu'une pondération se lise sur
des signaux mesurés plutôt que sur un récit — c'est l'outil qui a démonté le
mécanisme énoncé par D33 (voir [`../MESURE-D33.md`](../MESURE-D33.md)).

Il vit **hors du réacteur** : son `pom.xml` n'est dans aucun `<modules>`, il
consomme les artefacts installés, et il ne modifie rien.

## Avant toute mesure

```bash
cd hexaglue-next
mvn -q -o install -DskipTests    # sinon le harnais mesure un moteur périmé
```

**Le piège est réel et documenté** : le harnais résout le moteur depuis le dépôt
local. Sans installation préalable, on mesure l'état d'une session antérieure.

## Usage

```bash
cd hexaglue-next/docs/chantier/20260805-convergence/signaux
mvn -q -o compile

mvn -q -o exec:java -Dexec.mainClass=io.hexaglue.probe.Signaux \
  -Dsignaux.classpath="<jars:séparés:par:deux-points>" \
  -Dexec.args="<racines> <basePackage> <exclusions|-> <type…|*>"
```

| Argument | Sens |
|---|---|
| `racines` | une ou plusieurs racines de sources, séparées par `:` |
| `basePackage` | le périmètre de verdict |
| `exclusions` | paquets exclus, séparés par `,`, ou `-` |
| `type…` | un ou plusieurs FQN, ou `*` pour tout dumper |
| `-Dsignaux.classpath` | **jamais facultatif en pratique** |

### Le classpath n'est pas un détail

Sans lui, la fermeture transitive des supertypes ne se fait pas :
`extends JpaRepository` ne se relie plus à `Repository`, et le moteur rend
**d'autres verdicts** — sur `spring-petclinic`, trois agrégats deviennent un et
dix non classés deviennent quatorze. Mesure et détail dans
[`../MESURE-PROJETS.md`](../MESURE-PROJETS.md).

## Ce que la sortie donne

- le **registre de passage** : fichiers `.java` sous les racines → types dans le
  `CodeModel` → types dans l'`ArchModel`, l'écart disant ce qui s'est perdu ;
- les **diagnostics des deux étages**, qui disent *pourquoi* un type manque ;
- par type : le verdict, chaque signal retenu avec son palier, sa force, le
  jeton qui sert de clé de déduplication et l'**ancre** sur laquelle il s'appuie ;
- les candidats d'un verdict ambigu, avec leur score ;
- avec `*`, la liste des **enveloppes à une valeur** et leur nombre de signaux
  de possession — la population concernée par `E7-MODÈLE-1`.

## Exemple : le cas qui a démonté D33

```bash
mvn -q -o exec:java -Dexec.mainClass=io.hexaglue.probe.Signaux \
  -Dexec.args="../../../../../case-study-ecommerce/hexagonal/src/main/java \
               com.acme.shop \
               com.acme.shop.infrastructure,com.acme.shop.exception \
               com.acme.shop.domain.customer.Email"
```

```text
== com.acme.shop.domain.customer.Email ==
  verdict    : DOMAIN_EVENT (HIGH, INFERRED)
  signaux retenus : 1
    [S3/HIGH] ANNOUNCED_BY(com.acme.shop.ports.out.NotificationSender)   ancre=NotificationSender
```

Un signal, pas deux ; aucun signal de possession ; donc aucune pesée.
