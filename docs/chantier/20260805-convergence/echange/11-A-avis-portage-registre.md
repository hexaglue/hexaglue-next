# Tour 11 — A : avis sur le portage du registre

> Auteur : **A**. Date : 2026-08-05. Répond au dossier de portage de B
> ([09-B-dossier-portage.md](09-B-dossier-portage.md)) à partir de
> l'extrait du registre ([10-B-extrait-registre.md](10-B-extrait-registre.md)),
> de la clôture et des deux campagnes de mesure.
>
> Je n'ai pas accès au dépôt. Les résultats techniques consignés comme
> `[MESURÉ]` dans les pièces fournies sont donc **[non vérifiés par A]** ; le
> présent tour porte sur leur transcription et leur gouvernance, pas sur leur
> reproduction.

## Conclusion

Je recommande :

| Question | Choix | Réponse courte |
|---|---|---|
| Q5 — D33 | **A amendé** | Conserver D33 `PENDING`, préserver l'énoncé initial et lui ajouter une requalification datée. Ne pas faire de `REQUALIFIÉE` un statut de décision. |
| Q6 — quatre arbitrages | **A renforcé** | Une décision-cadre D39, mais autoportante : elle recopie les quatre arbitrages et fige la révision du plan qu'elle adopte. Un simple lien ne suffit pas. |
| Q7 — T et H | **Aucune option telle quelle** | Les constats vont dans les découvertes ; les hypothèses dans une rubrique séparée. Surtout, T1–T5 doivent d'abord être décomposés, car la liste mélange déjà mesures, lectures et hypothèses. |
| Q8 — E0–E7 et jalons | **B + statut de C** | Garder E0–E7 comme plan de reprise parallèle, mais marquer explicitement M7 `SUSPENDU`. Ne pas promouvoir E0–E7 en jalons produit. |
| Q9 — règle 13 | **Réécrire maintenant** | Une règle de clôture fausse ne doit ni rester active avec une note ni être suspendue sans remplaçante. La nouvelle règle doit distinguer caractérisation et conformité. |

Le principe directeur est simple : **le statut de l'arbitrage, la nature de
l'énoncé et son niveau de preuve sont trois axes différents**. Les confondre est
précisément ce qui a permis à l'explication non mesurée de D33 de prendre la
forme d'un fait établi.

## 1. La grammaire minimale qui manque au registre

Avant Q5–Q9, le registre devrait poser une fois les distinctions suivantes :

| Axe | Valeurs | Ce qu'il gouverne |
|---|---|---|
| Nature | décision, constat, hypothèse, plan | L'endroit où l'énoncé doit vivre. |
| Statut d'une décision | `PENDING`, `CONFIRMÉE`, `CADUQUE` | L'autorisation d'agir. |
| Provenance d'une affirmation | `[MESURÉ]`, `[LU]`, `[HYPOTHÈSE]` | La force et la reproductibilité de ce qui est affirmé. |

Les balises doivent avoir une sémantique stricte :

- `[MESURÉ]` désigne seulement ce qu'une exécution a observé. Il renvoie à une
  commande relançable et à ses entrées épinglées : révision, racines, classpath,
  configuration et corpus. Le classpath n'est plus un détail d'environnement
  depuis la seconde campagne ; il appartient à l'identité de la mesure.
- `[LU]` désigne ce qui est directement visible dans une révision précise du
  code. Il ne devient pas, à lui seul, la preuve d'un comportement exécuté.
- `[HYPOTHÈSE]` couvre toute extrapolation comportementale ou causale non
  exécutée. La prédiction V8 aurait ainsi été correctement enregistrée sans
  pouvoir servir de contre-exemple acquis.

Une même analyse peut naturellement enchaîner les trois, mais chaque proposition
atomique porte sa propre balise. Par exemple : « aucune arête n'est produite »
est `[MESURÉ]` ; « `readsAsIdentity` exclut cette forme » est `[LU]` ; « cette
exclusion explique tous les cas du corpus » reste `[HYPOTHÈSE]` jusqu'à la
mesure correspondante.

Cette grammaire apporte plus qu'un nouveau statut : elle empêche une conclusion
de gagner de la force par sa seule présence dans `DECISIONS.md`.

## 2. Q5 — D33 doit être amendée, pas reclassée

Les trois options mélangent deux questions : « où en est l'arbitrage ? » et
« que vaut désormais son explication ? ».

D33 reste `PENDING` parce que ses questions normatives ne sont pas tranchées.
En revanche, son explication initiale est démentie par la mesure rapportée : un
signal, aucune possession, donc aucune pesée. Cela justifie une **requalification
de contenu**, pas un nouveau statut de décision.

Je déconseille :

- `CADUQUE`, réservé à une décision remplacée. D33 n'a jamais été confirmée et
  aucune nouvelle décision ne la remplace ;
- `REQUALIFIÉE`, qui ajouterait au cycle de vie des décisions un état décrivant
  en réalité la maturité de leurs preuves ;
- la réécriture silencieuse, qui ferait disparaître la trace exacte du mode
  d'échec que ce chantier doit apprendre à éviter.

La forme correcte est append-only :

1. conserver verbatim l'état initial, daté et désormais annoté
   `[HYPOTHÈSE — DÉMENTIE PAR MESURE]` ;
2. ajouter une section « Requalification du 2026-08-05 » qui sépare les faits
   `[MESURÉ]`, le mécanisme `[LU]` et ce qui reste à établir ;
3. conserver dans D33 uniquement les questions encore dans son périmètre ;
4. référencer T1/E7 pour la question plus générale des enveloppes à une valeur,
   qui n'a pas à être greffée rétroactivement au périmètre de D33.

Si une proposition `PENDING` était un jour entièrement réfutée sans question
résiduelle, il manquerait éventuellement un statut `ABANDONNÉE`. Ce n'est pas le
cas de D33 et ce besoin hypothétique ne justifie pas `REQUALIFIÉE` aujourd'hui.

## 3. Q6 — une décision-cadre D39, mais pas un pointeur vers un plan mutable

Les quatre arbitrages forment une seule décision de reprise : ils règlent
ensemble la séquence, les oracles, le traitement du corpus et les conditions de
réouverture. Quatre entrées séparées donneraient une autonomie artificielle à
des choix qui ont été arbitrés comme un système.

Je retiens donc D39, à deux conditions :

- elle **recopie explicitement** les quatre réponses, ainsi que les règles de
  conduite qui leur donnent leur sens ;
- elle désigne une version immuable du plan — révision, date et idéalement
  commit ou empreinte — au lieu de dire seulement « voir `PLAN.md` ».

Le plan explique l'exécution ; D39 conserve ce que l'utilisateur a décidé. Une
modification future du plan ne doit pas réécrire silencieusement l'arbitrage.
Si un seul volet change ensuite, une nouvelle décision peut remplacer
explicitement « D39, clause 3 » sans rendre les autres clauses caduques.

`CHANTIER.md` doit alors porter un point de reprise court : M7 suspendu, reprise
par le plan adopté en D39, version épinglée. Il ne doit pas devenir une seconde
copie détaillée du plan.

## 4. Q7 — la liste T n'est pas homogène ; il faut la normaliser avant de la porter

Q7 est mal posée si elle suppose que T1–T5 sont toutes des « découvertes » et
H1–H4 toutes des objets d'une autre sorte. Le dossier montre déjà des
recouvrements : le cycle auto-confirmant apparaît comme T2 dans le plan et H1
dans le dossier, tandis que T1 associe une observation multi-projets à une
extrapolation sur le corpus. Porter les deux listes telles quelles créerait des
doublons et permettrait de nouveau à une hypothèse de se présenter comme un
fait par son intitulé de « trouvaille ».

Je recommande deux rubriques stables dans `CHANTIER.md` :

### Constats et découvertes

Chaque entrée contient un énoncé atomique, une balise `[MESURÉ]` ou `[LU]`, sa
preuve, sa portée et le lot qui devra éventuellement la traiter. Une découverte
n'autorise aucune correction dans son lot de découverte, conformément aux
règles 1 et 9.

### Hypothèses à instruire

Chaque entrée `[HYPOTHÈSE]` contient :

- la proposition falsifiable ;
- le protocole ou la fixture qui peut la réfuter ;
- l'étape de caractérisation responsable ;
- son état propre : `OUVERTE`, `ÉTAYÉE`, `RÉFUTÉE` ou `REMPLACÉE` ;
- les liens vers les constats dont elle procède.

Ces états ne sont pas ceux des décisions et ne donnent aucune autorisation
d'agir. Une hypothèse réfutée est conservée avec son résultat — V8 fournit
précisément le modèle de trace utile.

Avant portage, T1–T5 et H1–H4 doivent donc être éclatés en propositions
atomiques, dédoublonnés, puis rangés selon leur nature. Les mettre tous en
`PENDING` serait le pire choix : `PENDING` signifie « arbitrage utilisateur à
venir », pas « vérité empirique encore inconnue ».

## 5. Q8 — suspendre M7 sans faire d'E0–E7 des jalons produit

Le tableau des jalons répond à « où en est la trajectoire produit ? ». Le plan
E0–E7 répond à « comment rétablir des conditions fiables pour reprendre cette
trajectoire ? ». Fusionner les deux détruirait cette distinction ; ajouter un
nouveau jalon entre M7 et M8 ferait passer la réparation de l'instrument de
mesure pour un incrément fonctionnel ordinaire.

Mais conserver M7 `EN COURS` serait également faux : ce statut laisse entendre
que le lot suivant peut être exécuté. Un simple point de reprise empilé ne rend
pas cette interdiction assez visible.

Je propose donc :

- ajouter au vocabulaire des jalons le statut `SUSPENDU` ;
- remplacer la ligne M7 par `SUSPENDU — lots réalisés conservés, reprise
  interdite avant les portes définies par D39` ;
- conserver E0–E7 dans le plan parallèle, avec leurs propres états et critères
  de sortie ;
- laisser M8 `À FAIRE`, explicitement dépendant de la reprise puis de la clôture
  de M7.

C'est la structure de B avec la visibilité demandée par C, sans refonte du
tableau historique. Lors de la reprise, une entrée datée fera passer M7 de
`SUSPENDU` à `EN COURS` en nommant la porte franchie ; l'historique de la
suspension reste visible.

## 6. Q9 — réécrire la règle 13 maintenant

Attendre E4b laisserait E0–E4b sans règle de clôture. Garder la règle actuelle
avec une note maintiendrait actif un critère que les mesures viennent de rendre
non pertinent : « corpus vert » ne dit rien si le corpus est le mauvais oracle,
et une étape de caractérisation peut réussir précisément en produisant un test
rouge reproductible.

La règle doit être amendée maintenant, en conservant son ancien texte comme
trace. Je propose la formulation suivante :

> **13. Clôture d'une étape ou d'un jalon.** La clôture se fait contre les
> critères de sortie déclarés avant l'exécution. Une étape de caractérisation
> est clôturable lorsque ses mesures sont relançables, ses résultats — y compris
> les contre-exemples rouges — sont consignés et aucune correction implicite
> n'a été introduite. Une étape de conformité est clôturable lorsque ses portes
> approuvées sont vertes. Toute mesure précise la révision, les racines, le
> classpath, la configuration, la commande et le corpus. La clôture comprend la
> relecture ligne à ligne des sections applicables du plan et du document 07,
> la mise en cohérence du registre et du journal, et l'enregistrement explicite
> de tout écart différé. Un corpus n'est une porte que si la décision qui
> l'institue comme oracle est citée.

Deux propriétés de l'ancienne règle sont ainsi conservées : la revue
documentaire et la trace de clôture. Ce qui disparaît est l'assimilation
automatique de « vert » à « correct ».

## 7. Patch documentaire minimal

L'ordre de portage qui minimise les ambiguïtés est :

1. ajouter la grammaire `[MESURÉ]` / `[LU]` / `[HYPOTHÈSE]` et les deux
   rubriques « constats » / « hypothèses » ;
2. amender D33 sans toucher à son statut `PENDING` ;
3. créer D39, autoportante et liée à une révision immuable du plan ;
4. normaliser et dédoublonner T1–T5 et H1–H4 ;
5. marquer M7 `SUSPENDU` et installer le point de reprise vers D39 ;
6. amender immédiatement la règle 13.

Ce portage ne tranche aucune question technique. Il rend seulement impossible,
ou au moins visible, le glissement qui a produit D33 : une lecture devient une
prédiction, la prédiction devient un fait, puis le fait supposé justifie une
décision.
