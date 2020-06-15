-- :name create-market! :! :n
-- :doc creates a new market record
INSERT INTO dim_market
(market_name, source_name)
VALUES (:market-name, :source-name)

-- :name delete-market! :! :n
-- :doc deletes a market record given the market_name
DELETE FROM dim_market
WHERE market_name = :market-name

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
