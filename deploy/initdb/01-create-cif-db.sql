-- deploy/initdb/01-create-cif-db.sql — Step 33
-- The compose Postgres now serves TWO services, each with its OWN database (database-per-service,
-- Step 8): POSTGRES_DB creates demand_account; this script adds cif. Scripts in
-- /docker-entrypoint-initdb.d run ONCE, on first boot of an EMPTY data volume — if bab-pgdata
-- already exists from Step 32, run `docker compose -f deploy/compose.fullstack.yaml down -v` first.
CREATE DATABASE cif OWNER bank;
