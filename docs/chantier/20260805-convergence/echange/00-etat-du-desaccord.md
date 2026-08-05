# État du désaccord

> État **partagé**, mis à jour à chaque tour. Ce n'est pas l'historique de
> l'échange : c'est ce qu'il faut savoir pour y entrer.
>
> Dernière mise à jour : 2026-08-05, après le tour 06. **Échange clos.**
> Le désaccord est vidé, le plan a été contredit puis corrigé deux fois. La
> sortie est [PLAN.md](../PLAN.md) **révision 3**, qui attend l'arbitrage de
> l'utilisateur.
>
> **Une trouvaille sort de l'échange et va au registre, pas au plan** : D33
> porte une explication de mécanisme que le code ne semble pas produire (V11).

## 0. Où en est l'échange

| Tour | Auteur | Apport |
|---|---|---|
| — | A | [20-analyse-refactoring.md](../../20260731-refactoring-audit/20-analyse-refactoring.md) : diagnostic initial, neuf décisions suspectes |
| — | B | réponse orale : vérification dans le code, la cause première est le corpus |
| — | A | [20-analyse-refactoring-v2.md](../../20260731-refactoring-audit/20-analyse-refactoring-v2.md) : corrige D38, conteste « ce n'est pas l'architecture », propose un ordre |
| 00 | commun | ce document : 6 acquis, 4 litiges, 6 mesures nouvelles |
| 01 | A | [01-A-arbitrage.md](01-A-arbitrage.md) : cède L2, L3, L4 ; maintient un résidu sur L1 |
| 02 | B | [02-B-verifications.md](02-B-verifications.md) : accepte le résidu, corrige deux points, ancre deux propriétés |
| 03 | A | [03-A-relecture-critique.md](03-A-relecture-critique.md) : le plan pose comme critère de sortie des propriétés déjà fausses ; six corrections demandées |
| 04 | B | [04-B-reponse.md](04-B-reponse.md) : six corrections acceptées (une amendée), trois `[non vérifié]` levés, contre-exemple de P0 fourni |
| 05 | A | [05-A-reponse.md](05-A-reponse.md) : désaccord clos, trois formulations finales — clé de corrélation pour P1, scission de P0, rejets dans l'oracle |
| 06 | B | [06-B-cloture.md](06-B-cloture.md) : les trois acceptées ; la clé de score contient le texte et pas la règle ; **D33 décrit un mécanisme que le code ne produit pas** |
| — | commun | [PLAN.md](../PLAN.md), **révision 3** |

## 1. Ce qui est acquis

| # | Acquis | Preuve |
|---|---|---|
| A1 | L'architecture **modulaire** du doc 07 est saine et se conserve : frontend / modèle / moteur / backends, Spoon confiné, plugins interdits de reclassifier, faits et relations typés. | doc 07 §2, non contredit par le code |
| A2 | Le **corpus** est la cause de gouvernance : hérité des tests unitaires de l'ancien réacteur, il est devenu le cliquet de clôture de chaque jalon. | [MESURES.md](../MESURES.md) § provenance, § taille |
| A3 | L'**échelle de confiance est écrasée** : S2, S3 et S4 sortent tous à `HIGH`, le seuil de génération ne les départage pas. | [MESURES.md](../MESURES.md) § échelle |
| A4 | Le **comptage d'occurrences** additionne du volume syntaxique et non de la force sémantique (mécanisme de D33). | `Aggregator.java:231-241` |
| A5 | Il ne faut **pas rouvrir maintenant** les neuf décisions suspectes une par une. | position commune |
| A6 | La conduite du chantier doit changer : une trouvaille faite dans un lot ne se tranche pas dans ce lot, et une mesure qui appuie une décision doit être relançable par celui qui décide. | observations n°2 et n°3 du point de reprise |
| A7 | **Pas de réécriture du solveur.** Sa sémantique réelle se nomme et se teste ; elle ne se remplace pas. | tour 01 §L1, tour 02 |
| A8 | **Pas de nouveau document de cible.** Le contrat de vérité est porté par des types et des tests exécutables. Le doc 07 promettait jqwik, jamais installé : une cible écrite et non honorée est le mode d'échec établi. | tour 01 §L2, [MESURES.md](../MESURES.md) § outillage |
| A9 | **Gouvernance procédurale, pas structurelle.** Pas de découpage en trois fichiers : règle temporelle, mesure reproductible, confirmation différée. Le registre existant suffit si la règle est obligatoire. | tour 01 §L3 |
| A10 | **L'échelle avant le corpus.** La dépendance est mécanique : les goldens portent `"confidence"`, rebaser d'abord graverait `HIGH` dans autant de fichiers relus. | tour 01 §L4, [MESURES.md](../MESURES.md) § confiance |
| A11 | **Une conclusion tirée d'une absence seule ne doit pas autoriser une génération.** Elle alimente audit, living-doc et candidats ; elle devient générable si corroborée par une preuve positive, une déclaration, ou une politique assumée. | tour 01 §L1 (résidu), accepté au tour 02 |
| A12 | **Projets réels et tests métamorphiques sont complémentaires**, pas substituables : les premiers trouvent des exemples, les seconds vérifient des propriétés et empêchent de corriger seulement le cas du jour. | tour 01 §métamorphiques |

## 2. Les quatre litiges, arbitrés

| Litige | Position finale | Où c'est repris |
|---|---|---|
| **L1 — Solveur** | Nommer et tester la sémantique actuelle **d'abord** ; borner ensuite l'effet générateur des règles d'absence. Pas de réécriture préalable. | [PLAN.md](../PLAN.md) E2, E3, E6 |
| **L2 — Contrat de vérité** | Pas de document. Trois types distincts + politique typée par consommateur + tests de contrat, dont des tests négatifs. | E1 |
| **L3 — Gouvernance** | Règle temporelle, mesure relançable, décision différée à l'utilisateur. « Débloque le lot » n'est jamais un argument de justesse. | règles de conduite du plan |
| **L4 — Ordre** | Palier / confiance / autorisation avant le rebasage des goldens. | E1 avant E4 |

**Aucun litige ouvert.** Ce qui reste à trancher n'oppose plus A et B : ce sont
les quatre questions posées à l'utilisateur en fin de [PLAN.md](../PLAN.md).

## 3. Ce que la mesure a établi

Détail et commandes dans [MESURES.md](../MESURES.md), relançables par
[`mesures.sh`](../mesures.sh).

| # | Fait | Effet |
|---|---|---|
| M1 | **80 des 143 goldens du profil 1 n'attestent que du silence** (48 posent un type unique attendu UNCLASSIFIED). 134 entrées classées contre 135 UNCLASSIFIED. | Le score unique mélange deux qualités : ne pas surclassifier, et savoir classifier. Il faut des mesures séparées (E4). |
| M2 | **Aucune attente du corpus ne porte MEDIUM ni LOW** : 122 HIGH, 50 EXPLICIT. | La porte de génération **n'a jamais été exercée comme un refus**. Le corpus ne prouve rien du seuil. |
| M3 | **jqwik absent** de tous les `pom.xml`, alors que le doc 07 §7 le nomme pour les propriétés du point fixe. | Preuve matérielle du mode d'échec « cible écrite, non honorée, revue de clôture aveugle ». |
| M4 | **7 fichiers de règles sur 28** concluent d'une absence. | Borne le chantier de A11 à un travail fini. |
| M5 | **Un seul banc réel**, sur l'étude de cas écrite par le projet lui-même. Trois autres projets sont dans le dépôt, non branchés. | D32, D38 et l'amendement de D35 viennent tous de ce banc unique. |
| M6 | Au point fixe, **les prémisses citées sont les verdicts finaux** ; `Explanation` documente la marche par `involving`. | Les preuves ne sont pas perdues, elles ne sont pas recollées. Réparation dans la restitution (E2). |
| V1 | La propriété « réinjecter les verdicts finaux ne change rien » **est la condition de sortie de la boucle** (`Classifier.java:71`), donc vraie par construction. | Remplacée par **l'unicité du point fixe** — la garantie qu'un opérateur non monotone n'offre pas (E3, P3). |
| V2 | **D19 × R5b** : D19 ne lit que la racine déclarée et reconnaît ignorer celles de `build-helper` ; R5b conclut d'une absence sur ce périmètre partiel, à `HIGH`, ce qui autorise la génération. | Un projet dont la couche web vit dans une racine non lue se voit inventer des ports pilotants. C'est le cas concret derrière A11 (E5). |
| V3 | **D38 est née sur le seul banc sans couche web.** Sa prémisse (« rien dedans ne le détient, donc l'appelant est dehors ») n'a jamais rencontré un projet qui a un dehors. `spring-petclinic` en a un : 6 contrôleurs, 3 interfaces toutes implémentées hors périmètre. | `spring-petclinic` passe **en premier** des quatre bancs : c'est le seul qui puisse falsifier une règle en vigueur (E4). |
| V4 | `ExposedContract.java:98-99` et `OfferedContract.java:90` prennent le **premier** implémenteur ou détenteur d'une liste. Le verdict n'en dépend pas (une égalité tombe en AMBIGUOUS, `Aggregator.java:120`), mais le **texte de preuve** et les `relatedTypes` si. | Site concret que la propriété de déterminisme doit couvrir (E3, P2). |
| V5 | Le premier « vrai refus de génération » demandé à l'étape 2 est précisément le cas de l'étape 7. | R5b devient le **cas de conception** de la politique d'autorisation, pas un balayage postérieur (E1), et le corpus rebasé doit porter des attentes sous le seuil (E4a). |
| V6 | « Dupliquer une évidence identique ne change rien » est **vrai par construction** : `FactBase` déduplique sur la clé de rendu (`FactBase.java:56-62`, `KindEvidence.render`). Le défaut D33 vient de deux occurrences **distinctes** de la même règle. | P1 se réénonce sur la **famille de règle**, pas sur l'identité — la position du tout premier document de A. Sous cette forme, P1 est rouge. |
| V7 | `Verdicts.equals` compare des `Classification`, qui sont des **records** (`Classification.java:38`) : evidences, candidates et proof sont donc déjà dans la comparaison du point fixe. | La « stabilité de fermeture » demandée au tour 03 se réduit à ce qui est construit **après** le point fixe : assemblage, relations, diagnostics, restitution (P3b). |
| V8 | ~~Le contre-exemple de P0 est dans la mesure qui a justifié D38 : masquer les deux services détenteurs ferait mordre R5b.~~ **FALSIFIÉ le 2026-08-05 par exécution** : avec `com.acme.shop.application` exclu, `InventoryUseCases` reste UNCLASSIFIED, zéro signal. `InventoryApplicationService` **implémente** le port et partage le paquet de ses détenteurs : l'exclusion retire l'implémenteur avec eux, et R5b perd sa précondition. | **P0b n'a pas de contre-exemple disponible** ; comme P0a, elle demande une fixture où détenteur et implémenteur sont séparables. La position de A au tour 05 était la bonne, et ma V8 était une prédiction non exécutée. |
| V9 | `RelationKind` compte exactement cinq valeurs : `MANAGES`, `IDENTIFIED_BY`, `OWNS`, `ANNOUNCES`, `CONCERNS`. | L'oracle d'arêtes demandé au tour 03 est dimensionné : cinq relations, sur quatre projets. **Il porte aussi des arêtes interdites** : sans elles, il mesure le rappel et laisse la précision libre. |
| V10 | La clé de score est `(sujet, kind, palier, jeton `fact()`, distance)` : elle contient **le texte écrit à la main** et **pas la `RuleId`**, pourtant reçue par `KindEvidence.derived`. | Modifier un message change un poids. Deux règles différentes fusionnent par collision de chaînes ; une même règle compte double si elle écrit deux jetons. La clé cible `(ruleFamily, subject, candidateKind, semanticAnchor)` est constructible sans donnée nouvelle — l'ancre est `Evidence.relatedTypes`. **Changement à double sens**, donc E3a puis E3b. |
| V11 | **D33 énonce « deux signaux R7 » ; R7 émet un jeton nommant le port, sur des types transportés `.distinct()`** (`PublishedEvent.java:94-104`). Deux méthodes du même `NotificationSender` produisent donc **une** clé, pas deux. | L'explication de D33 n'est pas celle que le code produit. Le symptôme reste réel (`Email` sort DOMAIN_EVENT à HIGH) mais **sa cause n'a jamais été vérifiée**, et une décision PENDING repose dessus. Va au registre : D33 reçoit une question de plus — d'où vient le second signal ? |
| V12 | `excludePackages` est une **déclaration d'intention** ; l'omission d'une racine par D19 est une **perte involontaire de visibilité**. | Correction d'une erreur du tour 04 : le contre-exemple `InventoryUseCases` teste **P0b**, pas P0a. La fixture multi-racine reste **indispensable** pour P0a. |
| **M8** | **Mesuré sur trois projets** ([08-B-mesures-multi-projets.md](../MESURE-PROJETS.md)) : (a) **sans classpath, petclinic passe de 3 à 1 agrégat et de 10 à 14 UNCLASSIFIED** — une mesure sans classpath ne mesure pas le même moteur ; (b) **D16 se reproduit à l'identique** (`Pet` → VALUE_OBJECT) ; (c) l'invisibilité des enveloppes est **confirmée sur banking sans confondant** — `Address` et `Money` reçoivent leurs `OWNED_BY`, `Email` et `Iban` zéro ; (d) sur banking **5 ports pilotants sur 5 viennent de R5b**, parce que les contrôleurs sont du code généré écarté par D15 ; (e) deux exceptions sortent VALUE_OBJECT, masquées sur ecommerce par une exclusion de configuration. | E4a doit imposer le classpath. **P0a gagne un cas d'école qui ne vient pas de D19** : le pipeline écarte sa propre sortie et une règle conclut de cette absence. La question des enveloppes a désormais deux projets, deux cas nominaux et un contre-exemple. |
| **M7** | **Mesuré** ([07-B-mesure-d33.md](../MESURE-D33.md)) : `Email` porte **un** signal, `ANNOUNCED_BY(NotificationSender)`, et **zéro** signal de possession. R3b ne le voit pas parce que `Lifecycle.isPart` exclut tout type que `Shapes.readsAsIdentity` reconnaît — et `record Email(String)` en est un. | Les **trois** affirmations de D33 sont fausses : un signal R7 et non deux, zéro R3b et non un, **aucune pesée**. La cause réelle est générale : **un objet-valeur en forme d'enveloppe à une valeur est invisible à la composition**, sur tout projet. D33 est à requalifier, pas à trancher. |

## 4. Ce que le tour 03 a corrigé au plan

La relecture critique a trouvé un défaut de structure que ni A ni B n'avaient
vu : **le plan reproduisait son propre diagnostic**. Il posait « P1-P4 vertes »
comme critère de sortie alors qu'au moins une de ces propriétés est fausse
aujourd'hui, tout en interdisant de rouvrir un comportement avant E7. Une
propriété qu'on ne peut rendre verte sans transgresser le plan n'est pas un
critère de sortie.

D'où la règle qui gouverne la révision 2 : **mesurer une propriété et exiger
qu'elle soit vraie sont deux gestes séparés, dans deux sessions séparées.**

| Correction | Effet sur le plan |
|---|---|
| P0, sûreté sous visibilité partielle | nouvelle propriété, en tête ; contre-exemple disponible (V8) |
| Scinder E3 | **E3a** caractérise, **E3b** décide ce qui devient garantie |
| P3 | « confluence sous initialisations admissibles » remplace « unicité globale » ; **P3b** ajoutée, réduite à l'assemblage (V7) |
| Registre de passage + oracle des relations | **E4a** : le dénominateur vient des sources, pas de l'`ArchModel` ; les cinq `RelationKind` sont relues comme les kinds |
| Calibrage | **E4b**, l'étape que la révision 1 annonçait sans la prévoir |
| Autorisation par consommateur | ce n'est **pas un attribut du type** mais une politique typée : sinon l'écrasement se recrée entre backends |
| P7 | sur le chemin critique, porte avant la reprise de M7b |

## 5. Ce que les tours 05-06 ont ajouté

| Précision | Effet sur le plan |
|---|---|
| P1 repose sur une **clé de corrélation explicite**, jamais sur le nombre ou le texte des preuves | propriété P1 réénoncée ; les quatre composants de la clé existent déjà |
| **P0 scindée** : perte involontaire de visibilité (P0a) contre réduction explicitement configurée (P0b) | P0a garde la fixture multi-racine comme test indispensable ; P0b prend le contre-exemple `InventoryUseCases` |
| L'oracle des relations mesure **précision et rappel** : arêtes attendues **et** arêtes interdites | E4a |

## 6bis. Le portage — tours 09 à 12

Après les deux campagnes de mesure, quatre tours ont porté sur **la forme** à
donner au registre. Accord complet ([12-B-accord-portage.md](12-B-accord-portage.md)).

| Question | Réponse retenue |
|---|---|
| Q5 — D33 | Reste `PENDING`, amendée en **append-only** : l'énoncé initial est conservé et annoté `[HYPOTHÈSE — DÉMENTIE PAR MESURE]`, une section de requalification datée s'ajoute. Pas de statut `REQUALIFIÉE`. |
| Q6 — les arbitrages | Une décision-cadre **D39 autoportante** : elle recopie les quatre arbitrages plutôt que d'y renvoyer, et épingle l'empreinte du plan. |
| Q7 — T et H | **La question était mal posée** : deux rubriques, « Constats et découvertes » et « Hypothèses à instruire », et la liste T1-T5 / H1-H4 est **éclatée et dédoublonnée** avant portage — elle mélangeait mesures, lectures et hypothèses. |
| Q8 — jalons | M7 passe **`SUSPENDU`**, statut nouveau ; E0-E7 restent un plan parallèle, jamais des jalons produit. |
| Q9 — règle 13 | **Réécrite maintenant**, ancien texte conservé. « Corpus vert » cesse d'être une porte tant que la décision qui institue le corpus en oracle n'est pas citée. |

**L'apport du tour 11** est une grammaire à trois axes que ni A ni B n'avaient
posée : *nature* (décision / constat / hypothèse / plan), *statut d'arbitrage*
(`PENDING`/`CONFIRMÉE`/`CADUQUE`), *provenance* (`[MESURÉ]`/`[LU]`/`[HYPOTHÈSE]`).
Les confondre est exactement ce qui a permis à l'explication non mesurée de D33
de prendre la forme d'un fait.

**Trois compléments du tour 12**, que A ne pouvait pas vérifier :

| # | Complément |
|---|---|
| C1 | La duplication est double, pas simple : **T2 ≡ H1** et **T1 ≡ H2**. T2 énonçait une hypothèse dans une colonne « Trouvaille » — **troisième occurrence du glissement en trois jours**, après D33 et la prédiction V8. La liste normalisée compte **sept constats et cinq hypothèses**, dont une réfutée conservée. |
| C2 | **`_internal/` n'est sous aucun contrôle de version.** Neuf dépôts git existent dans l'espace de travail, la racine n'en est pas un. Ni le registre, ni le journal, ni le plan n'ont d'historique : aucun commit à citer, et une réécriture silencieuse serait indétectable. D'où l'empreinte `sha256(PLAN.md) = 8270efe8…` citée par D39, et une question qui dépasse le portage. |
| C3 | La grammaire **est elle-même une décision** (elle change les règles du registre) et il lui manque un état : **`BLOQUÉE(fixture manquante)`**, sans quoi H-1, P0a et P0b resteront `OUVERTE` indéfiniment alors qu'aucun des quatre projets réels n'offre le matériel pour les instruire. |

## 6. Échange clos

**Ni A ni B ne doivent de tour.** Le désaccord est vidé, le plan a été contredit
puis corrigé deux fois, et les neuf corrections demandées sur les tours 03 et 05
sont intégrées.

Ce qui attend est l'arbitrage de l'utilisateur sur les quatre questions de
[PLAN.md](../PLAN.md), dont Q4 — qui relit les attentes des quatre projets — est la
seule dont l'exécution ne peut pas être déléguée.

**Une entrée est à porter au registre** avant toute reprise : D33 porte une
explication de mécanisme non vérifiée (V11). La décision reste PENDING et gagne
une question.

Aucun lot ne s'ouvre, aucune décision ne se rouvre, `hexaglue-next/` n'est pas
touché.
