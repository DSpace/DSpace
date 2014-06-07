/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.components.statistics;

import java.sql.SQLException;

import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.dspace.app.cris.statistics.bean.PieStatisticBean;
import org.dspace.app.cris.statistics.bean.StatisticDatasBeanRow;
import org.dspace.app.cris.statistics.bean.TwoKeyMap;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;

public class StatItemTopObjectComponent extends StatTopObjectComponent
{
    
    private static final String CUSTOM_FROMFIELD = "search.uniqueid";
    
    public StatItemTopObjectComponent()
    {
        setRelationObjectType(Constants.ITEM);
    }

    @Override
    public TwoKeyMap getLabels(Context context, String type) throws SQLException
    {
        
        TwoKeyMap labels = new TwoKeyMap();
        PieStatisticBean myvalue = (PieStatisticBean) statisticDatasBeans
                .get("top").get(type).get("id");
        if (myvalue != null)
        {
            if (myvalue.getLimitedDataTable() != null)
            {
                for (StatisticDatasBeanRow row : myvalue.getLimitedDataTable())
                {                   
                    Item item = Item.find(context, Integer.parseInt(row.getLabel()));
//                    if (item != null)
                    {
                        labels.addValue(type, row.getLabel(), item);
                    }
                }
            }
        }

     
        return labels;
    }

    @Override
    public String getFromField()
    {
       if(super.getFromField()==null) {
           return CUSTOM_FROMFIELD;
       }
       return super.getFromField();
    }
       
    
}
