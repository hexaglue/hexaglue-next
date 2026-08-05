# Tour 01 — A : arbitrage des quatre litiges

> Auteur : **A**. Date : 2026-08-05. Consigné tel quel, en réponse à
> [00-etat-du-desaccord.md](00-etat-du-desaccord.md).

B m'a convaincu sur L2, L3 et L4. Sur L1, sa quatrième option est la bonne
première étape, mais elle ne suffit pas seule tant que les conclusions par
absence peuvent autoriser une génération.

## Arbitrage des quatre litiges

| Litige | Position finale |
|---|---|
| L1 — Solveur | Nommer et tester la sémantique actuelle d'abord ; borner ensuite les 7 règles d'absence pour la génération. Pas de réécriture préalable du solveur. |
| L2 — Contrat de vérité | Pas de nouveau document de cible. Le contrat doit être porté par les types et des tests de contrat exécutables. |
| L3 — Gouvernance | Pas de découpage en trois fichiers. Règle temporelle, mesure reproductible et confirmation différée. |
| L4 — Ordre | B a raison : séparer palier, confiance et autorisation avant de rebaser les goldens. |

### L1 — La quatrième option est nécessaire, mais pas suffisante

Je corrige d'abord mon affirmation : les preuves inter-tours ne sont pas
perdues. Les verdicts utilisés au point fixe sont bien les verdicts finaux. Ce
qui manque est leur raccordement dans l'arbre de restitution. La réparation peut
donc rester dans `Explanation`, sans refonte du solveur.

La sémantique réelle doit maintenant être nommée honnêtement, par exemple :

> Itération déterministe depuis un état initial vide jusqu'à un verdict
> auto-cohérent ; les faits sont recalculés à chaque itération ; la convergence
> n'est pas garantie par monotonie mais surveillée par un plafond.

Il ne faut plus parler de « monotonie garantie » ni de « preuves gratuites ».

Cette sémantique peut être conservée si elle satisfait des propriétés
exécutables :

- mêmes sources, même résultat indépendamment de leur ordre ;
- stabilité du point fixe : réanalyser avec les verdicts finaux ne change rien ;
- absence d'oscillation sur le corpus ;
- diagnostic explicite lorsqu'il n'y a pas convergence ;
- chaque verdict final possède encore un support valide ;
- l'ordre ou l'identifiant des règles ne décide pas accidentellement du
  résultat.

Mais nommer la sémantique ne règle pas le risque produit : une conclusion
obtenue uniquement par absence reste dépendante du périmètre fermé observé. Ma
position est donc :

- elle peut alimenter audit, living doc et candidats ;
- elle ne doit pas autoriser seule une génération ;
- elle devient générable seulement si elle est corroborée par une preuve
  positive, une déclaration explicite ou une politique spécifique assumée.

Il n'est pas nécessaire de réécrire les 7 règles immédiatement. Il faut
empêcher que leur signal seul devienne mécaniquement `HIGH` et franchisse le
seuil.

### L2 — Pas de second document de vérité

B a raison : un nouveau document risquerait de reproduire le mode d'échec de
jqwik, promis mais jamais installé.

Le tableau par consommateur que je proposais n'a de valeur que s'il devient
exécutable. Il doit donc se matérialiser dans :

- des types distincts pour le palier, la confiance et l'autorisation ;
- une politique typée par consommateur ;
- des tests de contrat couvrant living doc, audit, validation et génération ;
- des tests négatifs prouvant qu'un verdict insuffisant est réellement refusé.

Un tableau peut servir pendant la conception, mais il ne doit pas devenir une
nouvelle source de vérité durable.

### L3 — La séparation de fichiers n'apporte pas la garantie recherchée

Trois fichiers écrits par le même exécutant ne suppriment aucun biais de
cadrage. Je retire cette proposition.

La gouvernance utile est procédurale :

1. Une trouvaille découverte dans un lot est seulement enregistrée `PENDING`.
2. Elle n'est ni tranchée ni implémentée dans ce lot.
3. La mesure qui l'étaye doit être une commande relançable, avec ses entrées et
   son résultat attendu.
4. La décision est prise dans un tour ultérieur par l'utilisateur.
5. Elle doit présenter au minimum un cas nominal, un contre-exemple et son effet
   sur plusieurs projets lorsque la portée se veut générale.
6. « Débloque le lot » est un impact de calendrier, jamais un argument de
   justesse.

Le registre existant suffit si cette règle est rendue obligatoire.

### L4 — La dépendance mécanique est réelle

Je ne la conteste pas. Rebaser maintenant graverait dans les nouveaux goldens
une confiance déjà reconnue faussement grossière.

L'ordre proposé par B est donc le bon :

1. séparer palier, confiance et autorisation ;
2. rebaser le corpus ;
3. nommer et tester la sémantique du solveur ;
4. instruire ensuite les familles de décisions suspectes.

La première étape ne doit cependant pas consister à inventer immédiatement une
nouvelle table arbitraire S1-S6 → confiance. Il faut d'abord casser le
couplage :

```text
EvidenceTier        ce qui fonde la preuve
Confidence          solidité du verdict résultant
UsageAuthorization  ce qu'un consommateur peut en faire
```

Une autorisation de génération ne doit plus se résumer à `confidence >= HIGH`.

## Lecture des nouvelles mesures

### M1 — Le score 143/143 ne mesure pas la capacité principale

Avec 80 goldens n'attestant que du silence et presque autant d'entrées
classifiées que d'`UNCLASSIFIED`, le score unique mélange deux qualités
différentes :

- ne pas surclassifier ;
- savoir classifier un graphe suffisamment informatif.

Il faut conserver les tests de silence, mais publier des mesures séparées :

- exactitude des silences attendus ;
- rappel des types effectivement classables ;
- précision par kind ;
- taux d'ambiguïtés correctes ;
- taux d'inconnus inexpliqués ;
- couverture des relations nécessaires aux générateurs.

Un moteur peut alors être excellent pour se taire et médiocre pour reconnaître,
sans obtenir artificiellement 100 %.

### M2 — La porte de génération n'a jamais été testée

C'est un défaut de sortie de jalon important. Si le corpus ne contient ni
`MEDIUM` ni `LOW`, il ne prouve absolument pas le comportement du seuil.

Avant tout nouveau backend, il faut au minimum tester :

- un verdict explicitement autorisé ;
- un verdict sous le seuil effectivement refusé ;
- un verdict issu seulement d'une absence refusé pour la génération ;
- un verdict ambigu refusé malgré un candidat plausible ;
- un verdict suffisamment corroboré accepté ;
- le diagnostic et la remédiation produits lors du refus.

### M5 — Les quatre projets réels suffisent pour le premier rebasage

Oui, `case-study-ecommerce`, `case-study-banking`, `case-study-lombok` et
surtout `spring-petclinic` constituent un socle suffisant pour remplacer le faux
sentiment de couverture actuel.

Mais leurs attentes doivent être relues type par type par un humain. Il ne faut
surtout pas enregistrer la sortie actuelle du moteur comme nouveau golden de
référence.

Leur fonction peut être différenciée :

- e-commerce : architecture hexagonale et génération ;
- banking : multi-module, conventions et code généré ;
- Lombok : fidélité du frontend ;
- Spring Petclinic : application tierce JPA/Spring non écrite pour HexaGlue.

Ils ne constituent pas encore un échantillon universel, mais ils sont largement
suffisants pour reprendre le chantier sur des bases réelles.

## Les tests métamorphiques restent indispensables

Les projets réels trouvent des exemples de défauts. Les tests métamorphiques
vérifient des propriétés générales et empêchent de corriger uniquement le cas du
jour. Il faut les deux.

Priorité proposée :

1. **Duplication d'évidence** — dupliquer une invocation ou un transport
   identique ne change ni le kind ni la confiance. C'est le test direct de D33.
2. **Déterminisme par permutation** — réordonner fichiers, types, champs,
   méthodes, faits et règles ne change pas le résultat canonique.
3. **Idempotence du point fixe** — réinjecter les verdicts finaux produit
   exactement les mêmes verdicts et supports.
4. **Convergence ou diagnostic** — toute analyse termine sur un point fixe ou
   rend le diagnostic d'oscillation attendu. C'est la première utilisation
   légitime de jqwik.
5. **Invariance au nommage en mode structurel** — un renommage bijectif des
   types ne change aucun verdict lorsque S6 est désactivé.
6. **Ajout de code sans rapport** — ajouter un type isolé ou une dépendance sans
   relation avec le sous-graphe étudié ne modifie pas ses verdicts.
7. **Permutation de représentation** — réordonner champs et paramètres sans
   changer les affectations ne doit pas modifier le modèle métier ni la
   génération.

Le point 6 doit rester précisément borné : ajouter un détenteur ou un
implémenteur n'est pas « sans rapport » pour une règle d'absence et peut
légitimement changer le verdict.

## Ordre de reprise consolidé

1. Geler M7b après le lot 4.
2. Introduire les trois axes typés et leurs tests de contrat, avec au moins un
   vrai refus de génération.
3. Installer jqwik comme annoncé et poser les propriétés 1 à 4.
4. Transformer les anciens scénarios mono-type en fixtures/invariants, hors du
   score d'acceptation relationnel.
5. Brancher et relire les quatre projets réels ; établir des mesures séparées de
   silence, précision, rappel, ambiguïté et relations.
6. Documenter dans le code la sémantique réelle du solveur et raccorder les
   preuves dans `Explanation`.
7. Auditer les 7 règles d'absence et leur retirer toute autorisation implicite
   de génération.
8. Seulement ensuite ouvrir la file de réévaluation des décisions, par familles.

C'est désormais un compromis clair entre les deux analyses : B a raison sur la
priorité mécanique et sur la gouvernance ; je maintiens seulement qu'expliciter
le solveur ne dispense pas de neutraliser l'effet générateur des conclusions
fondées uniquement sur une absence.
