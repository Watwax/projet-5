---
name: front-feature
description: Implémente une feature côté client Angular 20 (dossier front/) — modèle TypeScript, service HTTP, composants standalone, routes et tests — en suivant les conventions du projet MDD. À utiliser pour tout ajout ou modification d'écran, de composant ou d'appel à l'API.
argument-hint: "<description de la feature, ex: page liste des thèmes avec abonnement>"
---

# Feature front-end MDD

Feature demandée : $ARGUMENTS

Travaille dans `front/`. Avance étape par étape et ne saute pas les tests.

## 0. Pré-requis d'architecture (à vérifier à chaque fois)

1. **Standalone** : si `src/app/app.module.ts` existe encore, l'app est toujours en NgModule. **Demande à l'utilisateur** s'il veut migrer avant de commencer la feature. Si oui :
   - lance `npx ng generate @angular/core:standalone` trois fois, en choisissant successivement « Convert all components, directives and pipes to standalone », « Remove unnecessary NgModule classes » puis « Bootstrap the application using standalone APIs » ;
   - vérifie ensuite que `src/app/app.config.ts` fournit `provideRouter(routes)` et `provideHttpClient(withInterceptors([...]))`, et que `main.ts` utilise `bootstrapApplication`. Crée `app.config.ts` et `app.routes.ts` s'ils manquent, et ajoute `<router-outlet />` dans le template racine ;
   - lance `npm run build` pour valider.
2. **Proxy API** : l'API tourne sur `http://localhost:9000`, sous le préfixe `/api`. Si `front/proxy.conf.json` n'existe pas, crée-le :
   ```json
   { "/api": { "target": "http://localhost:9000", "secure": false } }
   ```
   puis ajoute `"proxyConfig": "proxy.conf.json"` dans `angular.json` → `serve.options`. Les services appellent des URL relatives (`/api/topic`) : pas d'URL en dur vers `localhost:9000`.
3. Vérifie le contrat de l'API en lisant le controller et les DTO dans `back/src/main/java/com/openclassrooms/mddapi/`. Ne devine pas les noms de champs. Si l'endpoint n'existe pas encore, appuie-toi sur la table de l'API cible de `CLAUDE.md` et signale qu'il reste à faire côté back (`/back-feature`).
4. Relis la section « Product scope (MVP) » de `CLAUDE.md` : écrans, règles métier, et ce qui est hors périmètre (pas de back-office, ni d'édition ou suppression d'article, ni de sous-commentaires). Si la demande sort des spécifications, signale-le avant de coder.
5. **Maquettes Figma** (versions desktop et mobile) : https://www.figma.com/design/Rflr3TVBog35BNMnn0DF09/Maquettes-MDD--desktop-et-mobile-?node-id=0-1&p=f
   - Avant de créer ou de styler un écran, consulte la maquette de cet écran **dans ses deux versions, desktop et mobile**.
   - Si les outils Figma (`mcp__claude_ai_Figma__*`) sont disponibles, utilise-les pour récupérer la frame correspondante, sa capture d'écran et ses valeurs (couleurs, polices, tailles, espacements).
   - Sinon, demande à l'utilisateur une capture ou un export de l'écran, ou d'activer le connecteur Figma dans claude.ai. **Ne devine jamais le design.**
   - Reprends fidèlement les couleurs, la typographie et la disposition. Toute nouvelle valeur (couleur, police, espacement) devient une variable dans `styles.css`, et les points de rupture (breakpoints) des media queries suivent les largeurs des maquettes mobile et desktop.
   - Les icônes et images (logo, etc.) sont exportées dans `src/assets/`.

## 1. Organisation des fichiers

```
src/app/
  core/
    models/        interfaces TS = miroir des DTO back
    services/      services HTTP (providedIn: 'root')
    interceptors/  ex: jwt.interceptor.ts
    guards/        ex: auth.guard.ts
  features/<feature>/
    pages/         composants routés
    components/    composants propres à la feature
  shared/          composants/pipes réutilisés par plusieurs features
```

Garde le suffixe de fichier `.component.ts` / `.service.ts` déjà utilisé dans le projet (ex. `topic.component.ts`).

### Écrans et routes cibles (d'après la vidéo de présentation)

| Route | Écran | Accès | Points clés |
|---|---|---|---|
| `/` | Accueil public | invité | Accès rapide à « Se connecter » et « S'inscrire » |
| `/register` | Inscription | invité | email, nom d'utilisateur, mot de passe |
| `/login` | Connexion | invité | email **ou** nom d'utilisateur + mot de passe |
| `/feed` | Fil d'actualité (accueil connecté) | connecté | Tri plus récent / plus ancien, bouton « Créer un article », clic sur un article pour l'ouvrir |
| `/topics` | Thèmes | connecté | Tous les thèmes ; le bouton « S'abonner » devient inactif et affiche « Déjà abonné » |
| `/posts/new` | Créer un article | connecté | Choix du thème, titre, contenu. **Ni auteur ni date dans le formulaire** |
| `/posts/:id` | Article | connecté | Thème, titre, auteur, date, contenu, commentaires, et formulaire de commentaire (contenu uniquement) |
| `/me` | Profil | connecté | Modifier l'email, le nom d'utilisateur et le mot de passe ; liste des abonnements avec « Se désabonner » |

Un utilisateur déjà connecté qui ouvre `/`, `/login` ou `/register` est redirigé vers `/feed` (guard « invité »). Un en-tête avec la navigation (articles, thèmes, profil, déconnexion) est présent sur les pages connectées, et il doit être utilisable sur mobile (menu repliable).

## 2. Modèles (`core/models/`)

Une `interface` par DTO back, avec **exactement** les mêmes noms de champs. Les dates arrivent sous forme de `string` (ISO).

## 3. Service HTTP (`core/services/`)

```ts
@Injectable({ providedIn: 'root' })
export class TopicService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/topics';

  getAll(): Observable<Topic[]> {
    return this.http.get<Topic[]>(this.baseUrl);
  }
}
```

Utilise `inject()` plutôt que l'injection par constructeur. Le service ne fait aucun `subscribe`.

## 4. Composants

- Standalone : c'est la valeur par défaut en Angular 20, donc **n'écris ni `standalone: true` ni `standalone: false`**. Déclare les dépendances dans `imports: [...]`.
- `changeDetection: ChangeDetectionStrategy.OnPush`.
- État local en **signals** (`signal`, `computed`). Pour les Observables, utilise `toSignal()` ou le pipe `async`. Si un `subscribe` est inévitable, ajoute `takeUntilDestroyed()`.
- Templates avec le nouveau control flow : `@if`, `@for (x of xs; track x.id)`, `@empty`. Pas de `*ngIf`/`*ngFor`.
- Entrées/sorties via `input()`, `input.required()` et `output()`.
- Formulaires : `ReactiveFormsModule`, avec `FormBuilder` récupéré via `inject(FormBuilder).nonNullable`.
  - **Mot de passe** (inscription et profil) : `Validators.pattern(/^(?=.*\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[^A-Za-z\d]).{8,}$/)`, la même règle que le back. Place-la dans une constante partagée (`core/validators/`) et affiche un message qui explique la règle : au moins 8 caractères, avec un chiffre, une minuscule, une majuscule et un caractère spécial.
  - Le bouton de soumission est désactivé tant que le formulaire est invalide. Les erreurs renvoyées par l'API (400, 401, 409 « email déjà utilisé ») sont affichées à l'utilisateur.
- **Sécurité Angular** (contrainte du client, voir https://angular.dev/best-practices/security) : affiche le contenu saisi par les utilisateurs (articles, commentaires) par interpolation `{{ }}`, et conserve les retours à la ligne avec `white-space: pre-line`. Jamais de `[innerHTML]` sur ce contenu, ni de `bypassSecurityTrust*`.
- **Pas de librairie d'UI** : c'est un choix du projet. N'ajoute ni Material, ni PrimeNG, ni Tailwind, ni Bootstrap.
  - Les valeurs communes issues des maquettes (couleurs, polices, espacements, rayons, breakpoints de référence) sont des variables CSS déclarées sur `:root` dans `src/styles.css`. Les composants utilisent `var(--color-primary)` etc. : pas de couleur en dur. Si une valeur manque, ajoute-la dans `styles.css`.
  - Les styles propres à un composant vont dans son `.component.css`. Seuls le reset et quelques classes utilitaires vraiment partagées (ex. `.btn`) vont dans `styles.css`.
  - Rends chaque écran responsive (mobile et desktop) avec des media queries, et commence par la version mobile.

## 5. Routes (`app.routes.ts`)

- Charge les pages en lazy : `{ path: 'topics', loadComponent: () => import('./features/topics/pages/topic-list.component').then(m => m.TopicListComponent) }`.
- Protège les pages réservées aux utilisateurs connectés avec un guard fonctionnel (`CanActivateFn`), une fois l'authentification en place.

## 6. Authentification JWT (quand elle existe côté back)

- Le token est stocké dans `localStorage` via un `SessionService` / `AuthService` dans `core/services/`. Le choix de `localStorage` répond à l'exigence que **la connexion persiste entre les sessions** (pas de `sessionStorage`).
- Déconnexion : suppression du token, puis redirection vers `/`. Il n'y a pas d'appel API.
- Un intercepteur fonctionnel `HttpInterceptorFn` ajoute `Authorization: Bearer <token>` aux requêtes `/api/**`, sauf `/api/auth/**`. Il est enregistré dans `provideHttpClient(withInterceptors([jwtInterceptor]))`.
- Sur une réponse 401, déconnecte l'utilisateur et redirige-le vers la page de connexion.

## 7. Tests (Karma + Jasmine, un `.spec.ts` à côté de chaque fichier)

- **Service** : `TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] })` + `HttpTestingController`. Vérifie l'URL et la méthode, puis appelle `httpMock.verify()` dans `afterEach`.
- **Composant** : `imports: [LeComposant]`, avec les services mockés via `{ provide: XService, useValue: jasmine.createSpyObj(...) }` et `provideRouter([])` si le composant utilise le router. Vérifie le rendu après `fixture.detectChanges()`.
- Lance seulement les specs concernées : `cd front && npx ng test --watch=false --browsers=ChromeHeadless --include='src/app/features/<feature>/**/*.spec.ts'`.
- Termine par `npm run build` pour valider la compilation stricte.

## 8. Fin

Résume pour l'utilisateur :
- les routes et écrans ajoutés, et ce qu'il faut vérifier à la main (mobile et desktop, par rapport à la maquette) ;
- les endpoints consommés, et ce qu'il reste à faire côté back si l'un d'eux manque ;
- les tests lancés avec leur résultat ;
- si tu as ajouté une dépendance npm, son objectif, ses avantages et ses inconvénients de façon objective, pour le tableau des choix techniques de `JustificationsDesChoix.docx`.
