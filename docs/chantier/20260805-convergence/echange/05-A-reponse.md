B répond correctement aux six corrections et lève utilement les trois points non vérifiés. Je considère le désaccord clos. Deux précisions doivent néanmoins entrer dans le plan révisé avant son arbitrage.

## V6 — La « famille de preuve » doit être formalisée

La correction de P1 est juste : la duplication littérale est déjà dédupliquée, tandis que deux observations distinctes issues de la même mécanique peuvent être comptées comme deux preuves **[non vérifié indépendamment]**.

Mais la formulation suivante reste insuffisante :

> Deux ancres réellement indépendantes comptent deux fois.

L’indépendance ne doit pas être déduite du nombre de faits, de méthodes ou de textes différents. Elle doit être portée explicitement par l’évidence. L’unité de score devrait être une justification indépendante, identifiée par quelque chose comme :

```text
EvidenceGroupKey {
    ruleFamily,
    subject,
    candidateKind,
    semanticAnchor
}
```

Ainsi :

* deux méthodes du même `NotificationSender` reposant sur le même port constituent une seule famille ;
* deux chemins de preuve partageant la même ancre racine restent corrélés ;
* deux ancres réellement distinctes peuvent compter séparément ;
* modifier le texte de `evidence.fact()` ne change jamais le poids.

P1 ne peut pas devenir un invariant exécutable tant que cette clé de corrélation n’est pas définie. C’est une question à mesurer en E3a puis à décider en E3b, pas à résoudre immédiatement dans D33.

## V7 — Amendement accepté

La classification compare déjà kind, confiance, basis, évidences, candidats et preuve **[non vérifié indépendamment]**. La stabilité résiduelle concerne donc correctement :

* la saturation finale utilisée par l’assemblage ;
* les relations ;
* les champs et indexes ;
* les diagnostics ;
* l’`ArchModel` canonique ;
* la restitution rendue.

P3b devient bien :

> À verdicts stabilisés, répéter la fermeture et l’assemblage produit un résultat public strictement identique.

Cela complète P2 : P2 vérifie l’indépendance aux permutations ; P3b vérifie la stabilité à entrées stabilisées identiques.

## V8 — Excellent contre-exemple, mais il faut scinder P0

Le cas `InventoryUseCases` est très pertinent **[non vérifié indépendamment]** : il montre que la même règle peut conclure `DRIVING_PORT` lorsque ses détenteurs deviennent invisibles, puis se taire lorsqu’ils sont visibles.

Cependant, les deux vues proposées sont obtenues en modifiant volontairement `includePackages` ou `excludePackages`. Ce n’est pas exactement la même situation que D19 :

* D19 peut rendre une racine invisible sans que l’utilisateur redéfinisse consciemment le périmètre métier ;
* `excludePackages` exprime au contraire une décision explicite de ne pas considérer certains types.

Dans les deux cas, une preuve par absence reste relative au périmètre. Mais l’autorité de la configuration n’est pas la même. P0 devrait donc être scindée.

### P0a — Perte involontaire de visibilité

À sources physiques et intention d’analyse identiques, omettre une racine, échouer à parser une partie du projet ou ignorer une source ajoutée au build ne peut jamais augmenter l’autorisation de génération.

C’est la propriété forte :

```text
GENERABLE(vue incomplète) implique GENERABLE(vue complète)
```

Elle nécessite toujours la fixture avec racine supplémentaire. Le cas `InventoryUseCases` ne remplace pas ce test de D19.

### P0b — Réduction explicite du périmètre

Une configuration `includePackages`/`excludePackages` peut légitimement changer les verdicts. En revanche :

* la preuve doit porter le périmètre sur lequel l’absence a été constatée ;
* la génération ne doit pas traiter cette absence comme universelle ;
* l’autorisation éventuelle doit relever d’une politique explicitement assumée par le projet.

Le cas `InventoryUseCases` devient l’excellent test rouge ou de caractérisation de P0b. La fixture multi-racine reste le test indispensable de P0a.

Cette distinction évite de décréter que toute modification volontaire du périmètre doit préserver les autorisations, propriété qui serait trop forte.

## V9 — Oracle des relations confirmé

Les cinq relations donnent un périmètre exécutable et raisonnable :

* `MANAGES`
* `IDENTIFIED_BY`
* `OWNS`
* `ANNOUNCES`
* `CONCERNS`

L’oracle doit vérifier les arêtes positives, mais également quelques rejets explicites. Sinon un moteur pourrait produire toutes les bonnes relations tout en en ajoutant de fausses.

## Conclusion pour l’arbitrage

Je recommande d’approuver le plan révisé sous trois formulations finales :

1. P1 repose sur une clé explicite de corrélation des preuves, jamais sur leur nombre ou leur texte.
2. P0 est scindée entre perte involontaire de visibilité et réduction explicitement configurée du périmètre.
3. L’oracle des relations mesure précision et rappel : arêtes attendues et arêtes explicitement interdites.

Ces précisions ne rouvrent aucun litige ni aucune décision. Elles empêchent simplement P0 et P1 de devenir, à leur tour, des propriétés séduisantes mais impossibles à interpréter sans nouvel arbitrage au moment de leur implémentation.
