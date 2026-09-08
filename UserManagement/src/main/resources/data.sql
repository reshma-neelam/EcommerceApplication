-- Seed roles for the H2 demo profile (Flyway disabled, Hibernate owns schema).
-- Runs only against embedded databases (spring.sql.init.mode defaults to embedded),
-- so it never executes against MySQL where Flyway already seeds these rows.
INSERT INTO role (id, name, description) VALUES
    ('11111111-1111-1111-1111-111111111111', 'CUSTOMER', 'Standard customer role'),
    ('22222222-2222-2222-2222-222222222222', 'ADMIN', 'Administrative role');
