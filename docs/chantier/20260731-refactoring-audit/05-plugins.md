# Les 4 plugins : JPA, REST, living-doc, audit

État de la règle d'or (« les plugins consomment le modèle classifié, ils ne
classifient jamais ») : respectée sur le cœur (DomainIndex, PortIndex,
FieldRole, DrivenPortType correctement consommés), mais percée sur les bords —
toujours au même endroit : quand une information manque au modèle, le plugin la
re-dérive par nommage au lieu de la faire remonter dans arch/core.

Le code mort du plugin audit (~4 000 l.) et `jpa/extraction` (1 036 l.) sont
dans 01-code-mort.md.

## CRITICAL

### C1 — `ApplicationPurityValidator` classe par package des types hors périmètre
`audit/adapter/validator/hexagonal/ApplicationPurityValidator.java:180-182` :
`qualifiedName.contains(".application.")`, utilisé (`:160-169`) pour auditer des
types **absents du registry** — le plugin décide qu'un type est « application »
là où le core a décidé qu'il ne l'était pas. Une couche nommée `usecase`/`app`
n'est jamais auditée ; un package `.application.` de test l'est.
**Refactoring** : supprimer la seconde passe ; ne consommer que
`typeRegistry.all(ApplicationType.class)` (déjà présent `:148-149`). Si l'audit
des exclus est un vrai besoin : le core doit l'exposer (UnclassifiedType
OUT_OF_SCOPE, ModuleRole, ou `ArchitectureQuery#typesInLayer`).

### C2 — `BoundedContextDetector` (living-doc) re-classifie, et diverge du core (= B9)
`livingdoc/content/BoundedContextDetector.java:146-156` : `segments[2]` du
package, alors que `ArchitectureQuery#findBoundedContexts()` existe et est
consommé par le plugin audit. Les deux heuristiques divergent (core :
`Optional.empty()` si < 3 segments ; living-doc : dernier segment) → deux
documents du même build décrivent une topologie différente.
**Refactoring** : supprimer le detector, consommer
`context.architectureQuery().findBoundedContexts()`.

### C3 — Deux `toSnakeCase` divergents → mots réservés SQL non échappés (= B2)
`jpa/model/JpaModelUtils.java:90-96` (1 regex) vs
`jpa/util/NamingConventions.java:116-128` (2 regex + gestion `XMLParser`).
`PropertyFieldSpec:138` utilise la version JpaModelUtils, court-circuitant
`NamingConventions.toColumnName()` et sa liste `SQL_RESERVED_WORDS` (suffixe
`_col`). Émis tel quel dans `@Column(name=…)` par `JpaEntityCodegen:267,276` et
`JpaEmbeddableCodegen:147,153` → un champ `order`/`value`/`key`/`group` casse le
DDL. La Javadoc de JpaModelUtils est fausse (« XMLParser → xml_parser » :
l'impl produit `xmlparser`).
**Refactoring** : supprimer `JpaModelUtils.toSnakeCase` ;
`PropertyFieldSpec:138` → `NamingConventions.toColumnName(...)` ; test
paramétré sur les 22 mots réservés.

### C4 — `AuditPlugin` SPI incohérent + méthode `audit()` morte dupliquée
Le SPI déclare `audit(AuditContext)` abstraite puis fait de `execute()` (qui
devrait l'appeler) une méthode à surcharger qui lève
`UnsupportedOperationException`. `DddAuditPlugin.audit():176-214` (jamais
appelée) duplique à 90 % `executeDomainAudit():541-579`. Deux corrections de
bug à faire, une seule sera faite.
**Refactoring** : corriger le SPI (`execute()` par défaut construit
l'AuditContext et appelle `audit()`) ou retirer `audit()` de l'interface.

## HIGH

### H1 — État mutable dans des singletons ServiceLoader
`DddAuditPlugin:140,411` et `LivingDocPlugin:91,106` stockent
`this.archModel = context.model()` sur des instances réutilisées. En mode
réacteur multi-module, écrasement/fuite de modèle entre modules. Le commentaire
`:395` « The execution flow is stateless » est faux. `JpaPlugin:135` et
`RestPlugin:104` sont, eux, sans état.
**Refactoring** : `GeneratorContext.pluginContext` non-null (le fallback v4 n'a
plus lieu d'être en 6.x) ; supprimer les champs.

### H2 — L'audit re-parse ses propres messages texte
`ReportDataBuilder.extractCyclePathFromMessage:652-667` re-parse
« Circular dependency between aggregates: A -> B -> A » que
`AggregateCycleValidator` vient de sérialiser depuis une liste ;
`IssueEnricher:103,214,383,398` redevine la direction du port par
`message().startsWith("Driving")` / `contains("multiple aggregates")`.
Toute reformulation d'un message casse silencieusement diagrammes et
remédiation ; aucun test ne le détecte.
**Refactoring** : porter la sémantique dans des `Evidence` typées (le mécanisme
existe : StructuralEvidence, RelationshipEvidence, DependencyEvidence —
sous-utilisé) ; interdire la lecture de `message()` pour la logique.

### H3 — `FORBIDDEN_PREFIXES` + `categorizeDependency` dupliqués caractère pour caractère
`ddd/DomainPurityValidator:68-107,202-235` ≡
`hexagonal/ApplicationPurityValidator:80-119,258-295` (19 préfixes + ~35 lignes
de catégorisation, commentaires inclus). Liste en dur, non configurable — un
projet utilisant légitimement `jakarta.validation` dans le domaine ne peut pas
l'autoriser.
**Refactoring** : `InfrastructurePrefixes` partagé, alimenté par PluginConfig
(`forbiddenPrefixes` / `allowedPrefixes`).

### H4 — Identité par suffixe `Id` alors que le modèle porte l'information
`jpa/model/AdapterMethodSpec:311-315` (`"id".equals(name) || name.endsWith("Id")`
sur le nom du paramètre alors que le type est confrontable à
`domainIndex.identifiers()` — REST fait correct via `StrategyHelper:53-64`) ;
`jpa/model/PropertyFieldSpec:175` ajoute `typeQualifiedName.endsWith("Id")`
par-dessus deux booléens déjà dérivés du domainIndex — un `Identifier` nommé
`OrderKey` n'est pas reconnu comme FK.
**Refactoring** : comparaison de types via DomainIndex ; supprimer le
`endsWith("Id")` résiduel.

### H5 — Couplage au nom d'un critère interne du core + classification dans JPA
`JpaPlugin:439-442` : `"contained-entity".equals(classification().winningCriterion().name())`
décide entité enfant vs autonome — renommer le critère côté core casse la
génération JPA sans aucun signal. `JpaPlugin:406-419` : un type **non classifié**
avec un champ `"id"` est promu entité enfant — classification dans un plugin.
**Refactoring** : exposer `AggregateRoot#containedEntities()` (ou un
CompositionIndex fiable) et remplacer les 3 mécanismes de découverte
(`:363-459`, 95 l.) par une lecture du modèle ; le fallback `getField("id")`
devient un diagnostic + RemediationHint.

### H6 — Deux implémentations Mermaid concurrentes + DSL morte
audit : 12 builders en StringBuilder brut (ClassDiagramBuilder 502,
C4DiagramBuilder 422, FullArchitectureDiagramBuilder 484…). living-doc : une
DSL propre (`MermaidBuilder` avec sanitize/escape) dont les composants
ClassDiagramBuilder/ClassBuilder sont **morts** — le rendu vivant
(`DiagramRenderer`, 625 l.) est en StringBuilder brut sans échappement.
Deux paires de classes homonymes entre plugins (ClassDiagramBuilder,
DiagramGenerator).
**Refactoring** : promouvoir la DSL en module commun ; migrer DiagramRenderer
et les 12 builders audit dessus.

### H7 — Rendu de type Mermaid dupliqué ×3, avec bug
`livingdoc/util/TypeDisplayUtil:48-67` ne simplifie pas les génériques
(`Optional<com.example.Order>` reste qualifié) ; la bonne implémentation
récursive existe côté audit (`MermaidTypeConverter:59-85`, sur TypeRef) ; 3e
variante : `DiagramRenderer.extractBaseType:602-612`.
**Refactoring** : MermaidTypeConverter en commun, supprimer les 2 autres.

### H8 — `DddAuditPlugin` : 18 méthodes copiées-collées (~410 l.)
9 surcharges `toCodeUnitV5` + 9 `extractDependenciesV5` (`:929-1338`), qui ne
diffèrent que par le type du paramètre et — divergence probablement
involontaire — la présence des annotations (incluses pour AggregateRoot/Entity/
ApplicationService, absentes pour ValueObject/Identifier/DomainEvent/
DomainService). `buildCodebaseFromModel:844-924` : 9 boucles identiques.
**Refactoring** : un `toCodeUnit(ArchType)` + `roleOf` en switch exhaustif sur
`ArchKind` ; annotations toujours incluses. Gain ~350 l. (1 339 → ~950).

### H9 — Routage multi-module et écriture JavaPoet réimplémentés par plugin
`resolveEffectiveTargetModule` : `JpaPlugin:204-238` ≡ `RestPlugin:277-309`
(à un ModuleRole près) ; `writeJavaSource` + `toJavaSource` dupliqués. ~70 l.
de logique subtile (priorité config > auto-routage > défaut) ×2.
**Refactoring** : `ModuleTargetResolver` + `GeneratorContext#emit(TypeSpec, pkg)`
dans le SPI. Chaque futur plugin générateur en profite.

## MEDIUM

- **M1** `simpleName(fqn)`/`packageOf(fqn)` réimplémentés **25 fois** dans les
  4 plugins alors que `TypeId.simpleName()/packageName()` gèrent aussi les
  classes imbriquées (`Order$OrderLine`). → règle PMD sur `lastIndexOf('.')`.
- **M2** `capitalize`/`decapitalize` ×8 (2 publiques quasi identiques
  jpa/rest + 6 privées) ; `toSnakeCase`/`toKebabCase` : même paire de regex.
- **M3** `TypeRef → TypeName` (JavaPoet) ×4, dont 2 identiques dans REST
  (`StrategyHelper:79-99` ≡ `DtoFieldMapper:269-289`) ; `TypeMappings` importe
  `arch.model.ir.TypeRef` alors que le modèle v5 fournit `syntax.TypeRef`
  (dette de migration).
- **M4** `SubResourceActionStrategy.EXCLUDED_PREFIXES:46-69` : recopie manuelle
  des préfixes de Create/Update/DeleteStrategy — ajout d'un préfixe ailleurs
  casse silencieusement la priorité des stratégies. → `HttpVerbVocabulary`
  partagé + configurable via RestConfig.
- **M5** L'audit court-circuite `CodeWriter` (`Files.writeString` direct,
  `DddAuditPlugin:649-728`) : ignore le répertoire docs configuré, le routage
  multi-module et le log d'écrasement 5.1.0 ; rapports écrits sous
  `generated-sources` (sémantiquement faux). LivingDoc fait correct
  (`writer.writeDoc`).
- **M6** `AuditConfiguration.fromPluginConfig` : stub retournant toujours
  `Map.of()` — la fonctionnalité documentée `audit.severity.ddd:entity-identity=BLOCKER`
  n'existe pas ; l'objet est transporté partout sans être lu. → implémenter
  `PluginConfig#getStringMap` ou supprimer.
- **M7** `JpaConfig` : défaut `ASSIGNED` vs doc `IDENTITY` (= B15) ; `valueOf`
  sans message utile ; `defaults()` duplique les 14 valeurs de `from()` ;
  visibilités incohérentes entre les 4 configs. → `getEnum(key, class, default)`
  dans le SPI + `defaults() = from(PluginConfig.empty())`.
- **M8** Cardinalité par nom de type (`DomainContentSelector:360-373` : List/Set
  hardcodés, un `Deque` non reconnu) au lieu de `FieldRole.COLLECTION` /
  `Field.elementType()` ; idem `JpaAnnotationExtractor:449-450`.
- **M9** `{"createdAt","updatedAt"}` ×3 dans JPA ; usage détourné dans
  `MapperSpecBuilder:932-935` (classifie un paramètre de constructeur du
  domaine comme audit par son nom au lieu de `FieldRole.AUDIT`).
- **M10** Vocabulaire métier anglais en dur : `BOOLEAN_TOGGLE_PATTERNS`
  (`MapperSpecBuilder:695-703` : `active→deactivate`…) — un domaine français ou
  utilisant `close()` perd silencieusement l'état ;
  `C4ContextDiagramBuilder:113-133` invente la topologie externe par
  `contains("payment")`/`contains("redis")` alors que `DrivenPortType` est
  disponible.
- **M11** Renderers audit : le pivot `ReportData` partagé est sain (pas de
  duplication d'extraction), mais libellés divergents entre Markdown et HTML,
  `extractPluginShortName` dupliqué, et `JsonReportRenderer` (834 l.) sérialise
  le JSON à la main avec échappement incomplet. → ReportLabels partagés +
  sérialiseur JSON réel ; `SimpleTemplateEngine` du SPI (inutilisé par les
  4 plugins) pour le HTML/CSS inline de `HtmlRenderer` (957 l.).
- **M12** `JpaAnnotations` (938 l.) mélange JPA + Spring Data + MapStruct →
  scinder en 3 catalogues.
- **M13** God classes à découper : `MapperSpecBuilder` (1 365 → builder +
  ValueObjectMappingDetector + ReconstitutionResolver +
  ChildEntityMappingDetector + AfterMappingDetector) ; `ReportDataBuilder`
  (1 156 → 1 builder par section, en corrigeant au passage
  `determineTheme:832-861` qui route par `theme.contains("Domain")` alors que
  `constraintId` est structuré) ; `IssueEnricher.buildDefaultTemplates` :
  **465 lignes** dans une seule méthode.

## LOW

- `RestPlugin.deriveApiPackage` vs `RestConfig.deriveApiPackage` : même nom,
  sémantiques différentes ; `RestConfig:144-156` devine le base package par
  convention alors que `model.project().basePackage()` existe.
- `JpaEntityCodegen.extractTargetSimpleName:571-577` : suffixes
  `Entity`/`Embeddable` en dur alors que `entitySuffix()`/`embeddableSuffix()`
  sont configurables — un `entitySuffix: Jpa` produit un nom faux.
- `ArtifactWriter.of(CodeWriter)` : 75 l. de redélégation pure → supprimer.
- `HexaGluePlugin.execute` Javadoc référence `PluginContext#ir()` supprimée
  en 4.0.0.

## Module commun proposé : `hexaglue-plugin-commons`

Dépend de spi + arch + javapoet ; consommé par les 4 plugins ; PAS dans le SPI
public (utilitaires d'implémentation, pas un contrat).

Priorité 1 (supprime des bugs) : `Naming` (capitalize/toSnakeCase/toKebabCase…),
`JavaTypes` (TypeRef→TypeName), délégué `TypeId` pour simpleName/packageName,
`mermaid/` (DSL + MermaidTypeConverter).
Priorité 2 : `markdown/` (déjà mature dans living-doc), `ModuleTargetResolver`,
`PluginConfigs` (getEnum/getStringMap), `HtmlEscape`/`JsonWriter`.
Priorité 3 — à remonter dans arch/core, PAS dans commons (ce sont des
informations de classification) : bounded contexts (existe déjà : l'utiliser),
couche applicative (ModuleRole/LayerClassification), entités contenues
(`containedEntities()`), identité (FieldRole + DomainIndex), cardinalité
(FieldRole.COLLECTION), sémantique structurée des violations (Evidence typées).
