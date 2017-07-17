/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.item;

import java.sql.SQLException;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Constants;

import org.dspace.app.util.CollectionDropDown;

/**
 * This page displays collections to which the user can move an item.
 * 
 * @author Nicholas Riley
 */
public class MoveItemForm extends AbstractDSpaceTransformer {

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	private static final Message T_submit_cancel = message("xmlui.general.cancel");
	
	private static final Message T_title = message("xmlui.administrative.item.MoveItemForm.title");
	private static final Message T_trail = message("xmlui.administrative.item.MoveItemForm.trail");
	private static final Message T_head1 = message("xmlui.administrative.item.MoveItemForm.head1");
	private static final Message T_collection = message("xmlui.administrative.item.MoveItemForm.collection");
	private static final Message T_collection_help = message("xmlui.administrative.item.MoveItemForm.collection_help");
	private static final Message T_collection_default = message("xmlui.administrative.item.MoveItemForm.collection_default");
	private static final Message T_submit_move = message("xmlui.administrative.item.MoveItemForm.submit_move");
    private static final Message T_submit_inherit = message("xmlui.administrative.item.MoveItemForm.inherit_policies");
    private static final Message T_submit_inherit_help = message("xmlui.administrative.item.MoveItemForm.inherit_policies_help");


	public void addPageMeta(PageMeta pageMeta) throws WingException
	{
		pageMeta.addMetadata("title").addContent(T_title);
		
		
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath+"/admin/item", T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
	}

	public void addBody(Body body) throws WingException, SQLException 
	{
        // Get our parameters and state
        int itemID = parameters.getParameterAsInteger("itemID",-1);
        Item item = Item.find(context, itemID);
        
        // DIVISION: Main
        Division main = body.addInteractiveDivision("move-item", contextPath+"/admin/item", Division.METHOD_POST, "primary administrative item");
        main.setHead(T_head1.parameterize(item.getHandle()));

        Collection[] collections = Collection.findAuthorizedOptimized(context, Constants.ADD);

        List list = main.addList("select-collection", List.TYPE_FORM);
        Select select = list.addItem().addSelect("collectionID");
        select.setLabel(T_collection);
        select.setHelp(T_collection_help);
        
        Collection owningCollection = item.getOwningCollection();
        if (owningCollection == null) {
            select.addOption("",T_collection_default);
        }

        CollectionDropDown.CollectionPathEntry[] dropdownEntries = CollectionDropDown.annotateWithPaths(collections);
        
        for (CollectionDropDown.CollectionPathEntry entry : dropdownEntries)
        {
            // Only add the item if it isn't already the owner
            if (!item.isOwningCollection(entry.collection))
            {
                select.addOption(entry.collection.equals(owningCollection), entry.collection.getID(), entry.path);
            }
        }
        
        org.dspace.app.xmlui.wing.element.Item actions = list.addItem();
        CheckBox inheritPolicies = actions.addCheckBox("inheritPolicies");
        inheritPolicies.setLabel(T_submit_inherit);
        inheritPolicies.setHelp(T_submit_inherit_help);
        inheritPolicies.addOption("inheritPolicies");
        actions.addButton("submit_move").setValue(T_submit_move);
		actions.addButton("submit_cancel").setValue(T_submit_cancel);

		main.addHidden("administrative-continue").setValue(knot.getId());
	}
}
