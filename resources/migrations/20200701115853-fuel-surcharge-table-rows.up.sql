CREATE TABLE fuel_surcharge_table_rows (
  id SERIAL PRIMARY KEY,
  fuel_surcharge_table_id INTEGER REFERENCES fuel_surcharge_tables(id),
  price DOUBLE PRECISION not null,
  surcharge_amount DOUBLE PRECISION not null,
  created_at TIMESTAMP not null DEFAULT now());
