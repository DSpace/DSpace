/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.ws.discovery.CrisWebservicesExtraIndexPlugin;
import org.dspace.app.cris.ws.marshaller.bean.WSItem;
import org.dspace.app.cris.ws.marshaller.bean.WSMetadata;
import org.dspace.app.cris.ws.marshaller.bean.WSMetadataValue;
import org.dspace.core.Constants;

public class WSServicesPublications extends AWSServices<WSItem>
{

    private static Logger log = Logger.getLogger(WSServicesPublications.class);

    @Override
    protected List<WSItem> getWSObject(QueryResponse response)
    {
        List<WSItem> results = new LinkedList<WSItem>();
        for (SolrDocument solrDocument : response.getResults())
        {
            results.add(getItemFromSolrDoc(solrDocument));
        }
        return results;
    }

    public void internalBuildFieldList(SolrQuery solrQuery,
            String... projection)
    {

        solrQuery.addField("handle");
        solrQuery.addField("search.resourceid");

        for (String otherToIndex : CrisWebservicesExtraIndexPlugin.OTHERS_TO_INDEX)
        {
            solrQuery.addField(otherToIndex);
        }

        for (int j = 0; j < projection.length; j++)
        {
            solrQuery.addField(projection[j]);
        }

    }

    private WSItem getItemFromSolrDoc(SolrDocument sd)
    {
        WSItem item = new WSItem();
        item.setHandle((String) sd.getFieldValue("handle"));
        item.setItemID((Integer) sd.getFieldValue("search.resourceid"));

        item.setCollection((ArrayList) sd
                .getFieldValues(CrisWebservicesExtraIndexPlugin.FIELDNAME_COLLECTIONS));
        item.setCollectionName((ArrayList) sd
                .getFieldValues(CrisWebservicesExtraIndexPlugin.FIELDNAME_COLLECTIONS_NAME));
        item.setCollectionHandle((ArrayList) sd
                .getFieldValues(CrisWebservicesExtraIndexPlugin.FIELDNAME_COLLECTIONS_HANDLE));

        item.setCommunity((ArrayList) sd
                .getFieldValues(CrisWebservicesExtraIndexPlugin.FIELDNAME_COMMUNITIES));
        item.setCommunityName((ArrayList) sd
                .getFieldValues(CrisWebservicesExtraIndexPlugin.FIELDNAME_COMMUNITIES_NAME));
        item.setCommunityHandle((ArrayList) sd
                .getFieldValues(CrisWebservicesExtraIndexPlugin.FIELDNAME_COMMUNITIES_HANDLE));

        for (String m : sd.getFieldNames())
        {
            if (!m.startsWith("dc."))
            {
                continue;
            }
            WSMetadata metadata = new WSMetadata();
            metadata.setName(m);
            int place = 1;
            for (Object v : sd.getFieldValues(m))
            {
                String value = (String) v;
                String[] mv = value.split("\\|\\|\\|");
                WSMetadataValue mvalue = new WSMetadataValue();
                mvalue.setValue(mv[0]);
                mvalue.setAuthority(mv.length > 1 ? mv[1] : null);
                mvalue.setShare(mv.length > 2 ? Integer.parseInt(mv[2]) : null);
                mvalue.setPlace(place);
                place++;
                metadata.getValues().add(mvalue);
            }
            item.getMetadata().add(metadata);
        }
        return item;
    }

    @Override
    protected int getSupportedType()
    {
        return Constants.ITEM;
    }
}
