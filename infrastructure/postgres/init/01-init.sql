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
    role       VARCHAR(20) NOT NULL DEFAULT 'USER',
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
    trip_id    UUID NOT NULL REFERENCES travel.trips(id),
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
    trip_id    UUID NOT NULL REFERENCES travel.trips(id),
    user_id    UUID NOT NULL,
    rating     INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment    TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_feedbacks_trip_user ON travel.feedbacks(trip_id, user_id);
CREATE INDEX idx_feedbacks_user_id ON travel.feedbacks(user_id);

-- travel.reports
CREATE TABLE travel.reports (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    manager_id  UUID NOT NULL,
    reporter_id UUID NOT NULL,
    reason      TEXT NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_reports_manager_reporter UNIQUE (manager_id, reporter_id)
);

CREATE INDEX idx_reports_manager_id ON travel.reports(manager_id);

-- payment.payments
CREATE TABLE payment.payments (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    booking_id UUID NOT NULL,
    user_id    UUID NOT NULL,
    amount     DECIMAL(10,2) NOT NULL CHECK (amount > 0),
    status     VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_payments_booking_id ON payment.payments(booking_id);
CREATE INDEX idx_payments_user_id ON payment.payments(user_id);

-- Données de démo (catalogue voyages)
INSERT INTO travel.trips (title, origin_city, destination_city, departure_date, price, seats_available)
VALUES
    ('Paris → Londres', 'Paris', 'London', CURRENT_DATE + 30, 89.00, 50),
    ('Paris → Tokyo', 'Paris', 'Tokyo', CURRENT_DATE + 60, 650.00, 20),
    ('Londres → New York', 'London', 'New York', CURRENT_DATE + 45, 450.00, 30);
