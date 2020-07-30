CREATE TABLE market_price
  (id SERIAL,
  market_id INTEGER REFERENCES dim_market(id),
  price_date DATE not null,
  price DOUBLE PRECISION not null,
  currency text not null,
  created_at TIMESTAMP not null DEFAULT now(),
  PRIMARY KEY (id, market_id));
