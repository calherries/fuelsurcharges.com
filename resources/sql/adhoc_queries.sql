select
  p.market_id
  , p.price_date
  , p.price
  , min(r.price) as table_price
  , min(r.surcharge_amount) as surcharge_amount
from market_prices p
join fuel_surcharges f on f.market_id = p.market_id
join fuel_surcharge_tables t on f.id = t.fuel_surcharge_id
join fuel_surcharge_table_rows r on t.id = r.fuel_surcharge_table_id
where true
  and p.price_date > now() - interval '1 year' - interval '2 week'
  and p.price < r.price
group by 1, 2, 3
;
