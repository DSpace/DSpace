/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.versioning;

import org.apache.avalon.framework.parameters.ParameterException;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersionHistoryService;

import java.sql.SQLException;
import java.util.UUID;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */

// Versioning
public class VersionItemForm extends AbstractDSpaceTransformer {

    /** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");

	private static final Message T_title = message("xmlui.aspect.versioning.VersionItemForm.title");
	private static final Message T_trail = message("xmlui.aspect.versioning.VersionItemForm.trail");
	private static final Message T_head1 = message("xmlui.aspect.versioning.VersionItemForm.head1");
    private static final Message T_submit_version= message("xmlui.aspect.versioning.VersionItemForm.submit_version");
	private static final Message T_submit_update_version= message("xmlui.aspect.versioning.VersionItemForm.submit_update_version");
    private static final Message T_summary = message("xmlui.aspect.versioning.VersionItemForm.summary");


    protected AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected VersionHistoryService versionHistoryService = VersionServiceFactory.getInstance().getVersionHistoryService();

	public void addPageMeta(PageMeta pageMeta) throws WingException, SQLException {
		pageMeta.addMetadata("title").addContent(T_title);
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		//pageMeta.addTrailLink(contextPath+"/admin/item", T_item_trail);
        Item item = getItem();
        if(item != null)
        {
            HandleUtil.buildHandleTrail(context, item, pageMeta, contextPath);
            pageMeta.addTrailLink(contextPath + "/handle/" +  item.getHandle(), item.getName());
        }
        pageMeta.addTrail().addContent(T_trail);
	}

	public void addBody(Body body) throws WingException, SQLException, AuthorizeException {

        // Get our parameters and state
        Item item = getItem();

        //Only (collection) admins should be able to create a new version
        if(!authorizeService.isAdmin(context, item.getOwningCollection())){
            throw new AuthorizeException();
        }

        String summary;
        try {
            summary = parameters.getParameter("summary");
        } catch (ParameterException e) {
            throw new RuntimeException(e);
        }

        // DIVISION: Main
        Division main = body.addInteractiveDivision("version-item", contextPath+"/item/version", Division.METHOD_POST, "primary administrative version");
        main.setHead(T_head1.parameterize(item.getHandle()));

        // Fields
        List fields = main.addList("fields", List.TYPE_FORM);
        Composite addComposite = fields.addItem().addComposite("summary");
        addComposite.setLabel(T_summary);
        TextArea addValue = addComposite.addTextArea("summary");
        if(summary!=null)
        {
            addValue.setValue(summary);
        }


        // Buttons
        Para actions = main.addPara();

        org.dspace.versioning.VersionHistory history = retrieveVersionHistory(item);
        if(history!=null && versionHistoryService.hasNext(context, history ,item))
        {
            actions.addButton("submit_update_version").setValue(T_submit_update_version);
        }
        else
        {
            actions.addButton("submit_version").setValue(T_submit_version);
        }

        actions.addButton("submit_cancel").setValue(T_submit_cancel);

		main.addHidden("versioning-continue").setValue(knot.getId());
	}

    private Item getItem() throws SQLException {
        UUID itemID = UUID.fromString(parameters.getParameter("itemID",null));
        return itemService.find(context, itemID);
    }


    private org.dspace.versioning.VersionHistory retrieveVersionHistory(Item item) throws SQLException
    {
        return versionHistoryService.findByItem(context, item);
    }
}
