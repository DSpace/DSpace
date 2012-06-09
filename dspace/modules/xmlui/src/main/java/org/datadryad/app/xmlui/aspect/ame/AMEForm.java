package org.datadryad.app.xmlui.aspect.ame;

import java.io.IOException;

import java.sql.SQLException;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.Value;
import org.dspace.app.xmlui.wing.element.Xref;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.MetadataAuthorityManager;


/**
 * Implements the automatic metadata extraction (AME) form. This form depends 
 * on the choice authority feature of DSpace.
 * 
 * @author craig.willis@unc.edu
 */
public class AMEForm extends AbstractDSpaceTransformer 
{

	/* Localized Strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_submit_update = message("xmlui.general.update");
    private static final Message T_submit_return = message("xmlui.general.return");
    
	private static final Message T_title = message("xmlui.aspect.ame.AMEForm.title");
	private static final Message T_trail = message("xmlui.aspect.ame.AMEForm.trail");
	private static final Message T_instructions = message("xmlui.aspect.ame.AMEForm.instructions");
	private static final Message T_label_title = message("xmlui.aspect.ame.AMEForm.label_title");
	private static final Message T_label_abstract = message("xmlui.aspect.ame.AMEForm.label_abstract");
	private static final Message T_label_keywords = message("xmlui.aspect.ame.AMEForm.label_keywords");
	private static final Message T_label_scientificNames = message("xmlui.aspect.ame.AMEForm.label_scientificNames");
	private static final Message T_unlock = message("xmlui.authority.confidence.unlock.help");


	public void addPageMeta(PageMeta pageMeta) throws WingException,
			SQLException, AuthorizeException, IOException {

		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrail().addContent(T_trail);

		int itemID = parameters.getParameterAsInteger("itemID", -1);
		Item item = Item.find(context, itemID);

		if (item == null) return;
	}

	public void addBody(Body body) throws SQLException, WingException 
	{
		// Get our parameters and state
		int itemID = parameters.getParameterAsInteger("itemID",-1);
		if  (itemID == -1)
			return;
		
		String baseURL = contextPath+"/item/ame?ame-continue="+knot.getId();
		
		Item item = Item.find(context, itemID);

        // DIVISION: Main
        Division main = body.addInteractiveDivision("ame-item", contextPath+"/item/ame", 
        		Division.METHOD_POST, "ame");
        main.setHead(T_title);
		
		// LIST: Item meta-meta information
        Division metadata = main.addDivision("ame-item-metadata");
		Division instructions = main.addDivision("ame-instructions");
		Division subjectsDiv = main.addDivision("ame-item-subject", "ame-field-table");
		Division sciNameDiv = main.addDivision("ame-item-sciName", "ame-field-table");
		

        List itemInfo = metadata.addList("ame-item-info");
        itemInfo.addItem().addHidden("ame-item-id").setValue(itemID);
		
		// Add the item title
		String title = getItemTitle(item);
		if (title != null)
		{
			itemInfo.addLabel(T_label_title);
			org.dspace.app.xmlui.wing.element.Item itemTitle = itemInfo.addItem("ame-item-title", "ame-item-title");
			itemTitle.addContent(title);
		}
		
		// Add the item abstract
		String abstr = getItemAbstract(item);
		if (abstr != null)
		{
			itemInfo.addLabel(T_label_abstract);
			org.dspace.app.xmlui.wing.element.Item itemAbstract = itemInfo.addItem("ame-item-abstract", "ame-item-abstract");
			itemAbstract.addContent(abstr);
		}
		
		Para p = instructions.addPara("ame-instructions", "ame-instructions");
		p.addContent(T_instructions);

		// Add all dc.subjects 
		DCValue[] dcValues = item.getMetadata("dc.subject");

		Division subjects = subjectsDiv.addDivision("ame-item-subjects", "ame-field-list");


		List subjectsList = subjects.addList("ame-item-subjects", List.TYPE_BULLETED, "ame-field-list");
		subjectsList.addLabel(T_label_keywords);
		subjectsList.addItem();
		int fieldIndex = 1;
		for (DCValue dcValue: dcValues)
		{
			org.dspace.app.xmlui.wing.element.Item listItem = subjectsList.addItem("item" + fieldIndex, "ame-item-field");
			String nameField = "name_" + fieldIndex;
			String fieldName = getFieldName(dcValue);
			listItem.addHidden(nameField).setValue(fieldName);
			
			listItem.addXref(baseURL + "&submit_remove&remove_field_name=" + fieldName + "&remove_field_value=" + dcValue.value, "X",
					"ame-item-remove");
	        
			Text text = listItem.addText("value_" + fieldIndex, "submit-text");
	        // Setup the select field
	        text.setSize(30);
	        text.setValue(dcValue.value);
	        String fieldKey = MetadataAuthorityManager.makeFieldKey(dcValue.schema, dcValue.element, dcValue.qualifier);
            text.setAuthorityControlled();
            text.setAuthorityRequired(MetadataAuthorityManager.getManager().isAuthorityRequired(fieldKey));
            Value authValue =  text.setAuthorityValue((dcValue.authority == null)?"":dcValue.authority, Choices.getConfidenceText(dcValue.confidence));
            // add the "unlock" button to auth field
            Button unlock = authValue.addButton("authority_unlock_" + nameField,"ds-authority-lock");
            unlock.setHelp(T_unlock);
            text.setChoices(fieldKey);
            text.setChoicesPresentation("lookup");
            text.setChoicesClosed(false);
	        fieldIndex++;
		}

		// Add a blank input field for add operation
		Text blankText = subjectsList.addItem("add-dc_subject", "ame-add-item").addText("dc_subject", "ame-item");
        blankText.setSize(30);
        blankText.enableAddOperation();
		

		Division subjectsWidget = subjectsDiv.addDivision("dc_subject", "ame-suggest-widget");
		Division subjectsWidgetContainer = subjectsWidget.addDivision("container", "ame-suggest-container");		
		

        // Add all dwc.ScientificNames
		dcValues = item.getMetadata("dwc.ScientificName");
		
		
		Division sciNames = sciNameDiv.addDivision("ame-item-sciNames", "ame-field-list");
		List sciNameList = sciNames.addList("ame-item-sciName", List.TYPE_BULLETED, "ame-field-list");
		sciNameList.addLabel(T_label_scientificNames);
		sciNameList.addItem();
		
		for (DCValue dcValue: dcValues)
		{
			org.dspace.app.xmlui.wing.element.Item formItem = sciNameList.addItem("item" + fieldIndex, "ame-item-field");
			
			String nameField = "name_" + fieldIndex;
			String fieldName = getFieldName(dcValue);
			
			Xref remove = formItem.addXref(baseURL + "&submit_remove&remove_field_name=" + fieldName + "&remove_field_value=" + dcValue.value);
			remove.addContent("X");
			

			formItem.addHidden(nameField).setValue(fieldName);
			Text text = formItem.addText("value_"+ fieldIndex, "submit-text");
	        // Setup the select field
	        text.setSize(30);
	        text.setValue(dcValue.value);
	        String fieldKey = MetadataAuthorityManager.makeFieldKey(dcValue.schema, dcValue.element, dcValue.qualifier);

            text.setAuthorityControlled();
            text.setAuthorityRequired(MetadataAuthorityManager.getManager().isAuthorityRequired(fieldKey));
            Value authValue =  text.setAuthorityValue((dcValue.authority == null)?"":dcValue.authority, Choices.getConfidenceText(dcValue.confidence));
            // add the "unlock" button to auth field
            Button unlock = authValue.addButton("authority_unlock_" + nameField,"ds-authority-lock");
            unlock.setHelp(T_unlock);
            text.setChoices(fieldKey);
            text.setChoicesPresentation("lookup");
            text.setChoicesClosed(false);
	        fieldIndex++;
		}
		
		// Add a blank input field for add operation
		Text blankDwcField = sciNameList.addItem("add-dwc_ScientificName", "ame-add-item").addText("dwc_ScientificName");
		blankDwcField.setSize(30);
		blankDwcField.enableAddOperation();
		
		
		Division sciNameWidget = sciNameDiv.addDivision("dwc_ScientificName", "ame-suggest-widget");
		Division sciNameWidgetContainer = sciNameWidget.addDivision("container", "ame-suggest-container");
		sciNameWidgetContainer.addPara("test");

		Division bottomButtons = main.addDivision("ame-bottom", "ame-bottom");
		
        // PARA: actions
		Para actions = bottomButtons.addPara(null,"ame-actions bottom" );
        actions.addButton("submit_update").setValue(T_submit_update);
        actions.addButton("submit_return").setValue(T_submit_return);

        
        main.addHidden("ame-continue").setValue(knot.getId());
	}

	public static String getFieldName(DCValue value)
	{
		String dcSchema = value.schema;
		String dcElement = value.element;
		String dcQualifier = value.qualifier;
		if (dcQualifier != null && ! dcQualifier.equals(Item.ANY))
		{
			return dcSchema + "_" + dcElement + '_' + dcQualifier;
		}
		else
		{
			return dcSchema + "_" + dcElement;
		}
	}
	
	/**
	 * Obtain the item's title.
	 */
	public static String getItemTitle(Item item) {
		DCValue[] titles = item.getDC("title", Item.ANY, Item.ANY);

		String title;
		if (titles != null && titles.length > 0) title = titles[0].value;
		else title = null;
		return title;
	}
	
	/**
	 * Obtain the item's description.
	 */
	public static String getItemAbstract(Item item) {
		DCValue[] descriptions = item.getMetadata("dc.description.abstract");
		if (descriptions == null || descriptions.length == 0)
			descriptions = item.getMetadata("dc.description");
		

		String description;
		if (descriptions != null && descriptions.length > 0) description = descriptions[0].value;
		else description = null;
		return description;
	}
}
