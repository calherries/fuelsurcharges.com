CREATE TABLE market
  (id SERIAL PRIMARY KEY,
  market_name text not null,
  source_name text not null,
  created_at TIMESTAMP not null DEFAULT now());
