--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-- Kjør disse to kommandoene på metadatafieldregistrytabellen (database: brage-dspace-5.5) etter genering av ny base fra backup

-- Migrering av dc.identifier.cristinID til dc.identifier.cristin
UPDATE metadatafieldregistry SET qualifier = 'cristin' WHERE lower(element) = 'identifier' AND lower(qualifier) = 'cristinid';

-- Migrering av dc.relation.projectID til dc.relation.project
UPDATE metadatafieldregistry SET qualifier = 'project' WHERE lower(element) = 'relation' AND lower(qualifier) = 'projectid';