/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject.factory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.discovery.configuration.DiscoveryConfiguration;
import org.dspace.discovery.indexobject.IndexableItem;

/**
 * Factory interface for indexing/retrieving items in the search core
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public interface ItemIndexFactory extends DSpaceObjectIndexFactory<IndexableItem, Item> {

    /**
     * Store item fields in the solr document
     * @param doc                       Solr input document which will be written to our discovery solr core
     * @param context                   DSpace context object
     * @param item                      Item for which we want to index our fields in the provided solr document
     * @param discoveryConfigurations   The discovery configuration which holds information
     *                                  for which item fields we should index
     * @throws SQLException             If database error
     * @throws IOException              If IO error
     */
    void addDiscoveryFields(SolrInputDocument doc, Context context, Item item,
                            List<DiscoveryConfiguration> discoveryConfigurations)
            throws SQLException, IOException;

}