-- =============================================================
-- Train Booking System - Production-grade MySQL schema + dataset
-- Compatible with Spring Boot JPA (InnoDB, FK constraints, indexes)
-- =============================================================

CREATE DATABASE IF NOT EXISTS train_booking
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE train_booking;

SET NAMES utf8mb4;
SET time_zone = '+00:00';

-- Drop in FK-safe order for repeatable local runs
DROP TABLE IF EXISTS idempotency_records;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS seats;
DROP TABLE IF EXISTS trains;
DROP TABLE IF EXISTS users;

-- =============================================================
-- 1) USERS
-- =============================================================
CREATE TABLE users (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name          VARCHAR(120)    NOT NULL,
  email         VARCHAR(190)    NOT NULL,
  created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email)
) ENGINE=InnoDB;

-- =============================================================
-- 2) TRAINS
-- =============================================================
CREATE TABLE trains (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  train_number  VARCHAR(20)     NOT NULL,
  train_name    VARCHAR(150)    NOT NULL,
  source        VARCHAR(80)     NOT NULL,
  destination   VARCHAR(80)     NOT NULL,
  created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_trains_train_number (train_number)
) ENGINE=InnoDB;

-- =============================================================
-- 3) SEATS (critical concurrency table)
-- =============================================================
CREATE TABLE seats (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  train_id        BIGINT UNSIGNED NOT NULL,
  seat_number     VARCHAR(20)     NOT NULL,
  status          ENUM('AVAILABLE','HELD','BOOKED') NOT NULL DEFAULT 'AVAILABLE',
  locked_by       BIGINT UNSIGNED NULL,
  lock_expiry     TIMESTAMP NULL DEFAULT NULL,
  version         BIGINT UNSIGNED NOT NULL DEFAULT 0,
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_seats_train FOREIGN KEY (train_id) REFERENCES trains(id),
  CONSTRAINT fk_seats_locked_by FOREIGN KEY (locked_by) REFERENCES users(id),
  CONSTRAINT uk_seats_train_seat UNIQUE (train_id, seat_number),
  KEY idx_seats_train_status (train_id, status),
  KEY idx_seats_status (status),
  KEY idx_seats_lock_expiry (lock_expiry)
) ENGINE=InnoDB;

-- =============================================================
-- 4) BOOKINGS
-- =============================================================
CREATE TABLE bookings (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id         BIGINT UNSIGNED NOT NULL,
  seat_id         BIGINT UNSIGNED NOT NULL,
  train_id        BIGINT UNSIGNED NOT NULL,
  status          ENUM('CONFIRMED','FAILED') NOT NULL DEFAULT 'CONFIRMED',
  created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  CONSTRAINT fk_bookings_user FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_bookings_seat FOREIGN KEY (seat_id) REFERENCES seats(id),
  CONSTRAINT fk_bookings_train FOREIGN KEY (train_id) REFERENCES trains(id),
  CONSTRAINT uk_bookings_seat UNIQUE (seat_id),
  KEY idx_bookings_user_id (user_id),
  KEY idx_bookings_train_created (train_id, created_at)
) ENGINE=InnoDB;

-- =============================================================
-- 5) IDEMPOTENCY
-- =============================================================
CREATE TABLE idempotency_records (
  idempotency_key VARCHAR(100) NOT NULL,
  response        JSON         NOT NULL,
  created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (idempotency_key),
  KEY idx_idempotency_key (idempotency_key),
  KEY idx_idempotency_created_at (created_at)
) ENGINE=InnoDB;

-- =============================================================
-- SAMPLE DATA
-- 10 users, 3 trains, 50 seats each, a few pre-booked seats
-- =============================================================
INSERT INTO users(name, email) VALUES
('Aarav Sharma','aarav.sharma@example.com'),
('Diya Patel','diya.patel@example.com'),
('Vivaan Gupta','vivaan.gupta@example.com'),
('Anaya Nair','anaya.nair@example.com'),
('Ishaan Reddy','ishaan.reddy@example.com'),
('Saanvi Mehta','saanvi.mehta@example.com'),
('Arjun Singh','arjun.singh@example.com'),
('Myra Rao','myra.rao@example.com'),
('Kabir Verma','kabir.verma@example.com'),
('Aadhya Iyer','aadhya.iyer@example.com');

INSERT INTO trains(train_number, train_name, source, destination) VALUES
('12951', 'Mumbai Rajdhani Express', 'Mumbai', 'Delhi'),
('12628', 'Karnataka Express', 'Bengaluru', 'New Delhi'),
('11077', 'Jhelum Express', 'Pune', 'Jammu Tawi');

-- Generate 50 seats per train (S1..S50) using recursive CTE (MySQL 8+)
WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 50
)
INSERT INTO seats(train_id, seat_number, status)
SELECT 1, CONCAT('S', LPAD(n,2,'0')), 'AVAILABLE' FROM seq;

WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 50
)
INSERT INTO seats(train_id, seat_number, status)
SELECT 2, CONCAT('S', LPAD(n,2,'0')), 'AVAILABLE' FROM seq;

WITH RECURSIVE seq AS (
  SELECT 1 AS n
  UNION ALL
  SELECT n + 1 FROM seq WHERE n < 50
)
INSERT INTO seats(train_id, seat_number, status)
SELECT 3, CONCAT('S', LPAD(n,2,'0')), 'AVAILABLE' FROM seq;

-- Pre-booked seats
UPDATE seats
   SET status='BOOKED', locked_by=NULL, lock_expiry=NULL, version=version+1
 WHERE (train_id=1 AND seat_number IN ('S01','S02','S03'))
    OR (train_id=2 AND seat_number IN ('S04','S05'))
    OR (train_id=3 AND seat_number IN ('S10','S11'));

INSERT INTO bookings(user_id, seat_id, train_id, status)
SELECT 1, s.id, s.train_id, 'CONFIRMED' FROM seats s WHERE s.train_id=1 AND s.seat_number='S01'
UNION ALL
SELECT 2, s.id, s.train_id, 'CONFIRMED' FROM seats s WHERE s.train_id=1 AND s.seat_number='S02'
UNION ALL
SELECT 3, s.id, s.train_id, 'CONFIRMED' FROM seats s WHERE s.train_id=1 AND s.seat_number='S03'
UNION ALL
SELECT 4, s.id, s.train_id, 'CONFIRMED' FROM seats s WHERE s.train_id=2 AND s.seat_number='S04'
UNION ALL
SELECT 5, s.id, s.train_id, 'CONFIRMED' FROM seats s WHERE s.train_id=2 AND s.seat_number='S05'
UNION ALL
SELECT 6, s.id, s.train_id, 'CONFIRMED' FROM seats s WHERE s.train_id=3 AND s.seat_number='S10'
UNION ALL
SELECT 7, s.id, s.train_id, 'CONFIRMED' FROM seats s WHERE s.train_id=3 AND s.seat_number='S11';

INSERT INTO idempotency_records(idempotency_key, response) VALUES
('idem-0001', JSON_OBJECT('bookingId', 1, 'status', 'CONFIRMED', 'message', 'Idempotent replay served')),
('idem-0002', JSON_OBJECT('bookingId', 2, 'status', 'CONFIRMED', 'message', 'Idempotent replay served'));

-- =============================================================
-- EXAMPLE QUERIES (operational)
-- =============================================================

-- A) Check seat availability for a train
-- (fast via idx_seats_train_status)
SELECT id, seat_number, status, locked_by, lock_expiry
  FROM seats
 WHERE train_id = 1
   AND (
        status = 'AVAILABLE'
        OR (status = 'HELD' AND lock_expiry < UTC_TIMESTAMP())
       )
 ORDER BY seat_number;

-- B) Hold seat for 5 minutes (atomic conditional update)
-- returns affected_rows = 1 when hold succeeds
UPDATE seats
   SET status = 'HELD',
       locked_by = 8,
       lock_expiry = UTC_TIMESTAMP() + INTERVAL 5 MINUTE,
       version = version + 1
 WHERE train_id = 1
   AND seat_number = 'S12'
   AND (
        status = 'AVAILABLE'
        OR (status = 'HELD' AND lock_expiry < UTC_TIMESTAMP())
       );

-- C) Book seat transaction (SELECT ... FOR UPDATE pattern)
-- NOTE: run as a transaction from app/service layer.
-- START TRANSACTION;
-- SELECT id, status, locked_by, lock_expiry
--   FROM seats
--  WHERE train_id = 1 AND seat_number = 'S12'
--  FOR UPDATE;
--
-- -- validate in app/sql logic:
-- -- status='HELD' AND locked_by=8 AND lock_expiry >= UTC_TIMESTAMP()
--
-- UPDATE seats
--    SET status='BOOKED', locked_by=NULL, lock_expiry=NULL, version=version+1
--  WHERE train_id=1 AND seat_number='S12';
--
-- INSERT INTO bookings(user_id, seat_id, train_id, status)
-- SELECT 8, s.id, s.train_id, 'CONFIRMED'
--   FROM seats s
--  WHERE s.train_id=1 AND s.seat_number='S12';
-- COMMIT;

-- D) Release expired holds (batch job)
UPDATE seats
   SET status='AVAILABLE', locked_by=NULL, lock_expiry=NULL, version=version+1
 WHERE status='HELD'
   AND lock_expiry < UTC_TIMESTAMP();

-- =============================================================
-- BONUS: Stored procedure with deadlock retry
-- - handles SQLSTATE 40001 / ER_LOCK_DEADLOCK(1213)
-- - max 3 retries
-- =============================================================
DELIMITER $$

CREATE PROCEDURE book_seat_atomic(
    IN  p_user_id BIGINT UNSIGNED,
    IN  p_train_id BIGINT UNSIGNED,
    IN  p_seat_number VARCHAR(20),
    IN  p_idempotency_key VARCHAR(100),
    OUT o_booking_id BIGINT UNSIGNED,
    OUT o_result_message VARCHAR(255)
)
proc: BEGIN
    DECLARE v_attempt INT DEFAULT 0;
    DECLARE v_max_attempt INT DEFAULT 3;
    DECLARE v_seat_id BIGINT UNSIGNED;
    DECLARE v_status VARCHAR(10);
    DECLARE v_locked_by BIGINT UNSIGNED;
    DECLARE v_lock_expiry TIMESTAMP;
    DECLARE v_existing_booking BIGINT UNSIGNED;
    DECLARE v_retry BOOL DEFAULT FALSE;

    SET o_booking_id = NULL;
    SET o_result_message = NULL;

    -- Idempotency fast path
    SELECT JSON_UNQUOTE(JSON_EXTRACT(response, '$.bookingId'))
      INTO v_existing_booking
      FROM idempotency_records
     WHERE idempotency_key = p_idempotency_key
     LIMIT 1;

    IF v_existing_booking IS NOT NULL THEN
        SET o_booking_id = v_existing_booking;
        SET o_result_message = 'IDEMPOTENT_REPLAY';
        LEAVE proc;
    END IF;

    retry_loop: WHILE v_attempt < v_max_attempt DO
        SET v_attempt = v_attempt + 1;
        SET v_retry = FALSE;

        BEGIN
            DECLARE CONTINUE HANDLER FOR 1213, SQLSTATE '40001'
            BEGIN
                ROLLBACK;
                SET v_retry = TRUE;
            END;

            START TRANSACTION;

            SELECT id, status, locked_by, lock_expiry
              INTO v_seat_id, v_status, v_locked_by, v_lock_expiry
              FROM seats
             WHERE train_id = p_train_id
               AND seat_number = p_seat_number
             FOR UPDATE;

            IF v_seat_id IS NULL THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'SEAT_NOT_FOUND';
            END IF;

            IF v_status = 'BOOKED' THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'ALREADY_BOOKED';
            END IF;

            IF v_status = 'HELD' AND (v_lock_expiry >= UTC_TIMESTAMP()) AND (v_locked_by <> p_user_id) THEN
                SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'HELD_BY_OTHER_USER';
            END IF;

            -- If available or expired hold, set/refresh hold to caller before booking
            UPDATE seats
               SET status='HELD',
                   locked_by=p_user_id,
                   lock_expiry=UTC_TIMESTAMP() + INTERVAL 5 MINUTE,
                   version=version+1
             WHERE id = v_seat_id;

            -- Confirm booking
            UPDATE seats
               SET status='BOOKED',
                   locked_by=NULL,
                   lock_expiry=NULL,
                   version=version+1
             WHERE id = v_seat_id;

            INSERT INTO bookings(user_id, seat_id, train_id, status)
            VALUES (p_user_id, v_seat_id, p_train_id, 'CONFIRMED');

            SET o_booking_id = LAST_INSERT_ID();
            SET o_result_message = 'CONFIRMED';

            INSERT INTO idempotency_records(idempotency_key, response)
            VALUES (
                p_idempotency_key,
                JSON_OBJECT('bookingId', o_booking_id, 'status', 'CONFIRMED', 'seatId', v_seat_id)
            );

            COMMIT;
        END;

        IF v_retry = FALSE THEN
            LEAVE retry_loop;
        END IF;
    END WHILE;

    IF o_booking_id IS NULL AND o_result_message IS NULL THEN
        SET o_result_message = 'RETRY_EXHAUSTED_DUE_TO_DEADLOCK';
    END IF;
END$$

DELIMITER ;

-- Example call:
-- CALL book_seat_atomic(8, 1, 'S12', 'idem-9001', @booking_id, @result);
-- SELECT @booking_id, @result;
