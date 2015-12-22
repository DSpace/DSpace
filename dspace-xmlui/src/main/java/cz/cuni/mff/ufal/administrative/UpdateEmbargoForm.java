/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.administrative;

import java.sql.SQLException;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.aspect.administrative.item.ViewItem;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.embargo.EmbargoManager;


/**
 * Display information about the item and allow the user to change 
 * 
 * @author Amir Kamran
 */


public class UpdateEmbargoForm extends AbstractDSpaceTransformer {
	
	Logger log = Logger.getLogger(UpdateEmbargoForm.class);

	/** Language strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");	
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	private static final Message T_option_head = message("xmlui.administrative.item.general.option_head");

	private static final Message T_update = message("xmlui.general.update");
	private static final Message T_return = message("xmlui.general.return");
	private static final Message T_delete = message("xmlui.general.delete");

	
	public void addPageMeta(PageMeta pageMeta) throws WingException
	{
		pageMeta.addMetadata("title").addContent("Embargo");
		
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/admin/item",T_item_trail);
		pageMeta.addTrail().addContent("Embargo");
	}

	public void addBody(Body body) throws SQLException, WingException
	{
		// Get our parameters and state
		int itemID = parameters.getParameterAsInteger("itemID",-1);
		
		Item item = Item.find(context, itemID);
		String baseURL = contextPath+"/admin/item?administrative-continue="+knot.getId();

		// DIVISION: main
		Division main = body.addInteractiveDivision(
				"update-embargo", contextPath+"/admin/item", Division.METHOD_POST,"primary administrative update-embargo");
		main.setHead(T_option_head);

		String tabLink = baseURL + "&embargo";

		// LIST: options
		List options = main.addList("options", List.TYPE_SIMPLE, "horizontal");
		ViewItem.add_options(context, eperson, options, baseURL, ViewItem.T_option_embargo, tabLink, item);

		List form = main.addList("currentEmbargo", List.TYPE_FORM);
		form.setHead(
				String.format("Item embargo [%s]", item.getHandle()) );

		String terms = ConfigurationManager.getProperty("embargo.field.terms");
		form.addItem().addContent(
			String.format("Embargo is read from a specific metadata field [%s] if present.",
					terms));
		
		Text t = form.addItem().addText("embargo_date");
		DCDate date = null;
		try {
			date = EmbargoManager.getEmbargoTermsAsDate(context, item);
		} catch (Exception e) {
			main.addPara(e.toString(), "alert alert-error");
			t.setDisabled();
		}
		t.setLabel("Embargo date");
		t.setHelp("Fill out date e.g., 2020-09-12");
		if ( date == null ) {
		}else {
			t.setValue(date.toString());
		}

		org.dspace.app.xmlui.wing.element.Item form_item = form.addItem();
		form_item.addButton("submit_update").setValue(T_update);
		Button deleteButton = form_item.addButton("submit_delete");
		deleteButton.setValue(T_delete);
		form_item.addButton("return").setValue(T_return);

		if(date == null){
			deleteButton.setDisabled();
		}
		
		main.addPara().addHidden("administrative-continue").setValue(knot.getId());

	}
	
}
