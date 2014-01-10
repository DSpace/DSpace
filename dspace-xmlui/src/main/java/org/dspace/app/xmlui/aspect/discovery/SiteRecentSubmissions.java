/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Renders a list of recently submitted items for the homepage by using discovery
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class SiteRecentSubmissions extends AbstractRecentSubmissionTransformer {

    private static final Message T_head_recent_submissions =
            message("xmlui.ArtifactBrowser.SiteViewer.head_recent_submissions");


    /**
     * Display a single community (and reference any subcommunites or
     * collections)
     */
    public void addBody(Body body) throws SAXException, WingException,
            SQLException, IOException, AuthorizeException {

        getRecentlySubmittedItems(null);

        //Only attempt to render our result if we have one.
        if (queryResults == null)  {
            return;
        }

        if (0 < queryResults.getDspaceObjects().size()) {
            Division home = body.addDivision("site-home", "primary repository");

            Division lastSubmittedDiv = home
                    .addDivision("site-recent-submission", "secondary recent-submission");

            lastSubmittedDiv.setHead(T_head_recent_submissions);

            ReferenceSet lastSubmitted = lastSubmittedDiv.addReferenceSet(
                    "site-last-submitted", ReferenceSet.TYPE_SUMMARY_LIST,
                    null, "recent-submissions");

            for (DSpaceObject dso : queryResults.getDspaceObjects()) {
                if(dso != null){
                    lastSubmitted.addReference(dso);
                }
            }
            addViewMoreLink(lastSubmittedDiv, null);
        }

    }
}
