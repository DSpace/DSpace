/*
 * RelatedItems.java
 *
 * Version: $Revision: 5497 $
 *
 * Date: $Date: 2010-10-20 23:06:10 +0200 (wo, 20 okt 2010) $
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
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrDocument;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.LogManager;
import org.dspace.discovery.SearchUtils;
import org.xml.sax.SAXException;
import org.dspace.discovery.SearchServiceException;

/**
 * Displays related items to the currently viewable item
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class RelatedItems extends AbstractFiltersTransformer
{

    private static final Logger log = Logger.getLogger(RelatedItems.class);
    
    /**
     * Display a single item
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        DSpaceObject dspaceObject = HandleUtil.obtainHandle(objectModel);
        if (!(dspaceObject instanceof Item))
        {
            return;
        }
        Item item = (Item) dspaceObject;

        try {
            performSearch(item);
        } catch (SearchServiceException e) {
            log.error(e.getMessage(),e);
        }

        // Build the collection viewer division.


        if (this.queryResults != null) {

            NamedList nList = this.queryResults.getResponse();

            SimpleOrderedMap<SolrDocumentList> mlt = (SimpleOrderedMap<SolrDocumentList>)nList.get("moreLikeThis");

            //home.addPara(nList.toString());
            
            if(mlt != null && 0 < mlt.size())
            {
                //TODO: also make sure if an item is unresolved we do not end up with an empty referenceset !
                List<DSpaceObject> dsos = new ArrayList<DSpaceObject>();
                for(Map.Entry<String,SolrDocumentList> entry : mlt)
                {
                    //org.dspace.app.xmlui.wing.element.List mltList = mltDiv.addList(key);

                    //mltList.setHead(key);

                    for(SolrDocument doc : entry.getValue())
                    {
                        try{
                            dsos.add(SearchUtils.findDSpaceObject(context, doc));
                        }catch(Exception e){
                            log.error(LogManager.getHeader(context, "Error while resolving related item doc to dso", "Main item: " + item.getID()));
                        }
                        //mltList.addItem().addContent(doc.toString());
                    }
         

                }

                if(0 < dsos.size()){
                    Division home = body.addDivision("test", "secondary related");

                    String name = "Related Items";

                    //if (name == null || name.length() == 0)
                    //	home.setHead(T_untitled);
                    //else
                        home.setHead(name);

                    Division mltDiv = home.addDivision("item-related", "secondary related");

                    mltDiv.setHead("Items By Author:");

                    ReferenceSet set = mltDiv.addReferenceSet(
                            "item-related-items", ReferenceSet.TYPE_SUMMARY_LIST,
                            null, "related-items");

                    for (DSpaceObject dso : dsos) {
                        set.addReference(dso);
                    }
                }
            }

            }
        }

    @Override
    public void performSearch(DSpaceObject dso) throws SearchServiceException {

        if(queryResults != null)
        {
            return;
        }

        this.queryArgs = prepareDefaultFilters(getView());
        this.queryArgs.setRows(1);
        this.queryArgs.add("fl","author,handle");
        this.queryArgs.add("mlt","true");
        this.queryArgs.add("mlt.fl","author,handle");
        this.queryArgs.add("mlt.mindf","1");
        this.queryArgs.add("mlt.mintf","1");
        this.queryArgs.setQuery("handle:" + dso.getHandle());
        this.queryArgs.setRows(1);

        queryResults = getSearchService().search(queryArgs);

    }


    public String getView()
    {
        return "item";
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
