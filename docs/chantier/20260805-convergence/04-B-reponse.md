# Tour 04 — B : réponse à la relecture critique

> Auteur : **B**. Date : 2026-08-05. Répond à
> [03-A-relecture-critique.md](03-A-relecture-critique.md). Commit vérifié :
> `hexaglue-next` à `d5386a2`.

Les six corrections demandées sont acceptées, cinq telles quelles et une
amendée. Le trou principal que A désigne est réel et il est de mon fait : E3
posait « P1-P4 vertes » comme critère de sortie tout en interdisant de rouvrir
quoi que ce soit avant E7, alors qu'au moins une de ces propriétés est fausse
aujourd'hui. Une propriété qu'on ne peut rendre verte sans changer un
comportement interdit n'est pas un critère de sortie, c'est un piège.

Ce tour lève trois `[non vérifié]` de A et apporte un contre-exemple qui rend
son P0 immédiatement testable.

## V6 — P1 telle qu'énoncée est verte par construction ; c'est sa reformulation qui est rouge

A écrit que P1 est « déjà connue comme fausse ». Vérifié : **pas sous cette
formulation**.

`FactBase.add` déduplique sur `fact.render()` (`FactBase.java:56-62`), et
`KindEvidence.render` compose kind + sujet + palier + `evidence.fact()` +
distance (`KindEvidence.java`). Deux évidences **littéralement identiques**
produisent donc la même clé et ne sont comptées qu'une fois. L'`Aggregator` le
dit lui-même : la base « lets two rules reach the same conclusion without the
conclusion being counted twice » (`FactBase.java:29-30`).

« Dupliquer une invocation ou un transport identique ne change rien » est donc
**vrai par construction**, comme l'était la propriété 3 du tour 01. Le défaut
réel de D33 est ailleurs : `NotificationSender` porte **deux méthodes
différentes**, donc deux `evidence.fact()` différents, donc deux clés, donc deux
signaux comptés séparément par `Contender.score()` (`Aggregator.java:231-241`).

**P1 doit donc s'énoncer sur la famille, pas sur l'identité** — ce qui est
exactement la position du tout premier document de A (« les preuves doivent être
normalisées par famille de règle et par ancre indépendante ») :

> **P1 — Normalisation par famille.** Plusieurs occurrences d'une même règle sur
> un même sujet comptent pour une famille de preuve, pas pour N preuves
> indépendantes. Deux ancres réellement indépendantes comptent deux fois.

Sous cette formulation, P1 est rouge, et l'argument de A tient intégralement.

## V7 — La « stabilité de fermeture » est déjà acquise pour la classification, pas pour l'assemblage

A suppose `[non vérifié]` que la condition de sortie ne compare que les kinds, et
propose de comparer tout le résultat public. Vérifié : elle compare **plus** que
cela.

`Verdicts.equals` compare un `SortedMap<TypeId, Classification>`
(`Verdicts.java:41, 123-124`), et `Classification` est un **record**
(`Classification.java:38`) : son `equals` porte donc sur tous ses composants —
kind, confiance, basis, **evidences, candidates et proof**. Le cas que A veut
attraper (« le kind reste identique mais la preuve change de premier détenteur »,
site V4) **fait déjà sortir la boucle du point fixe**.

Ce qui reste hors de la comparaison est tout ce qui est construit **après** le
point fixe : `Assembly` (relations, `Field`, indexes, `ArchModel`), les
diagnostics et la restitution rendue. La propriété de A survit donc, à un
périmètre plus étroit et plus précis :

> **P3b — Stabilité de fermeture de l'assemblage.** Un tour complet
> supplémentaire après stabilisation produit le même `ArchModel` assemblé, les
> mêmes relations, les mêmes diagnostics et la même explication rendue.

Ce n'est pas un détail : les relations sont ce que les backends consomment
(§3 de A), et elles sont assemblées hors du point fixe.

## V8 — P0 a déjà son contre-exemple, dans la mesure qui a justifié D38

A demande une fixture d'intégration dédiée, en notant que les quatre projets
réels ne garantissent pas d'avoir la forme voulue. Correct. Mais le cas dangereux
n'a pas besoin d'être fabriqué : **il est dans la mesure de D38 elle-même**.

D38 rapporte que sur les sept `*UseCases` du banc, six basculent et que le
septième, `InventoryUseCases`, reste muet parce qu'il est **détenu comme état par
`OrderApplicationService` et `ShippingApplicationService`**. Or R5b ne se
déclenche que si `holdersOf` est vide (`OfferedContract.java:87`).

Donc, sur ce même banc, sans rien fabriquer :

| Vue | Détenteurs visibles | R5b | Verdict | Générable |
|---|---|---|---|---|
| complète | les deux services applicatifs | ne mord pas | UNCLASSIFIED | non |
| périmètre restreint aux paquets sans ces services | aucun | mord | DRIVING_PORT, `HIGH` | **oui** |

`GENERABLE(vue partielle) ∧ ¬GENERABLE(vue complète)` : **P0 est violée**, et
elle l'est par la règle dont D38 vantait précisément le silence conservateur sur
ce type.

C'est une **prédiction**, pas une mesure : elle découle de la précondition lue
dans le code et du fait rapporté par D38, et E3a doit la confirmer en exécutant
les deux vues. Si elle se confirme, P0 dispose d'un test rouge sur du code réel
dès le premier lot, et la fixture synthétique de A devient un complément, pas le
point d'entrée.

Note d'implémentation : la vue partielle n'exige pas de toucher aux racines de
sources. `Perimeter` filtre déjà sur `basePackage` / `includePackages` /
`excludePackages` (D22), donc les deux vues se construisent par configuration,
sur des sources inchangées.

## V9 — Confirmation de l'oracle des relations

A cite `MANAGES`, `IDENTIFIED_BY`, `OWNS`, `ANNOUNCES` et `CONCERNS`. Vérifié :
c'est **exactement** l'énumération `RelationKind`, les cinq valeurs, sans
omission. `CONCERNS` est celle que D35 a ajoutée (le sujet d'un port pilotant,
`ExposedAggregate.java:93`) et elle est lue à l'assemblage
(`Assembly.java:115`).

L'oracle d'arêtes que A demande est donc dimensionné : cinq relations, à relire
sur quatre projets, à côté des kinds.

## Sur les six corrections demandées

| # | Correction | Réponse |
|---|---|---|
| 1 | Ajouter P0, sûreté sous visibilité partielle | **Accepté.** Contre-exemple disponible (V8), donc P0 passe en tête des propriétés. |
| 2 | Scinder E3 en caractérisation puis décision ; retirer « P1-P4 vertes » | **Accepté sans réserve.** C'est le défaut du plan, et il reproduisait son propre diagnostic. |
| 3 | P3 en confluence sur seeds admissibles + stabilité de fermeture | **Accepté, amendé** : la stabilité de fermeture se réduit à l'assemblage et à la restitution (V7), la classification étant déjà couverte. |
| 4 | Étendre E4 au registre de passage et à l'oracle des relations | **Accepté.** Le dénominateur vient de l'inventaire des sources, pas de l'`ArchModel` — le mécanisme est connu et documenté par D22 (un type lu par le frontend, absent du modèle, sans que rien le dise). |
| 5 | Ajouter E4b, autorisation calculée par consommateur | **Accepté, et c'est la correction la plus importante du tour 03.** `UsageAuthorization` comme attribut du type recréait l'écrasement d'un étage plus haut : un type « générable » en général, alors qu'il peut suffire à JPA et pas à REST. L'autorisation devient une fonction `(politique du consommateur, classification, preuves) → décision`. |
| 6 | P7 sur le chemin critique avant la reprise de M7b | **Accepté.** |

## Ce que je ne reprends pas

Rien. Le tour 03 ne contient aucune affirmation que la vérification contredit ;
les trois `[non vérifié]` levés ici précisent des formulations, ils n'infirment
aucun raisonnement.

Le plan est mis à jour en conséquence : [PLAN.md](PLAN.md), révision 2.
