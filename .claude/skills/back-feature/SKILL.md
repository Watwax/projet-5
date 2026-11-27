---
name: back-feature
description: Implémente une feature côté API Spring Boot (dossier back/) — entité JPA, repository, DTO, mapper écrit à la main, service, controller REST et tests — en suivant les conventions du projet MDD. À utiliser pour tout ajout ou modification d'endpoint, d'entité ou de logique métier serveur.
argument-hint: "<description de la feature, ex: créer un article dans un thème>"
---

# Feature back-end MDD

Feature demandée : $ARGUMENTS

Travaille dans `back/` (package racine `com.openclassrooms.mddapi`). Avance étape par étape et ne saute pas les tests.

## 0. État des lieux

1. Relis la section « Product scope (MVP) » de `CLAUDE.md` : domaine, règles métier et **table de l'API cible**. L'endpoint que tu crées doit correspondre à cette table (chemin, body, réponse, codes d'erreur). Si la feature demandée n'est pas dans les spécifications (back-office, édition ou suppression d'article, sous-commentaires…), signale-le à l'utilisateur avant de coder. En cas de doute, relis `docsForContext/specificationsFonctionnelles.pdf`.
2. Lis les classes existantes liées à la feature dans `model/`, `repository/`, `service/` et `controller/`.
3. Vérifie dans `back/pom.xml` que les dépendances nécessaires sont présentes. S'il en manque, ajoute-les et signale-le à l'utilisateur. Les contraintes techniques imposent de **privilégier un module Spring** à une librairie tierce :
   - **Validation** (`spring-boot-starter-validation`) dès qu'un endpoint reçoit un body.
   - **Lombok** (`org.projectlombok:lombok`, `<optional>true</optional>`), déclaré aussi dans `maven-compiler-plugin` → `annotationProcessorPaths`, comme le fait Spring Initializr. Il doit aussi être exclu du jar dans `spring-boot-maven-plugin`.
   - **Pas de MapStruct** ni d'autre librairie de mapping : les mappers sont écrits à la main.
   - **Spring Security + JWT** (`spring-boot-starter-security`, `io.jjwt:jjwt-api/impl/jackson` 0.12.x) seulement si la feature touche à l'authentification.
   - **Swagger** (`org.springdoc:springdoc-openapi-starter-webmvc-ui`, version compatible avec Spring Boot 3.5) dès le premier controller ajouté ou modifié, avec `springdoc.swagger-ui.path=/api/swagger` et `springdoc.api-docs.path=/api/openapi`. Exemple fonctionnel : `../../Projet3/chatop` (`config/OpenApiConfiguration.java`, `application.properties`).
   - **Tests** : `com.h2database:h2` en scope `test` pour les tests d'intégration, et `jacoco-maven-plugin` (goals `prepare-agent` + `report`) pour la couverture. Exemple : le pom de `../../Projet4/Testez-et-am-liorez-une-application-full-stack/back`.
   - **Secrets** : si `back/.env` n'existe pas encore, ajoute `spring.config.import=optional:file:.env[.properties]` dans `application.properties`, remplace les valeurs en dur (`spring.datasource.username/password`, secret JWT) par `${DB_USERNAME}`, `${DB_PASSWORD}`, `${JWT_SECRET}`…, crée `back/.env.example` (versionné, sans vraie valeur) et ajoute `.env` au `.gitignore`. Préviens l'utilisateur qu'il doit créer son `back/.env`.
4. Si la feature a besoin d'un utilisateur connecté et que Spring Security n'est pas encore configuré, **ne le monte pas en douce dans une autre feature**. Préviens l'utilisateur et propose de faire d'abord la feature « authentification ».

## 1. Entité (`model/`)

- `@Entity`, `@Table(name = "<pluriel_snake_case>")`.
- Clé primaire : `@Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "<entite>_id") private Long id;`. C'est la convention déjà utilisée (`topic_id`, `post_id`, `comment_id`).
- Lombok : `@Getter @Setter @NoArgsConstructor`, et `@AllArgsConstructor`/`@Builder` si c'est utile. **Pas de `@Data` ni de `@EqualsAndHashCode`/`@ToString` par défaut sur les entités**, car ils provoquent des boucles et du chargement lazy sur les relations.
- Relations : `@ManyToOne(fetch = FetchType.LAZY)` + `@JoinColumn(name = "<autre>_id")`. Évite les `@OneToMany` bidirectionnels s'il n'y en a pas besoin.
- Dates : `@CreationTimestamp private LocalDateTime createdAt;`.
- Quand tu touches une entité existante écrite à la main (getters/setters), migre-la vers Lombok.
- Modèle cible (voir `CLAUDE.md`) :
  - `User` : `email` et `username` avec `unique = true`, `password`, et `@ManyToMany` `Set<Topic> subscriptions` avec `@JoinTable(name = "subscriptions", joinColumns = user_id, inverseJoinColumns = topic_id)`.
  - `Topic` : `name`, `description`.
  - `Post` : `title`, `content` (`@Column(columnDefinition = "TEXT")`), `topic`, `author`, `createdAt`.
  - `Comment` : `content`, `post`, `author`, `createdAt`. Pas de commentaire parent : les commentaires ne sont pas récursifs.
- Les thèmes n'ont pas d'écran de création : ils sont insérés au démarrage via `src/main/resources/data.sql`. Comme `ddl-auto=create`, ajoute `spring.jpa.defer-datasource-initialization=true` et `spring.sql.init.mode=always`.

## 2. Repository (`repository/`)

`public interface XxxRepository extends JpaRepository<Xxx, Long>`. Privilégie les requêtes dérivées (`findByTopicIdOrderByCreatedAtDesc`) ; n'utilise `@Query` que si nécessaire.

## 3. DTO (`dto/request/` et `dto/response/`)

- Des `record` Java : `CreateXxxRequest` / `UpdateXxxRequest` dans `dto/request/` pour l'entrée, `XxxDto` dans `dto/response/` pour la sortie.
- **Les controllers n'exposent jamais d'entité JPA.** Aucun DTO de sortie ne contient de mot de passe ou de hash.
- Pour les relations, aplatis ce qui est utile au front (`topicId`, `topicName`, `authorUsername`) plutôt que d'imbriquer des objets entiers.
- Les requêtes sont validées avec Bean Validation (`@NotBlank`, `@Email`, `@Size`…) et le controller met `@Valid` sur le `@RequestBody`.
- **Mot de passe** (inscription et modification du profil) : `@Pattern(regexp = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z\\d]).{8,}$")`, avec un message en français. Au moins 8 caractères, dont un chiffre, une minuscule, une majuscule et un caractère spécial. Dans `UpdateProfileRequest`, le mot de passe est facultatif : `null` veut dire qu'on ne le change pas.
- **Les DTO d'entrée ne contiennent jamais l'auteur ni la date** des articles et commentaires : c'est le serveur qui les fixe.

## 4. Mapper (`mapper/`), écrit à la main

```java
@Component
public class PostMapper {

    public PostDto toDto(Post post) {
        return new PostDto(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getTopic().getId(),
            post.getTopic().getName(),
            post.getCreatedAt());
    }

    public List<PostDto> toDtos(List<Post> posts) {
        return posts.stream().map(this::toDto).toList();
    }

    public Post toEntity(CreatePostRequest request) {
        Post post = new Post();
        post.setTitle(request.title());
        post.setContent(request.content());
        return post;
    }
}
```

- Une classe `@Component` par agrégat (`XxxMapper`), injectée dans le service.
- Le mapper ne fait que copier des champs : **aucun accès au repository**. Les relations (topic, auteur) sont chargées et affectées par le service.
- `toEntity` ne remplit jamais `id`, `createdAt` ni l'auteur.
- Gère explicitement les relations qui peuvent être `null`, pour éviter les NPE.

## 5. Service (`service/`)

- Une interface `IXxxService` et une implémentation `XxxService` annotée `@Service` (attention : `PostService` n'a pas encore cette annotation, à corriger si tu y touches).
- Injection par constructeur : `@RequiredArgsConstructor` et champs `private final`.
- Le service reçoit et renvoie des DTO. La conversion entité ↔ DTO se fait ici, via le mapper.
- Erreurs métier : lève une exception dédiée de `exception/` (`ResourceNotFoundException` → 404, `ConflictException` → 409 pour un email ou un nom d'utilisateur déjà pris, `InvalidCredentialsException` → 401…), avec un message en français. Crée-la si elle n'existe pas.
- Une seule classe `exception/GlobalExceptionHandler` (`@RestControllerAdvice`, qui peut étendre `ResponseEntityExceptionHandler`) transforme ces exceptions en **`ProblemDetail`** (RFC 9457) avec le bon status. Pour les erreurs de validation (400), ajoute une propriété `errors` : `{ "champ": "message" }`. Ne renvoie jamais de corps vide, contrairement au `GlobalExceptionHandler` du Projet 3.
- **JavaDoc** sur chaque méthode publique de l'interface du service : ce qu'elle fait, ses paramètres, son retour et les exceptions levées.
- L'auteur d'un article ou d'un commentaire est l'utilisateur courant, rechargé depuis son id (le sujet du JWT).
- Fil d'actualité : articles des thèmes auxquels l'utilisateur est abonné, triés par `createdAt` via `Sort.by(direction, "createdAt")`. Le sens vient du paramètre `sort` (`desc` par défaut). Charge l'auteur et le thème sans requêtes N+1 (`@EntityGraph` ou `JOIN FETCH`).
- S'abonner deux fois au même thème, ou se désabonner d'un thème non suivi, ne doit pas provoquer d'erreur : ces opérations sont idempotentes.
- Respecte SOLID (c'est une contrainte du client) : une responsabilité par service, et des dépendances vers des interfaces.
- Ajoute `@Transactional` sur les méthodes qui écrivent, et `@Transactional(readOnly = true)` sur les lectures qui parcourent des relations lazy.

## 6. Controller (`controller/`)

- `@RestController`, `@RequestMapping("/api/<ressource au pluriel>")`, conformément à la table de l'API cible. Le controller existant `/topic` doit devenir `/api/topics` : migre-le si tu le modifies, et préviens que le front doit suivre.
- Il dépend de l'**interface** du service, injectée par constructeur.
- Il renvoie des DTO, avec `ResponseEntity` quand le code HTTP n'est pas 200 (`201 Created` pour une création).
- Utilisateur courant : `@AuthenticationPrincipal`. Ne prends jamais l'id de l'auteur dans le body de la requête.
- **JavaDoc** sur chaque méthode d'endpoint, et annotations OpenAPI pour Swagger : `@Tag` sur la classe, `@Operation(summary = ...)` et les `@ApiResponse` des codes d'erreur possibles sur chaque méthode. Vérifie que l'endpoint apparaît bien dans Swagger UI (`/api/swagger`).

## 7. Sécurité (quand Spring Security est en place)

- Toutes les routes sont authentifiées sauf `/api/auth/**` et les chemins Swagger (`/api/swagger/**`, `/api/swagger-ui/**`, `/api/openapi/**`). Déclare le schéma de sécurité Bearer JWT dans la config OpenAPI pour pouvoir tester les routes protégées depuis Swagger UI.
- Une requête sans token ou avec un token invalide renvoie **401** avec un ProblemDetail (`authenticationEntryPoint`), pas 403.
- Exemple fonctionnel de `JwtService`, `JwtAuthenticationFilter` et `SecurityConfiguration` : `../../Projet3/chatop/src/main/java/com/openclassrooms/chatop/security/`. Attention, chatop met l'email dans le sujet du token : ici, c'est l'id (voir plus bas).
- Sessions stateless, JWT dans `Authorization: Bearer <token>`, mots de passe hashés avec `BCryptPasswordEncoder`.
- **Le sujet du JWT est l'id de l'utilisateur.** Il ne faut pas y mettre l'email ni le username, car ils sont modifiables : changer son profil ne doit pas invalider le token.
- Connexion : un champ `identifier` qui accepte l'email **ou** le username (`findByEmailOrUsername`). Pour ne pas révéler l'existence d'un compte, renvoie le même message d'erreur 401 que l'identifiant soit inconnu ou le mot de passe faux.
- La connexion doit persister entre les sessions du navigateur : choisis une durée de validité du token assez longue (par ex. 7 jours), configurable.
- La déconnexion se fait côté client (suppression du token) : pas d'endpoint de logout.
- Le secret JWT et sa durée de validité sont lus depuis `.env` (`app.jwt.secret=${JWT_SECRET}`, `app.jwt.expiration-ms=${JWT_EXPIRATION_MS:604800000}`), jamais écrits en dur. Le secret est une clé Base64 de 32 octets minimum (`openssl rand -base64 32`).
- CORS : inutile en dev grâce au proxy Angular. N'ouvre pas `*`.

## 8. Tests (`src/test/java/...`, même package que la classe testée)

- **Mapper** : test unitaire simple (`new XxxMapper()`), sans Spring ni mock. Vérifie que chaque champ est copié, y compris les champs aplatis des relations.
- **Service** : test unitaire `@ExtendWith(MockitoExtension.class)`, avec `@Mock` sur le repository, le vrai mapper via `@Spy private XxxMapper mapper = new XxxMapper();` et `@InjectMocks` sur le service. Couvre le cas nominal et le cas d'erreur (404…).
- **Controller** : `@WebMvcTest(XxxController.class)` + `MockMvc`, et `@MockitoBean` (`org.springframework.test.context.bean.override.mockito.MockitoBean` ; `@MockBean` est déprécié) sur le service. Si la sécurité est active, ajoute `@WithMockUser` et importe la config de sécurité.
- **Intégration** (`XxxIntegrationTest`) : `@SpringBootTest` + `@AutoConfigureMockMvc` sur une base **H2 en mode MySQL**, configurée dans `src/test/resources/application.properties` (`jdbc:h2:mem:mdd;MODE=MySQL;DB_CLOSE_DELAY=-1`, secret JWT de test). Parcours réel avec un vrai token : inscription, connexion, puis appel de l'endpoint ; vérifie aussi le 401 sans token. Un ou deux scénarios par feature suffisent : on teste les cas sensibles, pas toute l'application.
- Lance uniquement les tests concernés : `cd back && mvn test -Dtest='XxxServiceTest,XxxControllerTest,XxxIntegrationTest'`. Tant que H2 n'est pas en place, `MddApiApplicationTests` a besoin de MySQL : ne t'en sers pas comme preuve que la feature marche.
- Couverture : `mvn verify`, rapport dans `back/target/site/jacoco/index.html`.
- Termine par `mvn compile` (ou le test) pour vérifier que Lombok génère bien le code.

## 9. Fin

Résume pour l'utilisateur, sous une forme réutilisable dans `JustificationsDesChoix.docx` :
- **Endpoints** créés ou modifiés, sous forme de lignes de tableau `Endpoint | Méthode | Description | Corps / Réponse`, avec un exemple JSON (section 2.3) ;
- **Dépendances ajoutées**, chacune avec son objectif, ses avantages et ses inconvénients de façon objective, et le lien vers sa documentation (section 2.2) ;
- **Tests** lancés, avec leur résultat (section 3.1) ;
- les écarts éventuels avec la table de l'API cible de `CLAUDE.md`. Si l'API a changé, mets cette table à jour.

Rappelle que `ddl-auto=create` recrée le schéma à chaque démarrage.
