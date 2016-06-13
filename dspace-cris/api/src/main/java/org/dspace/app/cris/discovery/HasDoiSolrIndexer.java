package org.dspace.app.cris.discovery;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.discovery.SolrServiceIndexPlugin;

public class HasDoiSolrIndexer implements SolrServiceIndexPlugin
{

    Logger log = Logger.getLogger(HasDoiSolrIndexer.class);

    @Override
    public void additionalIndex(Context context, DSpaceObject dso,
            SolrInputDocument document)
    {
        if (dso instanceof Item)
        {
            try
            {
                Item item = Item.find(context, dso.getID());
                Metadatum[] dois = item.getMetadataByMetadataString("dc.identifier.doi");
                if (dois != null && dois.length > 0)
                {
                    document.addField("hasDoi", true);
                }
            }
            catch (SQLException e)
            {
                log.error(e.getMessage(), e);
            }
        }

    }
}
