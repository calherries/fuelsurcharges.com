select
  r.price
  , r.surcharge_amount
from fuel_surcharge f
join fuel_surcharge_table t on f.id = t.fuel_surcharge_id
join fuel_surcharge_table_row r on t.id = r.fuel_surcharge_table_id
where f.id = 3
;
