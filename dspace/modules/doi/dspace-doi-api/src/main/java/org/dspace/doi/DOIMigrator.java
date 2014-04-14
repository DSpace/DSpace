/*
 */
package org.dspace.doi;

import java.util.Set;
import org.apache.log4j.Logger;

/**
 * Migrates DOIs from perst DOI.db file to Postgres
 * @author Dan Leehr <dan.leehr@nescent.org>
 * @deprecated uses old DOIDatabase (doi.db file)
 */
@Deprecated
public class DOIMigrator {
    private static Logger log = Logger.getLogger(DOIMigrator.class);

    public static void migrate() {
        log.info("Beginning migration");
        DOIDatabase perstDatabase = DOIDatabase.getInstance();
        PGDOIDatabase postgresDatabase = PGDOIDatabase.getInstance();

        Set<DOI> DOIs = perstDatabase.getALL();
        for(DOI doi : DOIs) {
            log.info("Migrating " + doi.toString());
            postgresDatabase.put(doi);
        }
        log.info("Finished migration");
        log.info("Count of DOIs in doi.db: " + perstDatabase.size());
        log.info("Count of DOIs in Postgres: " + postgresDatabase.size());
    }

    public static void main(String args[]) {
        migrate();
    }
}
