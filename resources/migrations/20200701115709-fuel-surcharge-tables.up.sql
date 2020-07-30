CREATE TABLE fuel_surcharge_table (
  id SERIAL PRIMARY KEY,
  fuel_surcharge_id INTEGER REFERENCES fuel_surcharges(id),
  valid_at DATE not null DEFAULT date '1900-01-01',
  update_interval_unit text not null,
  update_interval INTEGER not null,
  delay_period_unit text,
  delay_periods INTEGER,
  surcharge_type text not null,
  price_is_rounded_to_cent BOOLEAN,
  created_at TIMESTAMP not null DEFAULT now());
