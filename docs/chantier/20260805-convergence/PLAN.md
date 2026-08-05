# Plan de reprise

> **Statut : révision 4 — trois amendements arbitrés le 2026-08-05.** La
> révision 3 (commit `ab52fb5`) a été adoptée puis amendée sur trois clauses
> identifiées, et sur elles seules : l'articulation des règles, l'indépendance de
> validation, et l'ouverture d'E1 par la caractérisation `E1a`. **Les quatre
> arbitrages ci-dessous sont inchangés.**
>
> **Statut hérité : échange clos, quatre arbitrages pris le 2026-08-05.**
> Le désaccord entre A et B est vidé
> ([00-etat-du-desaccord.md](00-etat-du-desaccord.md)) ; ce document est la
> sortie commune, corrigée par la relecture critique du tour 03, les
> vérifications du tour 04 et les trois formulations finales des tours 05-06.
> Les quatre questions ouvertes sont **tranchées** (voir la fin). Ce qui reste :
> porter ces décisions dans
> [DECISIONS.md](../20260731-refactoring-audit/DECISIONS.md) et
> [CHANTIER.md](../20260731-refactoring-audit/CHANTIER.md), après quoi ce
> répertoire se clôt.
>
> Date : 2026-08-05. État mesuré : `hexaglue-next` à `d5386a2`,
> [MESURES.md](MESURES.md).

## Ce que le plan corrige

Le chantier a été conduit en pensant migration d'abord. Ce qui a été transplanté
sans être requalifié n'est pas l'architecture — elle est saine — mais **la
définition de ce qui est correct** : 122 des 143 scénarios du profil dominant
portent encore le nom d'une méthode de test de l'ancien réacteur, 80 n'attestent
que du silence, et le corpus entier ne contient aucune attente sous `HIGH`.
Derrière ce mauvais étalon, trois dettes se sont installées : une échelle de
confiance écrasée, une sémantique de solveur non énoncée, et des conclusions
tirées d'absences qui autorisent la génération.

## Le principe qui gouverne les révisions

La première rédaction du plan reproduisait le défaut qu'elle diagnostique : elle
posait « P1-P4 vertes » comme critère de sortie alors qu'au moins une de ces
propriétés est fausse aujourd'hui, tout en interdisant de rouvrir un
comportement avant E7. Une propriété qu'on ne peut rendre verte sans transgresser
le plan n'est pas un critère de sortie.

D'où la règle qui structure les révisions :

> **Mesurer une propriété et exiger qu'elle soit vraie sont deux gestes
> séparés, dans deux sessions séparées.** On caractérise d'abord, avec des
> contre-exemples minimaux ; on décide ensuite lesquelles deviennent des
> garanties du produit. Une propriété non décidée reste une mesure informative :
> elle ne se rend jamais verte artificiellement, et le code ne s'adapte jamais à
> une propriété que personne n'a arbitrée.

## Règles de conduite du plan

**Articulation des règles.** Les six règles gouvernent le plan de reprise. Les
treize règles de `CHANTIER.md` restent applicables ; **toute contradiction est
consignée et arbitrée explicitement**.

1. **Une trouvaille faite dans un lot est enregistrée `PENDING`, jamais tranchée
   ni implémentée dans ce lot.**
2. **Toute mesure qui étaye une décision est une commande relançable**, avec ses
   entrées et son résultat attendu.
3. **Une décision de portée générale présente un cas nominal, un contre-exemple
   et son effet sur plusieurs projets.**
4. **« Débloque le lot » est un impact de calendrier, jamais un argument de
   justesse.**
5. **Indépendance de validation.** Une même session ne peut pas à la fois
   proposer ou implémenter une modification d'un artefact qui détermine ce qui
   est correct, puis **valider seule** cette modification. Une session distincte
   assure la validation et son enregistrement. Cette règle s'applique notamment
   au déplacement d'un cliquet, à l'enregistrement d'une attente d'oracle et à
   la fermeture d'un erratum.

   **Ce que « session distincte » exige, par objet.** Pour le **déplacement d'un
   cliquet** et l'**enregistrement d'une attente d'oracle** : une session de
   travail distincte de celle qui a produit la modification, l'arbitrage de
   l'utilisateur ne s'y substituant pas — c'est là qu'une sortie d'agent peut se
   graver en référence, et le piège est connu depuis M3, quand
   `assertMatches` créait le golden absent au lieu d'échouer. Pour la
   **fermeture d'un erratum** : l'arbitrage de l'utilisateur suffit, la
   correction se lisant en diff.

   Les rubriques locales ne répètent pas cette règle, elles la citent : le
   cliquet décrit ce qui constitue un déplacement, l'oracle conserve sa
   procédure en quatre temps, les errata conservent leurs transitions.
6. **Caractériser n'est pas garantir** (voir ci-dessus).

## Les propriétés

Elles sont **caractérisées** en E3a et **arbitrées** en E3b. Aucune n'est un
critère de sortie avant d'avoir été décidée.

| # | Propriété | État présumé | Note |
|---|---|---|---|
| **P0a** | **Sûreté sous perte involontaire de visibilité** : `GENERABLE(vue incomplète) ⇒ GENERABLE(vue complète)`, à sources physiques et intention d'analyse inchangées. Omettre une racine, échouer à parser une partie du projet ou ignorer une source ajoutée au build ne peut jamais **augmenter** l'autorisation. | à caractériser | C'est le test de D19 et **rien ne le remplace** : il demande une **fixture multi-racine**, écrite en **E2b**. |
| **P0b** | **Réduction explicite du périmètre** : `includePackages`/`excludePackages` peuvent légitimement changer les verdicts, mais la preuve porte le périmètre sur lequel l'absence a été constatée, et la génération ne traite jamais cette absence comme universelle. | à caractériser | **La prédiction V8 est falsifiée** : masquer `com.acme.shop.application` ne fait pas mordre R5b sur `InventoryUseCases`, il reste UNCLASSIFIED. Raison structurelle : `InventoryApplicationService` **implémente** le port et partage le paquet de ses deux détenteurs, donc l'exclusion retire l'implémenteur avec eux et R5b perd sa précondition (`implementersInTheCore` non vide). Une exclusion par paquet ne sait pas séparer détenteur et implémenteur ici : **P0b demande une fixture, comme P0a** — écrite en **E2b**. |
| **P1** | **Normalisation par clé de corrélation** : l'unité de score est une justification indépendante, identifiée par une clé explicite — `(ruleFamily, subject, candidateKind, semanticAnchor)` — jamais par le nombre de faits ni par le texte d'un message. | **rouge** | Reformulée deux fois. Tour 04 (V6) : « dupliquer une évidence identique » est vert par construction. Tour 06 (V10) : la clé actuelle est `(sujet, kind, palier, jeton `fact()`, distance)` — **elle contient le texte écrit à la main et pas la `RuleId`**, donc modifier un message change un poids. Les quatre composants de la clé cible existent déjà (`RuleId` passée à `KindEvidence.derived`, `Evidence.relatedTypes` pour l'ancre). **Changement à double sens** : il fusionne des signaux aujourd'hui séparés et scinde des signaux aujourd'hui fusionnés par collision de jetons. |
| **P2** | **Déterminisme par permutation** : réordonner fichiers, types, champs, méthodes et règles ne change pas le résultat canonique. | contre-exemple présumé | Site à couvrir : `ExposedContract.java:98-99`, `OfferedContract.java:90` — le `.get(0)` dont dépendent le texte de preuve et les `relatedTypes` (V4). |
| **P3a** | **Confluence sous initialisations admissibles** : état vide, permutations d'insertion, sous-ensembles cohérents d'un résultat produit. | à caractériser | Remplace « unicité globale » : un état initial arbitraire n'est pas un chemin produit, l'exiger imposerait une réécriture pour un cas qui n'existe pas (tour 03, §5.1). |
| **P3b** | **Stabilité de fermeture de l'assemblage** : un tour complet supplémentaire produit le même `ArchModel`, les mêmes relations, les mêmes diagnostics, la même explication rendue. | à caractériser | Périmètre réduit au tour 04 (V7) : la classification est déjà couverte, `Verdicts.equals` comparant des `Classification` qui sont des records — evidences et proof comprises. Ce qui reste hors du point fixe est l'assemblage. |
| **P4** | **Convergence ou diagnostic** : toute analyse termine sur un point fixe ou rend le diagnostic d'oscillation. | à caractériser | Première utilisation légitime de jqwik. |
| **P5** | **Invariance au nommage** : un renommage bijectif ne change aucun verdict quand S6 est éteint. | à caractériser | |
| **P6** | **Ajout de code sans rapport** : ajouter un type isolé ne modifie pas les verdicts du sous-graphe. | à caractériser | **Borné** : ajouter un détenteur ou un implémenteur n'est pas « sans rapport » pour une règle d'absence. C'est P0a/P0b qui couvrent ce cas, pas P6. |
| **P7** | **Permutation de représentation** : réordonner champs et paramètres sans changer les affectations ne modifie ni le modèle ni la génération. | **rouge, défaut connu** | C'est le défaut 4 du banc, ouvert depuis M7a. Sur le chemin critique (voir E7). |

## Étapes

---

### E0 — Geler M7b et consigner l'état

M7b s'arrête après le lot 4. Les quatre commits `5a53135` → `d5386a2` restent en
place ; le lot 5 n'ouvre pas. Le point de reprise de CHANTIER.md renvoie à ce
plan. D33 reste `PENDING` jusqu'à E7.

---

### E1 — Casser le couplage palier / confiance / autorisation

#### E1a — Caractériser ce que le marqueur de génération change

**Ouvre l'étape, avant toute conception de politique.** E1 prend le verdict
obtenu par absence seule comme cas de conception, et ce verdict est celui de
R5b. Or l'absence sur laquelle R5b conclut est en partie **créée par le
pipeline** : un adapter pilotant marqué `@jakarta.annotation.Generated` est
écarté du périmètre (D15), donc R5 ne peut plus mordre et R5b devient la seule
voix. Décider le traitement d'un verdict fondé sur une absence sans avoir mesuré
ce que cette exclusion change à l'absence serait décider avant de savoir.

**Matériel** : les mêmes sources en deux variantes, ne différant que par la
présence du marqueur sur l'adapter pilotant. **Aucun backend rest n'est
nécessaire.**

**Ce que la caractérisation rend** : le registre de passage montrant l'inclusion
puis l'exclusion de l'adapter, les signaux R5 et R5b dans les deux vues, le
verdict obtenu et l'autorisation rendue au consommateur — le tout par une
commande relançable sur la révision de référence.

**Ce que l'étape ne fait pas** : décider. Elle mesure ; E1 décide ensuite.

**Ce qui reste dehors** : la fermeture du cycle — HexaGlue régénère-t-il
l'adapter qu'il vient d'écarter ? — demande un générateur d'adapters pilotants
et reste affectée à **E8**, comme validation bout en bout du garde-fou conçu
ici.

#### E1b — La séparation elle-même

**Objet.** Trois notions aujourd'hui confondues sont séparées :

```text
EvidenceTier        ce qui fonde la preuve                    (S1…S6, inchangé)
Confidence          solidité du verdict                       (ne dérive plus mécaniquement du palier)
Politique typée     (consommateur, classification, preuves) → autorisation
```

**Correction majeure du tour 03** : l'autorisation **n'est pas un attribut du
type**. Une classification peut être assez sûre pour un inventaire, insuffisante
pour JPA, sans objet pour REST. La poser comme valeur unique sur la
classification recréerait l'écrasement d'un étage plus haut, cette fois entre
backends. C'est une **fonction**, appliquée par un consommateur.

**Ce que l'étape ne fait pas** : inventer une nouvelle table S1-S6 → confiance.
Elle casse le couplage ; le calibrage est E4b.

**Cas de conception, pas balayage** : le premier consommateur de la politique est
le verdict obtenu **par absence seule**. R5b sert de sujet ; les six autres
règles d'absence suivent en E6.

**Sortie.** `Contribution.eligible` ne se résume plus à `confidence >= HIGH`.
Tests de contrat pour les quatre consommateurs, dont **des tests négatifs** : un
verdict sous le seuil refusé, un verdict ambigu refusé malgré un candidat
plausible, un verdict par absence seule refusé pour la génération, et le refus
produisant son diagnostic et sa remédiation.

---

### E2 — Nommer la sémantique réelle du solveur

**Objet.** Le doc 07 §4.1 annonce une saturation monotone et des « preuves
gratuites ». Le code fait autre chose, délibérément
(`Classifier.java:28-31`) : itération déterministe depuis un état initial vide
jusqu'à un verdict auto-cohérent, faits recalculés à chaque tour, convergence
surveillée par un plafond et non garantie par monotonie.

**Sortie.** La sémantique est écrite dans le code, et le doc 07 §4.1 est
**corrigé** — pas amendé après coup pour suivre le code, corrigé parce qu'il
énonce un faux. Les preuves sont raccordées dans `Explanation` : une raison qui
cite un verdict du tour précédent expose le verdict final correspondant
(`Explanation.java:46-50`).

**Ce que l'étape ne fait pas** : réécrire le solveur.

---

### E2b — Produire le matériel de caractérisation manquant

**Objet.** Trois propriétés ne peuvent produire ni résultat vert ni
contre-exemple rouge faute de matériel, et E3a ne pourrait donc pas satisfaire
son propre critère de sortie sur trois de ses neuf propriétés — le défaut que la
relecture du tour 03 avait trouvé dans la révision 1, revenu sous une autre
forme.

| Propriété | Ce qui manque | Ce qu'il faut écrire |
|---|---|---|
| **P0a** | aucun des quatre projets réels n'a de détenteur dans une racine séparable | une fixture **multi-racine** : contrat implémenté par un type du cœur dans la racine A, détenteur dans la racine B ; analyse avec et sans B |
| **P0b** | l'exclusion par paquet ne sépare pas détenteur et implémenteur — c'est ce qui a réfuté la prédiction V8 | une fixture où **implémenteur et détenteurs vivent dans des paquets distincts**, donc séparables par `excludePackages` |
| **H-1** | observer la fermeture du cycle demande un générateur d'adapters pilotants ; le backend rest n'existe pas (M7b gelé au lot 4) | **reste `BLOQUÉE`** : la fixture ne suffit pas, il faut un générateur. À réinstruire à la reprise de M7b, pas avant |

**Sortie.** Deux fixtures au testkit, avec leurs deux configurations d'analyse.
Elles restent ensuite comme tests de non-régression : ce sont les seuls cas du
parc qui exercent une règle d'absence sous périmètre variable.

**Ce que l'étape ne fait pas** : corriger quoi que ce soit. Elle produit le
matériel, elle ne mesure pas — c'est E3a.

---

### E3a — Caractériser les propriétés

**Objet.** Installer jqwik (annoncé au doc 07 §7, absent de tous les `pom.xml`),
exécuter P0 à P7 comme **expériences relançables**, minimiser les
contre-exemples.

**Sortie.** Pour chaque propriété : vraie, fausse avec contre-exemple minimal,
ou hors du contrat produit. **Pas** « P1-P4 vertes ». Une propriété que le
matériel ne permet pas d'instruire sort **`BLOQUÉE(…)`** en nommant ce qui
manque — c'est le cas de **H-1** après E2b, et ce statut est une sortie
légitime, pas un échec de l'étape.

**Interdit explicite.** Aucun comportement n'est corrigé dans ce lot. Une
propriété rouge est un constat enregistré, pas une dette à solder sur-le-champ.

---

### E3b — Décider quelles propriétés deviennent des garanties

**Objet.** Session distincte, arbitrage de l'utilisateur : lesquelles des huit
propriétés sont des garanties du produit.

**Sortie.** Chaque propriété retenue devient un test bloquant et son correctif
est planifié. Les autres restent des mesures informatives. Cette séparation
évite les deux dérives symétriques : adapter le code à une propriété jamais
décidée, ou affaiblir la propriété pour conserver le vert.

---

### E4a — Construire l'étalon : registre de passage, kinds et relations

**Objet.** Le rebasage ne peut pas relire « type par type » sans définir
l'univers des types à relire. Si une racine n'est pas transmise, si un type reste
hors du `basePackage`, ou si le parsing le récupère partiellement, ce type est
absent de tous les goldens — et le nouvel étalon reproduirait le défaut initial :
ne mesurer que ce que le moteur a bien voulu montrer.

**Registre de passage**, construit indépendamment de l'`ArchModel`, pour chaque
projet :

```text
type présent dans les sources du build
  → racine découverte ou ignorée
  → fichier parsé, récupéré ou rejeté
  → nœud présent dans CodeModel
  → type dans / hors périmètre de verdict
  → candidats et évidences
  → verdict ou absence de verdict
  → relations assemblées
  → autorisations par consommateur
  → artefacts effectivement planifiables
```

Le mécanisme est déjà documenté : D22 constate qu'un type hors `basePackage` est
**lu** par le frontend, **absent** du modèle, et que **rien ne le dit** — c'est
pourquoi `Analysis` rend des diagnostics. Le registre se bâtit sur ce canal.

**Le dénominateur vient de l'inventaire des sources, jamais de
`ArchModel.allTypes()`** : sinon le rappel est mécaniquement surévalué.

**L'oracle porte aussi sur les arêtes.** Les backends consomment des relations
autant que des kinds : `MANAGES`, `IDENTIFIED_BY`, `OWNS`, `ANNOUNCES`,
`CONCERNS` — les cinq valeurs de `RelationKind`, vérifiées au tour 04. Un projet
peut avoir tous ses types bien étiquetés et rester inutilisable pour JPA ou REST
parce qu'une arête manque ou pointe sur le mauvais sujet. Le manifeste relu porte
donc deux ensembles indépendants : kinds attendus / ambiguïtés légitimes / hors
taxonomie, et relations attendues / **explicitement interdites**.

Les rejets ne sont pas facultatifs : un oracle qui ne porterait que les arêtes
attendues mesurerait le rappel et laisserait la précision libre — un moteur
produisant toutes les bonnes relations **plus** de fausses obtiendrait un score
parfait.

**Ordre des bancs** :

| Rang | Banc | Fonction |
|---|---|---|
| 1 | **`spring-petclinic`** | **falsification négative de R5b** (application tierce, avec couche web), puis oracle d'acceptation |
| 2 | `case-study-ecommerce` | hexagonal et génération (banc existant) |
| 3 | `case-study-banking` | multi-module, conventions, code généré |
| 4 | `case-study-lombok` | fidélité du frontend |

Réserve du tour 03, retenue : les trois interfaces de petclinic sont des dépôts
Spring Data implémentés hors périmètre, donc R5b n'y mord pas — le banc vérifie
que la règle **ne parle pas**, il n'exerce pas le cas dangereux complet
(implémentation du cœur visible, détenteur caché). Ce cas-là est **P0a**, et il
exige la fixture multi-racine ; le banc e-commerce couvre **P0b** (V8, corrigé
par V12).

**Relecture, en quatre temps** :

1. **passe aveugle** — proposition de kinds, relations, ambiguïtés et
   hors-périmètre depuis les sources, **sans afficher le verdict courant** ;
2. **arbitrage utilisateur** — validation ou correction, surtout sur le métier
   et les frontières ;
3. **révélation** — comparaison avec la sortie courante, production du score ;
4. **enregistrement** — par une session distincte de celle qui a calculé le
   diff.

**Interdit explicite** : ne jamais enregistrer la sortie actuelle du moteur comme
nouveau golden. **Le score peut sortir rouge** — c'est même l'issue attendue. Le
vert revient par des décisions et des correctifs, jamais par déplacement du
plancher.

**Le score unique disparaît** : exactitude des silences attendus, rappel des
types classables, précision par kind, taux d'ambiguïtés correctes, taux
d'inconnus inexpliqués, couverture des relations comparée à l'oracle d'arêtes.

---

### E4b — Calibrer et décider

**Objet.** L'étape que la révision 1 annonçait sans la prévoir. Session distincte
de celle qui produit les mesures.

- comparer précision, rappel et ambiguïtés **par famille de preuves** ;
- décider ce que signifie chaque niveau de `Confidence` ;
- décider les politiques d'autorisation par consommateur **et par backend** ;
- poser les tests de contrat correspondants ;
- **seulement ensuite** enregistrer les nouveaux snapshots canoniques.

---

### E5 — Examiner la composition D19 × R5b

**Objet.** D19 restreint l'analyse à la seule racine déclarée et reconnaît
ignorer celles de `build-helper`. R5b conclut d'une absence sur ce périmètre
partiel, à `HIGH`, ce qui autorise la génération (V2).

**Pourquoi E1 ne suffit pas** (arbitrage du tour 03, Q2) : l'autorisation traite
le **droit d'agir**, elle ne rend pas l'**observation** complète. D19 peut encore
produire des classes absentes, un audit incomplet, une living-doc fausse et des
candidats fondés sur une vue partielle.

**Sortie.** Provenance des racines portée par le modèle, ou diagnostic de
complétude, ou qualification explicite des preuves par le périmètre dont elles
sont tirées. L'instruction ne préjuge pas que D19 sera modifiée.

**Statut.** Rouvre D19, donc `PENDING` jusqu'à arbitrage.

---

### E6 — Politiques des six règles d'absence restantes

**Objet.** `AdapterCollaborator`, `ConsumedContract`, `DomainCollaboration`,
`ExposedContract`, `PortSignatures`, `Shapes`. R5b est traitée en E1.

**Sortie.** Chaque conclusion tirée d'une absence alimente l'audit, la
living-doc et les candidats, mais **n'autorise pas seule une génération** ; elle
le devient corroborée par une preuve positive, une déclaration explicite, ou une
politique assumée.

**Pourquoi E5 et E6 restent séparées** : E5 porte la **complétude des entrées**,
E6 la **sémantique des règles sur une entrée connue**. Les confondre rendrait à
nouveau le périmètre responsable d'une politique, ou la politique responsable
d'un trou de lecture.

---

### E7 — Ouvrir la file de réévaluation des décisions

| Famille | Décisions | Note |
|---|---|---|
| **génération** | D27, D30, **D34**, D35 | **en premier**, P7 comme premier sujet obligatoire |
| solveur et preuves | D6, D33, D38 | |
| modèle métier | D7, D13, D16 | |
| périmètre des sources | D15, D19 | recouvre E5 |

**Condition d'ouverture d'une famille** : le corpus rebasé apporte un cas
nominal, un contre-exemple et une mesure d'impact.

**P7 est une porte, pas un item de backlog** : caractérisée dès E3a, relue sur au
moins deux projets en E4a, premier sujet de la famille génération, et **franchie
avant la reprise de M7b** — au plus tard avant le lot DTO, qui réutilise la
lecture commune du domaine. Le correctif se décide séparément : noms de
paramètres, affectations observées dans les corps, constructeur canonique des
records ou déclaration explicite ne sont pas équivalents. P7 fixe l'invariant,
elle ne choisit pas la stratégie.

---

### E8 — Reprise de M7b

Seulement après les portes correspondantes.

## Ce que ce plan ne fait pas

- Il ne rouvre aucune décision avant E7 ; E5 est explicitement `PENDING`.
- Il ne réécrit pas le solveur.
- Il n'écrit pas de nouveau document de cible. Le contrat de vérité est porté
  par les types (E1) et par des tests, pas par une table en prose — le doc 07 et
  jqwik ont montré ce que devient une cible écrite et non honorée.
- Il ne rend aucune propriété verte avant qu'elle ait été décidée (E3b).
- Il ne touche ni à `hexaglue/` (gelée) ni au périmètre fonctionnel de la 7.0.0.

## Les quatre arbitrages — TRANCHÉS le 2026-08-05

| # | Question | Décision utilisateur |
|---|---|---|
| Q1 | Ordre des étapes | **L'ordre commun** : E0, E1 (dont **E1a**), E2, **E2b**, E3a, E3b, E4a, E4b, E5, E6, E7, puis M7b. (E2b ajoutée le 2026-08-05 : sans elle, E3a ne peut pas caractériser P0a et P0b.) E5 et E6 **non regroupées**. |
| Q2 | E1 suffit-il à laisser D19 intacte ? | **Non. E5 reste.** L'autorisation traite le droit d'agir, elle ne rend pas l'observation complète. |
| Q3 | Sort des 122 scénarios transplantés | **Requalification un par un**, quatre issues : conserver et renommer (l'invariant public tient encore), réécrire en scénario câblé (question pertinente, fixture mono-type incapable de l'exercer), remplacer par une propriété (plusieurs exemples du même invariant), supprimer (teste une notion du moteur abandonné, ou redondant). Les survivants **perdent le nom des anciennes méthodes de test** et portent celui de l'invariant. |
| Q4 | Qui relit les attentes | **Relecture en quatre temps** : passe aveugle de l'agent sans afficher le verdict courant, arbitrage utilisateur, révélation et score, enregistrement par une session distincte. L'agent ne fabrique pas seul l'oracle qu'il devra satisfaire ; **le dénominateur reste exhaustif**. |

## Ce qui reste ouvert

**Le portage au chantier.** Ces quatre arbitrages, le statut requalifié de D33 et
les six résultats de [08-B-mesures-multi-projets.md](08-B-mesures-multi-projets.md)
doivent être inscrits dans
[DECISIONS.md](../20260731-refactoring-audit/DECISIONS.md) et
[CHANTIER.md](../20260731-refactoring-audit/CHANTIER.md). Tant que ce n'est pas
fait, le registre du chantier porte encore un D33 dont l'explication est démentie.

**Les trouvailles de mesure**, enregistrées et non tranchées (règle 1) :

| # | Trouvaille | Où elle atterrit |
|---|---|---|
| T1 | L'invisibilité des enveloppes à une valeur pour la composition, confirmée sur deux projets | E7, famille modèle métier, avec D7/D13/D16 |
| T2 | La boucle auto-confirmante : le pipeline écarte sa sortie générée, R5 devient inatteignable, R5b conclut de cette absence | E3a (expérience P0a), avant E5 |
| T3 | Deux exceptions immuables lues VALUE_OBJECT | E4a, avec le dénominateur issu des sources |
| T4 | `HG-FRONTEND-006` ne compte les récupérations du parser que globalement | E4a, le registre de passage a besoin du détail |
| T5 | Le classpath change les verdicts : toute mesure qui l'omet mesure un autre moteur | E4a, condition d'admission d'un oracle |
