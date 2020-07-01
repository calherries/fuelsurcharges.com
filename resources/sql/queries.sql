-- :name create-market! :! :n
-- :doc creates a new market record
INSERT INTO dim_market
(market_name, source_name)
VALUES (:market-name, :source-name)

-- :name delete-market! :! :n
-- :doc deletes a market record given the market_id
DELETE FROM dim_market
WHERE id = :id

-- :name get-markets :? :*
-- :doc selects all markets
SELECT * from dim_market

-- :name insert-market-prices! :! :n
-- :doc creates new market prices record
INSERT INTO market_prices
(market_id, price_date, price, currency)
VALUES :tuple*:market-prices

-- :name delete-market-prices! :! :n
-- :doc deletes a market record given the market_id
DELETE FROM market_prices
WHERE market_id = :market-id

-- :name get-market-prices :? :*
-- :doc selects all market_prices
SELECT * from market_prices

-- :name get-last-year-market-prices :? :*
-- :doc selects all market_prices within the last year
select * from market_prices
where price_date > now() - interval '1 year' - interval '2 week';

-- :name create-fuel-surcharge! :! :n
-- :doc creates a new fuel-surcharge record
INSERT INTO fuel_surcharges
  (name, source_url, company_name, update_frequency, delay_weeks)
VALUES (:name, :source_url, :company_name, :update_frequency, :delay_weeks)

-- :name create-fuel-surcharge-table! :! :n
-- :doc creates a new fuel-surcharge table record
INSERT INTO fuel_surcharge_tables
(fuel_surcharge_id, valid_date)
VALUES (:fuel-surcharge-id, :valid-date)

-- :name delete-fuel-surcharge! :! :n
-- :doc deletes a fuel surcharge record given the id
DELETE FROM fuel_surcharges
WHERE id = :id

-- :name delete-fuel-surcharge-table! :! :n
-- :doc deletes a fuel-surcharge-table record given the id
DELETE FROM fuel_surcharges_tables
WHERE id = :id
