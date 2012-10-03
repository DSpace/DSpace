package org.dspace.app.xmlui.aspect.administrative;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.utils.DSpace;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 8/1/11
 * Time: 3:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class TombstoneTransformer extends AbstractDSpaceTransformer {

      /** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");


	private static final Message T_title = message("xmlui.utils.TombstoneTransformer.title");
    private static final Message T_item_removed = message("xmlui.utils.TombstoneTransformer.item_removed");
    private static final Message T_head1 = message("xmlui.utils.TombstoneTransformer.head1");
	private static final Message T_para1 = message("xmlui.utils.TombstoneTransformer.para1");

    private static final Message T_subject = message("xmlui.utils.TombstoneTransformer.subject");
    private static final Message T_message = message("xmlui.utils.TombstoneTransformer.message");
    private static final Message T_message_link = message("xmlui.utils.TombstoneTransformer.message_link");

    private static final Message T_submit_send = message("xmlui.utils.TombstoneTransformer.submit_send");
    private static final Message T_submit_cancel = message("xmlui.utils.TombstoneTransformer.submit_cancel");


	public void addPageMeta(PageMeta pageMeta) throws WingException {
		pageMeta.addMetadata("title").addContent(T_title);
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/admin/item",T_item_trail);
	}

	public void addBody(Body body) throws WingException, AuthorizeException {
		Division main = createMainDivision(body);
		createForm(main);
	}


    private Division createMainDivision(Body body) throws WingException {
        Division main = body.addInteractiveDivision("help-form", contextPath+"/tombstone", Division.METHOD_POST, "help form");
		main.setHead(T_head1);

        return main;
    }


    private void createForm(Division main) throws WingException {
        Division div = main.addDivision("notice", "notice");
        Para p = div.addPara();
        p.addContent(T_message);
        p.addXref("/feedback", T_message_link);
    }

    private void addButtons(Division main) throws WingException {
        Para buttons = main.addPara();
		buttons.addButton("submit_restore").setValue(T_submit_send);
		buttons.addButton("submit_cancel").setValue(T_submit_cancel);
    }
}
