-- GDPR: la suppression de compte (anonymizeAndDelete) doit pouvoir mettre ces
-- FK utilisateur à NULL. Sans ça, PostgreSQL rejette l'UPDATE pour violation
-- de contrainte NOT NULL, ce qui avorte toute la transaction et annule aussi
-- l'anonymisation déjà effectuée dans user.users / auth.users_auth.

ALTER TABLE travel.bookings ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE travel.feedbacks ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE payment.payments ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE "user".reports ALTER COLUMN reporter_id DROP NOT NULL;
