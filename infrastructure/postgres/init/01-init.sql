-- Travel Management System — init PostgreSQL (4 schémas)

CREATE SCHEMA IF NOT EXISTS auth;
CREATE SCHEMA IF NOT EXISTS "user";
CREATE SCHEMA IF NOT EXISTS travel;
CREATE SCHEMA IF NOT EXISTS payment;

-- auth.users_auth
CREATE TABLE auth.users_auth (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- user.users
CREATE TABLE "user".users (
    id         UUID PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name  VARCHAR(100) NOT NULL,
    role       VARCHAR(20) NOT NULL DEFAULT 'TRAVELER',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- travel.trips
CREATE TABLE travel.trips (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title            VARCHAR(255) NOT NULL,
    origin_city      VARCHAR(100) NOT NULL,
    destination_city VARCHAR(100) NOT NULL,
    departure_date   DATE NOT NULL,
    price            DECIMAL(10,2) NOT NULL CHECK (price >= 0),
    seats_available  INT NOT NULL CHECK (seats_available >= 0),
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    manager_id       UUID,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_trips_departure ON travel.trips(departure_date);
CREATE INDEX idx_trips_origin_dest ON travel.trips(origin_city, destination_city);

-- travel.bookings
CREATE TABLE travel.bookings (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id    UUID NOT NULL REFERENCES travel.trips(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_bookings_user_id ON travel.bookings(user_id);
CREATE INDEX idx_bookings_trip_id ON travel.bookings(trip_id);

-- travel.feedbacks
CREATE TABLE travel.feedbacks (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trip_id    UUID NOT NULL REFERENCES travel.trips(id) ON DELETE CASCADE,
    user_id    UUID NOT NULL,
    rating     INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_feedbacks_trip ON travel.feedbacks(trip_id);

-- user.reports
CREATE TABLE "user".reports (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL,
    reported_id UUID NOT NULL,
    trip_id     UUID,
    reason      TEXT NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_reports_reported ON "user".reports(reported_id);

-- payment.payments
CREATE TABLE payment.payments (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id     UUID NOT NULL,
    user_id        UUID NOT NULL,
    amount         DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    status         VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    payment_method VARCHAR(50) NOT NULL DEFAULT 'CARD',
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_booking_id ON payment.payments(booking_id);
CREATE INDEX idx_payments_user_id ON payment.payments(user_id);

-- Données de démo (BCrypt hash pour 'password123' : $2a$10$g6x9/Vsk8t5sC3uH8431eOnK24o82Wz/7l1c.3.d8vH0K5cuxQv4y)

-- Admins
INSERT INTO auth.users_auth (id, email, password_hash)
VALUES ('e5e5e5e5-e5e5-e5e5-e5e5-e5e5e5e5e5e5', 'admin@travel.com', '$2a$10$g6x9/Vsk8t5sC3uH8431eOnK24o82Wz/7l1c.3.d8vH0K5cuxQv4y');
INSERT INTO "user".users (id, email, first_name, last_name, role)
VALUES ('e5e5e5e5-e5e5-e5e5-e5e5-e5e5e5e5e5e5', 'admin@travel.com', 'System', 'Admin', 'ADMIN');

-- Managers
INSERT INTO auth.users_auth (id, email, password_hash)
VALUES 
    ('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', 'alice.manager@travel.com', '$2a$10$g6x9/Vsk8t5sC3uH8431eOnK24o82Wz/7l1c.3.d8vH0K5cuxQv4y'),
    ('b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2', 'bob.manager@travel.com', '$2a$10$g6x9/Vsk8t5sC3uH8431eOnK24o82Wz/7l1c.3.d8vH0K5cuxQv4y');
INSERT INTO "user".users (id, email, first_name, last_name, role)
VALUES 
    ('a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1', 'alice.manager@travel.com', 'Alice', 'Manager', 'MANAGER'),
    ('b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2', 'bob.manager@travel.com', 'Bob', 'Manager', 'MANAGER');

-- Travelers
INSERT INTO auth.users_auth (id, email, password_hash)
VALUES 
    ('c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', 'charlie.traveler@travel.com', '$2a$10$g6x9/Vsk8t5sC3uH8431eOnK24o82Wz/7l1c.3.d8vH0K5cuxQv4y'),
    ('d4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d4d4', 'david.traveler@travel.com', '$2a$10$g6x9/Vsk8t5sC3uH8431eOnK24o82Wz/7l1c.3.d8vH0K5cuxQv4y');
INSERT INTO "user".users (id, email, first_name, last_name, role)
VALUES 
    ('c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', 'charlie.traveler@travel.com', 'Charlie', 'Traveler', 'TRAVELER'),
    ('d4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d4d4', 'david.traveler@travel.com', 'David', 'Traveler', 'TRAVELER');

-- Voyages (Trips)
INSERT INTO travel.trips (id, title, origin_city, destination_city, departure_date, price, seats_available, manager_id)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'Paris → London Weekend', 'Paris', 'London', CURRENT_DATE + 30, 89.00, 48, 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1'),
    ('22222222-2222-2222-2222-222222222222', 'Discover Tokyo & Kyoto', 'Paris', 'Tokyo', CURRENT_DATE + 60, 650.00, 18, 'a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1'),
    ('33333333-3333-3333-3333-333333333333', 'London to New York Flight', 'London', 'New York', CURRENT_DATE + 45, 450.00, 29, 'b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2');

-- Réservations & Paiements de démo
-- Charlie réserve Paris -> London (Payé par CARD)
INSERT INTO travel.bookings (id, trip_id, user_id, status, payment_id)
VALUES ('99999999-9999-9999-9999-999999999999', '11111111-1111-1111-1111-111111111111', 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', 'CONFIRMED', '88888888-8888-8888-8888-888888888888');
INSERT INTO payment.payments (id, booking_id, user_id, amount, status, payment_method)
VALUES ('88888888-8888-8888-8888-888888888888', '99999999-9999-9999-9999-999999999999', 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', 89.00, 'COMPLETED', 'CARD');

-- David réserve Discover Tokyo (Payé par PAYPAL)
INSERT INTO travel.bookings (id, trip_id, user_id, status, payment_id)
VALUES ('77777777-7777-7777-7777-777777777777', '22222222-2222-2222-2222-222222222222', 'd4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d4d4', 'CONFIRMED', '66666666-6666-6666-6666-666666666666');
INSERT INTO payment.payments (id, booking_id, user_id, amount, status, payment_method)
VALUES ('66666666-6666-6666-6666-666666666666', '77777777-7777-7777-7777-777777777777', 'd4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d4d4', 650.00, 'COMPLETED', 'PAYPAL');

-- Feedbacks de démo
INSERT INTO travel.feedbacks (trip_id, user_id, rating, comment)
VALUES 
    ('11111111-1111-1111-1111-111111111111', 'c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', 5, 'Super weekend, organisation parfaite !'),
    ('22222222-2222-2222-2222-222222222222', 'd4d4d4d4-d4d4-d4d4-d4d4-d4d4d4d4d4d4', 4, 'Voyage fantastique, mais très fatigant.');

-- Signalements (Reports) de démo
INSERT INTO "user".reports (reporter_id, reported_id, trip_id, reason, status)
VALUES ('c3c3c3c3-c3c3-c3c3-c3c3-c3c3c3c3c3c3', 'b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2', '33333333-3333-3333-3333-333333333333', 'Le manager Bob ne répond pas aux questions concernant le départ.', 'PENDING');
