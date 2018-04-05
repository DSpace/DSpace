/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.item;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Map;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.xml.sax.SAXException;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;

import org.dspace.app.xmlui.aspect.administrative.FlowCurationUtils;
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
        private static final Message T_taskgroup_label_name = message("xmlui.administrative.CurateForm.taskgroup_label_name");

    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    /**
     *
     * @param resolver source resolver.
     * @param objectModel Cocoon object model.
     * @param src source to transform.
     * @param parameters transformer parameters.
     * @throws ProcessingException passed through.
     * @throws SAXException passed through.
     * @throws IOException passed through.
     */
    @Override
        public void setup(SourceResolver resolver, Map objectModel, String src,
		          Parameters parameters) throws ProcessingException, SAXException, IOException
		{
        	super.setup(resolver, objectModel, src, parameters);
        	FlowCurationUtils.setupCurationTasks();
		}

    @Override
    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(contextPath + "/admin/item",T_item_trail);
		pageMeta.addTrail().addContent(T_trail);
    }

    /**
     * @throws WingException passed through.
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     * @throws java.io.UnsupportedEncodingException passed through.
     */
    @Override
    public void addBody(Body body)
        throws WingException, SQLException, AuthorizeException, UnsupportedEncodingException
	{
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
        String curateGroup = "";
        try
        {
        	curateGroup = (parameters.getParameter("select_curate_group") != null) ? parameters.getParameter("select_curate_group") : FlowCurationUtils.UNGROUPED_TASKS;
        }
        catch (Exception pe)
        {
        	// noop
        }
        if (!FlowCurationUtils.groups.isEmpty())
        {
        	curationTaskList.addLabel(T_taskgroup_label_name); //needs to check for >=1 group configured
            Select groupSelect = curationTaskList.addItem().addSelect("select_curate_group");
            groupSelect = FlowCurationUtils.getGroupSelectOptions(groupSelect);
            groupSelect.setSize(1);
            groupSelect.setRequired();
            groupSelect.setEvtBehavior("submitOnChange");
            if (curateGroup.equals(""))
            {
            	curateGroup = (String) (FlowCurationUtils.groups.keySet().iterator().next());
            }
            groupSelect.setOptionSelected(curateGroup);
        }
        curationTaskList.addLabel(T_label_name);
        Select taskSelect = curationTaskList.addItem().addSelect("curate_task");
        taskSelect.setAutofocus("autofocus");
        taskSelect = FlowCurationUtils.getTaskSelectOptions(taskSelect, curateGroup);
        taskSelect.setSize(1);
        taskSelect.setRequired();

            // need submit_curate_task and submit_return
	    Para buttonList = main.addPara();
        buttonList.addButton("submit_curate_task").setValue(T_submit_perform);
        buttonList.addButton("submit_queue_task").setValue(T_submit_queue);
	    buttonList.addButton("submit_return").setValue(T_submit_return);
        main.addHidden("administrative-continue").setValue(knot.getId());

    }

        // Add a method here to build it into the dspace.cfg ... ui.curation_tasks = estimate = "Estate"
        // Mapping the task name to either the description or the mapping key

}
