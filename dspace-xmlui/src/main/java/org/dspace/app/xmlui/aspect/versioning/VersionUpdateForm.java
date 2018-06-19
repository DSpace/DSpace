/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.versioning;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.content.Item;
import org.dspace.versioning.factory.VersionServiceFactory;
import org.dspace.versioning.service.VersioningService;

import java.sql.SQLException;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */

// Versioning
public class VersionUpdateForm extends AbstractDSpaceTransformer {

    /** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");

	private static final Message T_title = message("xmlui.aspect.versioning.VersionUpdateForm.title");
	private static final Message T_trail = message("xmlui.aspect.versioning.VersionUpdateForm.trail");
	private static final Message T_head1 = message("xmlui.aspect.versioning.VersionUpdateForm.head1");
    private static final Message T_submit_version= message("xmlui.aspect.versioning.VersionUpdateForm.submit_version");
	private static final Message T_submit_update_version= message("xmlui.aspect.versioning.VersionUpdateForm.submit_update_version");
    private static final Message T_summary = message("xmlui.aspect.versioning.VersionUpdateForm.summary");

    protected VersioningService versioningService = VersionServiceFactory.getInstance().getVersionService();

	public void addPageMeta(PageMeta pageMeta) throws WingException {
		pageMeta.addMetadata("title").addContent(T_title);
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_trail);
	}

	public void addBody(Body body) throws WingException, SQLException{
        int versionID = parameters.getParameterAsInteger("versionID",-1);
        org.dspace.versioning.Version version = getVersion(versionID);

        Item item = version.getItem();

        // DIVISION: Main
        Division main = body.addInteractiveDivision("version-item", contextPath+"/item/versionhistory", Division.METHOD_POST, "primary administrative version");
        main.setHead(T_head1.parameterize(item.getHandle()));

        // Fields
        List fields = main.addList("fields", List.TYPE_FORM);
        Composite addComposite = fields.addItem().addComposite("summary");
        addComposite.setLabel(T_summary);
        TextArea addValue = addComposite.addTextArea("summary");
        addValue.setValue(version.getSummary());


        // Buttons
        Para actions = main.addPara();

        actions.addButton("submit_update").setValue(T_submit_update_version);
        actions.addButton("submit_cancel").setValue(T_submit_cancel);
		main.addHidden("versioning-continue").setValue(knot.getId());
	}


    private org.dspace.versioning.Version getVersion(int versionID) throws SQLException
    {
        return versioningService.getVersion(context, versionID);
    }
}
