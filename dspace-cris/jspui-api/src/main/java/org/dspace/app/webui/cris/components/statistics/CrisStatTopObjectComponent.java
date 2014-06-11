/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components.statistics;

import java.sql.SQLException;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.app.cris.statistics.bean.PieStatisticBean;
import org.dspace.app.cris.statistics.bean.StatisticDatasBeanRow;
import org.dspace.app.cris.statistics.bean.TwoKeyMap;
import org.dspace.app.cris.util.ResearcherPageUtils;
import org.dspace.core.Constants;
import org.dspace.core.Context;

public class CrisStatTopObjectComponent extends StatItemTopObjectComponent
{

    @Override
    protected String getObjectId(String id)
    {      
        return ResearcherPageUtils.getPersistentIdentifier(Integer.parseInt(id), getTargetObjectClass());
    }
      
    @Override
    public TwoKeyMap getLabels(Context context, String type)
            throws SQLException
    {
        if(getRelationObjectType()==Constants.ITEM) {
            return super.getLabels(context, type);
        }
        TwoKeyMap labels = new TwoKeyMap();

        PieStatisticBean myvalue = (PieStatisticBean) statisticDatasBeans
                .get("top").get(type).get("id");
        if (myvalue != null)
        {
            if (myvalue.getLimitedDataTable() != null)
            {
                for (StatisticDatasBeanRow row : myvalue.getLimitedDataTable())
                {
                    String pkey = (String)row.getLabel();
                    
                    ACrisObject object = ResearcherPageUtils.getCrisObject(Integer.parseInt(pkey), getRelationObjectClass());
                    
                    labels.addValue(row.getLabel(), "label", object.getName());
                    
                    if (object != null)
                    {
                        labels.addValue(type, row.getLabel(), object);
                    }
                }
            }
        }
        return labels;
        
    }
}
