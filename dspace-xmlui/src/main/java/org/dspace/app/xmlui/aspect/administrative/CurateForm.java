/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Map;
import org.xml.sax.SAXException;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.SourceResolver;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;

/**
 * Generates the Administrative Curate Form, from which any DSpace object can
 * be curated.
 *
 * @author tdonohue
 */
public class CurateForm extends AbstractDSpaceTransformer 
{
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");
    private static final Message T_submit_perform = message("xmlui.general.perform");
    private static final Message T_submit_queue = message("xmlui.general.queue");
    private static final Message T_title = message("xmlui.administrative.CurateForm.title");
    private static final Message T_trail = message("xmlui.administrative.CurateForm.trail");
    private static final Message T_task_label_name = message("xmlui.administrative.CurateForm.task_label_name");
    private static final Message T_taskgroup_label_name = message("xmlui.administrative.CurateForm.taskgroup_label_name");
    private static final Message T_object_label_name = message("xmlui.administrative.CurateForm.object_label_name");
    private static final Message T_object_hint = message("xmlui.administrative.CurateForm.object_hint");

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src,
              Parameters parameters) throws ProcessingException, SAXException, IOException
    {
        super.setup(resolver, objectModel, src, parameters);
        FlowCurationUtils.setupCurationTasks();
    }

    /**
     * Initialize the page metadata and breadcrumb trail
     *
     * @param pageMeta the metadata.
     * @throws WingException passed through.
     */
    @Override
    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
            pageMeta.addMetadata("title").addContent(T_title);
            pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
            pageMeta.addTrail().addContent(T_trail);
    }

    /** 
     * Add object curation form
     * 
     * @param body body of the form.
     * @throws WingException passed through.
     * @throws SQLException passed through.
     * @throws AuthorizeException passed through.
     * @throws java.io.UnsupportedEncodingException passed through.
     */
    @Override
    public void addBody(Body body)
            throws WingException, SQLException,
                   AuthorizeException, UnsupportedEncodingException
    {
        // Get our parameters and state;
        String objectID = parameters.getParameter("identifier", null);
        String taskSelected = parameters.getParameter("curate_task", null);

        // DIVISION: curate
        Division div = body.addInteractiveDivision("curate",
                contextPath + "/admin/curate",
                Division.METHOD_MULTIPART,
                "primary administrative curate");
        div.setHead(T_title);

        // Curate Form
        List form = div.addList("curate-form", List.TYPE_FORM);

        // Object ID Textbox (required)
        Text id = form.addItem().addText("identifier");
        id.setAutofocus("autofocus");
        id.setLabel(T_object_label_name);
        if (objectID != null)
        {
            id.setValue(objectID);
        }
        id.setRequired();
        id.setHelp(T_object_hint);

        // Selectbox of Curation Task options (required)
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
            Select groupSelect = form.addItem().addSelect("select_curate_group");
            groupSelect = FlowCurationUtils.getGroupSelectOptions(groupSelect);
            groupSelect.setLabel(T_taskgroup_label_name); 
            groupSelect.setSize(1);
            groupSelect.setRequired();
            groupSelect.setEvtBehavior("submitOnChange");
            if (curateGroup.equals(""))
            {
                curateGroup = (String) (FlowCurationUtils.groups.keySet().iterator().next());
            }
            groupSelect.setOptionSelected(curateGroup);
        }
        Select taskSelect = form.addItem().addSelect("curate_task");
        taskSelect = FlowCurationUtils.getTaskSelectOptions(taskSelect, curateGroup);
        taskSelect.setLabel(T_task_label_name);
        taskSelect.setSize(1);
        taskSelect.setRequired();
        if(taskSelected!=null)
        {    
            taskSelect.setOptionSelected(taskSelected);
        }

        // Buttons: 'curate' and 'queue'
        Para buttonList = div.addPara();
        buttonList.addButton("submit_curate_task").setValue(T_submit_perform);
        buttonList.addButton("submit_queue_task").setValue(T_submit_queue);
        div.addHidden("administrative-continue").setValue(knot.getId());
    }
}
