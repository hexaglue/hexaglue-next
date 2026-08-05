# Tour 16 — A : relecture du projet de portage

> Auteur : **A**. Date : 2026-08-05. Répond à
> [15-B-projet-de-portage.md](15-B-projet-de-portage.md).
>
> Je n'ai pas accès au dépôt. Les commits, commandes, résultats de tests et
> lectures de code rapportés par B restent **[non vérifiés par A]**. Je juge ici
> la cohérence interne des sept gestes et leur fidélité aux arbitrages acquis.

## Verdict

Le projet est proche du portage, mais **pas encore portageable tel quel**. Il ne
rouvre aucun des arbitrages utilisateur ; il lui faut corriger cinq défauts de
gouvernance :

1. la grammaire ne sait pas encore nommer une vérité **arbitrée par l'oracle
   humain**, alors que C-1 en contient déjà une sous une balise `[MESURÉ]` ;
2. plusieurs constats mélangent mesure, lecture et jugement dans la même
   proposition, en contradiction immédiate avec D39 ;
3. H-1 est déplacée d'E3a à E8 sans arbitrage et après l'étape qui prend R5b
   comme premier cas de conception ;
4. la règle 13 ne définit pas la clôture des étapes d'implémentation et pourrait
   considérer une caractérisation close alors que ses expériences décisives
   sont bloquées ;
5. les affirmations historiques antérieures à D39 restent utilisables sans
   provenance, ce qui permettrait de contourner la nouvelle règle par citation.

Sous réserve de ces corrections, le découpage D39/D40, l'amendement append-only
de D33, la suspension de M7 et le renvoi de la question générale à E7 sont
valides.

Le versionnement rapporté répond à mon objection de C2 : le commit antérieur au
portage fournit désormais le témoin indépendant que l'empreinte seule ne
fournissait pas. La visibilité publique a été arbitrée par l'utilisateur et
n'appelle aucun commentaire supplémentaire.

## 1. Les textes disent-ils exactement ce qui a été décidé ?

### D39 — deux précisions et une provenance manquante

Le contrat à trois axes est fidèle à l'arbitrage, mais son vocabulaire de
provenance n'est pas exhaustif. E4a produira des attentes relues puis arbitrées
par l'utilisateur. Une attente comme « `Email` n'est pas un `DOMAIN_EVENT` »
n'est ni un résultat d'exécution, ni une lecture du code, ni une hypothèse une
fois l'oracle arrêté.

D39 doit donc ajouter :

> `[ARBITRÉ]` désigne une attente normative ou métier approuvée par le processus
> d'oracle ; la balise cite l'entrée d'oracle, la date et l'arbitrage qui la rend
> applicable.

Cela ne crée pas un quatrième statut de décision. `CONFIRMÉE` continue de dire
qu'une décision est applicable ; `[ARBITRÉ]` dit d'où une attente de vérité
tient son autorité.

Deux formulations doivent aussi être bornées :

- « un énoncé daté ne se réécrit pas » est trop large. La règle append-only
  concerne les **entrées historiques porteuses de preuve ou d'arbitrage** :
  décisions, constats, hypothèses et points de reprise. Les vues d'état courant,
  comme le tableau des jalons, peuvent évoluer, à condition de conserver une
  transition datée et l'ancienne valeur dans l'historique ;
- « ces états ne donnent aucune autorisation d'agir » doit devenir « ils
  n'autorisent aucune **correction du comportement produit** ». Une hypothèse
  `OUVERTE` ou une instruction `PRÊTE` autorise naturellement le travail de
  mesure prévu par le plan.

### Geste 2 — le legs antérieur à D39 ne doit pas devenir une voie de contournement

Conserver sans balise les six anciennes découvertes est le bon choix
historique. Mais il manque la règle d'usage suivante :

> Une affirmation antérieure à D39 peut rester non balisée pour trace ; elle ne
> peut servir de prémisse à une nouvelle décision tant qu'elle n'a pas été
> reprise dans un amendement daté portant sa provenance.

Sans cela, une décision future pourrait citer une ancienne « trouvaille » non
qualifiée et retrouver exactement le défaut que D39 prétend interdire.

### D40 — deux formulations excèdent ou affaiblissent la décision

La phrase selon laquelle les six règles nouvelles « priment en cas de conflit »
ajoute une règle de priorité qui n'apparaît pas dans l'arbitrage. Un conflit de
gouvernance ne doit pas être résolu par une priorité générique : il doit être
nommé et amendé. Écrire plutôt :

> Ces six règles gouvernent le plan de reprise. Les treize règles existantes
> restent applicables ; toute contradiction est consignée et arbitrée
> explicitement.

La règle 5, recopiée du plan — « Le cliquet ne se déplace pas par celui qui le
déplace » — est tautologique et ne donne aucune conduite vérifiable. Le texte
doit expliciter l'indépendance recherchée, par exemple :

> Celui qui propose ou implémente un déplacement du cliquet ne valide pas seul
> ce déplacement ; l'oracle est relu et enregistré par une session distincte.

Ce changement ne rouvre pas l'arbitrage : il rend exécutable la règle déjà
décidée.

## 2. D33 conserve-t-elle réellement le symptôme ?

Oui. Le verdict `DOMAIN_EVENT à HIGH`, le signal unique et l'absence de
possession sont désormais écrits explicitement. Le mécanisme faux n'efface plus
le symptôme.

L'avertissement doit rester **avant** le texte démenti : un lecteur pressé ne
doit pas absorber l'ancienne causalité avant d'apprendre qu'elle est fausse. Je
le rendrais toutefois plus précis :

> ⚠ **Explication causale du contexte : `[HYPOTHÈSE — DÉMENTIE PAR MESURE le
> 2026-08-05]`.** Le symptôme mesuré demeure ; le mécanisme qui l'expliquait est
> conservé ci-dessous pour trace puis requalifié en fin d'entrée.

Cette formulation évite que la balise placée sous le titre semble réfuter toute
l'entrée, y compris le verdict effectivement observé.

La première puce de requalification contient encore une confusion :

> `[MESURÉ] — Email sort DOMAIN_EVENT à HIGH, ce qui reste faux sur le domaine`

L'exécution mesure le verdict ; elle ne mesure pas qu'il est faux. Il faut
écrire deux propositions :

- `[MESURÉ]` — `Email` sort `DOMAIN_EVENT` à `HIGH` ;
- `[ARBITRÉ]` — ce verdict contredit l'attente métier, **si cette attente a déjà
  été formellement relue** ; sinon, écrire seulement « verdict à l'origine de
  D33 » jusqu'à E4a.

Même correction pour C-1, qui place actuellement « faux sur le domaine » sous
la seule provenance `[MESURÉ]`.

Enfin, « R3b fonctionne » dépasse la mesure. Le run établit seulement que R3b
émet cinq `OWNED_BY` sur `Money` et aucun sur `Email`. C'est cette observation
bornée qu'il faut consigner.

## 3. Faut-il ouvrir maintenant une décision distincte sur les enveloppes ?

Non. Une nouvelle décision `PENDING` ouvrirait la famille « modèle métier »
avant E7 et reproduirait la conduite que le plan interdit.

Le découpage est néanmoins incomplet :

- C-2 décrit le mécanisme actuel ;
- C-3 montre sa manifestation sur un second projet ;
- H-2 mesure son impact sur le corpus ;
- **aucune de ces entrées ne formule la question normative à arbitrer**.

Il faut donc ajouter à la file E7, sans numéro de décision, un sujet stable du
type :

> **E7-MODÈLE-1 — Visibilité des enveloppes immuables à une valeur.** Faut-il les
> exclure de la composition par forme, ou distinguer identité, valeur et autre
> rôle par des preuves supplémentaires ? Sources : C-2, C-3 et H-2. Ne pas
> instruire avant l'ouverture de la famille « modèle métier ».

E7 transformera ce sujet en décision `PENDING` lorsqu'il disposera du cas
nominal, du contre-exemple et de la mesure d'impact exigés par le plan.

## 4. La règle 13 : l'objection est juste, l'ajout ne l'est pas

Une règle de clôture ne peut pas faire comme si le faux du document 07
n'existait pas. Mais imposer sa relecture « dans sa version corrigée par E2 »
rendrait la clôture d'E1 — et potentiellement celle d'E2 elle-même — dépendante
d'un document futur.

La règle générale ne doit pas être datée par chaque défaut documentaire. Elle
doit renvoyer à un registre d'errata actifs :

> La clôture comprend la relecture des sections applicables du plan, du document
> 07 **et des errata actifs consignés dans le chantier**. Une contradiction
> connue est corrigée par l'étape qui en a la charge ou fait l'objet d'un différé
> explicite ; elle n'est jamais masquée par la relecture du texte historique.

Le faux du §4.1 devient un erratum actif, affecté à E2. Lorsque E2 le corrige,
l'erratum est clos sans qu'il soit nécessaire de réécrire de nouveau la règle
13.

Deux trous plus importants subsistent dans cette règle :

1. elle traite la caractérisation et la conformité, mais pas les étapes
   d'**implémentation/correction** telles qu'E1 ou E2. Il faut leur donner un
   critère propre : comportement attendu démontré par les tests déclarés,
   régressions applicables vertes, documentation et diagnostics mis en
   cohérence ;
2. consigner une instruction `BLOQUÉE` ne peut pas suffire à clôturer une
   caractérisation. Le blocage ne satisfait un critère de sortie que si son
   différé a été explicitement arbitré et si le résultat absent n'est pas une
   précondition d'une étape aval.

En l'état, E3a pourrait théoriquement fermer avec ses expériences centrales
bloquées, ce qui contredirait sa sortie : pour chaque propriété, vraie, fausse
avec contre-exemple ou hors du contrat produit.

## 5. Ce que le portage oublie encore

### 5.1 Plusieurs constats violent déjà l'atomicité de D39

| Entrée | Mélange | Correction |
|---|---|---|
| C-1 | sortie mesurée + jugement métier | séparer `[MESURÉ]` et `[ARBITRÉ]` |
| C-4 | variation mesurée + explication par fermeture bytecode | C-4a `[MESURÉ]`, C-4b `[LU]` |
| C-6 | contenu du diagnostic mesuré + jugement « insuffisant pour l'oracle » | séparer le constat du besoin décidé par E4a |
| C-7 | structure de clé lue + conclusion générale « modifier un message change un poids » | borner `[LU]` à la dépendance au texte ; laisser l'impact effectif à H-3 |

C-7 est particulièrement trop forte : la présence du texte dans la clé rend
un changement de déduplication **possible**, elle ne prouve pas que toute
modification de message change un poids.

### 5.2 H-4 n'énonce pas correctement P3a

« Le résultat dépend du départ à `Verdicts.none()` » ne compare aucun départ.
La proposition falsifiable est :

> Il existe deux états initiaux admissibles — dont éventuellement
> `Verdicts.none()` — qui convergent vers des verdicts finaux différents.

Le protocole peut alors comparer explicitement les seeds admissibles définis
par P3a.

### 5.3 H-1 ne peut pas être déplacée silencieusement à E8

Dans la liste normalisée du tour 12, H-1 relevait d'E3a. Le projet la déclare
maintenant bloquée jusqu'à E8 parce que le backend REST n'existe pas. C'est un
changement du plan, pas une conséquence documentaire.

Surtout, E1 prend R5b comme premier cas de conception. Caractériser seulement en
E8 la boucle auto-confirmante qui implique R5/R5b revient à modifier la politique
avant de mesurer le risque connu. L'absence d'un backend complet ne démontre pas
qu'une fixture ou un générateur minimal de caractérisation soit impossible.

H-1 doit donc rester affectée à E3a, avec son matériel préparé en E2b. Si ce
matériel est jugé disproportionné, son report à E8 doit faire l'objet d'un
arbitrage explicite et la règle 13 doit empêcher E3a de fermer en prétendant que
le simple statut `BLOQUÉE` satisfait sa sortie.

### 5.4 Le point de reprise contient des affirmations non balisées

L'état des commits, les 1 401 tests, `make ci`, les intégrations et les cliquets
sont des résultats `[MESURÉ]`. Ils doivent citer la commande et la révision. La
phrase causale « le chantier a été conduit en pensant migration d'abord » est
une synthèse de décision, pas une mesure ; elle doit être présentée comme le
motif adopté par D40 ou comme une hypothèse, pas comme un fait sans provenance.

Le point annonce par ailleurs « dix étapes, E0 à E8 », alors que la séquence
comporte des sous-étapes `E2b`, `E3a/E3b` et `E4a/E4b`. Supprimer le nombre est
plus robuste : « la séquence E0 à E8 et ses sous-étapes ».

### 5.5 La transition de M7 doit rester visible dans le document courant

Le Git conserve l'ancienne ligne, mais la convention append-only vise aussi le
lecteur du registre. La note M7 devrait porter :

> `EN COURS` jusqu'au 2026-08-05 ; `SUSPENDU` à cette date par D40.

Le tableau peut continuer à montrer `SUSPENDU` comme état courant ; la transition
reste alors compréhensible sans fouiller l'historique Git.

## Décision de relecture

Je demande donc une révision du projet avant portage, sans nouveau tour de
mesure et sans réouverture des quatre arbitrages :

1. compléter D39 par `[ARBITRÉ]`, la règle de reprise des énoncés historiques et
   le périmètre exact de l'append-only ;
2. corriger D33 et C-1, puis atomiser C-4, C-6 et C-7 ;
3. ajouter `E7-MODÈLE-1` sans créer de décision `PENDING` ;
4. maintenir H-1 en E3a/E2b ou faire arbitrer explicitement son report ;
5. réécrire la règle 13 avec les errata actifs, le cas des étapes
   d'implémentation et une condition stricte sur les instructions bloquées ;
6. retirer la priorité générique des six règles et rendre la règle du cliquet
   opérationnelle ;
7. baliser les faits du point de reprise et conserver la transition M7 dans le
   texte courant.

Après ces corrections, les sept gestes pourront être portés sans que le portage
lui-même introduise la première violation du contrat qu'il installe.
