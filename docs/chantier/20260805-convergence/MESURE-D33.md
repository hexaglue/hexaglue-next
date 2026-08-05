# Mesure — D33 : le mécanisme énoncé n'existe pas

> Auteur : **B**. Date : 2026-08-05. Commit mesuré : `hexaglue-next` à
> `d5386a2`, installé au dépôt local avant la mesure.
>
> Ce document n'est pas un tour de l'échange — il est clos. C'est le traitement
> du seul PENDING qui se règle par la mesure, et il est **destiné au registre**.

## La commande

Le rapport d'audit ne publie que le verdict ; les évidences qui l'ont porté ne
sortent nulle part. D'où un harnais, hors du réacteur, qui les affiche :
[`signaux/`](signaux/).

```bash
cd hexaglue-next/docs/chantier/20260805-convergence/signaux
mvn -q -o compile
mvn -q -o exec:java -Dexec.mainClass=io.hexaglue.probe.Signaux \
  -Dexec.args="../../../../../case-study-ecommerce/hexagonal/src/main/java \
               com.acme.shop \
               com.acme.shop.infrastructure,com.acme.shop.exception \
               com.acme.shop.domain.customer.Email"
```

**Piège d'outillage** : le harnais résout le moteur depuis le dépôt local.
Installer d'abord (`mvn install -DskipTests` à la racine de `hexaglue-next`),
sinon on mesure un moteur périmé — c'est le piège déjà consigné au chantier pour
`hexaglue-acceptance` sans `-am`.

## Ce que D33 affirme

> `Email` sort **DOMAIN_EVENT à HIGH**. `NotificationSender` (deux méthodes
> one-way portant des valeurs) est lu EVENT_PUBLISHER par W2-ROLE, puis R7
> conclut que ce qu'un tel port transporte est un événement ; **deux signaux R7
> pèsent plus que la possession par `Customer`** (R3b, un signal, même palier
> S3). Une valeur passée deux fois à un notifieur bat une valeur détenue une
> fois.

## Ce que la mesure rend

```text
== com.acme.shop.domain.customer.Email ==
  verdict    : DOMAIN_EVENT (HIGH, INFERRED)
  signaux retenus : 1
    [S3/HIGH] ANNOUNCED_BY(com.acme.shop.ports.out.NotificationSender)   ancre=NotificationSender
```

**Un signal. Un seul. Aucun candidat concurrent.**

Pour comparaison, sur le même run, un vrai objet-valeur possédé :

```text
== com.acme.shop.domain.order.Money ==
  verdict    : VALUE_OBJECT (HIGH, INFERRED)
  signaux retenus : 6
    [S3/HIGH] OWNED_BY(…Order)      [S3/HIGH] OWNED_BY(…OrderLine)
    [S3/HIGH] OWNED_BY(…Payment)    [S3/HIGH] OWNED_BY(…Product)
    [S3/HIGH] OWNED_BY(…Shipment)   [S4/HIGH] IMMUTABLE_SHAPE(…Money)
```

R3b fonctionne. Il ne parle simplement **jamais** d'`Email`.

## Les trois affirmations de D33 sont fausses

| Affirmation | Mesure |
|---|---|
| « deux signaux R7 » | **un**. `readAnnouncements` itère `Signatures.taken(port).distinct()` et le jeton nomme le port, pas la méthode (`PublishedEvent.java:94-104`) : les deux méthodes de `NotificationSender` produisent une seule clé. |
| « la possession par `Customer` (R3b, un signal) » | **zéro**. R3b n'émet rien sur `Email`. |
| « deux signaux pèsent plus que un » | **aucune pesée n'a lieu.** DOMAIN_EVENT gagne 1-0, sans adversaire. |

`Customer` est pourtant bien lu AGGREGATE_ROOT et détient bien
`private final Email email`.

## La cause réelle

`Email` est `record Email(String value)`. Donc :

```java
// Shapes.readsAsIdentity
isImmutable(type) && wrapsSingleValue(state(type))     // → true pour Email

// Lifecycle.isPart
.filter(type -> !Shapes.readsAsIdentity(type))          // → Email exclu

// Lifecycle.partsOf → OwnedValue (R3b)
// Email n'est jamais une « partie » de Customer, donc R3b ne le voit pas.
```

**Un objet-valeur en forme d'enveloppe à une valeur ne peut pas être lu comme
une valeur possédée.** L'exclusion existe pour tenir les identifiants hors des
parties d'un agrégat ; elle emporte avec eux **tout objet-valeur de même
forme** — `Email`, mais aussi n'importe quel `record Sku(String)`,
`record PhoneNumber(String)`, `record Iban(String)`.

Ce n'est pas une singularité du banc : c'est une propriété du jeu de règles,
lisible dans le code, indépendante du projet analysé. Elle reste à confirmer sur
le corpus, mais elle ne s'explique pas par ce projet-ci.

## Ce que cela fait aux questions de D33

D33 pose trois questions. La mesure en invalide une et en laisse deux debout :

| Question de D33 | État |
|---|---|
| « la possession (R3b) devrait-elle primer la publication (R7) à palier égal ? » | **sans objet** — il n'y a pas de signal de possession à faire primer. |
| « R7 devrait-il se taire sur un type que le domaine GARDE ? » | **debout**, et c'est désormais le seul levier sur ce verdict : R7 est le seul à parler. |
| « le rôle EVENT_PUBLISHER est-il trop large ? » | **debout**. |

Et elle en fait apparaître une quatrième, que D33 ne nomme pas et qui porte plus
loin que le cas :

> **Un objet-valeur en forme d'enveloppe doit-il rester invisible à la
> composition ?** C'est la même impasse que D16 sous un autre angle : rien de
> structurel ne sépare `record Email(String)` de `record CustomerId(String)`.

Le run le montre du même coup sur le voisin immédiat :

```text
== com.acme.shop.domain.customer.CustomerId ==
  verdict    : UNCLASSIFIED (LOW, INFERRED)
  candidat IDENTIFIER   (score 100)  [S4/HIGH] SINGLE_VALUE_WRAPPER(…)
  candidat VALUE_OBJECT (score 100)  [S4/HIGH] IMMUTABLE_SHAPE(…)
```

Le duel S4 que D32 devait fermer reste ouvert sur `Customer` — c'est
l'agrégat dont le dépôt porte deux clés de même forme de réponse. `Email` et
`CustomerId` sont donc les deux faces d'une même absence de discriminant.

## Ce que cela dit de la conduite du chantier

D33 a été rédigée à partir d'une lecture du code, pas d'une mesure. Elle est au
registre depuis le 2026-08-04 avec la mention « ne pas agir avant arbitrage », et
l'arbitrage demandé portait sur une pondération qui n'a jamais lieu. **Trancher
D33 telle qu'écrite aurait modifié l'agrégateur pour rien.**

C'est la démonstration la plus nette de la règle 2 du plan : « deux signaux R7 »
est une lecture, pas une mesure, et la différence a coûté une décision entière.

## Ce qui est proposé au registre

1. **D33 est requalifiée**, pas tranchée : son constat de symptôme est conservé
   (`Email` sort DOMAIN_EVENT à HIGH, c'est faux sur le domaine), son
   explication est remplacée par la mesure ci-dessus, et ses questions sont
   ramenées à deux.
2. **Une question nouvelle est ouverte**, de portée générale : l'invisibilité
   des enveloppes à une valeur pour la composition. Elle rejoint la famille
   « modèle métier » (D7, D13, D16) de la file de réévaluation, pas le
   traitement d'un cas de banc.
3. **Rien n'est corrigé maintenant.** La règle 1 du plan s'applique à cette
   trouvaille comme aux autres.
