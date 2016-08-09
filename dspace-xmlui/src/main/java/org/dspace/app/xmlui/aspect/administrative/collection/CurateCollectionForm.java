/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.collection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
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
import org.dspace.content.Collection;

/**
 *
 * @author wbossons
 */
public class CurateCollectionForm extends AbstractDSpaceTransformer {
            /** Common Package Language Strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");

	private static final Message T_collection_trail = message("xmlui.administrative.collection.general.collection_trail");
	private static final Message T_options_metadata = message("xmlui.administrative.collection.general.options_metadata");
	private static final Message T_options_roles = message("xmlui.administrative.collection.general.options_roles");
	private static final Message T_options_harvest = message("xmlui.administrative.collection.GeneralCollectionHarvestingForm.options_harvest");
        private static final Message T_options_curate = message("xmlui.administrative.collection.general.options_curate");
        private static final Message T_submit_perform = message("xmlui.general.perform");
        private static final Message T_submit_queue = message("xmlui.general.queue");
        private static final Message T_submit_return = message("xmlui.general.return");
        // End common package language strings

        // Page/Form specific language strings
        private static final Message T_main_head = message("xmlui.administrative.collection.CurateCollectionForm.main_head");
        private static final Message T_title = message("xmlui.administrative.collection.CurateCollectionForm.title");
        private static final Message T_trail = message("xmlui.administrative.collection.CurateCollectionForm.trail");

        private static final Message T_label_name = message("xmlui.administrative.collection.CurateCollectionForm.label_name");
        private static final Message T_taskgroup_label_name = message("xmlui.administrative.CurateForm.taskgroup_label_name");
        

    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

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
        pageMeta.addTrail().addContent(T_collection_trail);
        pageMeta.addTrail().addContent(T_trail);
    }

    @Override
    public void addBody(Body body)
            throws WingException, SQLException, AuthorizeException, UnsupportedEncodingException
	{
            UUID collectionID = UUID.fromString(parameters.getParameter("collectionID", null));
            Collection thisCollection = collectionService.find(context, collectionID);
            String baseURL = contextPath + "/admin/collection?administrative-continue=" + knot.getId();

		// DIVISION: main
	    Division main = body.addInteractiveDivision("collection-curate",contextPath+"/admin/collection",Division.METHOD_MULTIPART,"primary administrative collection");
	    main.setHead(T_main_head.parameterize(thisCollection.getName()));

            List options = main.addList("options",List.TYPE_SIMPLE,"horizontal");
            options.addItem().addXref(baseURL+"&submit_metadata",T_options_metadata);
            options.addItem().addXref(baseURL+"&submit_roles",T_options_roles);
	    options.addItem().addXref(baseURL+"&submit_harvesting",T_options_harvest);
            options.addItem().addHighlight("bold").addXref(baseURL+"&submit_curate",T_options_curate);

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

}
