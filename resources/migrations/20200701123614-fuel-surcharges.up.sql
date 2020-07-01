CREATE TABLE fuel_surcharges (
  id SERIAL PRIMARY KEY,
  market_id INTEGER REFERENCES dim_market(id),
  name text not null,
  source_url text not null,
  company_name text not null,
  created_at TIMESTAMP not null DEFAULT now());
