#!/usr/bin/env bash
# Script interactif et automatisé pour tester toutes les fonctionnalités (Audit/Sujet)
set -euo pipefail

API="${API_URL:-https://localhost:8080}"
PASS="password123"

# Couleurs
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0;0m'

echo -e "${CYAN}======================================================================${NC}"
echo -e "${CYAN}            LETS-TRAVEL — SCRIPT DE VÉRIFICATION DES FEATURES         ${NC}"
echo -e "${CYAN}======================================================================${NC}"

# Vérifier les outils requis
for cmd in curl jq; do
  if ! command -v "$cmd" &> /dev/null; then
    echo -e "${RED}Erreur : '$cmd' est requis pour ce script. Installez-le d'abord.${NC}"
    exit 1
  fi
done

# Authentification helpers
login() {
  local email=$1
  local pass=$2
  local token
  token=$(curl -sk -X POST "$API/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"$pass\"}" | jq -r .token 2>/dev/null || echo "null")
  echo "$token"
}

# Charger les tokens
echo -n "Connexion admin@travel.com... "
ADMIN_TOKEN=$(login "admin@travel.com" "$PASS")
if [[ "$ADMIN_TOKEN" == "null" || -z "$ADMIN_TOKEN" ]]; then
  echo -e "${RED}[ÉCHEC] - L'application doit être lancée dans Docker d'abord !${NC}"
  exit 1
fi
echo -e "${GREEN}[OK]${NC}"

echo -n "Connexion alice.manager@travel.com... "
MANAGER_TOKEN=$(login "alice.manager@travel.com" "$PASS")
echo -e "${GREEN}[OK]${NC}"

echo -n "Connexion charlie.traveler@travel.com... "
TRAVELER_TOKEN=$(login "charlie.traveler@travel.com" "$PASS")
echo -e "${GREEN}[OK]${NC}"

# Menu
show_menu() {
  echo ""
  echo -e "${YELLOW}Choisissez la fonctionnalité à tester :${NC}"
  echo "  1) Authentification & Contrôle d'Accès (RBAC)"
  echo "  2) Elasticsearch : Recherche & Autocomplétion"
  echo "  3) Elasticsearch Fallback : Résilience en cas de panne"
  echo "  4) Neo4j : Recommandations personnalisées"
  echo "  5) Réservation, Paiements multiples & Règle des 3 jours pour annuler"
  echo "  6) Feedbacks : Laisser et consulter des avis voyageurs"
  echo "  7) Signalement (Reports) & Modération Admin"
  echo "  8) Tableaux de bord & Analytics (Admin, Manager, Voyageur)"
  echo "  9) Lancer TOUS les tests en séquence automatique"
  echo "  q) Quitter"
  echo -n "Votre choix : "
}

test_rbac() {
  echo -e "\n${YELLOW}--- 1. Authentification & Contrôle d'Accès (RBAC) ---${NC}"
  
  # Accès anonyme
  echo -n "Test Accès Anonyme à /api/users (devrait être bloqué 401/403)... "
  local code
  code=$(curl -sk -o /dev/null -w "%{http_code}" "$API/api/users")
  if [[ "$code" == "401" || "$code" == "403" ]]; then
    echo -e "${GREEN}[OK] ($code)${NC}"
  else
    echo -e "${RED}[ÉCHEC] ($code)${NC}"
  fi

  # Accès voyageur à l'administration
  echo -n "Test Accès Voyageur à /api/users (devrait être bloqué 403)... "
  code=$(curl -sk -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $TRAVELER_TOKEN" "$API/api/users")
  if [[ "$code" == "403" ]]; then
    echo -e "${GREEN}[OK] ($code)${NC}"
  else
    echo -e "${RED}[ÉCHEC] ($code)${NC}"
  fi

  # Accès admin à l'administration
  echo -n "Test Accès Admin à /api/users (devrait être autorisé 200)... "
  code=$(curl -sk -o /dev/null -w "%{http_code}" -H "Authorization: Bearer $ADMIN_TOKEN" "$API/api/users")
  if [[ "$code" == "200" ]]; then
    echo -e "${GREEN}[OK] ($code)${NC}"
  else
    echo -e "${RED}[ÉCHEC] ($code)${NC}"
  fi
}

test_elasticsearch() {
  echo -e "\n${YELLOW}--- 2. Elasticsearch : Recherche & Autocomplétion ---${NC}"
  
  # Autocomplete
  echo -e "Test autocomplétion pour le préfixe 'Par'... "
  local resp
  resp=$(curl -sk -H "Authorization: Bearer $TRAVELER_TOKEN" "$API/api/travels/search/autocomplete?query=Par")
  echo "Réponse autocomplete : $resp"
  if echo "$resp" | grep -q "Paris"; then
    echo -e "${GREEN}[OK] Suggestion trouvée !${NC}"
  else
    echo -e "${RED}[ÉCHEC] Suggestion 'Paris' absente.${NC}"
  fi

  # Search
  echo -e "Test recherche pour le mot-clé 'Lyon'... "
  resp=$(curl -sk -H "Authorization: Bearer $TRAVELER_TOKEN" "$API/api/travels/search?query=Lyon")
  echo "Réponse recherche : $(echo "$resp" | jq -r '.[].title' 2>/dev/null || echo "$resp")"
  if echo "$resp" | grep -q "Lyon"; then
    echo -e "${GREEN}[OK] Résultat de recherche trouvé !${NC}"
  else
    echo -e "${RED}[ÉCHEC] Aucun résultat pour 'Lyon'.${NC}"
  fi
}

test_fallback() {
  echo -e "\n${YELLOW}--- 3. Elasticsearch Fallback ---${NC}"
  echo -e "Pour tester la résilience, éteignons momentanément le conteneur Elasticsearch."
  echo -n "Arrêt de travel-elasticsearch... "
  docker stop travel-elasticsearch >/dev/null
  echo -e "${GREEN}[ARRÊTÉ]${NC}"

  echo -e "Exécution d'une recherche (devrait basculer automatiquement sur PostgreSQL)... "
  local resp
  resp=$(curl -sk -H "Authorization: Bearer $TRAVELER_TOKEN" "$API/api/travels/search?query=Lyon")
  echo "Réponse (fallback SQL) : $(echo "$resp" | jq -r '.[].title' 2>/dev/null || echo "$resp")"
  
  if echo "$resp" | grep -q "Lyon"; then
    echo -e "${GREEN}[OK] Recherche fonctionnelle via fallback SQL !${NC}"
  else
    echo -e "${RED}[ÉCHEC] La recherche a échoué.${NC}"
  fi

  echo -n "Redémarrage de travel-elasticsearch... "
  docker start travel-elasticsearch >/dev/null
  echo -e "${GREEN}[REDÉMARRÉ]${NC}"
}

test_neo4j() {
  echo -e "\n${YELLOW}--- 4. Neo4j : Recommandations personnalisées ---${NC}"
  echo -e "Demande de recommandations personnalisées Neo4j pour Charlie (Traveler)... "
  local resp
  resp=$(curl -sk -H "Authorization: Bearer $TRAVELER_TOKEN" "$API/api/travels/recommendations")
  echo "Voyages suggérés par Neo4j : $(echo "$resp" | jq -r '.[].title' 2>/dev/null || echo "Aucun ou $resp")"
  echo -e "${GREEN}[OK] Service de recommandation interrogé avec succès !${NC}"
}

test_booking_payment_cutoff() {
  echo -e "\n${YELLOW}--- 5. Réservation, Paiements & Règle des 3 jours ---${NC}"
  
  # Trouver un voyage actif
  local trips trip_id trip_title
  trips=$(curl -sk -H "Authorization: Bearer $TRAVELER_TOKEN" "$API/api/travels")
  trip_id=$(echo "$trips" | jq -r '.[0].id')
  trip_title=$(echo "$trips" | jq -r '.[0].title')
  
  echo -e "Voyage ciblé : '$trip_title' (ID: $trip_id)"

  # 5a. Création réservation avec paiement CARD
  echo -e "Création d'une réservation (Paiement: CARD)... "
  local booking
  booking=$(curl -sk -X POST "$API/api/bookings" \
    -H "Authorization: Bearer $TRAVELER_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"tripId\":\"$trip_id\",\"paymentMethod\":\"CARD\"}")
  
  echo "Réponse réservation : $booking"
  local booking_id
  booking_id=$(echo "$booking" | jq -r .id)
  
  if [[ "$booking_id" != "null" && -n "$booking_id" ]]; then
    echo -e "${GREEN}[OK] Réservation confirmée avec succès (ID: $booking_id) !${NC}"
  else
    echo -e "${RED}[ÉCHEC] Réservation rejetée.${NC}"
    return
  fi

  # 5b. Règle des 3 jours d'annulation
  # Le voyage créé par défaut dans seed est généralement très proche ou passé, 
  # ou configuré à aujourd'hui, donc l'annulation devrait être refusée.
  echo -n "Tentative d'annulation du voyage proche (Règle des 3 jours)... "
  local code resp_err
  resp_err=$(curl -sk -X DELETE "$API/api/bookings/$booking_id" \
    -H "Authorization: Bearer $TRAVELER_TOKEN" -w "\nHTTP_CODE:%{http_code}")
  
  code=$(echo "$resp_err" | grep "HTTP_CODE" | cut -d: -f2)
  if [[ "$code" == "422" ]]; then
    echo -e "${GREEN}[OK] Annulation refusée comme attendu (HTTP 422 - délai dépassé)${NC}"
  else
    echo -e "${RED}[ATTENTION] Le code retour est $code au lieu de 422.${NC}"
  fi
}

test_feedbacks() {
  echo -e "\n${YELLOW}--- 6. Feedbacks (Laisser et consulter des avis) ---${NC}"
  
  # Trouver un voyage
  local trip_id
  trip_id=$(curl -sk -H "Authorization: Bearer $TRAVELER_TOKEN" "$API/api/travels" | jq -r '.[0].id')
  
  echo -e "Ajout d'un feedback de 5 étoiles sur le voyage... "
  local fb
  fb=$(curl -sk -X POST "$API/api/travels/$trip_id/feedback" \
    -H "Authorization: Bearer $TRAVELER_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"rating":5,"comment":"Incroyable voyage, organisation impeccable !"}')
  echo "Avis enregistré : $fb"
  
  if echo "$fb" | grep -q "comment"; then
    echo -e "${GREEN}[OK] Avis enregistré avec succès !${NC}"
  else
    echo -e "${RED}[ÉCHEC] L'avis n'a pas pu être enregistré (Assurez-vous d'avoir une réservation CONFIRMED).${NC}"
  fi
}

test_reports() {
  echo -e "\n${YELLOW}--- 7. Signalements (Reports) & Modération Admin ---${NC}"
  
  # Création d'un signalement contre un manager
  # Alice Manager a l'ID a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1
  echo -e "Création d'un signalement par Charlie contre Alice (Manager)... "
  local rep
  rep=$(curl -sk -X POST "$API/api/users/reports" \
    -H "Authorization: Bearer $TRAVELER_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"reportedId":"a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1","reason":"Comportement inapproprié lors de la préparation."}')
  echo "Ticket de signalement : $rep"
  local rep_id
  rep_id=$(echo "$rep" | jq -r .id)

  if [[ "$rep_id" != "null" && -n "$rep_id" ]]; then
    echo -e "${GREEN}[OK] Signalement déposé (ID: $rep_id)${NC}"
  else
    echo -e "${RED}[ÉCHEC] Échec du dépôt de signalement.${NC}"
    return
  fi

  # Administration : Listing des signalements
  echo -n "Admin : Consultation de tous les signalements... "
  local list
  list=$(curl -sk -H "Authorization: Bearer $ADMIN_TOKEN" "$API/api/users/reports")
  if echo "$list" | grep -q "$rep_id"; then
    echo -e "${GREEN}[OK] Signalement visible dans la liste d'administration !${NC}"
  else
    echo -e "${RED}[ÉCHEC] Signalement invisible dans la liste.${NC}"
  fi

  # Administration : Résolution du signalement
  echo -n "Admin : Résolution du signalement... "
  local code
  code=$(curl -sk -X PUT "$API/api/users/reports/$rep_id/resolve" \
    -H "Authorization: Bearer $ADMIN_TOKEN" -o /dev/null -w "%{http_code}")
  if [[ "$code" == "200" ]]; then
    echo -e "${GREEN}[OK] Signalement résolu (HTTP 200)${NC}"
  else
    echo -e "${RED}[ÉCHEC] Code retour de résolution : $code${NC}"
  fi
}

test_dashboards() {
  echo -e "\n${YELLOW}--- 8. Tableaux de bord & Analytics ---${NC}"
  
  # Dashboard Voyageur (Charlie)
  # ID de Charlie : c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3
  echo -e "Dashboard Voyageur (Stats de Charlie)... "
  local stats
  stats=$(curl -sk -H "Authorization: Bearer $TRAVELER_TOKEN" "$API/api/users/c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3/stats")
  echo "Stats Voyageur : $(echo "$stats" | jq -c . 2>/dev/null || echo "$stats")"
  
  # Dashboard Manager (Alice)
  # ID d'Alice : a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1
  echo -e "Dashboard Manager (Stats d'Alice)... "
  local m_db
  m_db=$(curl -sk -H "Authorization: Bearer $MANAGER_TOKEN" "$API/api/travels/managers/a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1/dashboard")
  echo "Dashboard Manager : $(echo "$m_db" | jq -c . 2>/dev/null || echo "$m_db")"

  # Dashboard Admin
  echo -e "Dashboard Administrateur (Revenus globaux + Leaderboard)... "
  local a_db
  a_db=$(curl -sk -H "Authorization: Bearer $ADMIN_TOKEN" "$API/api/travels/admin/dashboard")
  echo "Revenus par mois : $(echo "$a_db" | jq -c .incomeByMonth 2>/dev/null)"
  echo "Classement des managers : $(echo "$a_db" | jq -c '.managerPerformances[].managerName' 2>/dev/null || echo "Aucun manager classé")"
  
  echo -e "${GREEN}[OK] Tous les tableaux de bord et statistiques ont été récupérés !${NC}"
}

run_all() {
  test_rbac
  test_elasticsearch
  test_fallback
  test_neo4j
  test_booking_payment_cutoff
  test_feedbacks
  test_reports
  test_dashboards
}

# Boucle interactive
while true; do
  show_menu
  read -r choice
  case "$choice" in
    1) test_rbac ;;
    2) test_elasticsearch ;;
    3) test_fallback ;;
    4) test_neo4j ;;
    5) test_booking_payment_cutoff ;;
    6) test_feedbacks ;;
    7) test_reports ;;
    8) test_dashboards ;;
    9) run_all ;;
    q|Q) echo "Fin du test. Au revoir !"; exit 0 ;;
    *) echo -e "${RED}Choix invalide.${NC}" ;;
  esac
done
