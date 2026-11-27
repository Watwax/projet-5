-- Topics are seed data: the MVP has no screen to create them.
-- Runs after Hibernate has created the schema (spring.jpa.defer-datasource-initialization=true).
INSERT INTO topics (name, description) VALUES
('Java', 'Le langage Java, la JVM et son écosystème : nouveautés du langage, performances, bonnes pratiques.'),
('Spring', 'Le framework Spring et Spring Boot : injection de dépendances, API REST, Spring Data, Spring Security.'),
('Angular', 'Le framework front-end Angular : composants, signals, routage, formulaires et tests.'),
('JavaScript', 'Le langage du web : ECMAScript moderne, navigateur, Node.js et outillage.'),
('TypeScript', 'Le typage statique pour JavaScript : types avancés, configuration et migration.'),
('Bases de données', 'SQL, modélisation relationnelle, MySQL, NoSQL et optimisation des requêtes.'),
('DevOps', 'Intégration et déploiement continus, conteneurs, supervision et infrastructure as code.'),
('Sécurité', 'Sécurité des applications : authentification, OWASP, chiffrement et bonnes pratiques.');
