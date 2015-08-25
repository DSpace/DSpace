--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--


BEGIN
	EXECUTE IMMEDIATE
    	'alter table jdyna_values add column classificationvalue integer';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'create table jdyna_widget_classification (
        id integer not null,
        chooseOnlyLeaves number(1,0) not null,
        rootResearchObject_id integer,
        treeObjectType varchar2(255) not null,
	    metadataBuilderTree_id integer,
        display clob
        primary key (id)
    )';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'alter table jdyna_values 
        add constraint FK51AA118F4524F5F0 
        foreign key (classificationvalue) 
        references cris_do';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;


BEGIN
	EXECUTE IMMEDIATE
    	'alter table jdyna_widget_classification 
        add constraint FK26F7B345C0346CE 
        foreign key (metadataBuilderTree_id) 
        references cris_do_pdef';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;

BEGIN
	EXECUTE IMMEDIATE
    	'alter table jdyna_widget_classification 
        add constraint FK26F7B34C25A6E3 
        foreign key (rootResearchObject_id) 
        references cris_do;   ';
	EXCEPTION
	WHEN OTHERS
    THEN
       NULL;
END;