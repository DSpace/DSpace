/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import java.io.IOException;
import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
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
public class RelatedItems extends AbstractDSpaceTransformer
{
    /**
     * Cached query results
     */
    protected DiscoverResult queryResults;

    /**
     * Cached query arguments
     */
    protected DiscoverQuery queryArgs;

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
//            TODO: develop this !
            /*
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
            */

            }
        }

    public void performSearch(DSpaceObject dso) throws SearchServiceException {

        if(queryResults != null)
        {
            return;
        }
        /*
        this.queryArgs = getQueryArgs(getView());
        this.queryArgs.setMaxResults(1);
        this.queryArgs.add("fl","author,handle");
        this.queryArgs.add("mlt","true");
        this.queryArgs.add("mlt.fl","author,handle");
        this.queryArgs.add("mlt.mindf","1");
        this.queryArgs.add("mlt.mintf","1");
        this.queryArgs.setQuery("handle:" + dso.getHandle());
        this.queryArgs.setRows(1);
        */
        //queryResults = SearchUtils.getSearchService().search(context, queryArgs);

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
