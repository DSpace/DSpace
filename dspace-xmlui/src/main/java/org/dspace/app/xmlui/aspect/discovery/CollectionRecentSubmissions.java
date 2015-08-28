/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.discovery;

import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Renders a list of recently submitted items for the collection by using discovery
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class CollectionRecentSubmissions extends AbstractRecentSubmissionTransformer {

    private static final Message T_head_recent_submissions =
            message("xmlui.ArtifactBrowser.CollectionViewer.head_recent_submissions");

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    /**
     * Displays the recent submissions for this collection
     */
    public void addBody(Body body) throws SAXException, WingException,
            SQLException, IOException, AuthorizeException {

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        // Set up the major variables
        Collection collection = (Collection) dso;
        if(!(dso instanceof Collection))
        {
            return;
        }


        getRecentlySubmittedItems(collection);

        //Only attempt to render our result if we have one.
        if(queryResults == null)
        {
            return;
        }

        if(0 < queryResults.getDspaceObjects().size()){
            // Build the collection viewer division.
            Division home = body.addDivision("collection-home", "primary repository collection");

            Division lastSubmittedDiv = home
                    .addDivision("collection-recent-submission", "secondary recent-submission");

            lastSubmittedDiv.setHead(T_head_recent_submissions);

            ReferenceSet lastSubmitted = lastSubmittedDiv.addReferenceSet(
                    "collection-last-submitted", ReferenceSet.TYPE_SUMMARY_LIST,
                    null, "recent-submissions");

            for (DSpaceObject resultObj : queryResults.getDspaceObjects()) {
                if(resultObj != null){
                    lastSubmitted.addReference(resultObj);
                }
            }

            if (itemService.countItems(context, collection) > maxRecentSubmissions)
                addViewMoreLink(lastSubmittedDiv, collection);
        }
    }
}
