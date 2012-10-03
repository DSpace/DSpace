/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;

import java.sql.SQLException;

/**
 * Class used to display facets for an item
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class ItemFacets extends AbstractFiltersTransformer
{

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ItemFacets.class);

    /**
     * Display a single item
     */
    public void addBody(org.dspace.app.xmlui.wing.element.Body body) throws org.xml.sax.SAXException, org.dspace.app.xmlui.wing.WingException,
            org.dspace.app.xmlui.utils.UIException, java.sql.SQLException, java.io.IOException, org.dspace.authorize.AuthorizeException
    {

        org.dspace.content.DSpaceObject dso = org.dspace.app.xmlui.utils.HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof org.dspace.content.Item))
        {
            return;
        }
        org.dspace.content.Item item = (org.dspace.content.Item) dso;

        try {
            performSearch(item);
        } catch (org.dspace.discovery.SearchServiceException e) {
            log.error(e.getMessage(),e);
        }


    }

    @Override
    public void performSearch(org.dspace.content.DSpaceObject dso) throws SQLException,  org.dspace.discovery.SearchServiceException {

        if(queryResults != null)
        {
            return;
        }

        this.queryArgs = prepareDefaultFilters(getView());

        this.queryArgs.setRows(1);
        this.queryArgs.setQuery("handle:" + dso.getHandle());

        Context context = ContextUtil.obtainContext(objectModel);
        queryResults = getSearchService().search(context, queryArgs);
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