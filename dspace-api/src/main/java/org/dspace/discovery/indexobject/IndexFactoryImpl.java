/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.request.AbstractUpdateRequest;
import org.apache.solr.client.solrj.request.ContentStreamUpdateRequest;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.handler.extraction.ExtractingParams;
import org.dspace.core.Context;
import org.dspace.discovery.FullTextContentStreams;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.SolrSearchCore;
import org.dspace.discovery.SolrServiceIndexPlugin;
import org.dspace.discovery.indexobject.factory.IndexFactory;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.SolrUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basis factory interface implementation for indexing/retrieving any IndexableObject in the search core
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public abstract class IndexFactoryImpl<T extends IndexableObject, S> implements IndexFactory<T, S> {

    @Autowired
    protected List<SolrServiceIndexPlugin> solrServiceIndexPlugins;
    @Autowired
    protected SolrSearchCore solrSearchCore;

    @Override
    public SolrInputDocument buildDocument(Context context, T indexableObject) throws SQLException, IOException {
        SolrInputDocument doc = new SolrInputDocument();
        // want to be able to check when last updated
        // (not tokenized, but it is indexed)
        doc.addField(SearchUtils.LAST_INDEXED_FIELD, SolrUtils.getDateFormatter().format(new Date()));

        // New fields to weaken the dependence on handles, and allow for faster
        // list display
        doc.addField(SearchUtils.RESOURCE_UNIQUE_ID, indexableObject.getType() + "-" + indexableObject.getID());
        doc.addField(SearchUtils.RESOURCE_TYPE_FIELD, indexableObject.getType());
        doc.addField(SearchUtils.RESOURCE_ID_FIELD, indexableObject.getID().toString());

        //Do any additional indexing, depends on the plugins
        for (SolrServiceIndexPlugin solrServiceIndexPlugin : solrServiceIndexPlugins) {
            solrServiceIndexPlugin.additionalIndex(context, indexableObject, doc);
        }

        return doc;
    }

    @Override
    public void writeDocument(Context context, T indexableObject, SolrInputDocument solrInputDocument)
            throws SQLException, IOException, SolrServerException {
        writeDocument(solrInputDocument, null);
    }

    /**
     * Write the document to the index under the appropriate unique identifier.
     *
     * @param doc     the solr document to be written to the server
     * @param streams list of bitstream content streams    DiscoverQueryBuilderTest.java:285
     * @throws IOException A general class of exceptions produced by failed or interrupted I/O operations.
     */
    protected void writeDocument(SolrInputDocument doc, FullTextContentStreams streams)
            throws IOException, SolrServerException {
        final SolrClient solr = solrSearchCore.getSolr();
        if (solr != null) {
            if (streams != null && !streams.isEmpty()) {
                ContentStreamUpdateRequest req = new ContentStreamUpdateRequest("/update/extract");
                req.addContentStream(streams);

                ModifiableSolrParams params = new ModifiableSolrParams();

                //req.setParam(ExtractingParams.EXTRACT_ONLY, "true");
                for (String name : doc.getFieldNames()) {
                    for (Object val : doc.getFieldValues(name)) {
                        params.add(ExtractingParams.LITERALS_PREFIX + name, val.toString());
                    }
                }

                req.setParams(params);
                req.setParam(ExtractingParams.UNKNOWN_FIELD_PREFIX, "attr_");
                req.setParam(ExtractingParams.MAP_PREFIX + "content", "fulltext");
                req.setParam(ExtractingParams.EXTRACT_FORMAT, "text");
                req.setAction(AbstractUpdateRequest.ACTION.COMMIT, true, true);
                req.process(solr);
            } else {
                solr.add(doc);
            }
        }
    }


    /**
     * Index the provided value as use for a sidebar facet
     * @param document  The solr document
     * @param field     The facet field name
     * @param authority The authority linked to the field
     * @param fvalue    The display value for the facet
     */
    protected void addFacetIndex(SolrInputDocument document, String field, String authority, String fvalue) {
        addFacetIndex(document, field, fvalue, authority, fvalue);
    }

    /**
     * Index the provided value as use for a sidebar facet
     * @param document  The solr document
     * @param field     The facet field name
     * @param sortValue The value on which we should sort our facet fields when retrieving
     * @param authority The authority linked to the field
     * @param fvalue    The display value for the facet
     */
    protected void addFacetIndex(SolrInputDocument document, String field, String sortValue, String authority,
                                 String fvalue) {
        // the separator for the filter can be eventually configured
        String separator = DSpaceServicesFactory.getInstance().getConfigurationService()
                .getProperty("discovery.solr.facets.split.char");
        if (separator == null) {
            separator = SearchUtils.FILTER_SEPARATOR;
        }
        String acvalue = sortValue + separator + fvalue + SearchUtils.AUTHORITY_SEPARATOR + authority;
        document.addField(field + "_filter", acvalue);
        // build the solr field used for the keyword search
        document.addField(field + "_keyword", fvalue);
        // build the solr fields used for the autocomplete
        document.addField(field + "_ac", fvalue.toLowerCase() + separator + fvalue);
        if (StringUtils.isNotBlank(authority)) {
            document.addField(field + "_acid", fvalue.toLowerCase() + separator + fvalue
                    + SearchUtils.AUTHORITY_SEPARATOR + authority);
            document.addField(field + "_authority", authority);
        }
    }

    /**
     * Add the necessary fields to the SOLR document to support a Discover Facet on resourcetypename (archived item,
     * workspace item, workflow item, etc)
     *
     * @param document    the solr document
     * @param filterValue the filter value (i.e. <sort_value>\n|||\n<display_value>###<authority_value>
     */
    protected void addNamedResourceTypeIndex(SolrInputDocument document, String filterValue) {

        // the separator for the filter can be eventually configured
        String separator = DSpaceServicesFactory.getInstance().getConfigurationService()
                .getProperty("discovery.solr.facets.split.char");
        if (separator == null) {
            separator = SearchUtils.FILTER_SEPARATOR;
        }

        // split the authority part from the sort/display
        String[] avalues = filterValue.split(SearchUtils.AUTHORITY_SEPARATOR);

        String sortValue = avalues[0];
        String authorityValue = avalues.length == 2 ? avalues[1] : filterValue;

        // get the display value
        int idxSeparator = sortValue.indexOf(separator);
        String displayValue = idxSeparator != -1 ? sortValue.substring(idxSeparator + separator.length())
                : sortValue;

        addFacetIndex(document, SearchUtils.NAMED_RESOURCE_TYPE, sortValue, authorityValue, displayValue);
    }

    @Override
    public void delete(T indexableObject) throws IOException, SolrServerException {
        solrSearchCore.getSolr().deleteById(indexableObject.getUniqueIndexID());
    }

    @Override
    public void delete(String indexableObjectIdentifier) throws IOException, SolrServerException {
        solrSearchCore.getSolr().deleteById(indexableObjectIdentifier);
    }

    @Override
    public void deleteAll() throws IOException, SolrServerException {
        solrSearchCore.getSolr().deleteByQuery(SearchUtils.RESOURCE_TYPE_FIELD + ":" + getType());
    }
}