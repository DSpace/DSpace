-- Kjør disse to kommandoene på metadatafieldregistrytabellen (database: brage-dspace-5.5) etter genering av ny base fra backup

-- Migrering av dc.identifier.cristinID til dc.identifier.cristin
UPDATE metadatafieldregistry SET qualifier = 'cristin' WHERE lower(element) = 'identifier' AND lower(qualifier) = 'cristinid';

-- Migrering av dc.relation.projectID til dc.relation.project
UPDATE metadatafieldregistry SET qualifier = 'project' WHERE lower(element) = 'relation' AND lower(qualifier) = 'projectid';