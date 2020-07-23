select
  r.price
  , r.surcharge_amount
from fuel_surcharges f
join fuel_surcharge_tables t on f.id = t.fuel_surcharge_id
join fuel_surcharge_table_rows r on t.id = r.fuel_surcharge_table_id
where f.id = 3
;
