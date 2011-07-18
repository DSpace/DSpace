/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.item;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.SQLException;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;


import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;

/**
 *
 * @author wbossons
 */
public class CurateItemForm extends AbstractDSpaceTransformer {

	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
        private static final Message T_submit_perform = message("xmlui.general.perform");
        private static final Message T_submit_queue = message("xmlui.general.queue");
	private static final Message T_submit_return = message("xmlui.general.return");
	private static final Message T_item_trail = message("xmlui.administrative.item.general.item_trail");
	private static final Message T_option_head = message("xmlui.administrative.item.general.option_head");
	private static final Message T_option_status = message("xmlui.administrative.item.general.option_status");
	private static final Message T_option_bitstreams = message("xmlui.administrative.item.general.option_bitstreams");
	private static final Message T_option_metadata = message("xmlui.administrative.item.general.option_metadata");
	private static final Message T_option_view = message("xmlui.administrative.item.general.option_view");
        private static final Message T_option_curate = message("xmlui.administrative.item.general.option_curate");
        private static final Message T_title = message("xmlui.administrative.item.CurateItemForm.title");
	private static final Message T_trail = message("xmlui.administrative.item.CurateItemForm.trail");
        private static final Message T_label_name = message("xmlui.administrative.item.CurateItemForm.label_name");

        /**
         * common package method for initializing form gui elements
         * Could be refactored.
         *
         * @param pageMeta
         * @throws WingException
         */
        public void addPageMeta(PageMeta pageMeta) throws WingException
    {
                pageMeta.addMetadata("title").addContent(T_title);
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/admin/item",T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
    }
    /** addBody
     *
     * @param body
     * @throws WingException
     * @throws SQLException
     * @throws AuthorizeException
     */
        public void addBody(Body body)
                                    throws WingException, SQLException,
                                                        AuthorizeException, UnsupportedEncodingException
	{
                int itemID = parameters.getParameterAsInteger("itemID", -1);
		Item item = Item.find(context, itemID);
                
		String baseURL = contextPath + "/admin/item?administrative-continue="
				+ knot.getId() ;

		// DIVISION: main
		Division main = body.addInteractiveDivision("edit-item-status", contextPath + "/admin/item", Division.METHOD_POST,"primary administrative edit-item-status");
		main.setHead(T_option_head);

		// LIST: options
		List options = main.addList("options", List.TYPE_SIMPLE, "horizontal");
		options.addItem().addXref(baseURL + "&submit_status", T_option_status);
		options.addItem().addXref(baseURL + "&submit_bitstreams", T_option_bitstreams);
		options.addItem().addXref(baseURL + "&submit_metadata", T_option_metadata);
		options.addItem().addXref(baseURL + "&view_item", T_option_view);
                options.addItem().addHighlight("bold").addXref(baseURL + "&submit_curate", T_option_curate);
                


	    List curationTaskList = main.addList("curationTaskList", "form");
	    curationTaskList.addLabel(T_label_name);
            Select select = curationTaskList.addItem().addSelect("curate_task");
            select = getCurationOptions(select);
            select.setSize(1);
            select.setRequired();

            // need submit_curate_task and submit_return
	    Para buttonList = main.addPara();
            buttonList.addButton("submit_curate_task").setValue(T_submit_perform);
            buttonList.addButton("submit_queue_task").setValue(T_submit_queue);
	    buttonList.addButton("submit_return").setValue(T_submit_return);
            main.addHidden("administrative-continue").setValue(knot.getId());

    }

        private Select getCurationOptions(Select select)
                                            throws WingException, UnsupportedEncodingException {
            String tasksString = ConfigurationManager.getProperty("curate", "ui.tasknames");
            String[] tasks = tasksString.split(",");
            for (String task : tasks)
            {
                String[] keyValuePair = task.split("=");
                select.addOption(URLDecoder.decode(keyValuePair[0].trim(), "UTF-8"),
                                 URLDecoder.decode(keyValuePair[1].trim(), "UTF-8"));
            }
            return select;
        }

        // Add a method here to build it into the dspace.cfg ... ui.curation_tasks = estimate = "Estate"
        // Mapping the task name to either the description or the mapping key




}
