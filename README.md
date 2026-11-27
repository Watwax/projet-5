# MDD — Monde de Dév

Réseau social pour développeurs (MVP) : les utilisateurs s'abonnent à des thèmes de programmation, lisent un fil d'actualité, publient des articles et les commentent.

Le dépôt contient deux applications :

- [`back/`](./back/) : API REST Spring Boot 4.2 (Java 21, Spring Data JPA, Spring Security + JWT, MySQL) ;
- [`front/`](./front/) : client Angular 22 (composants standalone, signals).

Les choix techniques sont justifiés dans [`docsForContext/JustificationsDesChoix.docx`](./docsForContext/JustificationsDesChoix.docx).

## Prérequis

- JDK 21 (Maven est fourni par le wrapper `./mvnw`, version 3.9) ;
- Node.js 24.15 ou plus récent (ou 22.22.3 et plus), avec npm ;
- MySQL 8 installé en local, port 3306.

## Installation

### 1. Base de données

Créez une base vide `mdd` :

```sql
CREATE DATABASE mdd CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Les tables sont créées par Hibernate au démarrage de l'API, à partir des entités. Les thèmes sont insérés par [`back/src/main/resources/data.sql`](./back/src/main/resources/data.sql).

> Pour le MVP, `spring.jpa.hibernate.ddl-auto=create` : **la base est vidée et recréée à chaque démarrage de l'API.**

### 2. Configuration du back-end

Copiez le modèle de configuration et renseignez vos valeurs (le fichier `.env` n'est pas versionné) :

```bash
cd back
cp .env.example .env
openssl rand -base64 32   # à coller dans JWT_SECRET
```

| Variable | Description |
| --- | --- |
| `DB_URL` | URL JDBC de MySQL (par défaut `jdbc:mysql://localhost:3306/mdd`) |
| `DB_USERNAME` / `DB_PASSWORD` | Identifiants MySQL |
| `JWT_SECRET` | Clé de signature des tokens, en Base64 (32 octets minimum) |
| `JWT_EXPIRATION_MS` | Durée de validité d'un token (par défaut 7 jours) |

### 3. Dépendances du front-end

```bash
cd front
npm install
npx cypress install   # télécharge le binaire Cypress (les scripts d'installation npm sont désactivés par défaut)
```

## Lancement

Dans deux terminaux :

```bash
cd back && ./mvnw spring-boot:run   # API sur http://localhost:9000
cd front && npm run start           # client sur http://localhost:4200
```

Le serveur de développement Angular redirige les appels `/api` vers l'API (fichier [`front/proxy.conf.json`](./front/proxy.conf.json)) : démarrez l'API avant le front.

Documentation interactive de l'API (Swagger UI) : <http://localhost:9000/api/swagger>.

## Tests

| Partie | Commande | Rapport |
| --- | --- | --- |
| Back (JUnit, Mockito, MockMvc, H2) | `cd back && ./mvnw verify` | `back/target/site/jacoco/index.html` |
| Front, unitaires (Jest) | `cd front && npm test` ou `npm run test:coverage` | `front/coverage/jest/index.html` |
| Front, E2E (Cypress) | `cd front && npm run e2e` (API et front démarrés) | console |

Les tests du back utilisent une base H2 en mémoire : ils ne nécessitent ni MySQL ni fichier `.env`.

## Organisation du code

```
back/src/main/java/com/openclassrooms/mddapi/
  config/  controller/  dto/  mapper/  service/  repository/  model/  exception/  security/
front/src/app/
  core/ (models, services, interceptors, guards)  features/<feature>/  shared/
```

## Difficultés rencontrées

_À compléter au fil du projet._
