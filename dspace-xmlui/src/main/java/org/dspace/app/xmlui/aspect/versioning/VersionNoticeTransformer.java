/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.versioning;

import org.apache.cocoon.ProcessingException;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.handle.HandleManager;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.VersioningService;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

/**
 * Adds a notice to item page in the following conditions
 *  A new version of an item is available
 *  If the person is an admin an message will also be shown if the item has a new version in the workflow
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 *
 */
public class VersionNoticeTransformer extends AbstractDSpaceTransformer {

    private static final Message T_new_version_head = message("xmlui.aspect.versioning.VersionNoticeTransformer.notice.new_version_head");
    private static final Message T_new_version_help = message("xmlui.aspect.versioning.VersionNoticeTransformer.notice.new_version_help");
    private static final Message T_workflow_version_head = message("xmlui.aspect.versioning.VersionNoticeTransformer.notice.workflow_version_head");
    private static final Message T_workflow_version_help = message("xmlui.aspect.versioning.VersionNoticeTransformer.notice.workflow_version_help");

    @Override
    public void addBody(Body body) throws SAXException, WingException, SQLException, IOException, AuthorizeException, ProcessingException {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Item))
        {
            return;
        }

        Item item = (Item) dso;

        if(item.isWithdrawn())
        {
            return;
        }

        //Always add a placeholder in which the item information can be added !
        Division mainDivision = body.addDivision("item-view","primary");
        String title = item.getName();
        if(title != null)
        {
            mainDivision.setHead(title);
        }else{
            mainDivision.setHead(item.getHandle());
        }


        //Check if we have a history for the item
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        VersionHistory history = versioningService.findVersionHistory(context, item.getID());

        if(history != null){
            Version latestVersion = retrieveLatestVersion(history, item);
            if(latestVersion != null && latestVersion.getItemID() != item.getID())
            {
                //We have a newer version
                Item latestVersionItem = latestVersion.getItem();
                if(latestVersionItem.isArchived())
                {
                    //Available, add a link for the user alerting him that a new version is available
                    addVersionNotice(mainDivision, latestVersionItem, T_new_version_head, T_new_version_help, true);
                }else{
                    //We might be dealing with a workflow/workspace item
                    addVersionNotice(mainDivision, latestVersionItem, T_workflow_version_head, T_workflow_version_help, false);
                }
            }


        }
    }

    private Version retrieveLatestVersion(VersionHistory history, Item item) throws SQLException {
        //Attempt to retrieve the latest version
        List<Version> allVersions = history.getVersions();
        for (Version version : allVersions) {
            if (version.getItem().isArchived() || AuthorizeManager.isAdmin(context, item.getOwningCollection()))
            {
                return version;
            }
        }
        return null;
    }

    protected void addVersionNotice(Division division, Item item, Message head, Message content, boolean addItemUrl) throws WingException, SQLException
    {
        Division noticeDiv = division.addDivision("general-message", "version-notice notice neutral");
        noticeDiv.setHead(head);

        Para para = noticeDiv.addPara();
        para.addContent(content);
        if(addItemUrl)
        {
            String url = HandleManager.resolveToURL(context, item.getHandle());
            para.addXref(url, url);
        }
    }
}
