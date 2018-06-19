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

import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.xml.sax.SAXException;

/**
 * Renders a list of recently submitted items for the community by using discovery
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class CommunityRecentSubmissions extends AbstractRecentSubmissionTransformer {

    private static final Message T_head_recent_submissions =
            message("xmlui.ArtifactBrowser.CommunityViewer.head_recent_submissions");

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    /**
     * Displays the recent submissions for this community
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Community))
        {
            return;
        }

        // Build the community viewer division.
        Division home = body.addDivision("community-home", "primary repository community");

        getRecentlySubmittedItems(dso);

        //Only attempt to render our result if we have one.
        if(queryResults == null)
        {
            return;
        }

        if (0 < queryResults.getDspaceObjects().size()) {
            Division lastSubmittedDiv = home
                    .addDivision("community-recent-submission", "secondary recent-submission");

            lastSubmittedDiv.setHead(T_head_recent_submissions);

            ReferenceSet lastSubmitted = lastSubmittedDiv.addReferenceSet(
                    "community-last-submitted", ReferenceSet.TYPE_SUMMARY_LIST,
                    null, "recent-submissions");

            for (DSpaceObject resultObject : queryResults.getDspaceObjects()) {
                if(resultObject != null){
                    lastSubmitted.addReference(resultObject);
                }
            }

            Community community = (Community) dso;

            if (itemService.countItems(context, community) > maxRecentSubmissions)
                addViewMoreLink(lastSubmittedDiv, dso);
        }
    }
}
