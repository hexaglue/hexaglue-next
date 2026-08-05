# Tour 02 — B : vérifications sur l'arbitrage de A

> Auteur : **B**. Date : 2026-08-05. Répond à
> [01-A-arbitrage.md](01-A-arbitrage.md). Commit vérifié : `hexaglue-next` à
> `d5386a2`.

J'accepte l'arbitrage des quatre litiges, y compris le résidu de L1 : une
conclusion tirée d'une absence ne doit pas autoriser seule une génération. Ce
tour n'ajoute rien au désaccord ; il corrige deux points de la réponse de A,
ancre deux de ses propriétés sur des sites réels, et documente le risque produit
qui justifie son résidu mieux que la formulation abstraite.

## V1 — La propriété 3 de A est tautologique ; la propriété qui manque est l'unicité

A demande : « réinjecter les verdicts finaux produit exactement les mêmes
verdicts et supports ». C'est **la condition de sortie de la boucle** :

```java
// Classifier.java:69-74
Verdicts next = Aggregator.decide(Saturation.saturate(rules, context.withVerdicts(verdicts)), …);
if (next.equals(verdicts)) {
    return next;
}
```

Un test de cette propriété prouve que `Verdicts.equals` fonctionne, rien de
plus. Elle est vraie par construction.

La garantie qu'un opérateur **non monotone** n'offre pas, et qu'il faut donc
tester, est **l'unicité du point fixe** : partir d'un état initial différent de
`Verdicts.none()` — verdicts partiels, sous-ensemble des verdicts finaux, ordre
d'insertion permuté — mène-t-il au **même** point fixe ? Si non, le résultat du
produit dépend d'un détail d'implémentation qui n'est écrit nulle part.

**Proposition** : remplacer la propriété 3 de A par « unicité du point fixe sous
état initial arbitraire », et conserver sa propriété 4 (convergence ou
diagnostic) telle quelle.

## V2 — Le risque produit derrière le résidu de L1 : la composition D19 × R5b

La formulation de A est abstraite (« dépendante du périmètre fermé observé »).
Elle a un cas concret, et il est déjà dans le code :

- **D19** restreint l'analyse à la seule racine de sources déclarée. Son propre
  paragraphe d'impact reconnaît que les racines ajoutées par `build-helper` ou
  les sources générées hors `target/` **ne sont pas lues**.
- **R5b** (`OfferedContract.java:85-100`) conclut `DRIVING_PORT` de ce que
  **personne dans le périmètre** ne détient le contrat, et émet en `S4`, dont
  `maxConfidence()` vaut `HIGH` (`EvidenceTier.java:36-37`).
- **La porte de génération** compare `confidence().isAtLeast(HIGH)`
  (`Contribution.java:106`), défaut `HIGH` (`GenerationConfig.java:45`).

Composition : **un projet dont la couche web vit dans une racine que D19 ne lit
pas se voit inventer des ports pilotants, à HIGH, et générer des contrôleurs
pour eux.** L'absence observée n'est pas l'absence réelle ; elle est l'absence
dans ce que la décision d'hôte a choisi de lire.

C'est l'argument le plus fort pour le résidu de A, et il ne demande aucune
hypothèse : les deux décisions existent, et leur composition n'a jamais été
examinée.

## V3 — D38 est née sur le seul banc qui ne pouvait pas la réfuter

`_probes/ecommerce-hexagonal` **n'a pas de couche web** — c'est le constat même
qui a fait écrire R5b (D38 : « le projet observé n'a ni contrôleur ni
listener »). La prémisse de la règle, « rien dedans ne le détient, donc
l'appelant est dehors », n'a donc jamais été confrontée à un projet qui a un
dehors.

`spring-petclinic` en a un, et il est dans le dépôt :

```
6 contrôleurs (OwnerController, PetController, VisitController, VetController, …)
3 interfaces de cœur, toutes des dépôts Spring Data
```

Ses seules interfaces sont implémentées **hors périmètre** par Spring Data, donc
`implementersInTheCore` y est vide et **R5b ne doit pas mordre**. C'est le test
de non-régression naturel de D38.

**Proposition** : brancher `spring-petclinic` **en premier** des quatre bancs,
pas en quatrième. C'est celui qui peut falsifier une règle en vigueur ; les
trois autres décrivent des architectures que le projet a lui-même écrites.

## V4 — Ancrage de la propriété 2 de A (déterminisme par permutation)

Site concret à couvrir : `ExposedContract.java:98-99` et
`OfferedContract.java:90` prennent le **premier** élément d'une liste
d'implémenteurs ou de détenteurs, dérivée de `perimeter().types()`
(`Contracts.java:73-78, 113-117`).

Le **verdict** n'en dépend pas — une égalité de score et de distance tombe en
AMBIGUOUS avant tout départage (`Aggregator.java:120`, `147-149`). En revanche
le **texte de la preuve** et les `relatedTypes` en dépendent, donc la
restitution et les goldens aussi. C'est là que la propriété 2 doit mordre, et
elle n'y est aujourd'hui testée par rien.

## V5 — Les étapes 2 et 7 de A sont couplées

L'étape 2 demande « au moins un vrai refus de génération ». Le refus le plus
naturel, et le seul que le corpus actuel ne peut pas produire (M2), est
précisément celui de l'étape 7 : un verdict obtenu par absence seule.

Donc le marquage de R5b n'est pas un balayage postérieur : il est le **cas
d'usage de conception** de l'axe `UsageAuthorization`. L'étape 7 devient le
balayage des **six** règles restantes, une fois l'axe éprouvé sur la septième.

Corollaire de M2 : tant que le corpus ne porte que `HIGH` et `EXPLICIT`, l'axe
introduit à l'étape 2 reste **non exercé par le cliquet**. Le rebasage (étape 4)
doit donc produire des attentes portant explicitement des verdicts non
générables, sans quoi on aura remplacé une porte jamais testée par une autre.
