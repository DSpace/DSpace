/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Renders the search box for a community
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * 
 * @deprecated Since DSpace 4 the system use an abstraction layer named
 *             Discovery to provide access to different search provider. The
 *             legacy system build upon Apache Lucene is likely to be removed in
 *             a future version. If you are interested in use Lucene as backend
 *             for the DSpace search system please consider to build a Lucene
 *             implementation of the Discovery interfaces
 */
@Deprecated
public class CommunitySearch extends AbstractDSpaceTransformer {

    private static final Message T_advanced_search_link=
    	message("xmlui.ArtifactBrowser.CommunityViewer.advanced_search_link");

    private static final Message T_full_text_search =
        message("xmlui.ArtifactBrowser.CommunityViewer.full_text_search");
    
    private static final Message T_go =
        message("xmlui.general.go");


    /**
     * Display a single community (and reference any subcommunites or
     * collections)
     */
    public void addBody(Body body) throws SAXException, WingException,
                UIException, SQLException, IOException, AuthorizeException
    {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Community))
        {
            return;
        }

        // Set up the major variables
        Community community = (Community) dso;

        Division home = body.addDivision("community-home", "primary repository community");

        Division search = home.addDivision("community-search-browse", "secondary search-browse");


        // Search query
        Division query = search.addInteractiveDivision("community-search",
                contextPath + "/handle/" + community.getHandle() + "/search",
                Division.METHOD_POST, "secondary search");

        Para para = query.addPara("search-query", null);
        para.addContent(T_full_text_search);
        para.addContent(" ");
        para.addText("query");
        para.addContent(" ");
        para.addButton("submit").setValue(T_go);
        query.addPara().addXref(contextPath + "/handle/" + community.getHandle() + "/advanced-search", T_advanced_search_link);

    }


}
