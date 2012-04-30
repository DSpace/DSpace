package org.dspace.app.xmlui.aspect.administrative.item;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.DCValue;
import org.dspace.content.DCDate;
import org.dspace.embargo.EmbargoManager;
import org.xml.sax.SAXException;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.ObjectModelHelper;

import java.sql.SQLException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;

/**
 * User interface for the user to enable/disable/edit the embargo of this item
 *
 * @author Kevin Van de Velde
 *
 * A new form where an admin can edit the embargo
 */
public class EditItemEmbargoForm extends AbstractDSpaceTransformer {

    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_submit_update = message("xmlui.general.update");
    private static final Message T_submit_return = message("xmlui.general.return");
    private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
    private static final Message T_option_head = message("xmlui.administrative.item.general.option_head");
    private static final Message T_option_status = message("xmlui.administrative.item.general.option_status");
    private static final Message T_option_bitstreams = message("xmlui.administrative.item.general.option_bitstreams");
    private static final Message T_option_embargo = message("xmlui.administrative.item.general.option_embargo");
    private static final Message T_option_metadata = message("xmlui.administrative.item.general.option_metadata");
    private static final Message T_option_view = message("xmlui.administrative.item.general.option_view");
    private static final Message T_title = message("xmlui.administrative.item.EditItemEmbargoForm.title");
    private static final Message T_trail = message("xmlui.administrative.item.EditItemEmbargoForm.trail");


    private static final Message T_embargo_custom = message("xmlui.administrative.item.EditItemEmbargoForm.embargo_custom");
    private static final Message T_embargo_curator_note  = message("xmlui.administrative.item.EditItemEmbargoForm.embargo_curator_note");
    private static final Message T_embargo_until = message("xmlui.administrative.item.EditItemEmbargoForm.embargo_until");
    private static final Message T_submit_lift_embargo = message("xmlui.administrative.item.EditItemEmbargoForm.lift_embargo");


    public void addPageMeta(PageMeta pageMeta) throws WingException
	{
		pageMeta.addMetadata("title").addContent(T_title);

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/admin/item", T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
	}




    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException, IOException, AuthorizeException {
        // Get our parameters and state
        int itemID = parameters.getParameterAsInteger("itemID",-1);
        Item item = Item.find(context, itemID);
        String baseURL = contextPath+"/admin/item?administrative-continue="+knot.getId();

        Request request = ObjectModelHelper.getRequest(objectModel);

        Division main = body.addInteractiveDivision("edit-item-status", contextPath+"/admin/item", Division.METHOD_POST,"primary administrative item");
        main.setHead(T_option_head);

        String dateString = getDateEmbargoString(item);

        addOptionsList(baseURL, main);

        Division embargoDiv = createForm(main, dateString, item);

        addButtons(embargoDiv);

        main.addHidden("administrative-continue").setValue(knot.getId());
    }

    private void addOptionsList(String baseURL, Division main) throws WingException {
        List options = main.addList("options",List.TYPE_SIMPLE,"horizontal");
        options.addItem().addXref(baseURL+"&submit_status",T_option_status);
        options.addItem().addXref(baseURL+"&submit_bitstreams",T_option_bitstreams);
        options.addItem().addHighlight("bold").addXref(baseURL + "&submit_embargo", T_option_embargo);
        options.addItem().addXref(baseURL+"&submit_metadata",T_option_metadata);
        options.addItem().addXref(baseURL + "&view_item", T_option_view);
    }

    private Division createForm(Division main, String dateString, Item item) throws WingException {
        Division embargoDiv = main.addDivision("edit_embargo_div");


        // Embargo Type
        DCValue[] values = item.getMetadata("dc.type.embargo");
        String type ="";
        if(values!=null && values.length > 0){
            type = values[0].value;
        }
        embargoDiv.addPara().addContent(T_embargo_custom.parameterize(type));

        Para p = addTextBox(embargoDiv, T_embargo_until, "embargoed_until", dateString, false);
        p.addButton("submit_update").setValue(T_submit_update);


        // Curator Note
        values = item.getMetadata("dryad.curatorNote");
        String curatorNote ="";
        if(values!=null && values.length > 0){
            curatorNote = values[0].value;
        }
        embargoDiv.addPara().addContent(T_embargo_curator_note.parameterize(curatorNote));
        return embargoDiv;
    }

    private void addButtons(Division embargoDiv) throws WingException {
        Para buttonPara = embargoDiv.addPara("buttons", "");

        buttonPara.addButton("submit_lift_emabrgo").setValue(T_submit_lift_embargo);
        buttonPara.addButton("submit_return").setValue(T_submit_return);
    }


    private String getDateEmbargoString(Item item) throws SQLException, AuthorizeException, IOException {
        DCDate embargoDate = EmbargoManager.getEmbargoDate(context, item);
        return embargoDate.toString();   //removed string generation via SimpleDateFormat, since it seems to produce one-off erroroneous dates
    }


    private Para addTextBox(Division div, Message label, String name, String value, boolean disabled) throws WingException {
        Para p = div.addPara();
        p.addContent(label);
        Text t1 = p.addText(name);
        t1.setValue(value);
        t1.setDisabled(disabled);
        return p;
    }
}
