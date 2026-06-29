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

## 🔑 Identifiants de Connexion

Voici un tableau récapitulatif de tous les comptes, identifiants et mots de passe par défaut pour accéder aux différents services :

### 🖥️ Services et Infrastructure

| Service | URL | Utilisateur / Email | Mot de passe | Notes / Rôles |
| :--- | :--- | :--- | :--- | :--- |
| **Neo4j Browser** | [http://localhost:7474](http://localhost:7474) | `neo4j` | `neo4j_secret` | Visualiseur de base graphe |
| **PostgreSQL** | `localhost:5432` | `travel` | `travel_secret` | Base de données : `travel_db` |
| **Jenkins** | [http://localhost:9092](http://localhost:9092) | `admin` | `admin123` | Serveur CI/CD (Job : `travel`) |
| **SonarQube** | [http://localhost:9002](http://localhost:9002) | `admin` | `admin123` | Changement requis au premier accès |

### ✈️ Comptes Applicatifs de Test (Pré-remplis)

L'application est pré-initialisée avec les comptes de démonstration et de test suivants (tous utilisent le mot de passe **`password123`**) :

| Rôle / Type | Adresse Email | Mot de passe | Description |
| :--- | :--- | :--- | :--- |
| **Administrateur** | `admin@travel.com` | `password123` | Accès total au tableau de bord admin |
| **Travel Manager** | `alice.manager@travel.com` | `password123` | Gère ses propres voyages et subscribers |
| **Travel Manager** | `bob.manager@travel.com` | `password123` | Autre manager de démo |
| **Voyageur (Traveler)** | `charlie.traveler@travel.com` | `password123` | Compte voyageur avec réservations actives |
| **Voyageur (Traveler)** | `david.traveler@travel.com` | `password123` | Compte voyageur avec réservations actives |

> 💡 **Créer un autre Admin :** Si vous créez un compte par vous-même (ex: `test@travel.com`), vous pouvez le promouvoir en `ADMIN` en exécutant :
> ```bash
> docker exec -it travel-postgres psql -U travel -d travel_db -c "UPDATE \"user\".users SET role = 'ADMIN' WHERE email = 'votre_email@travel.com';"
> ```



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
