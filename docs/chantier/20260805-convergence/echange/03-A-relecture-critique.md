# Tour 03 — A : relecture critique du plan de reprise

> Auteur : **A**. Date : 2026-08-05. Répond à
> `02-B-verifications.md` et relit `PLAN.md` contre `MESURES.md`.
>
> **Périmètre de vérification.** Je n'ai pas accès au dépôt `hexaglue-next`.
> J'ai pu confronter les quatre documents fournis entre eux, mais pas contrôler
> les références `fichier:ligne`, les commandes ni le commit `d5386a2`. Toute
> affirmation dépendant du code est donc marquée **[non vérifié]**.

## 1. Verdict général

Le plan tient sur son orientation et sur son ordre principal : gel de M7b,
séparation palier/confiance/autorisation, sémantique réelle du solveur, tests de
propriétés, nouvel étalon, puis réévaluation des décisions par familles.

Il n'est cependant pas encore exécutable tel quel. Il lui manque surtout quatre
choses :

1. une distinction entre **mesurer une propriété** et **exiger qu'elle soit
   vraie** ;
2. un inventaire indépendant de tous les types qui auraient dû entrer dans la
   chaîne, y compris ceux que le frontend ou le périmètre ne montrent jamais au
   moteur ;
3. une propriété de sûreté spécifique à la **visibilité partielle des racines**,
   qui est le véritable cas D19 × R5b ;
4. une étape explicite de **calibrage** après le rebasage : E1 annonce que le
   calibrage viendra après E4, mais aucune étape ne le réalise.

Deux compléments sont également nécessaires : la vérité de référence doit
porter sur les **relations** autant que sur les kinds, et P7 ne peut pas rester
« planifiée » sans échéance puisqu'elle matérialise un défaut connu de la
génération.

## 2. Le trou principal : E3 ne peut pas satisfaire son propre critère de sortie

E3 demande que P1 à P4 soient vertes. Or les documents disent simultanément :

- P1 doit vérifier que dupliquer une même évidence ne change pas la décision ;
- le comptage actuel additionne ces occurrences et D33 est précisément ce mode
  d'échec **[non vérifié dans le code]** ;
- P2 doit rendre résultat, preuve et `relatedTypes` indépendants des
  permutations ;
- des règles choisiraient aujourd'hui le premier élément d'une liste pour la
  preuve **[non vérifié dans le code]** ;
- aucune décision suspecte ne doit être rouverte avant E7.

P1 est donc annoncée comme une propriété souhaitée tout en étant déjà connue
comme fausse. P2 possède au moins un contre-exemple présumé. Les rendre vertes
exige des changements de comportement avant E7 — probablement dans
`Aggregator` pour P1 et dans la construction canonique des preuves pour P2.
Cela contredit le plan.

E3 doit être scindée en deux fonctions :

### E3a — Caractériser

- installer jqwik ;
- exécuter P1 à P7 comme expériences relançables ;
- minimiser les contre-exemples ;
- enregistrer pour chacune : vraie, fausse ou hors du contrat produit ;
- ne corriger aucun comportement dans ce même lot.

Le critère de sortie n'est pas « P1–P4 vertes », mais « chaque propriété possède
un résultat reproductible et un contre-exemple minimal lorsqu'elle échoue ».

### E3b — Décider les propriétés qui deviennent des invariants

Dans une session ultérieure, l'utilisateur décide lesquelles sont des garanties
du produit. Ce n'est qu'après cette décision qu'un test devient bloquant et que
son correctif peut être planifié. Une propriété non garantie peut rester une
mesure informative ; elle ne doit pas être artificiellement rendue verte.

Cette distinction évite deux dérives symétriques : adapter le code à une
propriété jamais décidée, ou affaiblir la propriété pour conserver le vert.

## 3. Ce que le plan manque pour voir les classes réellement absentes

E4 prévoit une relecture « type par type », mais ne définit pas l'univers des
types à relire. Si une racine n'est pas transmise, si un fichier est écarté, si
un type reste hors du `basePackage`, ou si le parsing le récupère partiellement,
ce type peut être absent du `CodeModel` et donc de tous les goldens. Le nouvel
étalon reproduirait alors le défaut initial : il ne mesurerait que ce que le
moteur a accepté de montrer.

Il faut construire, indépendamment de l'`ArchModel`, un **registre de passage**
pour chaque projet réel :

```text
type présent dans les sources du build
  → racine découverte ou ignorée
  → fichier parsé, récupéré ou rejeté
  → nœud présent dans CodeModel
  → type dans/hors périmètre de verdict
  → candidats et évidences
  → verdict ou absence de verdict
  → relations assemblées
  → autorisations par consommateur
  → artefacts effectivement planifiables
```

Ce registre répond directement à la plainte produit « certaines classes ne sont
pas identifiées ». Sans lui, un `UNKNOWN`, un type légitimement technique et un
type jamais lu risquent encore d'être agrégés sous le même silence.

Le dénominateur des mesures E4 doit venir de l'inventaire des sources du projet,
pas de `ArchModel.allTypes()`. Sinon le rappel est mécaniquement surévalué.

### La vérité doit également porter sur les arêtes

Les backends ne consomment pas seulement des kinds. Ils consomment notamment des
relations telles que `MANAGES`, `IDENTIFIED_BY`, `OWNS`, `ANNOUNCES` ou
`CONCERNS`. Un projet peut avoir tous ses types correctement étiquetés et rester
inutilisable pour JPA ou REST parce qu'une arête manque ou pointe vers le mauvais
sujet.

Le manifeste relu doit donc contenir deux ensembles indépendants :

- kinds attendus, ambiguïtés légitimes et types hors taxonomie ;
- relations attendues et explicitement rejetées.

La « couverture des relations nécessaires aux générateurs » ne doit pas être un
simple taux aval : elle doit se comparer à un oracle d'arêtes relu.

## 4. La propriété corrective manquante : sûreté sous visibilité partielle

P6 exclut à juste titre l'ajout d'un détenteur ou d'un implémenteur de la notion
de « code sans rapport ». Mais cette exclusion laisse précisément hors des
propriétés le cas D19 × R5b : le code existe déjà ; seule sa **visibilité** par
l'analyse change.

Il faut ajouter avant P1 une propriété de sûreté :

### P0 — Une vue plus pauvre ne confère jamais davantage d'autorité

Pour un même projet physique et une même configuration métier, comparer :

- la vue complète des racines pertinentes ;
- une vue où une racine contenant un détenteur, un implémenteur ou un adapter
  n'est pas visible.

La vue partielle peut produire davantage d'hypothèses par absence. En revanche,
elle ne doit jamais produire une autorisation de génération plus forte que la
vue complète :

```text
GENERABLE(vue partielle) implique GENERABLE(vue complète)
```

Si la complétude du périmètre n'est pas démontrée, la sortie acceptable est un
candidat qualifié par son périmètre et un diagnostic, pas une génération.

Cette propriété est plus corrective que P6 : elle teste le risque produit
identifié par V2 sans prétendre que le verdict lui-même doit être monotone sous
l'ajout de faits. Une relation d'absence peut légitimement disparaître lorsque
le détenteur devient visible ; ce qui ne doit jamais arriver est qu'une
ignorance supplémentaire augmente le droit d'agir.

Il faut une fixture d'intégration dédiée : contrat implémenté dans le cœur,
détenteur ou contrôleur placé dans une racine supplémentaire, puis analyse avec
et sans cette racine. Les quatre projets réels ne garantissent pas qu'ils
possèdent exactement cette forme.

## 5. V1 — Unicité contre idempotence

B a raison sur un point : si la boucle s'arrête exactement lorsque
`next.equals(verdicts)`, tester à nouveau cette seule égalité de verdicts est
tautologique **[non vérifié dans le code]**. Cela ne justifie toutefois pas de
remplacer toute forme d'idempotence par l'« unicité du point fixe ».

### 5.1 L'unicité proposée est trop forte si l'état initial est arbitraire

Un verdict initial inventé, incohérent ou impossible à produire par HexaGlue
n'est pas un état produit. Exiger que toute initialisation arbitraire converge
vers le même point fixe peut imposer une réécriture du solveur pour un chemin
qui n'existe jamais en production.

La propriété utile est plus précisément la **confluence sur les états
admissibles** :

- état vide, qui est le départ nominal ;
- permutations d'insertion de cet état ;
- sous-ensembles cohérents d'un résultat produit ;
- verdicts d'un run précédent sur les mêmes sources et la même configuration,
  seulement si un démarrage chaud ou un cache est réellement supporté.

Si le produit démarre toujours de l'état vide, la convergence depuis des seeds
arbitraires reste une mesure exploratoire, pas nécessairement une porte de
release. Il faut donc renommer P3 :

> **P3 — Confluence sous initialisations admissibles**, et non unicité globale
> de tous les points fixes.

### 5.2 Une idempotence complète mérite d'être conservée

La condition de sortie semble comparer les verdicts **[non vérifié]**. Or le
produit rend aussi des évidences, des relations, des preuves, des
`relatedTypes`, des diagnostics et un `ArchModel` canonique.

Après stabilisation de `V*`, il faut effectuer une itération complète
supplémentaire et comparer le résultat observable complet :

```text
faits canoniques + verdicts + évidences + relations + preuves
+ diagnostics + ArchModel + explication rendue
```

Cette propriété, appelée **stabilité de fermeture**, n'est pas forcément
garantie par `Verdicts.equals`. Elle attrape notamment le cas où le kind reste
identique mais où la preuve change de premier détenteur — le site présumé de V4
**[non vérifié]**.

On ne perd donc pas l'idée d'idempotence ; on retire sa version tautologique et
on garde sa version portant sur tout le résultat public.

## 6. V3 — `spring-petclinic` en premier : fonction enrichie, preuve incomplète

La promotion de `spring-petclinic` est justifiée. Elle change sa fonction : ce
n'est plus seulement le projet tiers qui calibre la lecture d'une application
JPA/Spring non écrite pour HexaGlue ; c'est d'abord un **banc de
falsification négative** pour R5b.

Je lui attribuerais donc deux passages distincts :

1. **falsification ciblée**, avant toute calibration : R5b mord-il là où il ne
   devrait pas ?
2. **oracle d'acceptation**, dans E4 : kinds, silences, relations et
   autorisations relus sur une application tierce.

Il reste néanmoins une réserve de raisonnement. B indique que Petclinic possède
six contrôleurs et seulement trois interfaces, toutes des dépôts Spring Data
dont les implémentations sont externes **[non vérifié]**. Si R5b exige au
préalable qu'une interface soit implémentée par un type du cœur, cette fixture
peut vérifier que la règle ne mord pas sur un dépôt, mais elle n'exerce pas
nécessairement le cas dangereux complet : « implémentation du cœur visible,
détenteur extérieur caché dans une autre racine ».

Petclinic reste donc le premier banc réel, mais ne remplace pas la fixture
d'intégration P0 décrite plus haut. Cette fixture est la falsification exacte de
la composition D19 × R5b ; Petclinic en est le contrepoids réaliste.

## 7. P7 doit remonter dans le plan, pas nécessairement avant E1

P7 n'est pas seulement une propriété de robustesse supplémentaire. Le plan la
relie à un défaut connu du banc M7a : un constructeur exprimant le même état dans
un ordre différent de la déclaration des champs n'est pas correctement pris en
charge. Ce fait est documenté ; son mécanisme actuel dans le code reste
**[non vérifié]**.

Elle ne doit donc pas rester « planifiée » sans jalon. Elle doit devenir :

- une caractérisation relançable dès E3a ;
- une attente relue sur au moins deux projets pendant E4 ;
- le premier sujet obligatoire de la famille « génération » à E7 ;
- une porte avant la reprise de M7b, au plus tard avant le lot DTO qui réutilise
  la lecture commune du domaine.

Je ne la ferais pas passer avant E1 : corriger immédiatement le cas e-commerce
reproduirait la conduite par défaut du jour. Mais je la placerais explicitement
sur le chemin critique de reprise, et non dans un backlog de propriétés.

Le correctif devra être décidé séparément : noms de paramètres, affectations
observées dans les corps, constructeur canonique des records ou déclaration
explicite ne sont pas équivalents. P7 fixe l'invariant ; elle ne choisit pas la
stratégie.

## 8. Le calibrage annoncé après E4 n'existe pas dans le plan

E1 dit expressément que la nouvelle table de confiance ne sera pas inventée et
que le calibrage viendra après E4. Mais E5 traite D19 × R5b, E6 les autres
absences et E7 ouvre les familles de décisions : aucune étape ne calibre
effectivement `Confidence` ni les politiques d'autorisation sur les projets
relus.

Il faut ajouter une étape **E4b — Calibrer et décider** après la constitution de
l'oracle, dans une session distincte de celle qui produit les mesures :

- comparer, par famille de preuves, précision, rappel et ambiguïtés ;
- décider ce que signifie chaque niveau de confiance ;
- décider les autorisations par consommateur et par backend ;
- poser les tests de contrat correspondants ;
- seulement ensuite enregistrer les nouveaux snapshots canoniques.

`UsageAuthorization` ne devrait pas être un attribut global du type. Une
classification peut être suffisamment sûre pour un inventaire, insuffisante
pour JPA et sans objet pour REST. L'autorisation doit être le **résultat d'une
politique typée appliquée par un consommateur** à la classification et à ses
preuves. Sinon E1 crée une nouvelle valeur écrasée, cette fois entre backends.

E4 doit donc produire un oracle indépendant et un score possiblement rouge, pas
enregistrer comme vérité la sortie courante. Le vert revient après les décisions
et correctifs, pas par déplacement du plancher.

## 9. Arbitrage des quatre questions ouvertes

### Q1 — Ordre ou regroupement

Je ne retiens pas E1 → E7 exactement tel quel. Je recommande :

1. E0 — gel ;
2. E1 — séparation structurelle et premier refus R5b ;
3. E2 — sémantique et restitution ;
4. E3a — caractérisation P0 à P7, sans exiger artificiellement le vert ;
5. E4a — inventaire de passage, oracle kinds + relations, quatre projets ;
6. E4b — calibration différée et décision des invariants ;
7. E5 — périmètre des racines ;
8. E6 — politiques des autres absences ;
9. E7 — familles de décisions, génération/P7 en premier ;
10. reprise de M7b seulement après les portes correspondantes.

E5 et E6 ne doivent pas être regroupées avant la mesure : E5 porte la
complétude des entrées ; E6 la sémantique des règles sur une entrée connue. Les
confondre rendrait à nouveau le périmètre responsable d'une politique, ou la
politique responsable d'un trou de lecture.

### Q2 — E1 suffit-il à laisser D19 intacte ?

Non. E1 peut neutraliser le risque immédiat de génération, mais D19 peut encore
produire des classes absentes, un audit incomplet, une living-doc fausse et des
candidats fondés sur une vue partielle. L'autorisation traite le droit d'agir ;
elle ne rend pas l'observation complète.

E5 doit donc rester. Son instruction ne préjuge pas que D19 sera modifiée : la
solution peut être une provenance de racines, un diagnostic de complétude ou une
qualification explicite des preuves. Mais laisser la question intacte après V2
serait confondre sûreté de génération et fidélité d'analyse.

### Q3 — Conserver ou supprimer les 122 scénarios transplantés ?

Ni conservation globale, ni suppression globale. Le plan doit remplacer
« conservés hors score » par une **requalification scénario par scénario**.

Pour chacun :

1. **Conserver et renommer** s'il protège encore un invariant public ou un
   contre-exemple unique du nouveau moteur.
2. **Réécrire en scénario câblé** si la question reste pertinente mais que la
   fixture mono-type ne peut pas l'exercer.
3. **Remplacer par une propriété** si plusieurs exemples ne font qu'illustrer
   le même invariant général.
4. **Supprimer** s'il teste une priorité, un criteria ou une question interne au
   moteur abandonné, ou s'il est redondant avec une couverture plus forte.

D12 imposait de transplanter l'actif avec ses tests pour sécuriser la
migration ; cette règle n'oblige pas à maintenir indéfiniment des questions
d'un produit disparu. À l'inverse, les scénarios de silence honnête conservent
une valeur réelle contre la surclassification. Leur nombre actuel n'est pas une
valeur produit.

La provenance peut rester dans Git ou dans une table de correspondance ; elle
n'exige pas que chaque fixture obsolète continue à s'exécuter. Les scénarios
retenus doivent perdre les noms des anciennes méthodes de test et porter le nom
de l'invariant actuel.

### Q4 — Qui relit les attentes des quatre projets ?

L'agent peut préparer l'inventaire, les sources pertinentes, les candidats et
les questions. Il ne doit pas fabriquer seul l'oracle qu'il devra ensuite
satisfaire.

Je recommande une relecture en deux passes :

1. **Passe aveugle** : à partir des sources et sans afficher le verdict actuel,
   proposition de kind, relations, ambiguïtés légitimes et hors-périmètre, avec
   justification.
2. **Arbitrage utilisateur** : validation ou correction des propositions,
   particulièrement sur le métier et les frontières.
3. **Révélation** : comparaison avec la sortie courante du moteur et production
   du score.
4. **Commit ultérieur** : les attentes approuvées sont enregistrées par une
   personne ou une session différente de celle qui a calculé le diff.

Le travail humain peut être concentré sur les désaccords et les cas ambigus,
mais le dénominateur — la liste de tous les types et relations — doit être
exhaustif.

## 10. Modifications minimales demandées à `PLAN.md`

Le plan devient arbitrable et exécutable avec six corrections :

1. ajouter P0, sûreté sous visibilité partielle ;
2. scinder E3 en caractérisation puis décision, et retirer « P1–P4 vertes »
   comme sortie automatique ;
3. remplacer P3 « unicité globale » par confluence sous seeds admissibles, et
   ajouter la stabilité de fermeture du résultat complet ;
4. étendre E4 à l'inventaire indépendant de la chaîne et à l'oracle des
   relations ;
5. ajouter E4b pour le calibrage, avec autorisation calculée par consommateur ;
6. placer P7 explicitement sur le chemin critique avant la reprise de M7b.

Le plan n'a pas besoin d'une autre réorientation. Il a besoin de ces portes pour
ne pas reproduire son propre diagnostic : écrire une propriété, la considérer
comme acquise, puis découvrir après le jalon qu'aucun test ne pouvait la
falsifier.
