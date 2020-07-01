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

-- :name get-fuel-surcharges :? :*
-- :doc selects all fuel surcharges
SELECT * from fuel_surcharges

-- :name get-fuel-surcharge-tables :? :*
-- :doc selects all fuel surcharges
SELECT * from fuel_surcharge_tables

-- :name get-fuel-surcharge-table-rows :? :*
-- :doc selects all fuel surcharge table rows
select *
from fuel_surcharges f
left join fuel_surcharge_tables t on f.id = t.fuel_surcharge_id
left join fuel_surcharge_table_rows r on t.id = r.fuel_surcharge_table_id

-- :name insert-fuel-surcharge-table-rows! :! :n
-- :doc inserts a list of fuel surcharge rows into a table
INSERT INTO fuel_surcharge_table_rows
(fuel_surcharge_table_id, price, surcharge_amount)
VALUES :tuple*:fuel-surcharge-table

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
where price_date > now() - interval '1 year' - interval '2 week'

-- :name get-last-year-fuel-surcharges :? :*
-- :doc selects all fuel-surcharges within the last year
select
  f.id as fuel_surcharge_id
  , p.market_id
  , p.price_date
  , p.price
  , max(r.price) as table_price
  , max(r.surcharge_amount) as surcharge_amount
from market_prices p
join fuel_surcharges f on f.market_id = p.market_id
join fuel_surcharge_tables t on f.id = t.fuel_surcharge_id
join fuel_surcharge_table_rows r on t.id = r.fuel_surcharge_table_id
where true
  and p.price_date > now() - interval '1 year' - interval '2 week'
  and p.price > r.price
group by 1, 2, 3, 4
order by p.price_date

-- :name create-fuel-surcharge! :! :n
-- :doc creates a new fuel-surcharge record
INSERT INTO fuel_surcharges
(market_id, name, source_url, company_name)
VALUES (:market-id, :name, :source-url, :company-name)

-- :name create-fuel-surcharge-table! :! :n
-- :doc creates a new fuel-surcharge table record
INSERT INTO fuel_surcharge_tables
(fuel_surcharge_id, update_interval_unit, update_interval, delay_period_unit, delay_periods, surcharge_type, price_is_rounded_to_cent)
VALUES (:fuel-surcharge-id, :update-interval-unit, :update-interval, :delay-period-unit, :delay-periods, :surcharge-type, :price-is-rounded-to-cent)

-- :name delete-fuel-surcharge! :! :n
-- :doc deletes a fuel surcharge record given the id
DELETE FROM fuel_surcharges
WHERE id = :id

-- :name delete-fuel-surcharge-table! :! :n
-- :doc deletes a fuel-surcharge-table record given the id
DELETE FROM fuel_surcharges_tables
WHERE id = :id

-- :name delete-fuel-surcharge-table_rows! :! :n
-- :doc deletes all fuel-surcharge-table rows given the table id
DELETE FROM fuel_surcharges_table_rows
WHERE fuel_surcharge_table_id = :id
