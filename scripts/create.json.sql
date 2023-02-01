do $$
declare
curr text;
complete text;
tabless text[] := (select array(select table_name from information_schema.tables where is_insertable_into = 'YES' AND table_schema = 'public'));
path_ text := 'C:\Json\file.json';
begin
for tn in array_lower(tabless, 1) .. array_upper(tabless, 1)
loop
	execute 'select json_agg(row_to_json(t)) from ' || tabless[tn] || ' t' into curr;
	complete := concat(complete, chr(10), tabless[tn], ': ', curr);
end loop;
raise notice '%',complete;
end $$