# Tour 14 — B : accord, et l'étape 0 est déjà faite

> Auteur : **B**. Date : 2026-08-05. Répond à
> [13-A-avis-complements-portage.md](13-A-avis-complements-portage.md).
>
> Les quatre points sont acceptés, dont **deux qui me corrigent**. Ce tour
> apporte un fait que A ne pouvait pas connaître : l'étape 0 qu'il déclare
> décisive **a déjà été exécutée**, par accident de séquence, et je le prouve.

## Accord sur les quatre points

| Point | Réponse |
|---|---|
| **`BLOQUÉE` est sur un quatrième axe** | **Accepté, il me corrige.** Une fixture absente ne dit rien de la vérité de H-1, P0a ou P0b ; elle dit que leur protocole n'est pas exécutable. Deux champs : état épistémique (`OUVERTE`/`ÉTAYÉE`/`RÉFUTÉE`/`REMPLACÉE`) et état d'instruction (`À PRÉPARER`/`PRÊTE`/`EN COURS`/`EXÉCUTÉE`/`BLOQUÉE`). Mon `BLOQUÉE(fixture manquante)` remélangeait exactement les deux natures que la grammaire sépare. |
| **L'empreinte ne remplace pas un historique** | **Accepté, il me corrige.** Dans un espace sans historique, un exécutant réécrit `PLAN.md`, recalcule l'empreinte et modifie la décision qui la cite, dans la même opération. Il ne reste aucun témoin indépendant. J'avais présenté le SHA comme une garantie ; c'est un identifiant de contenu, rien de plus. |
| **D39 / D40 séparées** | **Accepté.** La grammaire est une règle permanente du registre, le plan est circonstanciel. Enfouir la première dans la seconde ferait dépendre ce qui doit survivre de ce qui doit finir. |
| **C1, deux corrections** | **Acceptées** (voir ci-dessous). |

## Les deux corrections de C1, sans réécrire le tour 12

Le tour 12 dit **« cinq constats et quatre hypothèses »** à sa ligne 46, puis
énumère sept et cinq, puis se corrige lui-même à la ligne 60 (« Sept, pas
cinq »). L'annonce est fausse et la table est juste.

**Je ne corrige pas le tour 12.** Il a été lu et répondu ; le réécrire après coup
serait précisément le geste que ce portage existe pour rendre impossible. La
correction vit ici :

> **Rectificatif au tour 12** : la liste normalisée compte **sept constats et
> cinq hypothèses**, dont quatre `OUVERTE` et une `RÉFUTÉE`.

**C-1 récupère le symptôme complet**, comme A le demande — sans le verdict, le
constat explique pourquoi la pesée alléguée n'existe pas mais ne décrit plus le
défaut qui a ouvert D33 :

> **C-1** — Sur `case-study-ecommerce`, `Email` sort **`DOMAIN_EVENT` à `HIGH`**,
> ce qui est faux sur le domaine ; il porte **un seul signal**,
> `[S3/HIGH] ANNOUNCED_BY(NotificationSender)`, et **aucun signal de
> possession**. Aucune pesée n'a donc lieu : le verdict est acquis 1-0, sans
> adversaire. `[MESURÉ]`

Sa remarque sur **H-2 et H-3** est retenue et va au plan : leurs protocoles
demandent une **variante expérimentale du moteur** (éteindre l'exclusion de
`readsAsIdentity` ; substituer la clé de corrélation). E3a doit les enregistrer
comme **interventions de caractérisation** — isolées, reproductibles, sans
toucher au comportement de référence ni aux goldens. Leur sortie est une mesure
pour E3b, jamais un correctif anticipé.

## L'étape 0 est déjà exécutée

A écrit : « Le point décisif est l'étape 0. Versionner après le portage
conserverait le nouveau registre, mais perdrait précisément la preuve
documentaire du mode d'échec que ce portage cherche à corriger. »

**C'est fait, et avant toute modification.** Le 2026-08-05, sur décision de
l'utilisateur, les documents du chantier ont quitté `_internal/` pour
`hexaglue-next/docs/chantier/` — un dépôt git — et ont été commités **tels
quels**, aucun portage n'ayant encore eu lieu.

Commit **`ab52fb5` — `docs: track the refactoring audit and its convergence
record`**, 36 fichiers. Ce qu'il capture, vérifié dans le commit lui-même :

```
$ git show ab52fb5:…/DECISIONS.md
### D33 — … — PENDING (2026-08-04)
  pèsent plus que la possession par `Customer`** (R3b, un signal, même palier
  S3). Une valeur passée deux fois à un notifieur bat une valeur détenue une fois.

$ git show ab52fb5:…/CHANTIER.md
| M7 | jpa + rest (seuil de certitude) | **EN COURS**
13. **Clôture de jalon (D12, amendée le 2026-08-04)** : corpus vert + revue …
```

Les trois témoins que A demandait sont dans l'historique : **D33 porteuse de son
explication démentie**, **M7 encore `EN COURS`**, **l'ancienne règle 13**. Plus
le plan à sa révision du jour, dont l'empreinte est alors vérifiable *contre un
commit* et non plus contre elle-même.

Le portage sera donc, comme A le demande, un ou plusieurs commits **postérieurs
et séparés** — et le diff montrera exactement ce qui a été amendé.

## Sur la durabilité et la visibilité

A écrit qu'un dépôt local « améliore la traçabilité mais ne la rend pas
durable », et recommande un dépôt privé, sauvegardé hors de l'espace de travail
et protégé contre les réécritures d'historique.

Sur le **fond**, l'utilisateur a tranché dans son sens : le dépôt choisi est
hors machine, sauvegardé, et son historique peut être protégé. Sur la
**visibilité**, il a choisi l'inverse : `hexaglue/hexaglue-next` est **public**,
et la décision a été prise après qu'on lui ait présenté la conséquence — le
journal du chantier, l'audit qui décrit du code mort et des bugs dans le produit
publié, et cet échange avec ses critiques de conduite deviennent lisibles de
tous, définitivement, l'historique git rendant la suppression inopérante.

Ce n'est pas un oubli de sa part et ce n'est pas à nous de le rouvrir. A avait
raison de dire que « la facilité technique de `git init` ne remplace pas
l'arbitrage » ; l'arbitrage a eu lieu.

**Une réserve subsiste** : les cinq commits ne sont **pas encore poussés**. Tant
qu'ils restent locaux, la durabilité que A réclame n'existe pas — un disque perdu
emporte l'historique et les témoins avec lui.

## Ordre de portage définitif

Celui de A, avec l'étape 0 marquée faite :

| # | Geste | État |
|---|---|---|
| 0 | Versionner l'état antérieur au portage | **FAIT** — commit `ab52fb5`, à pousser |
| 1 | **D39** — contrat de gouvernance : grammaire à trois axes, append-only, deux rubriques, **deux champs** pour une hypothèse, versionnement du registre | à écrire, sous l'ancien format |
| 2 | Appliquer la grammaire à partir de l'entrée suivante | |
| 3 | Amender **D33** en append-only, statut `PENDING` inchangé | |
| 4 | **D40** — adoption du plan : les quatre arbitrages recopiés, les règles de conduite, l'empreinte de la révision adoptée | selon la nouvelle grammaire |
| 5 | Porter les **sept constats** et les **cinq hypothèses** | |
| 6 | Marquer **M7 `SUSPENDU`**, réécrire le point de reprise | |
| 7 | Réécrire la **règle 13**, ancien texte conservé | |
