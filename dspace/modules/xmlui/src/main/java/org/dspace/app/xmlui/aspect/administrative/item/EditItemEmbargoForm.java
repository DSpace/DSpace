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


        // DIVISION: main
        Division main = body.addInteractiveDivision("edit-item-status", contextPath+"/admin/item", Division.METHOD_POST,"primary administrative item");
        main.setHead(T_option_head);

        DCDate embargoDate = EmbargoManager.getEmbargoDate(context, item);
        String dateString = null;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        if(embargoDate != null)
            dateString = " (embargoed until: " + format.format(embargoDate.toDate()) + ")";

        // LIST: options
        List options = main.addList("options",List.TYPE_SIMPLE,"horizontal");
        options.addItem().addXref(baseURL+"&submit_status",T_option_status);
        options.addItem().addXref(baseURL+"&submit_bitstreams",T_option_bitstreams);
        options.addItem().addHighlight("bold").addXref(baseURL + "&submit_embargo", T_option_embargo);
        options.addItem().addXref(baseURL+"&submit_metadata",T_option_metadata);
        options.addItem().addXref(baseURL + "&view_item", T_option_view);

        //Add a radio button to enable/disable embargo
        Division embargoDiv = main.addDivision("edit_embargo_div");
        Para para = embargoDiv.addPara();
        //This will become a radio in the xsl
        CheckBox embargoRadio = para.addCheckBox("embargo");
        embargoRadio.addOption(embargoDate == null, "disabled", "Disabled");
        embargoRadio.addOption(embargoDate != null, "enabled", "1 year embargo" + (dateString != null ? dateString : ""));

        //Add the textboxes for the embargo
//        Composite dateBox = para.addComposite("date");
//        Text dayTxt = dateBox.addText("date_day", "Day:");
//        Text monthTxt = dateBox.addText("date_month", "Month:");
//        Text yearTxt = dateBox.addText("date_year", "Year:");

        //In case we have an embargodate show it
//        if(embargoDate != null){
//            dayTxt.setValue(String.valueOf(embargoDate.getDay()));
//            monthTxt.setValue(String.valueOf(embargoDate.getMonth()));
//            yearTxt.setValue(String.valueOf(embargoDate.getYear()));
//        }

        Para buttonPara = embargoDiv.addPara("buttons", "");

        buttonPara.addButton("submit_update").setValue(T_submit_update);
        buttonPara.addButton("submit_return").setValue(T_submit_return);

        main.addHidden("administrative-continue").setValue(knot.getId());
    }
}
