# Lets Travel — Guide de Lancement et Arrêt du Projet

Ce guide décrit les commandes nécessaires pour démarrer et arrêter l'application, en local ou via l'environnement conteneurisé.

## 1. Démarrage de l'Application (Stack Complet)

Pour lancer l'ensemble des services (base de données, moteurs de recherche, microservices et frontend Angular), utilisez les commandes suivantes depuis la racine du projet :

```bash
# Copier le fichier d'environnement (si ce n'est pas déjà fait)
cp .env.example .env

# Lancer la compilation et le démarrage de tous les conteneurs en arrière-plan
sudo docker compose -f infrastructure/docker-compose.yml --project-name travel up --build -d
```

### URLs d'accès local
Une fois les conteneurs démarrés et sains (`healthy`), vous pouvez accéder aux services :
- **Eureka (Discovery)** : [http://localhost:8761](http://localhost:8761)
- **Gateway API (Sécurisée)** : [https://localhost:8080](https://localhost:8080)
- **Application Web Frontend (Angular SSL)** : [https://localhost:4200](https://localhost:4200)
- **Base de données Neo4j Browser** : [http://localhost:7474](http://localhost:7474)
- **Elasticsearch Engine** : [http://localhost:9200](http://localhost:9200)

---

## 2. Arrêt de l'Application

### Arrêt Standard
Pour arrêter les conteneurs sans perdre les données stockées dans la base de données :

```bash
sudo docker compose -f infrastructure/docker-compose.yml --project-name travel down
```

### Réinitialisation Complète (Nettoyage des Volumes)
Pour arrêter l'application et supprimer toutes les données de démonstration (PostgreSQL, Neo4j) afin de repartir sur une base propre lors du prochain démarrage :

```bash
docker compose -f infrastructure/docker-compose.yml --project-name travel down -v
```

---

## 3. Développement et Lancement Local (Hybride)

Si vous souhaitez modifier et déboguer des microservices ou le frontend en dehors de Docker :

### Étape A : Lancer uniquement les services d'infrastructure dans Docker
```bash
docker compose -f infrastructure/docker-compose.yml --project-name travel up -d postgres neo4j elasticsearch discovery-service
```

### Étape B : Lancer les microservices Backend
Chaque service peut être démarré depuis son dossier via Maven :
```bash
cd microservices/user-service
mvn spring-boot:run
```

### Étape C : Lancer le Frontend Angular
```bash
cd frontend/travel-admin
npm install
npm run start
```
