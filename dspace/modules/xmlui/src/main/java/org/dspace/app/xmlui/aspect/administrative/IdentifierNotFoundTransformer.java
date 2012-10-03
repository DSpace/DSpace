package org.dspace.app.xmlui.aspect.administrative;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.authorize.AuthorizeException;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 8/1/11
 * Time: 3:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class IdentifierNotFoundTransformer extends AbstractDSpaceTransformer {

      /** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	private static final Message T_title = message("xmlui.utils.IdentifierNotFoundTransformer.title");
    private static final Message T_head1 = message("xmlui.utils.IdentifierNotFoundTransformer.head1");
    private static final Message T_message = message("xmlui.utils.IdentifierNotFoundTransformer.message");
    private static final Message T_message_link = message("xmlui.utils.IdentifierNotFoundTransformer.message_link");


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
        Division main = body.addInteractiveDivision("help-form", contextPath+"/identifier-not-found", Division.METHOD_POST, "help form");
		main.setHead(T_head1);

        return main;
    }

    private void createForm(Division main) throws WingException {

        Request request = ObjectModelHelper.getRequest(objectModel);
        String id = (String) request.getAttribute("identifier");
        Division div = main.addDivision("notice", "notice");
        Para p = div.addPara();
        p.addContent(T_message);
        p.addXref("/feedback", T_message_link);
        p.addContent("(" + id + ")");

    }
}
