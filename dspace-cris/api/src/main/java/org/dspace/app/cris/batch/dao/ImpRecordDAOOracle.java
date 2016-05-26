package org.dspace.app.cris.batch.dao;

import org.apache.log4j.Logger;
import org.dspace.core.Context;

public class ImpRecordDAOOracle extends ImpRecordDAO
{
    
    /** log4j logger */
    private static Logger log = Logger.getLogger(ImpRecordDAOOracle.class);
    
    private final String NEXTVALUESEQUENCE ="SELECT ?_seq.nextval FROM dual";
    
    ImpRecordDAOOracle(Context ctx)
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
