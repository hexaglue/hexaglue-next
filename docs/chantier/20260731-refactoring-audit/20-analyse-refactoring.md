Mon diagnostic principal : le refactoring n’a pas échoué à cause de Spoon. Il a produit une architecture plus propre, mais il a remplacé les heuristiques nominales par un moteur structurel trop conservateur, validé sur un corpus insuffisamment représentatif. Plusieurs décisions ont ensuite contourné ses silences au cas par cas, parfois au prix d’incohérences conceptuelles.

## Ce qui est solide et doit être conservé

L’architecture générale de [07-architecture-page-blanche.md](sandbox:/workspace/scratch/c4b724d58b39/upload/07-architecture-page-blanche.md) reste pertinente :

* séparation `CodeModel` / `ArchModel` ;
* Spoon confiné au frontend ;
* faits et relations typés ;
* connaissance des frameworks centralisée ;
* plugins interdits de reclassification ;
* candidats et preuves conservés ;
* code généré distingué du code analysé ;
* diagnostics codés plutôt que silences invisibles.

Le problème se situe principalement dans les règles d’inférence, la qualification de leur confiance et le corpus utilisé pour les valider.

## Les causes structurelles

### 1. Le corpus a davantage validé l’implémentation que la capacité réelle

Le corpus comporte 154 scénarios, mais :

* le profil 1 provient largement de tests unitaires mono-type ;
* le profil entreprise ne comporte que 6 scénarios ;
* le profil sans conventions n’en comporte que 5 ;
* les exemples réels de la carrière n’étaient pas branchés lors de D13.

Les premiers projets réels ont immédiatement trouvé des défauts absents du corpus :

* 5 identifiants sur 6 perdus à cause de plusieurs méthodes de recherche ;
* aucun port pilotant reconnu sur un projet dépourvu de couche web ;
* enum mal généréré ;
* constructeur rejeté lorsque l’ordre des paramètres diffère de celui des champs ;
* `Email` classé `DOMAIN_EVENT`.

Le cas bancaire contredit aussi la portée donnée à D13 : le vocabulaire déclaré y récupère 5 identifiants, 1 agrégat, 1 port piloté et fait sortir 7 types d’`UNCLASSIFIED`. Les « 0 gain » des profils 2 et 3 n’étaient donc pas généralisables. Ces mesures figurent dans [CHANTIER.md](sandbox:/workspace/scratch/c4b724d58b39/upload/CHANTIER.md).

### 2. Le moteur annoncé comme monotone ne l’est pas réellement

La page blanche promet une saturation monotone : les faits ne feraient que s’ajouter, donc la terminaison et les preuves seraient garanties.

L’implémentation décrite dans le chantier fonctionne autrement :

* la base de faits est recréée à chaque tour ;
* un verdict peut remplacer le précédent ;
* les conclusions dérivées peuvent disparaître ;
* des règles peuvent se défaire mutuellement jusqu’au plafond de tours ;
* les preuves inter-tours sont perdues.

Cela explique pourquoi l’arbre `explain` s’arrête souvent avant les véritables prémisses. Or les règles R4, R5 et D38 reposent justement sur des absences : « personne n’implémente », « personne ne détient », « aucun adapter n’appelle ». Ce sont des propriétés non monotones et très sensibles au périmètre analysé.

### 3. Les occurrences sont comptées comme des preuves indépendantes

D33 en donne la démonstration : deux transports d’`Email` par un port de notification battent une possession par `Customer`, simplement parce qu’il existe deux occurrences contre une.

Le moteur additionne donc parfois du volume syntaxique, pas de la force sémantique. Répéter le même signal ne devrait pas augmenter la certitude. Les preuves doivent être normalisées par famille de règle et par ancre indépendante.

### 4. Certaines confiances sont choisies pour satisfaire le générateur

D38 classe un port pilotant sur l’absence de détenteur, à un palier inférieur à R5, mais lui donne néanmoins une confiance `HIGH` parce que, sinon, le seuil de génération REST le refuserait.

C’est une inversion dangereuse : le besoin du consommateur modifie la valeur épistémique du verdict. La confiance doit décrire ce que les sources permettent d’affirmer, indépendamment de ce que JPA ou REST souhaitent générer.

### 5. D7/D16 produit sciemment un modèle faux

Le cas `Pet` est révélateur :

* `Pet` possède un `@Id Integer` ;
* `Integer` est externe et n’a pas de verdict ;
* R3a ne voit donc pas d’identité ;
* R3b classe `Pet` comme `VALUE_OBJECT`.

D16 conserve ce verdict pourtant reconnu faux et demande au finding de l’expliquer.

Il faut séparer deux dimensions :

* « `@Id` désigne ce champ comme identité persistée » ;
* « ce type est une entité du domaine ».

Lire `@Id` comme un rôle de champ n’oblige pas à laisser JPA décider directement du kind. En revanche, une partie possédée qui porte une identité explicite peut ensuite être inférée `ENTITY` par une règle structurelle. D7 peut être préservée dans son intention sans conserver l’erreur de D16.

### 6. « Exactement un, sinon silence » est devenu un schéma de faux négatifs

Ce motif apparaît dans R2 et D35. Il est sûr, mais trop brutal :

* une méthode de recherche par clé naturelle suffit à faire disparaître l’identité ;
* plusieurs agrégats mentionnés empêchent toute ressource REST ;
* les candidats connus sont réduits à un silence aval.

D32 a corrigé une manifestation, mais pas le défaut de conception. Le modèle devrait conserver une relation ambiguë avec ses candidats, sa cardinalité et sa cause, au lieu de confondre « plusieurs réponses possibles » et « aucune information ».

### 7. Le périmètre des sources est trop globalement restreint

D19 ne lit que la racine principale afin d’éviter que la sortie HexaGlue ne perturbe l’analyse suivante. C’est légitime pour les propres fichiers générés par HexaGlue, mais trop large pour :

* les sources ajoutées par `build-helper` ;
* du code généré par protobuf, OpenAPI ou MapStruct ;
* une racine métier légitime ajoutée au build.

Il faut porter la provenance des sources :

* source utilisateur classifiable ;
* source externe générée utilisable comme contexte ;
* sortie HexaGlue exclue de la dérivation.

## Décisions à rouvrir

| Décision | Révision recommandée                                                                                                                                             |
| -------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| D6       | Remplacer le comptage d’occurrences par des preuves indépendantes et conserver la provenance inter-tours.                                                        |
| D7 + D16 | Lire `@Id` comme rôle structurel du champ, sans faire de JPA une preuve directe de kind. Ne plus conclure `VALUE_OBJECT` par simple absence d’identité reconnue. |
| D13      | Garder le nommage hors des preuves fortes, mais l’utiliser comme hypothèse `LOW/MEDIUM`, visible dans l’audit et interdite à la génération sans corroboration.   |
| D19/D15  | Exclure spécifiquement les sorties HexaGlue ; modéliser les autres racines par provenance.                                                                       |
| D27      | Ne pas considérer qu’un backend couvre toute une famille seulement parce qu’il est installé.                                                                     |
| D34      | Passer de `COMMAND/QUERY` à `COMMAND/QUERY/UNKNOWN`. Un effet non détecté ne prouve pas la pureté ; `UNKNOWN` ne doit jamais produire un GET par défaut.         |
| D38      | Garder la règle comme candidat structurel, mais ne pas lui attribuer `HIGH` pour franchir le seuil du générateur.                                                |
| D30      | Apparier constructeur et champs par les affectations observées dans les corps, pas par leur ordre.                                                               |
| D35      | Conserver le sujet optionnel, mais permettre les ports d’action sans ressource unique, par exemple un transfert.                                                 |

D27 mérite une évolution précise : chaque backend devrait produire un `GenerationPlan` pur, par port, avec `GENERATABLE`, `REFUSED` ou `NOT_APPLICABLE`. L’audit pourrait alors taire un adapter manquant seulement lorsque le backend sait effectivement le générer. Cela évite aussi de prendre la sortie générée comme nouvel intrant.

## Stratégie de remédiation

Je suspendrais temporairement l’ajout de nouvelles règles et procéderais ainsi :

1. Construire un registre exhaustif de classification sur les projets réels.

   Pour chaque type source : lu ou non, raison d’exclusion, faits observés, candidats, verdict, confiance, relations, utilisation par chaque backend et verdict humain attendu.

2. Séparer les résultats en cinq catégories.

   * correctement classé ;
   * faux positif ;
   * faux négatif ;
   * ambiguïté réelle ;
   * type technique ou hors taxonomie, donc légitimement non classé.

   Le chiffre « 4 non classés » du banc n’est pas exploitable tant que ces quatre types ne sont pas nommés et répartis ainsi.

3. Corriger d’abord les invariants du moteur.

   * dédupliquer les preuves ;
   * interdire qu’une boucle de règles se fournisse sa propre justification ;
   * conserver les prémisses entre les tours ;
   * dissocier niveau d’évidence, confiance et autorisation de génération ;
   * ne jamais transformer une absence d’information en preuve `HIGH`.

4. Ajouter des tests métamorphiques.

   * ajouter une seconde recherche ne doit pas effacer l’identité ;
   * dupliquer une notification ne doit pas transformer une valeur en événement ;
   * renommer les types ne doit rien changer en posture structurelle ;
   * réordonner champs et paramètres ne doit pas casser le mapper ;
   * ajouter un adapter ne doit pas reclasser le cœur ;
   * une entité JPA à identité primitive ne doit pas devenir `VALUE_OBJECT`.

5. Remplacer le seul cliquet « goldens verts » par des mesures de précision et rappel, par kind et par profil réel.

Le refactoring n’est donc pas à jeter. Le `CodeModel`, l’`ArchModel` et les frontières sont de bonnes fondations. En revanche, le solveur et plusieurs décisions prises pour débloquer les générateurs doivent être repris avant M8. Le prochain travail utile est d’analyser le code actuel et le rapport détaillé du banc e-commerce afin de produire le registre type par type, puis de transformer les constats ci-dessus en correctifs et tests rouges précis.
