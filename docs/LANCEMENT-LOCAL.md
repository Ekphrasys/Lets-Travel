# Lancement du projet en local

> Stack complète : PostgreSQL · Neo4j · 5 microservices Spring Boot · Angular · API Gateway

---

## Prérequis

| Outil | Version minimale | Vérification |
|-------|-----------------|--------------|
| Docker | 24+ | `docker --version` |
| Docker Compose | v2 (`compose` plugin) | `docker compose version` |
| Java (JDK) | 17 | `java --version` |
| Maven | 3.8+ | `mvn --version` |
| Node.js | 18+ | `node --version` |
| npm | 9+ | `npm --version` |

---

## 1 — Cloner et configurer l'environnement

```bash
git clone https://zone01normandie.org/git/ajoly/travel-plan.git
cd travel-plan
```

Créer le fichier `.env` à la racine (une seule fois) :

```bash
cp .env.example .env
```

> Les valeurs par défaut de `.env.example` fonctionnent tel quel en local. Modifiez-les pour un déploiement en production.

---

## 2 — Lancer la stack complète (Docker)

> **Méthode recommandée** — tout démarre en un seul appel, images comprises.

```bash
docker compose -f infrastructure/docker-compose.yml --project-name travel up -d --build
```

Ce que ça lance :

| Conteneur | Rôle | Port |
|-----------|------|------|
| `travel-postgres` | Base relationnelle PostgreSQL | interne |
| `travel-neo4j` | Base graphe Neo4j | interne |
| `travel-neo4j-seed` | Injection des données Neo4j (s'arrête seul) | — |
| `travel-discovery` | Eureka Service Registry | `8761` |
| `travel-gateway` | API Gateway (HTTPS, JWT) | `8080` |
| `travel-user` | Microservice utilisateurs | interne |
| `travel-auth` | Microservice authentification | interne |
| `travel-payment` | Microservice paiements | interne |
| `travel-app` | Microservice voyages / réservations | interne |
| `travel-admin` | Frontend Angular (HTTPS) | `4200` |

Vérifier que tout est `healthy` :

```bash
docker compose -f infrastructure/docker-compose.yml --project-name travel ps
```

### Accès

| Interface | URL |
|-----------|-----|
| Frontend Angular | https://localhost:4200 |
| API Gateway | https://localhost:8080 |
| Eureka (registre) | http://localhost:8761 |

> Le certificat SSL est auto-signé. Acceptez l'exception de sécurité dans le navigateur.

---

## 3 — Lancer CI/CD (Jenkins + SonarQube) — optionnel

Jenkins et SonarQube tournent dans un `docker-compose` séparé :

```bash
# Paramétrage du kernel pour SonarQube (Elasticsearch)
sudo sysctl -w vm.max_map_count=262144

# Démarrer Jenkins + SonarQube
docker compose -f jenkins-compose.yml up -d --build

# Configuration automatique (token SonarQube, job Jenkins, SCM)
bash scripts/setup-ci.sh
```

| Interface | URL | Identifiants |
|-----------|-----|--------------|
| Jenkins | http://localhost:9092 | `admin` / `admin` |
| SonarQube | http://localhost:9002 | `admin` / `admin` |

Déclencher un build manuellement : **Jenkins → travel → Build Now**

---

## 4 — Lancer via Ansible (audit)

```bash
# Installation d'ansible-core (si absent)
pip3 install --user --break-system-packages ansible-core
ansible-galaxy collection install ansible.posix

# Déploiement complet (prérequis + stack Docker)
./scripts/run-ansible.sh

# Ou étape par étape
./scripts/run-ansible.sh prerequisites   # sysctl + vérification Docker
./scripts/run-ansible.sh deploy          # docker compose up
```

---

## 5 — Développement microservice par microservice

Si vous travaillez sur un seul microservice, démarrez uniquement les dépendances :

```bash
# Bases de données + registre Eureka uniquement
docker compose -f infrastructure/docker-compose.yml --project-name travel \
  up -d postgres neo4j discovery-service
```

Puis lancez le microservice voulu depuis votre IDE ou en ligne de commande :

```bash
# Exemple : user-service
cd microservices/user-service
mvn spring-boot:run
```

Frontend Angular en mode développement (avec rechargement automatique) :

```bash
cd frontend/travel-admin
npm install
npm start          # https://localhost:4200 (HTTPS avec SSL auto-signé)
```

---

## 6 — Lancer les tests

### Tests backend (tous les microservices)

```bash
cd microservices/auth-service    && mvn test
cd microservices/user-service    && mvn test
cd microservices/payment-service && mvn test
cd microservices/travel-service  && mvn test
cd microservices/gateway-service && mvn test
```

Ou depuis la racine si un `pom.xml` parent existe :

```bash
mvn test -pl microservices/auth-service,microservices/user-service,microservices/payment-service,microservices/travel-service,microservices/gateway-service
```

### Tests frontend

```bash
cd frontend/travel-admin
npm test                    # tests unitaires (watch mode)
npm test -- --watch=false   # tests unitaires (une seule exécution)
```

### Test API (script d'audit CRUD)

```bash
bash scripts/audit-api-test.sh
```

---

## 7 — Arrêt et nettoyage

```bash
# Arrêter la stack applicative (conserve les volumes)
docker compose -f infrastructure/docker-compose.yml --project-name travel down

# Arrêter Jenkins + SonarQube
docker compose -f jenkins-compose.yml down

# Arrêt + suppression des volumes (repart de zéro)
docker compose -f infrastructure/docker-compose.yml --project-name travel down -v
```

---

## Résumé express

```bash
# Démarrage en 3 commandes
cp .env.example .env
docker compose -f infrastructure/docker-compose.yml --project-name travel up -d --build
# → https://localhost:4200
```
