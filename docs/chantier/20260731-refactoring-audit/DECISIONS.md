# Registre des décisions — Chantier de refactoring HexaGlue

Règles du registre :

- Une décision n'existe que si elle est consignée ici. Toute « décision » issue
  d'un document antérieur au 2026-07-31 (autres dossiers `_internal/`, CLAUDE.md,
  Javadoc, discussions passées) est réputée **caduque** tant qu'elle n'a pas été
  re-confirmée ici.
- Statuts : `PENDING` (ne pas agir), `CONFIRMÉE` (applicable), `CADUQUE`
  (remplacée — garder la trace, ne pas supprimer la ligne).
- Chaque décision : contexte en une phrase, options, décision, date, impact.

## Décisions en attente

### D33 — Une valeur détenue par un agrégat lue DOMAIN_EVENT parce qu'un port de notification la transporte — PENDING (2026-08-04)

- **Contexte** : sur le premier banc réel (`_probes/ecommerce-hexagonal`),
  `Email` sort **DOMAIN_EVENT à HIGH**. `NotificationSender` (deux méthodes
  one-way portant des valeurs) est lu EVENT_PUBLISHER par W2-ROLE, puis R7
  conclut que ce qu'un tel port transporte est un événement ; **deux signaux R7
  pèsent plus que la possession par `Customer`** (R3b, un signal, même palier
  S3). Une valeur passée deux fois à un notifieur bat une valeur détenue une
  fois.
- **Questions à instruire** : la possession (R3b) devrait-elle primer sur la
  publication (R7) à palier égal ? ou R7 devrait-il se taire sur un type que le
  domaine GARDE (un événement se publie, il ne se détient pas comme état) ? ou
  le rôle EVENT_PUBLISHER de W2-ROLE est-il trop large (un notifieur n'est pas
  un bus d'événements) ?
- **Ne pas agir avant arbitrage.** Sans effet sur M7 ; à trancher au plus tard
  avec le gate de parité M8.

## Décisions confirmées

### D38 — Un port pilotant se lit aussi par l'intérieur : answered par le cœur, détenu par personne — CONFIRMÉE (2026-08-05)

- **Contexte** : mesuré au lot 1 de M7b sur le banc `ecommerce-hexagonal`, ses
  **sept interfaces `*UseCases` sortent UNCLASSIFIED** et le projet n'a donc
  aucun port pilotant. Vérifié à la source : `ExposedContract` (R5) est la
  **seule** règle qui conclut DRIVING_PORT, et elle exige qu'un DRIVING_ADAPTER
  détienne ou appelle l'interface. Le projet observé n'a ni contrôleur ni
  listener — `infrastructure/` ne porte que de la configuration, deux adapters
  pilotés et des utilitaires. **Conséquence : le backend rest n'écrirait rien
  sur un hexagone qui n'a pas encore sa couche web**, c'est-à-dire précisément
  sur le projet qui en aurait le plus besoin. Miroir exact de D27, et
  circulaire : il faudrait un adapter pilotant pour que le port qui justifie de
  l'écrire soit lu.
- **Options** : **A** le dual structurel de R5 — une interface qu'un type du
  cœur implémente et que **personne du périmètre ne détient** ne peut être que
  pour le dehors ; **B** le projet déclare ses ports pilotants au YAML ; **C**
  accepter, et changer de banc — rest ne servirait que les projets ayant déjà
  un adapter pilotant.
- **Décision** (utilisateur) : **A**. Un contrat appelé depuis l'intérieur est
  exclu par construction, donc un service de domaine interne ne bascule pas ; ce
  qui reste — answered par le cœur, tenu par personne dedans — n'a pas d'autre
  destinataire que l'extérieur. Palier plus bas que R5 : l'anneau qui parle est
  un signal plus fort que l'anneau qui manque.
- **Rendement mesuré avant écriture, sur le banc** : **6 des 7** ports basculent.
  Le septième, `InventoryUseCases`, est **détenu comme état par deux services
  applicatifs** (`OrderApplicationService`, `ShippingApplicationService`) — un
  cas d'usage qui en appelle un autre. Il reste donc muet, et le silence est
  celui qu'on annonce : détenu par le dedans, il a exactement la forme d'un
  collaborateur interne. C'est la lecture conservatrice, cohérente avec « un
  seul, sinon silence ».
- **Impact** : une règle nouvelle du moteur et sa ligne au doc 09 ; le cliquet
  mesure ce qu'elle déplace sur les 154 scénarios, arbitré dans le lot qui le
  déplace. Débloque le banc de M7b et le cas nominal du backend rest.

### D34 — Ce qui départage une lecture d'un changement d'état : les corps, lus en production — CONFIRMÉE (2026-08-05)

- **Contexte** : un backend REST doit choisir un verbe HTTP, et le verbe porte une
  promesse que le web tient à la place de l'auteur — un GET est recharché,
  préchargé, rejoué. Or la forme d'un cas d'usage ne suffit pas à savoir s'il lit
  ou s'il change quelque chose. Mesuré sur le banc `ecommerce-hexagonal`, sur ses
  27 cas d'usage : **cinq partagent `[identité] → agrégat`** (`getOrder`,
  `getCustomer`, `getProduct` lisent ; `placeOrder`, `deactivate` changent) et
  **quatre partagent `[une chaîne] → agrégat`** (`getOrderByNumber` lit ;
  `capturePayment`, `shipOrder`, `markDelivered` changent). `UseCase.type()`
  existe au modèle mais ne veut rien dire : `Assembly:121` le pose à
  `void ? COMMAND : QUERY`, et **aucun consommateur ne le lit** (living-doc
  n'affiche que les noms).
- **Constat d'instruction** : la capacité `METHOD_BODIES` du frontend n'est
  **jamais demandée en production** — `ProjectSources` bâtit sa requête sans
  aucune capacité. Toutes les règles marquées `[M]` au doc 09 (R5 en renfort,
  R6b) sont donc dormantes dans un vrai build, et personne ne l'avait consigné.
- **Options** : **A** l'hôte demande les corps, toujours, et le moteur rend
  `UseCase.type()` véridique ; **B** les corps demandés seulement si un backend
  installé les réclame (ressusciterait `consumes`, refusé à D28 faute de second
  consommateur) ; **C** la forme seule, l'ambigu refusé avec un diagnostic (9 cas
  d'usage sur 27 sans endpoint sur le banc, dont `getOrder`) ; **D** la forme
  puis un vocabulaire déclaré au YAML (l'option C laissée ouverte par D30).
- **Décision** (utilisateur) : **A**. Le fait qui départage existe dans le code
  lu ; ne pas le lire pour ensuite le demander à l'auteur serait lui faire
  écrire ce que le projet dit déjà.
- **La lecture, sans lire aucun nom** : un cas d'usage **change quelque chose**
  quand son implémentation **remet un type que le domaine possède** (un
  AGGREGATE_ROOT ou une ENTITY) à un port piloté. Remettre une identité ou une
  valeur ne compte pas — c'est ainsi qu'on demande, pas qu'on rend.
  Vérifié sur les 27 : `placeOrder` remet `Order` à son dépôt, `deactivate`
  remet `Product`, `shipOrder` et `markDelivered` remettent `Order` **et**
  `Shipment` ; `getOrder`, `getProduct`, `getOrderByNumber` et
  `getAvailableQuantity` ne remettent rien.
- **La marche suit les renvois internes** : sur ce banc les lectures passent par
  une méthode privée (`getProduct` → `findProductOrThrow`) là où les changements
  remettent directement. Une marche à un seul niveau serait donc sûre par
  accident ici et fausse ailleurs : elle se poursuit sur les invocations dont la
  cible **est le type déclarant lui-même**, ensemble fini, avec les visités
  retenus.
- **Le doute penche vers POST** : plusieurs surcharges d'un même nom ne se
  départagent pas dans les faits de corps (une `Invocation` porte la cible et le
  nom, pas les arguments) ; **si l'une d'elles remet un type possédé, le cas
  d'usage change quelque chose**. Un COMMAND lu à tort donne un POST honnête ;
  un QUERY lu à tort donne un GET qui ment, et c'est le web qui l'exécute.
- **Impact** : `ProjectSources` (la capacité demandée), `Assembly.useCase` (la
  lecture), le doc 09 (les règles `[M]` deviennent actives en production — le
  cliquet mesure ce qu'elles déplacent, et ce qu'elles déplacent s'arbitre dans
  le lot qui le déplace, protocole du lot 19). Le coût d'analyse des corps se
  mesure au lot, pas à la décision.

### D35 — Ce dont un port pilotant parle : un lien du moteur, exactement un sinon silence — CONFIRMÉE (2026-08-05)

- **Contexte** : un contrôleur sert une ressource — un chemin de base, et un
  `/{id}` qui n'a de sens que si l'identité est celle de quelque chose. La
  carrière déshabille le nom du port (`stripSuffix` sur
  `UseCases|Service|Port`), puis marque les types au score (retour 2, paramètre
  1). v7 n'a **aucun lien** entre un port pilotant et un agrégat : `RelationKind`
  porte `MANAGES`, `IDENTIFIED_BY` et `OWNS`, et `MANAGES` ne vaut que pour les
  ports pilotés.
- **Options** : **A** une relation nouvelle au moteur, énoncée par une règle ;
  **B** côté backend, depuis `inputTypes`/`outputTypes` que `DrivingPort` porte
  déjà ; **C** aucune notion de ressource, le chemin toujours tiré du nom du
  port.
- **Décision** (utilisateur) : **A**. Même raison qu'à D24 : ce qui se conclut du
  modèle vit au moteur, sinon deux consommateurs concluront deux choses. L'audit
  et living-doc y accèdent du même coup.
- **La règle** : l'agrégat dont un port pilotant parle est le seul que ses cas
  d'usage **nomment** — **exactement un, sinon silence**, comme R2. Un port qui
  parle de deux agrégats n'obtient pas de ressource, et son backend le dit
  plutôt que d'en élire une.
- **Amendement du 2026-08-05, au lot 3, avant d'écrire** : la formulation
  initiale disait « celui que ses cas d'usage **prennent et rendent** », calquée
  sur la convergence que `PortSignatures` (W2-ROLE) lit d'un port piloté. **Cette
  lecture ne se transporte pas**, et la mesure est sans appel : sur les **six**
  ports pilotants du banc, **aucun ne prend jamais son agrégat en paramètre**.
  Un port piloté converge parce qu'il reçoit l'agrégat pour le garder
  (`save(Order)`) ; un port pilotant reçoit une identité ou des valeurs et rend
  l'agrégat (`getOrder(OrderId) → Order`, `createProduct(String, Money, …) →
  Product`). L'intersection est **vide sur les six**, donc la règle en
  convergence se serait tue partout. Ce qui se lit des deux côtés est
  « l'agrégat que les signatures nomment », et c'est ce qui est écrit.
- **Impact** : `RelationKind`, une règle du moteur, `DrivingPort` porte le lien
  comme `DrivenPort` porte `managedAggregate`. Les goldens du corpus qui bougent
  se re-arbitrent dans le lot qui les bouge.

### D36 — La lecture du domaine sort de jpa : un commun, maintenant qu'un second backend lit — CONFIRMÉE (2026-08-05)

- **Contexte** : un DTO de requête se reconstruit en type du domaine, un DTO de
  réponse se lit depuis lui. C'est mot pour mot la question que D30 a tranchée
  pour le mapper JPA — lire par une méthode sans paramètre dont le type de retour
  est celui du champ, reconstruire par le constructeur qui prend l'état — et son
  code vit dans `DomainAccess` et `Crossing`, privés à jpa.
- **Options** : **A** des DTO, sur la lecture de D30 extraite vers un commun ;
  **B** pas de DTO, le contrôleur prend et rend les types du domaine ; **C** des
  DTO, rest refaisant sa propre lecture.
- **Décision** (utilisateur) : **A**. **C** est exactement §10.6 (deux
  implémentations du même concept) et **B** ferait fuir le domaine jusqu'à l'API
  tout en exigeant de lui, pour le désérialiseur, ce que D30 refuse justement de
  deviner.
- **§10.1 est satisfait, et c'est le moment** : l'extraction n'était pas
  permise à M7a — un seul consommateur. Le second existe maintenant, donc le
  commun s'écrit contre les deux, pas contre un plus une hypothèse.
- **Impact** : un module (ou un paquet) commun aux backends, jpa reposé dessus
  **sans changer une ligne de sa sortie** — le lot le vérifie sur les goldens
  existants, comme le dépôt reposé sur `StoreQuestion` à M7a.
- **Exécution au lot 4 (2026-08-05), partielle et datée** : **`DomainAccess`
  seul** part au commun (paquet `io.hexaglue.spi`, pas un module : une centaine
  de lignes ne justifie pas une structure, et le doc 07 §2.1 n'en liste pas).
  Sortie de jpa vérifiée **identique octet à octet** sur les 25 fichiers du banc,
  générés avant et après le déplacement. **`Crossing` reste dans jpa** : il écrit
  les noms de méthodes du mapper JPA, nomme les types `Stored` et lit
  `JpaOptions` — sa question est « traverser vers **la ligne** ». Ce qui est
  partageable dessous est la partition qu'il applique (conteneur / hors modèle /
  identité déballée / ensemble fermé / valeur portée / vie propre), et
  l'extraire avant que rest existe reviendrait à la dessiner contre un
  consommateur plus une hypothèse — le §10.1 que ce lot applique par ailleurs.
  **Extraction reportée au lot 7**, quand le constructeur de DTO en sera le
  second consommateur réel.

### D37 — Le handler d'exceptions et le câblage de beans sortent du périmètre — CONFIRMÉE (2026-08-05)

- **Contexte** : la carrière écrit aussi un `@ControllerAdvice` global et une
  classe `@Configuration` déclarant les services applicatifs en beans. Le premier
  repose sur **quatorze règles de suffixe** (`…NotFoundException` → 404,
  `…AlreadyExistsException` → 409, jusqu'à `contains("Insufficient")` → 400) sur
  des exceptions dont le modèle ne porte rien ; le second décide du câblage
  d'injection à la place de l'auteur.
- **Options** : **A** les deux au backlog ; **B** un handler réduit à ce que le
  langage garantit plus ce que le YAML déclare ; **C** les deux, sur déclaration
  seule.
- **Décision** (utilisateur) : **A**. M7b livre des contrôleurs et leurs DTO.
- **Impact** : périmètre de M7b, et deux entrées de plus au backlog post-7.0.0, à
  côté des quatre sorties de la carrière écartées par D23.

### D32 — R2 départage plusieurs clés de recherche par la forme de la réponse, puis par le point fixe — CONFIRMÉE (2026-08-04)

- **Contexte** : R2 (« la clé par laquelle un port cherche l'agrégat est son
  identité, exactement une sinon silence ») a été calibrée sur des fixtures à
  une seule clé. Sur le premier banc réel, **cinq agrégats sur six** ont deux
  clés candidates (`findByCustomerId(CustomerId)` à côté de
  `findById(OrderId)`), et le silence de R2 est total : l'identifiant reste
  UNCLASSIFIED (duel S4 ouvert), l'agrégat n'a pas d'`identityField`
  (`IDENTIFIED_BY` est **l'unique source d'identité** du modèle), la génération
  refuse tout. Mesuré : 5/6 identifiants UNCLASSIFIED à LOW, grade C sur un
  hexagone propre. Aucun des 154 scénarios du corpus n'avait deux clés.
- **Options** : **A** la forme de la réponse départage (une clé prise par une
  méthode qui rend **au plus un** agrégat — `A` ou `Optional<A>` — cherche ;
  une clé prise par une méthode qui en rend plusieurs — `List<A>` — filtre, et
  ne compte pas) ; **B** en plus, au point fixe, une clé déjà identité d'un
  **autre** agrégat est disqualifiée quand plusieurs candidates restent ;
  **C** en dernier ressort, le mot du magasin (`findById`) — écarté : lirait un
  nom dans le classifieur (§10.9), où une lecture fausse se propage au modèle
  entier.
- **Décision** (utilisateur) : **A+B**. Deux lectures purement structurelles,
  aucun nom lu. Sur le banc : résout `Order`, `Payment`, `Shipment` (par A) et
  `Inventory` (par B, `ProductId` identifiant déjà `Product`) ; `Customer`
  reste muet **honnêtement** — `findByEmail(Email) → Optional<Customer>` a
  exactement la forme d'une recherche par clé naturelle, rien de structurel ne
  sépare `Email` de `CustomerId` ; c'est le cas D16, le rapport porte la
  question et la remédiation dit de déclarer.
- **Garde de monotonie** : B **départage, ne met jamais de veto** — une clé
  unique gagne même si elle identifie déjà un autre agrégat (un port qui ne
  cherche `Inventory` que par `ProductId` dit que c'est sa clé ; lecture 1:1
  légitime). Sans cette garde, l'arrivée d'un fait `IDENTIFIED_BY` pourrait
  invalider une élection déjà émise — la saturation semi-naïve exige des
  conclusions qui ne font que croître.
- **Impact** : `LookupIdentity` seul (la machinerie suffit : R2 lit et écrit
  déjà `RELATION`, la saturation re-déclenche sur fait nouveau). Le doc 09
  (ligne R2) est amendé dans le même lot. Les attentes de corpus qui bougent se
  re-arbitrent dans le lot qui les bouge (protocole du lot 19).

### D31 — Quand deux opérations du magasin partagent une forme, le mot du magasin départage — CONFIRMÉE (2026-08-04)

- **Contexte** : l'adapter (lot 7 de M7a) dérive de la **forme** d'une méthode
  de port l'opération du magasin qui y répond. Une forme est ambiguë :
  « prend l'agrégat entier, ne répond rien » est aussi bien `save` que
  `delete`. Or `void save(Order)` est une des formes de port les plus
  répandues.
- **Options** : **A** la forme, puis le vocabulaire du magasin départage (les
  mots que Spring Data emploie : `save`, `delete`, `deleteById`) ; **B** la
  forme seule, l'ambiguïté refusée avec un diagnostic (refuse `void
  save(Order)`) ; **C** la forme seule, `[entier] → rien` lu comme enregistrer
  (un `void delete(Order)` produirait un enregistrement — du code qui compile
  et fait le contraire).
- **Décision** (utilisateur) : **A**. Prolonge D30 d'un cran : la forme décide,
  et quand elle laisse un choix, seuls les mots que le **magasin** emploie sont
  consultés — jamais une convention du projet lu. `void save(Order)` produit
  `repository.save(...)` ; `void archive(Order)` n'obtient rien et le dit
  (HG-JPA-005 nommant la méthode).
- **Impact** :
  - **Effacer n'est jamais conclu de la seule forme** — conséquence appliquée
    uniformément : `[identité] → rien` ne devient `deleteById` que si le port a
    employé le mot du magasin. Une requête qui lit les mauvaises lignes se
    relance, une suppression qui efface les bonnes ne se relance pas.
  - Le reste est décidé sans lire aucun nom : `[entier] → l'agrégat` = `save`,
    `[identité] → Optional` = `findById`, `[identité] → booléen` =
    `existsById`, `[rien] → collection` = `findAll`, `[rien] → nombre` =
    `count`, `[champs] → …` = la requête que l'interface déclare.
  - Ce que le magasin ne sait pas rendre **dans les termes demandés** est
    refusé plutôt que converti : un `int` pour un compte (le magasin compte en
    `long`), un `Set` ou un `Stream` pour une collection, l'agrégat lui-même là
    où le magasin peut ne rien trouver. Convertir serait le générateur décidant
    ce que personne ne lui a demandé.
  - L'adapter est **tout ou rien** (une classe qui implémente un port
    l'implémente en entier) : HG-JPA-005 nomme **toutes** les méthodes sans
    réponse, pas seulement la première — l'auteur a besoin de la liste.

### D30 — Le générateur lit le domaine par la forme et le reconstruit par son constructeur — CONFIRMÉE (2026-08-04)

- **Contexte** : le mapper (lot 6 de M7a) est le **seul endroit de la
  génération où le code produit appelle le code écrit à la main**. Partout
  ailleurs le backend écrit des fichiers neufs : une erreur y coûte une
  régénération. Ici elle casse la compilation du projet de l'utilisateur. La
  carrière y faisait deux paris nominaux — `"get" + nom capitalisé` pour lire
  (`JpaMapperCodegen:644`), et une **méthode de fabrication devinée par son
  nom** pour reconstruire (`reconstitution.factoryMethodName()`).
- **Options instruites** : pour la lecture, **A** forme d'abord et nom en
  départage, **B** convention JavaBean par le nom comme la carrière, **C** rien
  deviner du tout ; pour la reconstruction, **A** le constructeur qui prend
  l'état, **B** constructeur puis factory devinée, **C** constructeur puis
  factory nommée dans le vocabulaire déclaré.
- **Décision** (utilisateur) : **A** et **A**.
  - **Lire** : une méthode sans paramètre dont le type de retour est celui du
    champ. Si une seule répond, c'est elle et **aucun nom n'est lu**. Si
    plusieurs (deux champs `Money`), départage par les trois orthographes que
    **le langage** impose — composant de record, `get`, `is` — jamais par une
    convention du projet lu. Sinon : rien.
  - **Reconstruire** : le constructeur dont les paramètres reprennent l'état du
    type, dans l'ordre ; celui d'un record y répond toujours. Aucune factory
    devinée. Sinon : rien.
- **Impact** : le mapper est **tout ou rien** — un mapper partiel perdrait des
  données à l'aller et reconstruirait autre chose au retour —, et son refus est
  `HG-JPA-004`, qui **nomme le champ** qui a bloqué. Conséquence assumée : un
  domaine qui cache son constructeur derrière une fabrique n'obtient pas de
  mapper et l'apprend par un diagnostic. Si cela se révèle courant sur un parc
  réel, l'option C de la reconstruction (vocabulaire **déclaré**, jamais une
  liste écrite par nous) reste ouverte sans rien contredire ici.

### D29 — `Field.elementType` / `wrappedType` / `roles` : remplis par le moteur, à l'assemblage — CONFIRMÉE (2026-08-04)

- **Contexte** : écart M2 consigné au lot 21 de M3, échéance explicite « à
  trancher au plus tard à l'ouverture de M7 ». Personne ne les remplit
  (aucune occurrence dans le frontend ni le moteur) ; le générateur JPA de la
  carrière les lit à douze endroits dans neuf fichiers ;
  `TypeStructure.fieldsWithRole` existe — une API de lecture sur du vide.
- **Options** : **A** scindé — déballage syntaxique au frontend
  (`Members.fieldOf`), rôles au moteur ; **B** tout au moteur, à
  l'assemblage ; **C** retirer `elementType`/`wrappedType` (raccourcis de
  `TypeRef.typeArguments`) et ne garder que `roles`.
- **Décision** (utilisateur) : **B**. La doctrine du lot 21 (Javadoc de
  `Links`) la commande : *une décision de règle se relit dans le lien que
  cette règle a énoncé*. `IDENTITY` ← lien `IDENTIFIED_BY` (R1/R2) ;
  `AGGREGATE_REFERENCE` ← la composition ; `EMBEDDED` ← `OWNS` ;
  `COLLECTION`/`elementType` ← déballage de `TypeRef` ; `wrappedType` ←
  `Shapes.readsAsIdentity`. C'est ce que la Javadoc de `Field` promet déjà ;
  le frontend reste intouché et le `CodeModel` purement syntaxique.
- **Impact** : les 154 goldens bougent **une fois, au lot 1 de M7a** — la
  garde « golden existant » fait relire le diff. Piège écrit d'avance :
  `AUDIT`/`TECHNICAL` ne peuvent se poser que sur annotation de pack
  (`@Version`, `@CreatedDate`…), **jamais sur nom** (règle de conduite 4).

### D28 — Le SPI de génération : `SourceSink` typé, `DiagnosticSink`, seuil dans la `Contribution` — CONFIRMÉE (2026-08-04)

- **Contexte** : D25 datait `SourceSink` à M7 ; `DiagnosticSink` (doc 07
  §6.1) est le seul des quatre sinks dont le sort n'avait jamais été énuméré.
  Or §6.4 exige « sous le seuil, diagnostic + remédiation au lieu de code
  faux », et le seul canal actuel d'un plugin est l'échec total
  (`PluginConfigException` → HG-PLUGIN-003, contribution perdue, dépendants
  écartés). `generation.minConfidence` est chargé depuis le YAML
  (`ConfigLoader`) mais personne ne le lit.
- **Décision** (utilisateur), en trois points :
  1. **`Sinks(documents, sources)`** : une émission source porte paquet +
     nom de type + contenu + **module cible optionnel** — pas un chemin. Le
     chemin se dérive côté hôte (confinement structurel, comme `Document`) ;
     le routage multi-module de §6.1 est dans la forme dès le premier jour,
     seul le mono-module est exercé en M7a.
  2. **`DiagnosticSink`, troisième sink** : un plugin émet des diagnostics
     codés sans cesser de contribuer ; ils rejoignent `PluginRun.diagnostics`
     et la restitution existante de l'hôte (`Diagnostics`).
  3. **Le seuil voyage typé dans la `Contribution`** : c'est une politique du
     moteur, pas une option de plugin (`PluginConfig` reste opaque). Le champ
     `minConfidence` du manifeste **reste dehors** — le refus du lot 1 de M6
     (§10.7) tient tant qu'aucun backend n'exige plus que le plancher du
     projet ; `consumes` reste dehors pour la même raison (aucun plugin ne
     demande de capacité du modèle).
- **Impact** : l'hôte gagne un goal `generate` (phase GENERATE_SOURCES) qui
  matérialise sous `target/generated-sources/hexaglue`, enregistre la racine
  (`addCompileSourceRoot`, absent aujourd'hui) et restitue les diagnostics.
  `generate` **écrit et ne juge pas** : sous le seuil il émet, c'est
  `validate` qui gate. `reactor-generate` attend son exemple multi-module
  (M7b ou gate de parité).

### D27 — HG-HEX-002 sur un projet qui génère : le manifeste déclare `produces`, le jugement le lit — CONFIRMÉE (2026-08-04)

- **Contexte** : au rejeu de `case-study-banking` (clôture M6), cinq fausses
  alertes `HG-HEX-002` (« rien n'implémente ce port piloté ») sur un projet
  dont les adapters sont générés — 51 fichiers sur 99 portent `@Generated`,
  écartés par D15/D19. Le finding dit vrai du code écrit à la main, faux du
  projet ; la génération (M7) est précisément ce qui le rend faux chez ceux
  qui l'utilisent.
- **Options** : **A** le manifeste déclare `produces`, le jugement lit les
  déclarations des backends installés ; **B** ne rien changer (le README
  l'énonce déjà) ; **C** le backend déclare après coup ce qu'il a produit —
  inverse le flux, un plugin renseignerait le juge (contre D24) ; **D**
  rouvrir D15 — mesuré destructeur au lot 20 (le port tombe, le service
  applicatif avec lui, la seconde exécution ne rend plus le même modèle).
- **Décision** (utilisateur) : **A**. `PluginManifest` gagne `produces` — ce
  qu'un backend produira, **par famille de port couverte** (jpa : les
  adapters des ports pilotés à rôle REPOSITORY ; rest : les adapters
  pilotants). Le grain par famille évite de taire un port GATEWAY que jpa ne
  couvre pas. `Judgement` gagne un quatrième composant (les couvertures
  déclarées) ; `HG-HEX-002`/`HG-HEX-005` se taisent sur un port couvert **en
  le disant** (diagnostic INFO : combien de ports tus, couverts par quel
  backend) — cinq fausses alertes ne s'échangent pas contre un silence
  inexpliqué.
- **Impact** : l'hôte tend au moteur les manifestes qu'il découvre déjà
  (ServiceLoader) — les trois goals **et `validate`**, pour que le gate et le
  rapport jugent pareil (la question « `validate` mono-module » de la clôture
  M6 y touche). Déterminisme intact : le classpath du plugin est déclaré au
  POM ; on lit une déclaration, jamais une sortie — D15 intacte. Le champ est
  écrit contre son premier déclarant réel (jpa, lot 6 de M7a), pas avant
  (§10.1).

### D26 — La lecture S5 établit une propriété de module, jamais un kind — CONFIRMÉE (2026-08-04)

- **Contexte** : le doc 09 §3 donne à l'ancre A4 (S5, palier `TOPOLOGY`) ce
  qu'elle établit : « module candidat domaine = ne dépend d'aucun module interne
  ni d'infra ». Or le moteur ne sait porter qu'une `KindEvidence`, qui exige
  **un** `ArchKind` précis, et « domaine » n'en est pas un — six kinds le
  composent. L'ancre telle qu'elle est écrite n'a pas de forme dans la
  machinerie.
- **Ce que l'instruction a montré** : le corpus d'acceptation est entièrement
  mono-module, donc un signal S5 qui pèserait sur la classification ne serait
  mesurable par **aucun** test du réacteur ; et le seul mapping rôle → kind qui
  serait unique (INFRASTRUCTURE → DRIVEN_ADAPTER) serait faux sur les entités de
  persistance, les mappers et la configuration que tout module d'infra contient.
- **Options** : **A** une propriété de module, aucune évidence de kind ;
  **B** des évidences de kind par anneau depuis le rôle déclaré ; **C** A plus
  un finding quand le rôle déclaré et la structure se contredisent.
- **Décision** (utilisateur) : **A**. S5 n'émet aucune évidence : elle établit
  dans la `ModuleTopology` qu'un module est **candidat domaine** — il ne
  référence aucun autre module du réacteur et aucun de ses types ne porte
  `INFRA_DEPENDENCY`. La classification est strictement inchangée. C'est
  cohérent avec A2, qui établit « des faits techniques, jamais un kind ».
- **Impact** :
  - S5 n'est **pas** une `Rule` du catalogue : une règle écrit des faits indexés
    par `TypeId`, et une propriété de module n'en est pas un. La lecture vit au
    moteur avec le substrat de graphe du lot 4 (`Modules`, à côté de
    `Dependencies` et `BoundedContexts`).
  - Le consommateur réel est le rapport agrégé du goal réacteur, qui rend la
    disposition du build dans sa section d'inventaire.
  - La candidature est décidée sur le réacteur **entier**, y compris les modules
    sans rôle déclaré : un module domaine qui dépend d'un module non déclaré
    dépend de quelque chose, et l'ignorer transformerait un trou de
    configuration en compliment.
  - Le doc 09 §3 (ligne A4, colonne « État ») est à relire à cette lumière :
    l'ancre existe, elle ne produit pas d'évidence.

### D25 — Le SPI est écrit contre living-doc — CONFIRMÉE (2026-08-03)

- **Contexte** : D18 place le SPI en tête de M6, « écrit contre son premier
  plugin et arbitré par lui ». Deux candidats au jalon : living-doc (6 627
  lignes à la carrière) et audit (26 858 lignes).
- **Options** : **A** living-doc d'abord ; **B** audit d'abord.
- **Décision** (utilisateur) : **A**. Le plus petit consommateur façonne le
  contrat — manifest, sinks de rapport, exécution en deux passes, isolation
  `LinkageError`, l'hôte matérialise. La chaîne est démontrable de bout en bout
  tôt dans le jalon, et B7 (NPE du tri topologique) devient testable dès ce lot.
- **Impact** : `FindingSink` n'est ajouté qu'à l'arrivée de l'audit et
  `SourceSink` qu'à M7 avec jpa/rest — aucun sink n'existe avant son
  consommateur (interdit 07 §10.1). Le contrat est éprouvé une seconde fois
  dans le même jalon par l'audit, ce qui est le test de généralité que
  l'interdit réclame.

### D24 — Les règles de findings vivent au moteur — CONFIRMÉE (2026-08-03)

- **Contexte** : le doc 07 se contredit. §3.3 dit que `Finding` est « produit
  par les règles d'audit » ; §2.2 et §6.3 disent que la validation est une
  politique du **moteur** qui gate sur les statistiques de classification **et**
  sur les findings. Si les règles vivent dans le plugin, `validate` ne peut
  gater sur des findings sans exécuter un plugin, et `findingThresholds` — lu
  depuis M5, consommé par personne — n'a toujours pas de sujet.
- **Options** : **A** les règles de findings et les algorithmes de graphe au
  moteur ; **B** au plugin audit, l'hôte enchaînant analyse → plugins → gates ;
  **C** un module de règles partagé entre les deux.
- **Décision** (utilisateur) : **A**. Les 15 règles (8 DDD, 7 hexagonales) et le
  substrat de graphe — SCC de Tarjan (B10), métriques de Martin avec le SDP dans
  le bon sens (B1), bounded contexts relatifs au `basePackage` (B8) — vivent
  dans `hexaglue-engine`. Elles sont pures, ne font aucune E/S et lisent le
  modèle : la même nature que les règles de classification.
- **Impact** : `validate` gate sur les findings sans exécuter de plugin ;
  `findingThresholds` trouve son sujet ; **B9 est vrai par construction** (une
  seule implémentation des bounded contexts, que tous les plugins consomment) ;
  le plugin audit devient ce que le doc 07 §6.2 en dit — du contenu et du rendu,
  qui ne re-dérive rien (interdit §10.5, tenu par ArchUnit). Le doc 07 §3.3 est
  amendé : « produit par les règles d'audit **du moteur** ».

### D23 — Périmètre du rapport d'audit : le rapport publié fait foi — CONFIRMÉE (2026-08-03)

- **Contexte** : le périmètre gelé (règle de conduite 12) dit « fonctionnalités
  actuelles », or l'audit de la carrière pèse 26 858 lignes — 15 contraintes,
  20 calculateurs de métriques, 10 constructeurs de diagrammes, 4 renderers,
  ~60 records de rapport, plus un estimateur de dette, un comparateur
  d'historique, un générateur de configuration CI et un moteur de
  recommandations. Tout n'y a pas la même valeur d'usage, et rien ne dit
  lesquelles de ces sorties sont réellement consommées.
- **Ce qui donne un critère objectif** : le rapport **publié** — celui que les
  pages du site rendent pour l'étude de cas — montre sept sections : verdict
  (score, grade, violations), décomposition du score, violations, métriques
  qualité, inventaire, stabilité des packages, estimation de remédiation.
- **Options** : **A** parité complète avec la carrière ; **B** le rapport publié
  fait foi ; **C** noyau strict (règles + inventaire + fiabilité + verdict).
- **Décision** (utilisateur) : **B**. Entrent au jalon : les 15 règles en
  findings codés, les sept sections du rapport publié, la section « fiabilité de
  la classification », la provenance dans l'inventaire, les renderers console /
  markdown / JSON et les diagrammes Mermaid. **Sortent au backlog post-7.0.0,
  explicitement** : générateur de configuration CI, comparateur d'historique
  d'audit, charts radar et quadrant, renderer HTML.
- **Impact** : le gate de parité (M8) compare ce qui est publié, section par
  section ; les quatre sorties écartées sont des écarts **décidés**, à porter au
  gate comme tels et non à découvrir. Si une page du site consomme l'une d'elles,
  la décision se rouvre sur un besoin constaté.

### D16 — Une partie dont l'identité est un type de plateforme nu : Q1 muet, Q2 le dit — CONFIRMÉE (2026-08-03, ouverte le 2026-08-03 au lot 22)

- **Contexte** : mesuré sur le profil 2 du corpus. `Owner` tient
  `List<Pet> pets` ; `Pet` porte `@Id Integer id`. R3a exige « T porte un champ
  IDENTIFIER » ; or `java.lang.Integer` est hors périmètre et ne reçoit aucun
  verdict, donc c'est R3b qui mord : **`Pet` = VALUE_OBJECT**. Le moteur est
  conforme au doc 09 ; la lecture est néanmoins fausse sur le domaine. Ce n'est
  pas un défaut d'implémentation — c'est le référentiel qui produit ce résultat,
  d'où une décision et non un correctif.
- **Ce qui ferme les deux issues évidentes** :
  - le seul élément des sources qui dit « `id` est l'identité de `Pet` » est
    `@Id`, et **D7** interdit à l'annotation de persistance de contribuer à un
    kind, « ni positive ni négative » ;
  - le discriminant structurel le plus évident, la **mutabilité**, a été
    explicitement écarté au lot 20 : le doc 09 vague 4 pose R3b « **même si T
    est mutable** (la mutabilité devient un finding, jamais un obstacle) ».
- **Enjeu, en volume** : la racine d'agrégat s'en sort toujours (R1 la rattrape
  par la déclaration Spring Data) ; ce sont les **parties** qui tombent —
  `Pet`, `Visit` chez petclinic. C'est la forme dominante du parc que le
  profil 2 existe pour représenter, pas un cas de bord.
- **Enjeu, en aval** : le kind n'est pas une étiquette. À M6 l'audit lira une
  composition fausse ; à M7 un générateur de mapping traitera une entité
  persistée à part entière comme une valeur embarquée.
- **Options** :
  - **A — statu quo.** Q1 se tait honnêtement, l'utilisateur déclare. Coût :
    faux sur la forme la plus courante du parc cible.
  - **B — amender R3a** en séparant deux questions que D7 confond peut-être :
    « quel kind ce type a-t-il » (interdit au mapping) et « quel champ porte
    l'identité de ce type » (lecture de structure, dont R3a resterait seule à
    tirer un kind). Contre-argument à peser : si la présence de `@Id` fait
    basculer VALUE_OBJECT→ENTITY, l'annotation a décidé du kind dans les faits,
    et la distinction est rhétorique. Rouvre D7.
  - **C — laisser Q1 muet et charger Q2** (M6) : finding « cette partie est
    cartographiée comme entité persistée mais rien dans le modèle ne la
    distingue d'une valeur — déclarez-la », plus la sortie S1 déjà prévue
    (jMolecules par FQN, `classification.explicit`). Cohérent avec « Q1 tolère
    ce que Q2 condamne » et avec D10 ; ne coûte aucune décision antérieure.
- **Recommandation de l'analyse** : **C**, avec B en réserve si la mesure
  montre que le finding ne suffit pas — B rouvre D7 et paraît difficile à
  borner une fois la brèche ouverte. Sans attachement : l'arbitrage touche le
  référentiel et revient à l'utilisateur.
- **Où c'est épinglé** : `corpus/profile2/Clinic-theWholeClinic/expectations.txt`
  porte `expect: …Pet = VALUE_OBJECT` avec le paragraphe qui dit pourquoi c'est
  juste par les règles et faux sur la clinique. Quelle que soit l'issue, le
  changement se lira en diff sur une claim relue **et** sur le golden.
- **Jalon** : à trancher au plus tard à **M6** (le finding de l'option C y est
  rédigé) et de préférence avant **M7** (la génération de mapping en dépend).
- **Décision** (utilisateur, 2026-08-03, à l'ouverture de M6) : **C**. Le verdict
  reste VALUE_OBJECT — Q1 se tait plutôt que de deviner — et **Q2 le dit** : un
  finding codé énonce que cette partie est cartographiée comme entité persistée
  alors que rien dans le modèle ne la distingue d'une valeur, avec pour
  remédiation de la déclarer (jMolecules par FQN ou `classification.explicit`).
  Cohérent avec « Q1 tolère ce que Q2 condamne » (doc 09) et avec D10 ; ne rouvre
  ni D7 ni le débat de la mutabilité clos au lot 20.
- **Impact** : le finding se rédige au lot des règles de findings (M6, D24) et se
  mesure sur `Clinic-theWholeClinic`, dont l'attente reste inchangée — le verdict
  ne bouge pas, c'est le rapport qui parle. Aucun golden déplacé. M7 lira le
  finding pour refuser de générer un mapping sur une partie ambiguë plutôt que
  d'en générer un faux.

### D22 — Le moteur rend ses propres diagnostics — CONFIRMÉE (2026-08-03)

- **Contexte** : trouvaille du lot 4, mise au jour par un test. Le périmètre est
  lu à **deux** endroits et pas de la même façon : `AnalysisPerimeter` (frontend)
  filtre sur `includePackages`/`excludePackages`, `Perimeter` (moteur) filtre en
  plus sur `basePackage` — le paramètre que tout hôte règle. Un type hors du
  `basePackage` est donc **lu** par le frontend, n'entre **pas** dans
  l'`ArchModel`, et **rien ne le dit**. C'est la dette D20 un étage plus loin, et
  le canal du frontend ne peut pas la combler : de son point de vue, le type a
  été lu.
- **Ce qui n'est pas en cause** : le comportement lui-même est délibéré et reste
  inchangé. Le contexte hors périmètre nourrit la dérivation — un adapter hors
  du `basePackage` est ce qui fait reconnaître le port qu'il implémente — et les
  154 goldens en dépendent. Ce qui manque est de le **dire**.
- **Options** : **A** le moteur rend un résultat porteur (modèle + diagnostics) ;
  **B** l'hôte compare ce qui a été lu à ce qui a un verdict ; **C** unifier les
  deux périmètres (le frontend cesse de lire hors `basePackage`) ; **D** ne rien
  faire.
- **Décision** (utilisateur) : **A**. `Analysis.analyze` rend un résultat portant
  l'`ArchModel` **et** les diagnostics codés. Symétrie exacte de D20 : celui qui
  écarte est celui qui le dit. **B** ferait re-dériver à l'hôte une information
  du modèle (interdit 07 §10.5, énoncé pour les plugins et valant pour un hôte) ;
  **C** appauvrirait la dérivation et déplacerait tous les goldens.
- **Impact** : signature de M3 changée, trois appelants (chaîne d'acceptation,
  hôte, test du moteur) ; les goldens ne bougent pas, un diagnostic n'étant pas
  un verdict ; les stubs de classpath ne sont pas dénombrés — ce n'est pas le
  code de l'utilisateur.

### D21 (amendement du 2026-08-03) — la topologie de modules et S5 partent à M6

- **Ce que la mesure a montré**, à l'ouverture du lot : `TypeNode.moduleName`
  n'est alimenté par personne et le frontend n'a aucune notion de module ; le
  modèle ne porte **aucune dépendance inter-modules**, donc « S5 structurel =
  rôle déduit des dépendances » n'a pas de substrat ; `Assembly` ne construit
  jamais de `ModuleTopology` ; aucune règle ne lit de rôle.
- Brancher S5 à M5 demanderait quatre gestes — nom de module dans la requête
  frontend, canal de rôles en configuration, assemblage de la topologie, la
  règle — pour un signal dont le premier consommateur réel est le rapport
  agrégé (M6) ou le routage de génération (M7), que D21 différait déjà.
- **Décision** (utilisateur) : **la topologie et S5 voyagent avec l'analyse
  réacteur, à M6**, où la règle a à la fois un producteur et un consommateur.
  M5 s'arrête à l'hôte mono-module, démontré de bout en bout. Même raisonnement
  que D18, et sur des faits mesurés cette fois-ci.

### D18 — M5 livre l'hôte ; le SPI et l'exécution des plugins passent à M6 — CONFIRMÉE (2026-08-03)

- **Contexte** : le doc 08 place à M5 « SPI + exécution + validate » — sinks typés,
  manifest de plugin, DAG deux passes, isolation `LinkageError`. Instruction du
  code à l'ouverture du jalon : aucun de ces objets n'a de consommateur possible
  à M5.
- **Ce que la lecture a montré** :
  - aucun module plugin au réacteur — audit et living-doc sont à M6, jpa et rest
    à M7 ;
  - **rien à écrire à M5** : la validation « ne produit qu'un verdict »
    (07 §6.3), et le rapport que `validate` écrit dans la carrière, l'hôte le
    tire de la restitution de M4 — « le moteur n'écrit rien, l'hôte
    matérialise » (07 §2.2) n'a pas besoin d'un sink pour cela ;
  - le critère de sortie annoncé (« `examples/test-param-*` portés et verts »)
    n'est pas atteignable tel quel : sur les 43, **neuf** relèvent de l'hôte
    (`classification-*`, `fail-on-unclassified`, `skip`, `skip-validation`,
    `validation-report-path`) et **34** sont des paramètres des plugins
    jpa/rest/livingdoc/audit ;
  - B7 (NPE du tri topologique) n'a aucun test de régression possible sans un
    plugin à ordonner.
- C'est l'interdit 07 §10.1 (publier une abstraction sans second consommateur
  réel), et la forme exacte du défaut que M4 a débusqué : neuf lots de preuves
  que rien ne lisait.
- **Décision** (utilisateur) : M5 est **le jalon de l'hôte** — YAML strict,
  racines de sources, canal de diagnostics, gates `validate`, plugin Maven
  mince, topologie de modules. Le SPI, les sinks, le manifest, le DAG deux
  passes et l'isolation `LinkageError` passent **en tête de M6**, écrits contre
  le premier plugin qui les consomme ; leur second consommateur arrive à M7.
- **Impact** :
  - doc 08 amendé : lignes M5 et M6 du tableau §3, et B7 du tableau §5
    (jalon M5 → M6) ;
  - critère de sortie de M5 recentré : les **neuf** `test-param-*` d'hôte portés
    et verts, chaque gate démontrée par un test ; les 34 autres suivent leur
    plugin ;
  - M5 reste démontrable de bout en bout : `mvn hexaglue:validate` sur de vraies
    sources, gates armées, restitution M4 dans les logs ;
  - D17 est inchangée — M5 demeure le jalon des décisions d'hôte qu'un CLI
    reprendrait ; le SPI n'en est pas une, c'est un contrat de plugin.

### D19 — Racines de sources : la racine principale seule — CONFIRMÉE (2026-08-03)

- **Contexte** : décision d'hôte renvoyée à M5 par D17 et inscrite comme dette
  au chantier. La carrière (`MojoSourceRootsResolver`) passe **toutes** les
  racines de compilation du projet, `target/generated-sources` comprise, en s'en
  remettant à un filtre de code généré en aval.
- **Ce que la mesure a établi** (lot 20, D15) : le code généré remis dans le
  périmètre détruit la lecture du code écrit à la main — l'adapter généré
  implémente le port, R4 tombe, R6 tombe avec lui, et la seconde exécution sur
  des sources inchangées ne rend pas le même modèle. Le filtre `@Generated` du
  frontend ne couvre que ce qui est **marqué** : un générateur qui ne marque pas
  sa sortie passe au travers.
- **Options** : **A** la racine de sources déclarée seule ; **B** toutes les
  racines de compilation (comportement de la carrière) ; **C** toutes sauf
  celles sous le répertoire de build.
- **Décision** (utilisateur) : **A**. L'hôte passe la racine de sources déclarée
  du projet — substituée par la sortie delombok quand elle existe — et **jamais**
  une racine sous le répertoire de build. Le filtre `@Generated` du frontend
  reste en place : c'est le filet pour du code généré commité dans les sources,
  pas la ligne de défense.
- **Impact** : les racines ajoutées par d'autres plugins (build-helper, sources
  générées hors `target/`) ne sont pas lues — écart visible que le canal de
  diagnostics de D20 doit **dénombrer et nommer**, sans quoi un modèle plus
  petit se lit comme un code plus petit. Aucune clé de configuration nouvelle :
  si un projet doit faire lire une racine supplémentaire, la question se rouvre
  sur un besoin réel, pas par précaution.

### D20 — Canal de diagnostics : le frontend rend un résultat porteur — CONFIRMÉE (2026-08-03)

- **Contexte** : écart assumé de M2, dette explicite de D15, rencontrée par M4
  sans être traitée — rien ne dit *pourquoi* un type absent du rapport est
  absent : hors périmètre de package, code généré, ou récupération partielle au
  parsing. `SpoonFrontend.analyze` rend un `CodeModel` nu ou lève.
- **Options** : **A** un résultat porteur (modèle + diagnostics codés) ; **B** un
  collecteur passé dans la requête ; **C** un décompte journalisé.
- **Décision** (utilisateur) : **A**. `SpoonFrontend.analyze` rend un résultat
  portant le modèle **et** la liste des diagnostics codés. Ce que le frontend
  écarte devient dénombrable et nommable ; l'hôte journalise, et aucun étage
  n'est obligé de le lire.
- **Impact** : la signature de M2 change (deux appelants — la chaîne
  d'acceptation, le plugin Maven) ; **B** est écartée parce qu'un collecteur rend
  le frontend impur et serait une abstraction sans second consommateur tant
  qu'un seul hôte existe ; le contrat « le modèle est complet ou l'appel
  échoue » **est conservé** — un diagnostic dit ce qui a été *écarté*, il ne
  décrit jamais un modèle partiel. **Un diagnostic n'est jamais un verdict**
  (D15) : si M6 réclame un inventaire du code écarté dans le rapport d'audit,
  c'est D15 qui se rouvre, et son prix est toujours conservé.

### D21 — Multi-module à M5 : la topologie alimentée, les goals réacteur avec leur sortie — CONFIRMÉE (2026-08-03)

- **Contexte** : `ModuleNode`, `ModuleDescriptor` et `ModuleTopology` sont livrés
  et testés au modèle depuis M1 et n'ont **aucun producteur** — le frontend n'en
  construit jamais. S5 structurel a été reporté de M3 à M5 pour cette raison
  (« modules non alimentés avant l'hôte »). La carrière porte des goals
  `reactor-generate`/`reactor-audit` et un participant de cycle de vie.
- **Options** : **A** mono-module complet + topologie alimentée + règle S5
  structurelle, les goals réacteur suivant leur sortie ; **B** tout le
  multi-module à M5.
- **Décision** (utilisateur) : **A**. M5 livre l'hôte mono-module complet,
  alimente la topologie quand l'hôte connaît le réacteur, et branche la règle S5
  structurelle. L'analyse unifiée, l'agrégation et le routage arrivent avec ce
  qu'ils agrègent ou routent : **M6** pour le rapport agrégé, **M7** pour la
  génération.
- **Impact** : la règle S5 se teste au moteur sur un modèle porteur de modules,
  sans hôte ; sur le corpus (mono-module, sans modules) elle est **muette**, et
  le plancher ne doit pas bouger — s'il bouge, c'est une régression. Les goals
  `reactor-*` de la carrière restent une spécification pour M6/M7, pas un
  livrable de M5.

### D17 — Le CLI n'est pas un livrable de la 7.0.0 ; la restitution est indépendante de l'hôte — CONFIRMÉE (2026-08-03)

- **Contexte** : le doc 08 plaçait `hexaglue-cli` (`analyze`, `explain`, `audit`)
  au jalon M4. Deux objections à l'ouverture du jalon : le CLI est un **hôte**,
  et toutes les décisions d'hôte sont à M5 (YAML strict, racines de sources,
  canal de diagnostics, sinks) — l'écrire d'abord, c'est l'écrire deux fois ou
  préempter M5 depuis un module qui ne portera pas ces décisions ; et la
  justification que le doc 08 lui donnait est un argument de calendrier
  (« longue période sans démontrable »), pas de dépendance : aucun jalon aval ne
  l'attend. L'audit, lui, est un plugin de M6 : à M4 la commande n'aurait rien
  eu à lire.
- **Ce que l'utilisateur a énoncé** : HexaGlue sera principalement utilisé via
  le plugin Maven ; la possibilité d'un CLI reste ouverte si le besoin se fait
  sentir ; ce qu'il faut dès maintenant est un moyen d'obtenir les informations
  de classification qui puisse alimenter **aussi bien** les logs du plugin
  Maven que ceux d'un CLI ou d'autre chose.
- **Décision** (utilisateur) : M4 livre la **restitution**, pas un hôte. Le CLI
  n'est engagé sur aucun jalon ; il reste possible, et son coût est réduit à
  celui d'un hôte, la matière étant déjà là.
- **Impact** :
  - M4 devient « Explain : la restitution » (doc 08 amendé) : rendu du verdict,
    de ses raisons, de l'arbre de dérivation, et bilan agrégé d'un run.
  - Trois consommateurs réels, ce qui écarte l'interdit 07 §10.1 : le corpus
    d'acceptation aujourd'hui, le plugin Maven à M5, le rapport d'audit à M6.
  - **Le rendu est une feuille, jamais un étage** : les hôtes reçoivent une
    `List<String>`, et la structure reste lisible séparément (`ArchType`,
    `Classification`, `Outcome`). Aucun consommateur ne relit le texte —
    c'est ce qui tient l'interdit 07 §10.2 (pivot `String`) et la leçon 05-H2.
  - Si un CLI est décidé plus tard, il arrive après M5 et reprend les décisions
    d'hôte de M5 ; le tableau d'outillage du doc 07 §7 devra alors trancher le
    parsing d'arguments, question laissée ouverte ici faute d'objet.

### D15 — Code généré : écarté du modèle par le frontend — CONFIRMÉE (2026-08-02)

- **Contexte** : deux doctrines livrées se contredisaient. Le frontend
  (`AnalysisPerimeter.covers`, M2) écarte du `CodeModel` tout type portant
  `@Generated` (javax, jakarta, javax.annotation.processing, lombok) : le
  moteur ne le voit jamais et ne rend **aucun** verdict. Le référentiel
  (doc 09, « Ce qui reste UNCLASSIFIED ») prévoyait à l'inverse un verdict
  UNCLASSIFIED catégorisé alimenté par le fait S2 `GENERATED_CODE`, que les
  packs `platform` et `jakarta` émettent sur ces quatre mêmes marqueurs.
  Ouverte au lot 16, instruite au lot 20.
- **Options** : **A** le frontend garde la main (le code généré n'entre pas
  dans le modèle) ; **B** le moteur tranche (verdict plein) ; **C** mixte (au
  modèle, hors périmètre de verdict, visible à l'inventaire).
- **B écartée par la mesure.** Sonde sur le moteur livré, port et service
  écrits à la main plus la sortie du plugin JPA remise dans le périmètre :
  l'adapter généré implémente le port, R4 exige « rien du cœur ne l'implémente »
  donc le port tombe en UNCLASSIFIED, et R6 n'ayant plus de port sur quoi
  pivoter, le service applicatif tombe avec lui — l'adapter généré étant lu à sa
  place comme la couche applicative. **La deuxième exécution sur des sources
  inchangées ne rend pas le même modèle** : l'invariant 1 du doc 07 est attaqué
  de biais. Ce n'est pas théorique : un outil de build passe à l'analyse toutes
  ses racines de compilation, `target/generated-sources` compris.
- **Décision** (utilisateur) : **A**, et rien n'est supprimé. Ce qui devait être
  tranché avant le lot 21 est la seule question mûre — le code généré ne
  participe pas à la dérivation et ne reçoit ni verdict ni `ArchType`. La
  reconnaissance du code généré est une question de **périmètre de lecture**,
  réglée avant qu'un fait soit énoncé, et non une classification anticipée.
- **Instruction qui a écarté C** : le rapport d'audit livré ne perd rien.
  `InventoryTotals` (carrière) ne compte que des kinds classifiés — **ni total
  de types, ni ligne « unclassified »**, donc aucun dénominateur que le code
  généré fausserait ; `HealthScoreCalculator` note des packages, pas des types.
  Le récit de l'étude de cas nomme d'ailleurs les adaptateurs générés depuis le
  côté générateur, qui sait ce qu'il a écrit. La seule chose que C achetait —
  transporter un décompte jusqu'aux plugins via l'`ArchModel` — coûtait un
  **5ᵉ amendement du contrat M1**, pour un besoin que le produit n'exprime pas.
- **Le prix de l'option est conservé** : les quatre entrées `GENERATED_CODE`
  des packs `platform`/`jakarta` et la valeur `KnowledgeFact.GENERATED_CODE`
  **restent en place**, commentées comme sans sujet possible aujourd'hui. Si
  M6 décide que l'inventaire du code généré fait partie du produit d'audit, C
  devient un petit pas et non une re-litigation.
- **Impact** : le lot 21 ne catégorise pas le code généré (il n'en voit aucun) ;
  la phrase du doc 09 sur `GENERATED_CODE` → catégorie est amendée ; la question
  « où est passé mon type ? » relève du **canal de diagnostics du frontend**,
  écart assumé de M2 à trancher à M5 avec l'hôte — un diagnostic, pas un
  verdict. Parité M8 par construction : l'ancien moteur écarte lui aussi le code
  généré. Les trois scénarios de corpus qui épinglaient le comportement livré
  (`= NO VERDICT`) deviennent la référence, et non plus une divergence.

### D13 — Posture du moteur quand seul un signal faible parle : dissoute par retrait du nommage de la posture par défaut — CONFIRMÉE (2026-08-02)

- **Contexte** : les capteurs S4 (forme) et S6 (nom) livrés, restait à trancher
  ce que le moteur répond sur un type dont **rien d'autre** ne parle : conclure
  sur le signal faible (option A, en vigueur aux lots 13-14), se taire en
  conservant les candidats (B), ou seuil configurable (C). L'instruction sur
  corpus a montré que la question était mal posée : 39 des 118 verdicts rendus
  sur le profil 1 reposaient sur le seul suffixe, **dont 3 faux** (interfaces
  marqueurs lues DOMAIN_EVENT) et une famille entière fausse sur le parc réel
  (adapters `*Repository`/`*Gateway` lus DRIVEN_PORT) ; et 73 des 122 scénarios
  posent au moteur un type unique isolé — une question à laquelle aucun moteur
  contextuel ne peut répondre. Le nommage était retombé en position
  structurante, l'écueil n°1 de la carrière (doc 06 §2.1), reproduit à un
  étage plus propre.
- **Décision** (utilisateur) : **aucune des trois options — la prémisse est
  retirée.** Le rôle d'un type est une position dans un graphe, pas une
  propriété de sa déclaration : la classification repose sur les ancres et la
  dérivation relationnelle du référentiel
  [09-referentiel-regles.md](09-referentiel-regles.md). Le vocabulaire de
  nommage (S6) et le nommage de packages (moitié conventionnelle de S5)
  **sortent de la posture par défaut** jusqu'à la fin de M3 ; quand rien ne
  parle, le verdict est UNCLASSIFIED catégorisé, candidats conservés,
  remédiation explicite (élargir le périmètre, câbler le type, ou le déclarer
  via S1). La nécessité du nommage sera **mesurée** en fin de M3, pas
  présumée.
- **Impact** :
  - `ClassificationConfig.defaults()` ne porte plus de suffixes : le
    vocabulaire devient opt-in. Le code reste livré et testé
    (`ConventionalName`, `namingSuffixes`) — c'est la posture qui change, pas
    la capacité. `Aggregator.decideOne` est inchangé (le chemin du silence
    existe). *Appliqué au lot 15* : préset `conventional()` +
    `conventionalNamingSuffixes()` ; `silent()` retiré, `defaults()` produisant
    désormais exactement la même configuration.
  - Les attentes de corpus reposant sur le seul suffixe sont ré-arbitrées vers
    UNCLASSIFIED — le verdict de la carrière, pour la bonne raison cette
    fois ; le plancher du cliquet est recompté. *Mesuré au lot 15* : **trois**
    et non deux — les deux relues au lot 14
    (`ConflictDetectionTest-interfaceWithoutMarkersIsUnclassified`,
    `DomainClassifierTest-shouldHandleInterfaceTypes`) plus
    `PortClassifierTest-shouldHandleRecordTypes`, dont la relecture s'appuyait
    sur la forme *et* le nom ; sans le nom, le duel S4 IDENTIFIER/VALUE_OBJECT
    reste ouvert, faute d'un port qui en fasse une clé de recherche. Plancher
    inchangé à 20/18.
  - **Corpus réorienté** : les 73 scénarios mono-type deviennent des fixtures
    de silence honnête (attente UNCLASSIFIED, épinglant l'anti-règle « toute
    interface n'est pas un port ») ; le poids du corpus bascule sur des
    scénarios contextuels (golden files multi-types, `examples/`, case
    studies, puis profils 2-3).
  - **S5 est scindé** : la moitié structurelle (dépendances inter-modules,
    `ModuleRole`) reste une ancre au plan ; la moitié conventionnelle (mots
    dans les packages, `ports.in`) suit le sort de S6.
  - **Protocole de réévaluation, fin M3** : corpus 3 profils + exemples réels,
    vocabulaire éteint vs allumé ; pour chaque écart, compter *gain* (le nom
    rejoint l'arbitrage humain) et *dégât* (il le contredit). Le vocabulaire
    ne revient en posture par défaut que si le gain domine nettement sur les
    profils 2-3. L'issue sera consignée ici.
  - **ISSUE MESURÉE (lot 23, 2026-08-03, commit `05fda64`) : le vocabulaire
    reste opt-in.** Harnais `NamingVocabularyTest` — les 154 scénarios relus
    passés deux fois dans la chaîne complète, `defaults()` puis
    `conventional()` ; rapport commité en golden
    (`golden/naming-vocabulary.txt`).
    - **Profils 2 et 3 : aucun verdict ne bouge.** Zéro gain, zéro dégât. La
      clause exigeait que le gain domine *nettement* sur ces deux profils ;
      il y est nul, donc la posture ne change pas. Là où le vocabulaire a
      pourtant de quoi parler (`OwnerRepository`, `OwnerService`), il dit ce
      que la position disait déjà : sur du code câblé, le nom est redondant.
    - **Profil 1 : 55 dégâts, 0 gain, sur 47 scénarios.** Tous de la même
      forme, `UNCLASSIFIED → un kind` : le nom parle là où le relu dit que le
      silence est la réponse. Plusieurs sont faux au fond, pas seulement
      bruyants — interfaces marqueurs lues DOMAIN_EVENT, un type de
      `ports.in` lu DRIVEN_PORT, `AbstractService` lu APPLICATION_SERVICE,
      `PlaceOrderUseCase` lu DRIVING_PORT dans le scénario même qui affirme
      qu'il ne doit pas l'être.
    - **À ne pas sur-lire** : ces 55 portent sur des fixtures mono-type
      héritées des tests unitaires de la carrière, pas sur du code de
      production. Ce qu'elles établissent est le mode d'échec que D13 nommait
      (le nom conclut sur des types isolés), pas que le nommage nuirait sur du
      code réel — sur du code réel, il ne fait simplement rien.
    - **Les trois résidus du doc 09 §6 sont couverts par la mesure** :
      départager le duel d'un wrapper non possédé (S6 le tranche — et le relu
      dit que le duel doit rester ouvert quand rien ne câble le type),
      étiqueter COMMAND/QUERY_HANDLER (une occurrence, comptée en dégât),
      conclure sur les types définitivement isolés (les 55).
    - **Limites de la mesure, énoncées dans le rapport** : elle compare des
      *verdicts*, pas les candidats ni la confiance derrière eux ; elle ne
      couvre que le corpus relu — les `examples/` vivent dans la carrière
      gelée et ne sont pas branchés au nouveau réacteur.
    - Le code (`ConventionalName`, `conventional()`,
      `conventionalNamingSuffixes()`) reste livré et testé : c'est la posture
      qui est confirmée, pas la capacité qui est retirée. Le harnais reste au
      réacteur — une règle qui ferait gagner ou perdre du terrain au nommage
      se verra dans le diff du golden le jour où elle arrive.
  - Le plan de M3 se ré-étalonne sur les vagues du référentiel : W1 (adapters
    depuis les ancres S2) → R4/R5 (ports par position) → R6/R8 (application)
    → R1b/R2/R3/R7 (domaine par cycle de vie), chaque vague apportant ses
    scénarios câblés au corpus.

### D14 — Signal d'un port driving : structurel en base, faits de corps en renfort — CONFIRMÉE (2026-08-02)

- **Contexte** : R5 (doc 06 §3.2) reconnaît un port driving à ce qu'il est
  « implémenté par le cœur et **appelé depuis une ancre driving** ». Savoir qui
  appelle quoi demande les faits de corps de méthode, que le frontend n'extrait
  que sous la capacité optionnelle `METHOD_BODIES`. Options : signal structurel
  seul (A) ou exiger la capacité (B).
- **Décision** (utilisateur) : **A, étagé** — absorbée par la règle R5 du
  référentiel (doc 09 §4, vague 2). La base est structurelle : interface
  implémentée par un type du cœur **et** détenue par un DRIVING_ADAPTER (champ
  ou paramètre de constructeur). Quand `METHOD_BODIES` est présent, les arêtes
  `INVOKES` émettent des évidences S3 **supplémentaires** sur le même port —
  la capacité renforce la conclusion, elle ne conditionne jamais la règle.
- **Impact** :
  - Le moteur ne dépend d'aucune option de l'hôte pour reconnaître un port
    driving ; les appels statiques et usages en corps seuls sont couverts
    uniquement quand la capacité est là — manque assumé et documenté.
  - La direction est purement relationnelle (le cœur implémente + le bord
    appelle → DRIVING ; le cœur consomme → DRIVEN) : plus aucun départage par
    suffixe ou package. Anti-règle associée : une interface implémentée **et**
    consommée par le cœur n'est pas un port (contrat interne, candidat
    DOMAIN_SERVICE).
  - Sur le parc en migration, l'absence de ports driving (contrôleurs appelant
    directement les services) ne produit aucun faux verdict : aucune règle ne
    mord, et c'est le finding « le bord court-circuite les ports » (famille
    HG-PORT, M6) qui porte le diagnostic.

### D1 — Unification de l'abstraction de parsing : Option B — CADUQUE (2026-08-01, remplacée par D12)
- **Contexte** : deux abstractions concurrentes ; celle publiée
  (`hexaglue-syntax`) est morte, le core utilise `core.frontend` + Spoon direct
  (audit [03-architecture-modules.md](03-architecture-modules.md), C1/C2).
- **Décision** (utilisateur) : **Option B** — supprimer `hexaglue-syntax-spoon`
  et l'abstraction `SyntaxProvider`/`TypeSyntax` ; assumer le couplage
  core→Spoon ; déplacer les DTO vivants (`TypeRef`, `Modifier`,
  `SourceLocation`, `TypeForm`) dans `hexaglue-arch`.
- **Impact** : la « cible » décrite dans le doc 03 (option A) et la Phase 4 du
  README sont amendées en conséquence : plus de promotion de `core.frontend`,
  le module `hexaglue-syntax` disparaît du reactor à terme. Les downcasts
  `instanceof SpoonMethodAdapter` (C2) restent à résorber par des méthodes
  d'interface sur `JavaMethod`, même en option B. Le garde-fou « core sans
  Spoon » est remplacé par « Spoon confiné au package `core/frontend` »
  (règle ArchUnit).
- **Caducité** : D12 gèle l'ancien réacteur en lecture seule ; l'opération
  prescrite n'aura pas lieu. Le design des `AnnotationValue` typés de
  `hexaglue-syntax-spoon` reste exploitable comme référence de récolte
  (doc 08, jalon M1).

### D2 — Module `hexaglue-testing` : suppression — CADUQUE (2026-08-01, remplacée par D12)
- **Décision** (utilisateur) : supprimer le module (aucun consommateur, jamais
  une API publique assumée). Intégré au périmètre de la Phase 1.
- **Caducité** : sans objet sous D12 — le nouveau réacteur ne le contient
  simplement pas.

### D3 — Retrait direct des API publiées mortes — CADUQUE (2026-08-01, remplacée par D12)
- **Décision** (utilisateur) : suppression pure dans la version majeure (D4),
  sans cycle de dépréciation. Concerne `spi/core/ResolutionConfig`,
  `spi/classification/*` (sauf `PrimaryClassificationResult`),
  `spi/enrichment/*`, le module `hexaglue-syntax-spoon`, et les 16 méthodes
  mortes d'`ArchitectureQuery`.
- **Impact** : Phase 1 débloquée sur tout son périmètre.
- **Caducité** : sans objet sous D12 — l'ancien réacteur n'est plus modifié ;
  les API mortes disparaissent avec lui à la bascule (doc 08, jalon M8).

### D4 — Numérotation cible : 7.0.0 — CONFIRMÉE (2026-07-31)
- **Décision** (utilisateur) : le chantier est livré comme une version majeure
  unique **7.0.0** (core) / **4.0.0** plugins si le versionnage séparé des
  plugins est conservé — à préciser au moment de la release. La 6.1.1-SNAPSHOT
  n'est jamais releasée.
- **Impact** : les breaking changes sont assumés d'emblée ; pas de release
  intermédiaire 6.2.

### D0 — Sources de vérité du chantier — CONFIRMÉE (2026-07-31)
- **Décision** (utilisateur) : la documentation existante ne sert pas de base ;
  elle contient des faits erronés et des décisions caduques. Font foi : le code,
  l'audit 2026-07-31, ce registre. Voir CHANTIER.md § Sources de vérité.
- **Impact** : toute affirmation issue de la doc doit être re-vérifiée dans le
  code avant d'être utilisée (règle déjà présente dans CLAUDE.md, désormais
  étendue explicitement aux sections architecture de CLAUDE.md lui-même).

### D5 — Gel des releases pendant le chantier — CONFIRMÉE (2026-07-31)
- **Décision** : aucun `make release` / `mvn deploy` / `mvn release:*` /
  `git push --force` pendant le chantier. Bloqué mécaniquement par
  `.claude/settings.json` (deny) + hook `block-release-commands.sh`.
  `make release-check` (dry-run) reste autorisé.
- **Impact** : levée du gel = décision explicite à consigner ici + retrait du
  hook.

### D12 — Stratégie v7 : réécriture ancrée — CONFIRMÉE (2026-08-01)
- **Contexte** : les docs 06 (cible fonctionnelle) et 07 (architecture page
  blanche) définissent une cible ; deux voies possibles : réécriture ou
  migration progressive en place.
- **Décision** (utilisateur) : **réécriture ancrée** — nouveau réacteur
  construit selon le doc 07 ; corpus de tests, exemples et savoir-faire des
  plugins transplantés ; B1-B15 convertis en tests de régression ; ancien
  réacteur `hexaglue/` gelé en **référence lecture seule** jusqu'au gate de
  parité ; livraison en 7.0.0 (D4).
- **Impact** :
  - Les phases 0-5 du chantier sont remplacées par le plan de construction
    [08-plan-reecriture-ancree.md](08-plan-reecriture-ancree.md) (jalons
    M0-M8). Leur contenu est recyclé en intrants (bugs → tests de
    régression, inventaire du code mort → exclusions de récolte,
    garde-fous → jalon M0).
  - D1, D2, D3 deviennent caduques ; D6, D9, D11 sont confirmées par voie de
    conséquence ; D7, D8, D10 restent en attente (à trancher au plus tard à
    M3).
  - Nouveau code : `hexaglue-next/` à côté de `hexaglue/` (défaut,
    déplaçable en dépôt dédié sans impact sur le plan).
  - D5 (gel des releases) demeure jusqu'au gate de parité (M8) et s'applique
    aux deux réacteurs.

### D6 — Moteur à évidences hiérarchisées et point fixe — CONFIRMÉE (2026-08-01, par D12)
- **Décision** : option A (refonte) — le moteur du nouveau réacteur est le
  solveur à saturation du doc 07 §4.1 : évidences S1-S6, strates,
  agrégateur lexicographique avec marge, propagation jusqu'au point fixe,
  arbre de preuve par conclusion.

### D9 — Types externes dans le graphe — CONFIRMÉE (2026-08-01, par D12)
- **Décision** : le CodeModel du nouveau frontend inclut des nœuds externes
  légers, les arêtes vers le classpath et la fermeture de supertypes
  (doc 07 §3.1 ; G1/G4 du doc 06 §4) dès le jalon M2.

### D11 — Corpus d'acceptation à trois profils — CONFIRMÉE (2026-08-01, par D12)
- **Décision** : le corpus (style HexaGlue / entreprise JPA-Spring Data /
  sans conventions de nommage) est le critère d'acceptation exécutable de la
  réécriture : parité exigée sur le profil 1, golden nouveaux sur les
  profils 2 et 3 (doc 06 §6 ; doc 08 §6).

### D7 — Domaine annoté JPA : posture unique, sans profil — CONFIRMÉE (2026-08-02)
- **Contexte** : sur une application « modèle unique » (domaine annoté
  `jakarta.persistence`), l'ancien moteur produit des contresens (B3).
  Le doc 06 §3.4 proposait deux profils de configuration, `strict` et
  `pragmatic`.
- **Décision** (utilisateur) : **pas de profil**. Une posture unique. La cible
  reste hexagonale/DDD — il ne peut pas y avoir d'annotation de persistance
  dans un domaine correct — mais HexaGlue analyse aussi des applications qui
  n'y sont pas encore : le verdict doit le dire, pas le masquer. Donc :
  l'annotation de persistance n'est **jamais** une évidence de kind, ni
  positive ni négative ; le kind vient de S3/S4 comme si elle n'existait pas ;
  un domaine couplé à la persistance produit un **finding**.
- **Impact** :
  - Le fait `PERSISTENCE_MODEL` (pack, S2) ne contribue à aucune décision de
    kind : il alimente un finding et rien d'autre.
  - Le discriminant entre « type du domaine couplé à la persistance » et
    « modèle de persistance interne à un adapter » est la **position dans le
    graphe**, pas l'annotation : référencé par l'hexagone ⇒ domaine + finding ;
    référencé seulement par un adapter ⇒ interne à l'adapter (D8 lui donne un
    kind). Une application déjà séparée ne produit donc aucun faux positif,
    sans configuration.
  - **La sévérité doctrinale se déplace dans la porte de validation** :
    `ValidationConfig.findingThresholds` (déjà au contrat M1) permet
    `HG-DDD-0xx: BLOCKER`. C'est ce que le profil `strict` aurait fait, avec un
    mécanisme qui existe déjà — d'où zéro clé de configuration nouvelle et
    aucune branche de profil dans le moteur.
  - Jalons : M3 le fait et la règle ; M6 le code de finding et sa remédiation
    type par type ; M7 un garde-fou de génération que
    `generation.minConfidence` ne couvre pas — ne pas générer de mapping JPA
    pour un type déjà annoté (double mapping), émettre un diagnostic.
  - **Remédiation par marquage jMolecules retenue** : le finding propose
    `org.jmolecules.ddd.annotation.AggregateRoot` sur le type de domaine
    (INFERRED → DECLARED) et
    `org.jmolecules.architecture.hexagonal.SecondaryAdapter` sur l'adapter de
    persistance. Prérequis constaté : `RemediationHint.addAnnotation` prend
    aujourd'hui un **nom simple** et produirait « Add @Entity » sur un type
    déjà porteur de `@jakarta.persistence.Entity` — le hint doit porter le FQN
    (correction M3 du contrat M1).
  - **Hors périmètre 7.0.0 — extraction du domaine depuis l'entité JPA**
    (scinder l'entité en type de domaine pur + entité de persistance +
    mapper). Écartée non par manque de valeur mais par nature : elle écrit
    dans les sources de l'utilisateur (non régénérables, à relire) et non dans
    les sources générées, elle inverse le sens du pipeline, et elle exige des
    arbitrages hors de portée d'un outil non interactif (propriétaire des
    associations bidirectionnelles, chargement paresseux, stratégies
    d'héritage, ids embarqués, constructeur sans argument contre record
    immuable, réécriture des sites d'appel). Son vecteur serait une commande
    de migration CLI ou une recette de réécriture de sources, pas un goal du
    cycle de build. Ce que le contrat rend déjà possible sans elle : un
    finding portant les points de couplage et le geste d'extraction, type par
    type — soit le plan de migration sans la chirurgie.

### D8 — Classifier les adapters existants : oui — CONFIRMÉE (2026-08-02)
- **Contexte** : `DRIVING_ADAPTER`/`DRIVEN_ADAPTER` existaient dans
  `ElementKind` de la carrière sans jamais être produits ; faute de couverture
  du modèle, le plugin audit re-classifie par package
  (`qualifiedName.contains(".application.")`, 05-C1) et la living-doc fait de
  même (05-C2/B9). Le nouveau `ArchKind` ne les porte pas : écart assumé à la
  clôture M1, explicitement suspendu à cette décision.
- **Décision** (utilisateur) : **oui**. Les adapters présents dans les sources
  sont classifiés.
- **Impact** :
  - **Amendement du principe « hors périmètre »** de CLAUDE.md : un adapter
    **présent dans les sources** est classifié (c'est l'intrant de l'audit et
    de la migration) ; un adapter **généré** est une sortie, jamais un intrant
    de classification. La génération est inchangée.
  - `hexaglue-model` (contrat M1, non publié — D5) : deux valeurs
    `DRIVING_ADAPTER`/`DRIVEN_ADAPTER` dans `ArchKind` (+ `isAdapter()`), une
    quatrième branche scellée dans `ArchType` aux côtés de
    `DomainType`/`PortType`/`ApplicationType`/`UnclassifiedType`, et sa
    couverture dans `KindCoherence`. Coût borné aujourd'hui, changement
    cassant après la 7.0.0 : c'est la raison de trancher maintenant.
  - Débloque le garde-fou du doc 07 §10.5 (« un plugin qui re-dérive une
    information du modèle fait échouer le build », ArchUnit/PMD) : sans
    couverture des adapters, l'audit de M6 n'aurait eu que le choix entre
    violer la règle et perdre la fonctionnalité.
  - Les adapters sont décidés à partir des ancres frameworks des packs
    (`DRIVING_ENTRYPOINT`, `INFRA_DEPENDENCY`), pas du nommage ni du package.
  - Donne sa zone d'atterrissage au cas D7 « modèle de persistance interne à
    un adapter ».

### D10 — Annotations HexaGlue propres : non — CONFIRMÉE (2026-08-02)
- **Contexte** : aucune annotation `io.hexaglue.*` n'existe dans aucun des deux
  réacteurs ; l'intention déclarée (S1) passe par jMolecules ou par la
  configuration. Un module d'annotations propre = surface publique
  supplémentaire à maintenir indéfiniment.
- **Décision** (utilisateur) : **non**. L'intention déclarée passe par les
  annotations et interfaces **jMolecules** (par FQN exact) et par la
  configuration explicite du `hexaglue.yaml`. **Rien n'est inscrit au backlog
  post-7.0.0** : la question est close, pas différée.
- **Impact** :
  - Zéro artefact publié nouveau, zéro dépendance de compilation imposée au
    domaine des utilisateurs. Asymétrie assumée : ne pas publier reste
    réversible, publier ne l'est pas.
  - Cohérence doctrinale : HexaGlue audite la pureté du domaine ; jMolecules
    est neutre et conçu pour porter l'intention, là où des annotations propres
    coupleraient le domaine à l'outil d'audit puis devraient être déclarées
    neutres dans nos propres packs.
  - Le corpus profil 1 déjà porté dans `hexaglue-testkit` s'appuie sur 8 FQN
    jMolecules (`ddd.annotation.*`, `ddd.types.*`, `architecture.hexagonal.*`,
    `event.annotation.*`) : entrées du pack jMolecules à M3.
  - **Manque constaté à combler à M3** : le vecteur S1 « configuration
    explicite » n'a pas de porteur — `HexaGlueConfig` n'a que
    `analysis`/`validation`/`generation`, alors que
    `RemediationHint.configureExplicit` et `EvidenceTier.DECLARED_INTENT` le
    présupposent. `classification.explicit` est à ajouter à la configuration
    typée.
