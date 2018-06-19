/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.collection;

import java.sql.SQLException;
import java.util.UUID;


import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;

/**
 * Confirmation step for the deletion of an entire collection
 * @author Alexey Maslov
 */
public class DeleteCollectionConfirm extends AbstractDSpaceTransformer   
{
	/** Language Strings */
	private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
	
	private static final Message T_title = message("xmlui.administrative.collection.DeleteCollectionConfirm.title");
	private static final Message T_trail = message("xmlui.administrative.collection.DeleteCollectionConfirm.trail");

	private static final Message T_main_head = message("xmlui.administrative.collection.DeleteCollectionConfirm.main_head");
	private static final Message T_main_para = message("xmlui.administrative.collection.DeleteCollectionConfirm.main_para");
	private static final Message T_confirm_item1 = message("xmlui.administrative.collection.DeleteCollectionConfirm.confirm_item1");
	private static final Message T_confirm_item2 = message("xmlui.administrative.collection.DeleteCollectionConfirm.confirm_item2");
	private static final Message T_confirm_item3 = message("xmlui.administrative.collection.DeleteCollectionConfirm.confirm_item3");

	private static final Message T_submit_confirm = message("xmlui.general.delete");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");
	
	protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
		UUID collectionID = UUID.fromString(parameters.getParameter("collectionID", null));
		Collection thisCollection = collectionService.find(context, collectionID);
		
		// DIVISION: main
	    Division main = body.addInteractiveDivision("collection-confirm-delete",contextPath+"/admin/collection",Division.METHOD_POST,"primary administrative collection");
	    main.setHead(T_main_head.parameterize(collectionID));
	    main.addPara(T_main_para.parameterize(collectionService.getMetadata(thisCollection, "name")));
	    List deleteConfirmHelp = main.addList("consequences",List.TYPE_BULLETED);
	    deleteConfirmHelp.addItem(T_confirm_item1);
	    deleteConfirmHelp.addItem(T_confirm_item2);
	    deleteConfirmHelp.addItem(T_confirm_item3);
	    
	    Para buttonList = main.addPara();
	    buttonList.addButton("submit_confirm").setValue(T_submit_confirm);
	    buttonList.addButton("submit_cancel").setValue(T_submit_cancel);
	    
    	main.addHidden("administrative-continue").setValue(knot.getId());
    }
}
