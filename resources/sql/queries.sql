-- :name create-market! :! :n
-- :doc creates a new market record
INSERT INTO market
(market_name, source_name)
VALUES (:market-name, :source-name)

-- :name delete-market! :! :n
-- :doc deletes a market record given the market_id
DELETE FROM market
WHERE id = :id

-- :name get-fuel-surcharge-table-rows :? :*
-- :doc selects all fuel surcharge table rows
select *
from fuel_surcharge f
left join fuel_surcharge_table t on f.id = t.fuel_surcharge_id
left join fuel_surcharge_table_row r on t.id = r.fuel_surcharge_table_id

-- :name insert-fuel-surcharge-table-rows! :! :n
-- :doc inserts a list of fuel surcharge rows into a table
INSERT INTO fuel_surcharge_table_row
(fuel_surcharge_table_id, price, surcharge_amount)
VALUES :tuple*:fuel-surcharge-table

-- :name insert-market-prices! :! :n
-- :doc creates new market prices record
INSERT INTO market_price
(market_id, price_date, price, currency)
VALUES :tuple*:market-prices

-- :name delete-market-prices! :! :n
-- :doc deletes a market record given the market_id
DELETE FROM market_price
WHERE market_id = :market-id

-- :name create-fuel-surcharge! :! :n
-- :doc creates a new fuel-surcharge record
INSERT INTO fuel_surcharge
(market_id, name, source_url, company_name)
VALUES (:market-id, :name, :source-url, :company-name)

-- :name create-fuel-surcharge-table! :! :n
-- :doc creates a new fuel-surcharge table record
INSERT INTO fuel_surcharge_table
(fuel_surcharge_id, update_interval_unit, update_interval, delay_period_unit, delay_periods, surcharge_type, price_is_rounded_to_cent)
VALUES (:fuel-surcharge-id, :update-interval-unit, :update-interval, :delay-period-unit, :delay-periods, :surcharge-type, :price-is-rounded-to-cent)

-- :name delete-fuel-surcharge! :! :n
-- :doc deletes a fuel surcharge record given the id
DELETE FROM fuel_surcharge
WHERE id = :id

-- :name delete-fuel-surcharge-table! :! :n
-- :doc deletes a fuel-surcharge-table record given the id
DELETE FROM fuel_surcharge_table
WHERE id = :id

-- :name delete-fuel-surcharge-table-rows! :! :n
-- :doc deletes all fuel-surcharge-table rows given the table id
DELETE FROM fuel_surcharge_table_row
WHERE fuel_surcharge_table_id = :id

-- :name delete-all-fuel-surcharge-table-rows! :! :n
-- :doc deletes all fuel-surcharge-table rows given the table id
DELETE FROM fuel_surcharge_table_row
