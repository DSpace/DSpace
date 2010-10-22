/*
 * CollectionSearch.java
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
package org.dspace.app.xmlui.aspect.artifactbrowser;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Renders the search box for a collection
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class CollectionSearch extends AbstractDSpaceTransformer {

    private static final Message T_full_text_search =
        message("xmlui.ArtifactBrowser.CollectionViewer.full_text_search");

    private static final Message T_go =
        message("xmlui.general.go");

    private static final Message T_advanced_search_link=
    	message("xmlui.ArtifactBrowser.CollectionViewer.advanced_search_link");
   

    @Override
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Collection))
        {
            return;
        }

        // Set up the major variables
        Collection collection = (Collection) dso;

        Division home = body.addDivision("collection-home", "primary repository collection");

        Division search = home.addDivision("collection-search-browse",
                            "secondary search-browse");

        // Search query
        Division query = search.addInteractiveDivision("collection-search",
                contextPath + "/handle/" + collection.getHandle() + "/search",
                Division.METHOD_POST, "secondary search");

        Para para = query.addPara("search-query", null);
        para.addContent(T_full_text_search);
        para.addContent(" ");
        para.addText("query");
        para.addContent(" ");
        para.addButton("submit").setValue(T_go);
        query.addPara().addXref(contextPath + "/handle/" + collection.getHandle()+ "/advanced-search", T_advanced_search_link);
    }
}
