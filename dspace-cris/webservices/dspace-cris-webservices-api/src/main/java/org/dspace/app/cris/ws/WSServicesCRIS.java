/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.ws;

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.core.Context;

public class WSServicesCRIS<T extends ACrisObject> extends AWSServices<T>
{
    private static Logger log = Logger.getLogger(AWSServices.class);

    private int supportedType;

    public void setSupportedType(int supportedType)
    {
        this.supportedType = supportedType;
    }

    public int getSupportedType()
    {
        return supportedType;
    }

    public void internalBuildFieldList(SolrQuery solrQuery,
            String... projection)
    {
        solrQuery.setFields(projection);
        solrQuery.addField("search.resourceid");
        solrQuery.addField("search.resourcetype");
    }

    @Override
    protected List<T> getWSObject(QueryResponse response)
    {
        Context context = null;
        List<T> results = new LinkedList<T>();
        try
        {
            context = new Context();
            for (SolrDocument solrDocument : response.getResults())
            {
                T aa = (T) getSearchServices().findDSpaceObject(context,
                        solrDocument);
                results.add(aa);
            }

        }
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        finally
        {
            if (context != null && context.isValid())
            {
                context.abort();
            }
        }
        return results;
    }
}
