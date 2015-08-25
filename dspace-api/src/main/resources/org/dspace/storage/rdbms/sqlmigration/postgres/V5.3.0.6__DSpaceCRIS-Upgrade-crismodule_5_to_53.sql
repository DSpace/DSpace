--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

do $$
begin
    
    alter table jdyna_values 
        add column classificationvalue int4;

    create table jdyna_widget_classification (
        id int4 not null,
        chooseOnlyLeaves boolean not null,
        rootResearchObject_id int4,
        treeObjectType varchar(255) not null,
        metadataBuilderTree_id int4,
        display text,
        primary key (id)
    );

    alter table jdyna_values 
        add constraint FK51AA118F4524F5F0 
        foreign key (classificationvalue) 
        references cris_do;
  	
   alter table jdyna_widget_classification 
        add constraint FK26F7B345C0346CE 
        foreign key (metadataBuilderTree_id) 
        references cris_do_pdef;

   alter table jdyna_widget_classification 
        add constraint FK26F7B34C25A6E3 
        foreign key (rootResearchObject_id) 
        references cris_do;       
 	
exception when others then
 
    raise notice 'The transaction is in an uncommittable state. '
                     'Transaction was rolled back';
 
    raise notice 'Yo this is good! --> % %', SQLERRM, SQLSTATE;
end;
$$ language 'plpgsql';
