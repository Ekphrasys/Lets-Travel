# Conformité audit.md (hors bonus)

> Guide pour la soutenance — mapping critères audit ↔ projet Travel

---

## Comprehension

### Architecture microservices

| Critère audit | Réponse projet |
|---------------|----------------|
| Frontières par domaine métier | 6 services : discovery, gateway, auth, user, travel, payment |
| Alignement fonctions métier | Auth (login), User (profils), Travel (voyages/réservations/Neo4j), Payment (paiements) |
| Indépendance | Chaque service = repo Maven + Dockerfile + BDD dédiée (schéma PostgreSQL) |
| Déploiement sans impact | `docker compose up` service par service ; Eureka re-route |
| Scalabilité | Réplicas possibles via Eureka (`USER-SERVICE`, etc.) |
| Résilience | Healthchecks Docker ; services dégradés isolés |
| API Gateway | `gateway-service` HTTPS :8080 |
| Traçabilité | Header `X-Correlation-Id` propagé par la Gateway (`CorrelationIdFilter`) |

Docs : `docs/architecture/ARCHITECTURE.md`, `COMMUNICATION.md`

### Ansible

| Playbook | Fichier | Rôle |
|----------|---------|------|
| Prérequis | `ansible/playbooks/prerequisites.yml` | `vm.max_map_count`, vérif Docker |
| Déploiement | `ansible/playbooks/deploy.yml` | `.env` + `docker compose up` |
| Complet | `ansible/playbooks/site.yml` | Orchestre les deux (idempotent) |

```bash
cd ansible && ansible-playbook playbooks/site.yml
```

### CI/CD

| Critère | Implémentation |
|---------|----------------|
| Tests unitaires | `mvn verify` × 6 services + `npm test` Angular |
| Pipeline | `Jenkinsfile` : build → test → SonarQube → deploy |
| SonarQube | `sonar-project.properties`, JaCoCo + lcov |

### Sécurité

| Mesure | Détail |
|--------|--------|
| SSL/TLS | Gateway HTTPS + Angular `--ssl` |
| Secrets | `.env` (JWT, DB, keystore, `INTERNAL_API_KEY`) — jamais en Git |
| Moindre privilège | RBAC `USER` / `ADMIN` ; endpoints admin `@PreAuthorize` |
| Inter-services | Clé interne `X-Internal-Key` (auth→user, travel→payment) |

Doc : `docs/architecture/SECURITY.md`

### Bases de données

| BDD | Usage |
|-----|-------|
| PostgreSQL | 4 schémas : auth, user, travel, payment |
| Neo4j | Graphe villes + `CONNECTS_TO` (itinéraires) |

Docs : `docs/architecture/database/postgresql.md`, `neo4j.md`

---

## Functional

### Ansible

- Exécution : `ansible-playbook playbooks/site.yml`
- Idempotent : `.env` non écrasé ; `docker compose up -d` relançable

### Docker + Ansible

- Stack : `infrastructure/docker-compose.yml`
- Automatisation : `ansible/playbooks/deploy.yml`

### APIs microservices — accès Admin

| Test | Résultat attendu |
|------|------------------|
| Sans JWT | 401 Unauthorized |
| JWT USER sur `/api/users` | 403 Forbidden |
| JWT ADMIN | 200 OK |

Endpoints publics : `POST /api/auth/register`, `POST /api/auth/login`, `/actuator/health`

Script : `scripts/audit-api-test.sh`

### CRUD Admin

| Entité audit | Entité projet | API | Frontend |
|--------------|---------------|-----|----------|
| Users | Utilisateurs | `GET/POST/PUT/DELETE /api/users` | `/admin/users` |
| Travelers | Voyages (trips) | `GET/POST/PUT/DELETE /api/travels` | `/trips`, `/trips/new` |
| Payment methods | Paiements | `GET/POST/PUT/DELETE /api/payments` | `/admin/payments` |

### Auth & RBAC

- JWT HS256, 24h, rôles `USER` / `ADMIN`
- Promotion admin : `UPDATE "user".users SET role = 'ADMIN' WHERE email = '...'`

### Load balancing

- Eureka + Spring Cloud LoadBalancer
- Script : `scripts/load-test.sh`
- Démo : `http://localhost:8761` (instances enregistrées)

### CI/CD & qualité

- Jenkins : http://localhost:9092
- SonarQube : http://localhost:9002
- **Setup complet** : `./scripts/setup-ci.sh`
- **Guide audit** : `docs/CI-AUDIT-SETUP.md`
- Checklist : `docs/architecture/PHASE5-CHECKLIST.md`

### Code review

- Conventions Java : packages `com.travel.*`, PascalCase classes, camelCase méthodes
- PR : branches feature, messages descriptifs

---

## Commandes rapides soutenance

```bash
# Stack
docker compose --env-file .env -f infrastructure/docker-compose.yml up -d

# Ansible
cd ansible && ansible-playbook playbooks/site.yml

# Tests audit
chmod +x scripts/*.sh
./scripts/audit-api-test.sh
./scripts/load-test.sh

# Frontend
cd frontend/travel-admin && npm start
```

## Correspondance terminologie audit

| Terme audit | Projet Travel |
|-------------|---------------|
| Travelers | Voyages (`trips`) |
| Payment methods | Paiements (`payments`) |
| Ansible | `ansible/playbooks/` |
