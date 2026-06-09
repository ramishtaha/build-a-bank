-- steps/step-08/seed.sql — demo customers to play with.
-- Load into your running Postgres, e.g.:
--   docker exec -i cif-postgres psql -U bank -d cif < steps/step-08/seed.sql
-- (Run AFTER the CIF service has started once, so Flyway has created the table.)

insert into customer (customer_number, first_name, last_name, email, date_of_birth, kyc_status, created_at)
values
  ('CIF-DEMO001', 'Ada',   'Lovelace', 'ada@demo.bank',   '1990-05-17', 'VERIFIED', now()),
  ('CIF-DEMO002', 'Alan',  'Turing',   'alan@demo.bank',  '1992-06-23', 'PENDING',  now()),
  ('CIF-DEMO003', 'Grace', 'Hopper',   'grace@demo.bank', '1906-12-09', 'VERIFIED', now())
on conflict (customer_number) do nothing;
