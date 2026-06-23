// Seed Neo4j — graphe de villes (idempotent avec MERGE)

MERGE (paris:City {name: 'Paris'})
  SET paris.country = 'FR';
MERGE (london:City {name: 'London'})
  SET london.country = 'GB';
MERGE (tokyo:City {name: 'Tokyo'})
  SET tokyo.country = 'JP';
MERGE (newyork:City {name: 'New York'})
  SET newyork.country = 'US';

MERGE (paris)-[r1:CONNECTS_TO]->(london)
  SET r1.duration_min = 75, r1.price = 89.00;
MERGE (london)-[r2:CONNECTS_TO]->(newyork)
  SET r2.duration_min = 480, r2.price = 450.00;
MERGE (paris)-[r3:CONNECTS_TO]->(tokyo)
  SET r3.duration_min = 720, r3.price = 650.00;
MERGE (london)-[r4:CONNECTS_TO]->(tokyo)
  SET r4.duration_min = 660, r4.price = 580.00;
