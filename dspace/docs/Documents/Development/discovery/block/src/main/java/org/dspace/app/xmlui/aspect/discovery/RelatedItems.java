/*
 * ItemViewer.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 10:02:24 -0700 (Sat, 11 Apr 2009) $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.xmlui.aspect.discovery;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Community;
import org.dspace.content.Collection;
import org.dspace.core.ConfigurationManager;
import org.xml.sax.SAXException;
import org.dspace.discovery.ServiceFactory;
import org.dspace.discovery.SearchServiceException;

/**
 * Display a single item.
 * 
 * @author Scott Phillips
 */
public class RelatedItems extends AbstractDSpaceTransformer
{
    private static final Logger log = Logger.getLogger(RelatedItems.class);
    
    /** Language strings */
    private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");

    private static final Message T_trail =
        message("xmlui.ArtifactBrowser.ItemViewer.trail");

    private static final Message T_show_simple =
        message("xmlui.ArtifactBrowser.ItemViewer.show_simple");

    private static final Message T_show_full =
        message("xmlui.ArtifactBrowser.ItemViewer.show_full");

    private static final Message T_head_parent_collections =
        message("xmlui.ArtifactBrowser.ItemViewer.head_parent_collections");

    /**
     * The cache of recently submitted items
     */
    protected QueryResponse queryResults;

    /**
     * Cached query arguments
     */
    protected SolrQuery queryArgs;


    
    /**
     * Display a single item
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Item))
            return;
        Item item = (Item) dso;

        this.queryArgs = createSolrQuery(null);
        this.queryArgs.setRows(1);
        this.queryArgs.add("fl","author,handle");
        this.queryArgs.add("mlt","true");
        this.queryArgs.add("mlt.fl","author,handle");
        this.queryArgs.add("mlt.mindf","1");
        this.queryArgs.add("mlt.mintf","1");
        this.queryArgs.setQuery("handle:" + item.getHandle()); //"search.resourcetype:2");
        this.queryArgs.setRows(1);//RECENT_SUBMISISONS);

        //query = setSortField(query);

        //query = setFacets(query);


        queryResults = getQueryResponse(queryArgs);

        // Build the collection viewer division.
        Division home = body.addDivision("test", "secondary related");

        String name = "Related Items";

        //if (name == null || name.length() == 0)
        //	home.setHead(T_untitled);
        //else
        	home.setHead(name);


        if (this.queryResults != null) {

            NamedList nList = this.queryResults.getResponse();

            SimpleOrderedMap<SolrDocumentList> mlt = (SimpleOrderedMap<SolrDocumentList>)nList.get("moreLikeThis");

            //home.addPara(nList.toString());
            
            if(mlt != null)
            {
                Division mltDiv = home.addDivision("item-related", "secondary related");

                mltDiv.setHead("Items By Author:");


                

                ReferenceSet set = mltDiv.addReferenceSet(
                    "item-related-items", ReferenceSet.TYPE_SUMMARY_LIST,
                    null, "related-items");



                for(Map.Entry<String,SolrDocumentList> entry : mlt)
                {
                    String key = entry.getKey();

                    //org.dspace.app.xmlui.wing.element.List mltList = mltDiv.addList(key);

                    //mltList.setHead(key);

                    for(SolrDocument doc : entry.getValue())
                    {
                        Reference ref = set.addReference(
                            ServiceFactory.getSearchService().findDSpaceObject(context, doc));

                        //mltList.addItem().addContent(doc.toString());
                    }
         

                }
            }

            }
        }

   // dc.relation.ispartof

    //protected void renderResults()


    protected SolrQuery createSolrQuery(DSpaceObject scope)
    {

        SolrQuery query = new SolrQuery();

        if (scope != null) /* top level search / community */
         {
             if (scope instanceof Community) {
                 query.setFilterQueries("location:m" + scope.getID());
             } else if (scope instanceof Collection) {
                 query.setFilterQueries("location:l" + scope.getID());
             }
         }



        return query;
    }

    protected SolrQuery setSortField(SolrQuery query)
    {
        query.setSortField(
                 ConfigurationManager.getProperty("recent.submissions.sort-option"),
                 SolrQuery.ORDER.asc
        );

        return query;
    }

    protected SolrQuery setFacets(SolrQuery query)
    {
        Request request = ObjectModelHelper.getRequest(objectModel);

        //Set the default limit to 11
        //query.setFacetLimit(11);
        query.setFacetMinCount(1);
        query.setFacet(true);

        java.util.List<String> fields = Arrays.asList(ServiceFactory.getSearchService().getFacetFields());

        String field = request.getParameter("field");

        if(field != null && fields.contains(field))
        {
            fields = Collections.singletonList(field);
        }

        query.addFacetField(fields.toArray(new String[0]));

        return query;
    }


    /**
      * Get the recently submitted items for the given community or collection.
      *
      * @param query
      */
     protected QueryResponse getQueryResponse(SolrQuery query) {

        if(queryResults == null)
        {
            try {
                queryArgs = query;
                queryResults =  ServiceFactory.getSearchService().search(queryArgs);
            } catch (Throwable e) {
                log.error(e.getMessage(),e);
            }
        }

        return queryResults;
     }

    /**
     * Recycle
     */
    public void recycle() {
        this.queryArgs = null;
        this.queryResults = null;
    	super.recycle();
    }
}
