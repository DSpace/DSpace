-- Brage var mer eller mindre nede mandag 14/5/2018 kl. 14 - tirsdag 15/5/2018 kl. 14.

-- Symptomene var at tabellen epersongroup2eperson som teoretisk kan ha 10.000 rader,
-- vokste til ca 65 mill. Og dermed belastet postgreSQL og i praksis hele maskinen.
-- Mats Gøran Karlson løste det ved å innføre en begrensning i tabell-definisjonen.
-- Det er meldt som duraspace issue (https://jira.duraspace.org/browse/DS-3955)

DO $$
BEGIN

  BEGIN
    ALTER TABLE epersongroup2eperson ADD CONSTRAINT one_to_one UNIQUE (eperson_group_id, eperson_id) ;
    EXCEPTION
    WHEN duplicate_table THEN RAISE NOTICE 'Table constraint epersongroup2eperson.one_to_one already exists';
  END;

END $$;