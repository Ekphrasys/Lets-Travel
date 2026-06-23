# Phase 1 — Checklist (version finale validée)

> **Statut : VALIDÉE** — Prêt pour Phase 2 (Infrastructure Docker)

**Projet :** Travel Management System — projet étudiant  
**Stack :** Java 17 · Spring Boot 3 · Angular · PostgreSQL · Neo4j

---

## Décisions finales

| Sujet | Décision |
|-------|----------|
| Microservices | 6 (discovery, gateway, auth, user, travel, payment) |
| PostgreSQL | 1 instance, 4 schémas (auth, user, travel, payment) |
| Neo4j | Graphe villes uniquement (travel-service) |
| Communication | REST synchrone — pas de Kafka |
| JWT | HS256, 24 h, pas de refresh token |
| Paiement | Mock — amount > 0 = succès |
| HTTPS | Obligatoire (Gateway + Angular) |

---

## Checklist

### Architecture
- [x] 6 microservices définis avec rôles clairs
- [x] Stack Java + Angular + PostgreSQL + Neo4j documentée
- [x] Structure monorepo définie
- [x] 9 cas d'usage listés

### Bases de données
- [x] 4 schémas PostgreSQL (5 tables au total)
- [x] Modèle Neo4j simplifié (City + CONNECTS_TO)
- [x] Répartition PostgreSQL / Neo4j justifiée
- [x] Statuts booking et payment définis

### Communication
- [x] Routage Gateway documenté
- [x] 4 appels inter-services identifiés
- [x] Flux booking → payment synchrone
- [x] 3 diagrammes de séquence

### Sécurité
- [x] JWT émis par auth-service
- [x] 2 rôles : USER, ADMIN
- [x] Matrice d'accès simplifiée
- [x] BCrypt + CORS + secrets via .env

### API
- [x] OpenAPI auth, user, travel, payment

---

## Supprimé (sur-complexité)

| Élément retiré | Raison |
|----------------|--------|
| Kafka | Inutile pour un flux synchrone étudiant |
| Refresh token + table refresh_tokens | JWT 24 h suffit |
| RS256 / clés asymétriques | HS256 plus simple |
| payment_status_history | Statut actuel suffit |
| Table travelers | 1 user = 1 réservation |
| Tables roles / user_roles | Colonne `role` sur users |
| 4 containers PostgreSQL | 1 instance, 4 schémas |
| Nœuds Airport, Route (Neo4j) | City + CONNECTS_TO suffit |
| Saga distribuée | REST synchrone |
| Idempotency key | Flux simple |
| X-Request-Id / enveloppe JSON | Réponses Spring standard |
| Validation JWT par Gateway | Chaque service valide |
| neo4j_route_id sync | PG et Neo4j indépendants |

---

## Prochaine étape — Phase 2

1. `infrastructure/docker-compose.yml`
   - PostgreSQL (1 container, init scripts 4 schémas)
   - Neo4j (seed 4 villes)
   - discovery-service, gateway-service
2. `.env.example`
3. Scripts SQL init + seed Neo4j
4. Réseau `travel-network`

---

## Index documents

| Document | Fichier |
|----------|---------|
| Architecture | [ARCHITECTURE.md](./ARCHITECTURE.md) |
| Communication | [COMMUNICATION.md](./COMMUNICATION.md) |
| Sécurité | [SECURITY.md](./SECURITY.md) |
| PostgreSQL | [database/postgresql.md](./database/postgresql.md) |
| Neo4j | [database/neo4j.md](./database/neo4j.md) |
| API | [api/](./api/) |
| Séquences | [sequences/](./sequences/) |
