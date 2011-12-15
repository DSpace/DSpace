/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.item;

import java.util.ArrayList;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Text;
import org.xml.sax.SAXException;

/**
 * Query the user for an item's identifier.
 * 
 * @author Jay Paz
 */

public class FindItemForm extends AbstractDSpaceTransformer {

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	
	private static final Message T_title = message("xmlui.administrative.item.FindItemForm.title");
	private static final Message T_head1 = message("xmlui.administrative.item.FindItemForm.head1");
	private static final Message T_identifier_label = message("xmlui.administrative.item.FindItemForm.identifier_label");
	private static final Message T_identifier_error = message("xmlui.administrative.item.FindItemForm.identifier_error");
	private static final Message T_find = message("xmlui.administrative.item.FindItemForm.find");
	
	public void addPageMeta(PageMeta pageMeta) throws WingException  
	{
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_item_trail);
		pageMeta.addMetadata("title").addContent(T_title);
	}

	
	public void addBody(Body body) throws SAXException, WingException
	{
		// Get our parameters and state;
		String identifier = parameters.getParameter("identifier",null);
		
		String errorString = parameters.getParameter("errors",null);
		ArrayList<String> errors = new ArrayList<String>();
		if (errorString != null)
        {
			for (String error : errorString.split(","))
            {
				errors.add(error);
            }
        }
		
		// DIVISION: find-item
		Division findItem = body.addInteractiveDivision("find-item",contextPath + "/admin/item", Division.METHOD_GET,"primary administrative item");
		findItem.setHead(T_head1);
		
		List form = findItem.addList("find-item-form", List.TYPE_FORM);
		
		Text id = form.addItem().addText("identifier");
        id.setAutofocus("autofocus");
		id.setLabel(T_identifier_label);
		if (identifier != null)
        {
            id.setValue(identifier);
        }
		if (errors.contains("identifier"))
        {
            id.addError(T_identifier_error);
        }
		
		form.addItem().addButton("submit_find").setValue(T_find);
		
		findItem.addHidden("administrative-continue").setValue(knot.getId());
	}
}
