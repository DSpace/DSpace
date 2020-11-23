--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- ===============================================================
-- WARNING WARNING WARNING WARNING WARNING WARNING WARNING WARNING
--
-- DO NOT MANUALLY RUN THIS DATABASE MIGRATION. IT WILL BE EXECUTED
-- AUTOMATICALLY (IF NEEDED) BY "FLYWAY" WHEN YOU STARTUP DSPACE.
-- http://flywaydb.org/
-- ===============================================================

-------------------------------------------------------------
-- This will create COMMUNITY handle metadata
-------------------------------------------------------------

insert into metadatavalue (metadata_field_id, text_value, text_lang, place, authority, confidence, dspace_object_id) 
  select distinct 
  	T1.metadata_field_id as metadata_field_id, 
  	concat('${handle.canonical.prefix}', h.handle) as text_value, 
  	null as text_lang, 0 as place, 
  	null as authority, 
  	-1 as confidence, 
  	c.uuid as dspace_object_id
  	
  	from community c 
  	left outer join handle h on h.resource_id = c.uuid 
  	left outer join metadatavalue mv on mv.dspace_object_id = c.uuid 
  	left outer join metadatafieldregistry mfr on mv.metadata_field_id = mfr.metadata_field_id
  	left outer join metadataschemaregistry msr on mfr.metadata_schema_id = msr.metadata_schema_id
  
  	cross join (select mfr.metadata_field_id as metadata_field_id from metadatafieldregistry mfr 
	 	left outer join metadataschemaregistry msr on mfr.metadata_schema_id = msr.metadata_schema_id
	 	where msr.short_id = 'dc' 
	  		and mfr.element = 'identifier'
	  		and mfr.qualifier = 'uri') T1
	  
  	where uuid not in (
		select c.uuid as uuid from community c 
	 	left outer join handle h on h.resource_id = c.uuid 
	 	left outer join metadatavalue mv on mv.dspace_object_id = c.uuid 
	 	left outer join metadatafieldregistry mfr on mv.metadata_field_id = mfr.metadata_field_id
	 	left outer join metadataschemaregistry msr on mfr.metadata_schema_id = msr.metadata_schema_id
	 	where msr.short_id = 'dc' 
	  		and mfr.element = 'identifier'
	  		and mfr.qualifier = 'uri'
	)
;  

-------------------------------------------------------------
-- This will create COLLECTION handle metadata
-------------------------------------------------------------	
	
insert into metadatavalue (metadata_field_id, text_value, text_lang, place, authority, confidence, dspace_object_id) 
  select distinct 
  	T1.metadata_field_id as metadata_field_id, 
  	concat('${handle.canonical.prefix}', h.handle) as text_value, 
  	null as text_lang, 0 as place, 
  	null as authority, 
  	-1 as confidence, 
  	c.uuid as dspace_object_id
  	
  	from collection c 
  	left outer join handle h on h.resource_id = c.uuid 
  	left outer join metadatavalue mv on mv.dspace_object_id = c.uuid 
  	left outer join metadatafieldregistry mfr on mv.metadata_field_id = mfr.metadata_field_id
  	left outer join metadataschemaregistry msr on mfr.metadata_schema_id = msr.metadata_schema_id
  
  	cross join (select mfr.metadata_field_id as metadata_field_id from metadatafieldregistry mfr 
	 	left outer join metadataschemaregistry msr on mfr.metadata_schema_id = msr.metadata_schema_id
	 	where msr.short_id = 'dc' 
	  		and mfr.element = 'identifier'
	  		and mfr.qualifier = 'uri') T1
	  
  	where uuid not in (
		select c.uuid as uuid from collection c 
	 	left outer join handle h on h.resource_id = c.uuid 
	 	left outer join metadatavalue mv on mv.dspace_object_id = c.uuid 
	 	left outer join metadatafieldregistry mfr on mv.metadata_field_id = mfr.metadata_field_id
	 	left outer join metadataschemaregistry msr on mfr.metadata_schema_id = msr.metadata_schema_id
	 	where msr.short_id = 'dc' 
	  		and mfr.element = 'identifier'
	  		and mfr.qualifier = 'uri'
	)
;  	

