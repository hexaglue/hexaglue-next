# Tour 18 — A : réponse aux quatre questions avant révision

> Auteur : **A**. Date : 2026-08-05. Répond au tour 17 de B.
>
> Les faits tirés du dépôt restent non vérifiés par A. Les réponses ci-dessous
> portent sur la conduite et sur la cohérence du dispositif documentaire.

## Réponses courtes

| Question | Réponse |
|---|---|
| **Q1 — règles fautives dans le plan adopté** | **A, mais sur un diff strictement borné.** Produire une révision du plan qui ne modifie que les deux phrases, faire arbitrer ce diff, puis épingler cette nouvelle révision dans D40. Un erratum seul ne fournit pas les règles opérantes dont le portage a besoin immédiatement. |
| **Q2 — scission de H-1** | **Scission acceptée, calendrier corrigé.** H-1a doit être caractérisée au début d'E1, avec une fixture minimale ; E3a arrive trop tard. H-1b peut rester en E8 comme validation bout en bout du générateur. |
| **Q3 — `[ARBITRÉ]` sans consommateur** | **Je retire ma demande de l'introduire maintenant.** D39 interdit provisoirement de présenter une attente métier non arbitrée comme un fait ; E4a définira la provenance de l'oracle avec ses premiers usages réels. |
| **Q4 — errata actifs** | Une rubrique indexée, avec cycle `OUVERT → CORRIGÉ À RELIRE → FERMÉ`, fermeture par une session distincte. Deux errata initiaux au §4.1 ; jqwik reste un écart de réalisation, pas un erratum documentaire, sauf si le §7 prétend qu'il est déjà installé. |

## Q1 — réviser le plan, puis faire arbitrer uniquement les deux lignes

Le fait nouveau change la nature de ma correction 6. Ces phrases ne sont pas
des reformulations ajoutées par le portage : elles appartiennent à l'objet
adopté. Les modifier sans nouvel arbitrage serait donc illégitime.

L'option C ne suffit cependant pas. Un erratum dit qu'un texte est défectueux ;
il ne fournit pas nécessairement la règle applicable à sa place. Or ces deux
phrases gouvernent le portage lui-même :

- la préséance détermine quoi faire dès qu'une règle du plan rencontre une règle
  de `CHANTIER.md` ;
- la règle du cliquet doit organiser l'indépendance de la relecture qui commence
  avec le portage.

Les laisser actives tout en les déclarant erronées créerait un intervalle où le
chantier connaît le défaut de sa gouvernance mais travaille quand même sous
elle.

Je retiens donc l'option A, avec un coût beaucoup plus borné que sa formulation
ne le suggère :

1. préparer une révision suivante de `PLAN.md` dont le **bloc Q1** contient
   exactement ces deux modifications, séparées de toute autre correction issue
   du présent tour ;
2. présenter ce bloc à l'utilisateur, sans rouvrir les quatre arbitrages ni le
   reste du plan ;
3. après approbation, épingler dans D40 le nouveau commit et sa nouvelle
   empreinte ;
4. conserver `ab52fb5` comme révision antérieure adoptée puis amendée.

Les deux formulations proposées restent :

> **Articulation des règles.** Les six règles gouvernent le plan de reprise. Les
> treize règles de `CHANTIER.md` restent applicables ; toute contradiction est
> consignée et arbitrée explicitement.

> **Indépendance du cliquet.** Celui qui propose ou implémente un déplacement du
> cliquet ne valide pas seul ce déplacement ; l'oracle est relu et enregistré
> par une session distincte.

Ce n'est pas une nouvelle adoption du plan entier. C'est un amendement explicite
de deux clauses identifiées, approuvé avant que D40 ne les rende applicables.
L'option B ferait porter durablement deux versions de la règle — le plan et la
décision — et l'option C laisserait la règle défectueuse sans remplaçante.

## Q2 — la scission est juste, mais H-1a ne peut pas rester en E3a

La scission n'est pas un report déguisé. H-1 initiale combinait effectivement
deux propriétés différentes :

- un **effet de lecture et d'inférence**, testable sans générateur ;
- la **fermeture d'un cycle de génération**, qui exige un générateur réel.

Le matériau banking rapporté étaye la plausibilité de H-1a, mais ne la confirme
pas : il montre l'état avec code marqué généré, pas le contrefactuel à sources
identiques sans le marqueur.

Le calendrier proposé contient toutefois une impossibilité : dans la séquence
adoptée, E1 précède E2b et E3a. Une H-1a préparée en E2b puis caractérisée en E3a
ne peut donc pas informer le premier cas de conception d'E1.

### H-1a remonte à l'entrée d'E1

Elle devient une caractérisation préalable, ou un premier sous-lot `E1a`, sans
changer l'ordre macroscopique du plan. Le matériel minimal est :

- le même port et le même adapter dans deux variantes ne différant que par
  `@jakarta.annotation.Generated` ;
- le registre de passage montrant l'inclusion puis l'exclusion de l'adapter ;
- les signaux R5 et R5b, le verdict obtenu et l'autorisation rendue au
  consommateur ;
- une commande relançable sur la révision de référence.

Aucun backend REST complet n'est nécessaire. Cette fixture doit établir ce que
le marqueur change avant qu'E1 décide le traitement d'un verdict fondé sur
l'absence seule.

Cette remontée constitue elle aussi un amendement du plan. Elle doit apparaître
comme un troisième diff normatif, distinct des deux corrections de Q1, et être
approuvée explicitement avant que D40 épingle la révision consolidée.

### H-1b reste en E8

H-1b est une propriété bout en bout : la sortie effectivement générée est-elle
écartée, puis régénérée au run suivant ? Elle peut rester `OUVERTE` avec une
instruction bloquée jusqu'à l'existence du générateur.

Son report est légitime à une condition : E1 doit déjà interdire qu'une absence
seule autorise la génération. H-1b vérifie alors l'efficacité du garde-fou sur
le vrai backend ; elle ne fournit plus une prémisse manquante à sa conception.
Si son résultat devait encore pouvoir changer la politique décidée en E1, elle
devrait elle aussi être simulée avant E1 avec un générateur minimal.

Les deux entrées deviennent donc :

| Hypothèse | Étape | Rôle |
|---|---|---|
| H-1a | **entrée d'E1 / E1a** | caractérisation nécessaire à la conception de la politique R5b |
| H-1b | **E8** | validation bout en bout du générateur et du garde-fou adopté en E1 |

## Q3 — `[ARBITRÉ]` doit attendre son premier usage réel

La question révèle une faiblesse de ma proposition du tour 16. J'avais cherché
à rendre la grammaire complète par anticipation ; cela crée précisément une
catégorie sans instance ni lecteur actuel.

L'interdit du document 07 vise d'abord les abstractions de code. Il ne s'applique
donc pas littéralement à une balise Markdown. Mais son principe est pertinent :
une catégorie vide fige un modèle avant que le besoin réel ait précisé sa forme.
Le processus d'E4a pourra produire autre chose qu'une simple balise : auteur de
l'arbitrage, date, projet, type, kind attendu, relations attendues, portée et
dissensus éventuel.

Je retire donc la demande d'ajouter `[ARBITRÉ]` à D39 maintenant.

D39 doit plutôt poser la règle transitoire suivante :

> Une attente normative ou métier n'est ni `[MESURÉ]` ni `[LU]`. Avant la mise
> en place de l'oracle en E4a, elle cite une décision `CONFIRMÉE` ou reste
> explicitement une question à arbitrer ; elle ne peut pas être présentée comme
> un fait acquis. E4a définit la provenance et la représentation des attentes
> arbitrées avec leurs premiers consommateurs réels.

Conséquences immédiates :

- D33 et C-1 conservent « verdict à l'origine de D33 », sans « faux sur le
  domaine » présenté comme mesure ;
- les quatre arbitrages de D40 n'ont pas besoin de `[ARBITRÉ]` : ce sont des
  décisions `CONFIRMÉE`, dont le statut et l'auteur sont déjà explicites ;
- E4a décidera si la balise finale s'appelle `[ARBITRÉ]`, si elle prend une forme
  plus structurée dans le registre d'oracle, et comment E4b et les portes de
  conformité la consomment.

La grammaire n'est pas incomplète : elle interdit explicitement une catégorie
d'affirmation qu'elle ne sait pas encore représenter, au lieu de lui inventer
une représentation sans usage.

## Q4 — forme, fermeture et périmètre des errata actifs

La rubrique dédiée est justifiée, non comme quatrième nature de vérité, mais
comme **index opérationnel** consommé par la règle 13. Elle ne duplique ni les
constats ni les décisions : elle relie un défaut documentaire à sa preuve, à son
étape correctrice et à sa vérification.

### Forme d'une entrée

| Champ | Contenu |
|---|---|
| Identifiant | `ERR-001`, stable |
| Cible | document, commit ou version, section et énoncé précis |
| Défaut | contradiction, omission ou ambiguïté ; formulation bornée |
| Fondement | liens vers constats, lectures, mesures ou décisions balisés |
| Impact | ce que le défaut permet de conclure ou d'exécuter à tort |
| Correction attendue | critère observable, sans préjuger du patch |
| Affectation | étape responsable |
| Statut | `OUVERT`, `CORRIGÉ À RELIRE`, `FERMÉ`, `DIFFÉRÉ (Dxx)` ou `RÉFUTÉ` |
| Historique | ouverture, correction, relecture, commits et acteurs/sessions |

`RÉFUTÉ` conserve la trace d'un erratum finalement mal fondé. `DIFFÉRÉ` exige
une décision citée et ne vaut pas fermeture.

### Qui corrige et qui ferme ?

L'étape affectée produit la correction et passe l'erratum à
`CORRIGÉ À RELIRE`. Elle ne le ferme pas.

Une session distincte vérifie :

1. que le texte fautif a été corrigé ou explicitement remplacé ;
2. que le nouveau texte correspond à la décision, au code ou à la mesure citée ;
3. qu'aucun autre passage dépendant ne conserve l'ancienne affirmation.

Elle seule passe l'entrée à `FERMÉ`, en inscrivant la révision relue. Si la
correction change une sémantique ou une politique plutôt que d'aligner le texte
sur un arbitrage existant, l'utilisateur tranche avant la fermeture. Une simple
correction factuelle ou éditoriale n'exige pas que l'utilisateur rejoue le rôle
de relecteur technique.

La règle 13 doit alors préciser qu'une étape ne clôt pas si un erratum de son
périmètre reste `OUVERT` ou `CORRIGÉ À RELIRE`, sauf différé explicitement
autorisé par décision.

### Errata initiaux

Deux entrées distinctes doivent être ouvertes sur le §4.1, car elles n'ont pas
le même critère de fermeture :

| ID | Défaut | Affectation | Fermeture attendue |
|---|---|---|---|
| `ERR-001` | le document annonce une saturation monotone que le solveur n'offre pas | E2 | sémantique réelle nommée et §4.1 corrigé |
| `ERR-002` | les « preuves gratuites » laissent entendre une dérivation complète alors que la restitution ne recolle pas les verdicts cités en prémisses | E2 | promesse documentaire bornée et explication raccordée selon la sortie d'E2 |

L'absence de jqwik relève d'un autre registre. Si le §7 décrit une stratégie
cible — « jqwik sera utilisé pour… » — le document n'est pas faux : la
réalisation est incomplète. C'est un constat `[MESURÉ]` affecté à E3a. Il ne
devient un erratum que si le §7 prétend que jqwik est déjà installé ou que les
propriétés sont déjà exercées.

Les deux formulations fautives du plan ne deviennent pas des errata actifs si
Q1 est traité par une révision approuvée avant D40. Le commit antérieur suffit à
conserver leur trace.

## Ordre de révision qui en découle

1. produire les deux corrections de gouvernance de Q1 et l'amendement séparé
   plaçant H-1a à l'entrée d'E1, puis soumettre ces trois diffs à l'utilisateur ;
2. intégrer la règle transitoire sur les attentes métier dans D39, sans
   `[ARBITRÉ]` ;
3. scinder H-1 et placer H-1a à l'entrée d'E1 ;
4. créer la rubrique d'errata et ouvrir `ERR-001` et `ERR-002` ;
5. appliquer les cinq corrections déjà acceptées ;
6. épingler dans D40 la révision du plan approuvée ;
7. seulement alors effectuer le portage.

Ce tour modifie donc deux de mes propres recommandations antérieures :
`[ARBITRÉ]` attend E4a, et H-1a ne reste pas dans E3a. C'est précisément la
fonction de cette relecture préalable : corriger le contrat avant qu'il ne
devienne la source de vérité.
