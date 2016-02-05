package org.dspace.app.xmlui.aspect.versioning;

import org.apache.avalon.framework.parameters.ParameterException;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.utils.DSpace;
import org.dspace.versioning.PluggableVersioningService;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionServiceUtil;
import org.dspace.versioning.VersioningService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: Apr 4, 2011
 * Time: 8:59:47 AM
 * To change this template use File | Settings | File Templates.
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
    private static final Message T_reason = message("xmlui.aspect.versioning.VersionItemForm.reason");


	public void addPageMeta(PageMeta pageMeta) throws WingException{
		pageMeta.addMetadata("title").addContent(T_title);
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		//pageMeta.addTrailLink(contextPath+"/admin/item", T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
	}

	public void addBody(Body body) throws WingException, SQLException{

        String errorString = parameters.getParameter("errors",null);
        ArrayList<String> errors = new ArrayList<String>();
        if (errorString != null)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
            }
        }

        // Get our parameters and state
        int itemID = parameters.getParameterAsInteger("itemID",-1);
        String summary=null;
        try {
            summary = parameters.getParameter("summary");
        } catch (ParameterException e) {
            throw new RuntimeException(e);
        }
        Item item = Item.find(context, itemID);

        // DIVISION: Main
        Division main = body.addInteractiveDivision("version-item", contextPath+"/item/version", Division.METHOD_POST, "primary administrative version");
        main.setHead(T_head1.parameterize(item.getHandle()));

        // Fields
        List fields = main.addList("fields",List.TYPE_FORM);
        Composite addComposite = fields.addItem().addComposite("summary");
        addComposite.setLabel(T_summary);
        TextArea addValue = addComposite.addTextArea("summary");
        addValue.setMaxLength(255);
        if (errors.contains("version_reason")) {
            addValue.addError(T_reason);
        }
        else{ addValue.setValue(summary);
        }


        // Buttons
        Para actions = main.addPara();


        org.dspace.versioning.VersionHistory history = retrieveVersionHistory(item);

        if(history!=null && history.hasNext(item))
            actions.addButton("submit_update_version").setValue(T_submit_update_version);
        else
            actions.addButton("submit_version").setValue(T_submit_version);



        actions.addButton("submit_cancel").setValue(T_submit_cancel);

		main.addHidden("versioning-continue").setValue(knot.getId());
	}


    private org.dspace.versioning.VersionHistory retrieveVersionHistory(Item item){
           VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
           org.dspace.versioning.VersionHistory history = versioningService.findVersionHistory(context, item.getID());
           return history;
    }
}
