package org.dspace.app.cris.batch.dao;

import org.apache.log4j.Logger;
import org.dspace.core.Context;

public class ImpRecordDAOPostgres extends ImpRecordDAO
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(ImpRecordDAOPostgres.class);

    private final String NEXTVALUESEQUENCE = "SELECT getnextid('?') AS result";

    

    public ImpRecordDAOPostgres(Context ctx)
    {
        super(ctx);
    }

    @Override
    public String getNEXTVALUESEQUENCE(String table)
    {
        log.debug(NEXTVALUESEQUENCE.replace("?", table));
        return NEXTVALUESEQUENCE.replace("?", table);
    }

 
}
