/*
 * ItemFacets.java
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

/**
 * Class used to display facets for an item
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class ItemFacets extends org.dspace.app.xmlui.aspect.discovery.AbstractFiltersTransformer
{

    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(org.dspace.app.xmlui.aspect.discovery.ItemFacets.class);

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
    public void performSearch(org.dspace.content.DSpaceObject dso) throws org.dspace.discovery.SearchServiceException {

        if(queryResults != null)
        {
            return;
        }

        this.queryArgs = prepareDefaultFilters(getView());
        
        this.queryArgs.setRows(1);
        this.queryArgs.setQuery("handle:" + dso.getHandle());

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