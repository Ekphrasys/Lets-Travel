# Travel Management System

Application de gestion de voyages — microservices Java (Spring Boot), frontend Angular, PostgreSQL, Neo4j, CI/CD Jenkins + SonarQube.

---

## Prérequis

- Docker et Docker Compose
- Git
- (Optionnel) Node.js 22+ et Java 17 pour le développement local

---

## Lancer l'intégralité du projet

### 1. Variables d'environnement

```bash
cd /chemin/vers/travel
cp .env.example .env
```

### 2. Stack applicative (BDD, microservices, frontend)

```bash
docker compose -f infrastructure/docker-compose.yml up --build -d
```

Premier démarrage : compter **5 à 10 minutes** (build Maven + Angular).

Vérifier l'état :

```bash
docker compose -f infrastructure/docker-compose.yml ps
```

### 3. CI/CD Jenkins + SonarQube (optionnel, audit)

```bash
chmod +x scripts/*.sh
./scripts/setup-ci.sh
```

Ce script configure SonarQube, Jenkins, le job pipeline `travel` et les credentials Git.

---

## Identifiants

### Jenkins

| Champ | Valeur |
|-------|--------|
| URL | http://localhost:9092 |
| Utilisateur | `admin` |
| Mot de passe | `admin123` |

Job pipeline : **travel** → http://localhost:9092/job/travel/

### SonarQube

| Champ | Valeur |
|-------|--------|
| URL | http://localhost:9002 |
| Utilisateur | `admin` |
| Mot de passe | `admin123` |

> Au premier accès, SonarQube peut demander un changement de mot de passe. Les scripts CI utilisent la valeur définie dans `.env.ci` (`SONAR_ADMIN_PASSWORD`).

### Application (frontend admin)

| Champ | Valeur |
|-------|--------|
| URL | https://localhost:4200 |
| Compte démo | `test@travel.com` / `password123` |

Pour accéder à l'interface admin, promouvoir le compte en ADMIN :

```bash
docker exec -it travel-postgres psql -U travel -d travel_db \
  -c "UPDATE \"user\".users SET role = 'ADMIN' WHERE email = 'test@travel.com';"
```

Se reconnecter ensuite sur https://localhost:4200.

### Base de données (PostgreSQL)

| Champ | Valeur |
|-------|--------|
| Utilisateur | `travel` |
| Mot de passe | `travel_secret` |
| Base | `travel_db` |

### Neo4j

| Champ | Valeur |
|-------|--------|
| Utilisateur | `neo4j` |
| Mot de passe | `neo4j_secret` |

---

## URLs des services

| Service | URL |
|---------|-----|
| Frontend Angular | https://localhost:4200 |
| API Gateway | https://localhost:8080 |
| Eureka | http://localhost:8761 |
| Jenkins | http://localhost:9092 |
| SonarQube | http://localhost:9002 |

> Accepter le certificat auto-signé pour les URLs en `https://`.

---

## Arrêter le projet

```bash
# Stack applicative
docker compose -f infrastructure/docker-compose.yml down

# CI (Jenkins + SonarQube)
docker compose -f jenkins-compose.yml down
```

Reset complet des données (BDD) :

```bash
docker compose -f infrastructure/docker-compose.yml down -v
```

---

## Documentation complémentaire

| Fichier | Contenu |
|---------|---------|
| `infrastructure/README.md` | Infrastructure Docker |
| `docs/CI-AUDIT-SETUP.md` | Pipeline CI/CD et audit |
| `docs/AUDIT-COMPLIANCE.md` | Conformité audit |
| `ansible/README.md` | Déploiement Ansible |
