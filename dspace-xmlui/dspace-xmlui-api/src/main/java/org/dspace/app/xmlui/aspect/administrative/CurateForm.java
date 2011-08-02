/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

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
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;

/**
 * Generates the Administrative Curate Form, from which any DSpace object can
 * be curated. 
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
        private static final Message T_object_label_name = message("xmlui.administrative.CurateForm.object_label_name");
        private static final Message T_object_hint = message("xmlui.administrative.CurateForm.object_hint");
        
        /**
         * Initialize the page metadata & breadcrumb trail
         *
         * @param pageMeta
         * @throws WingException
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
         * @param body
         * @throws WingException
         * @throws SQLException
         * @throws AuthorizeException
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
                Division div = body.addInteractiveDivision("curate", contextPath + "/admin/curate", Division.METHOD_MULTIPART,"primary administrative curate");
                div.setHead(T_title);
                
                // Curate Form
                List form = div.addList("curate-form", List.TYPE_FORM);
		
                // Object ID Textbox (required)
                Text id = form.addItem().addText("identifier");
                id.setLabel(T_object_label_name);
                if (objectID != null)
                {
                    id.setValue(objectID);
                }
                id.setRequired();
                id.setHelp(T_object_hint);
                
                // Selectbox of Curation Task options (required)              
                Select select = form.addItem().addSelect("curate_task");
                select.setLabel(T_task_label_name);
                select = getCurationOptions(select);
                select.setSize(1);
                select.setRequired();
                if(taskSelected!=null)
                {    
                    select.setOptionSelected(taskSelected);
                }
                
                // Buttons: 'curate' and 'queue'
                Para buttonList = div.addPara();
                buttonList.addButton("submit_curate_task").setValue(T_submit_perform);
                buttonList.addButton("submit_queue_task").setValue(T_submit_queue);
                div.addHidden("administrative-continue").setValue(knot.getId());
        }

        /**
         * Build a selectbox of all available Curation Task options
         * 
         * @param select the empty selectbox
         * @return a selectbox full of options
         * @throws WingException
         * @throws UnsupportedEncodingException 
         */
        private Select getCurationOptions(Select select)
            throws WingException, UnsupportedEncodingException 
        {
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

}
