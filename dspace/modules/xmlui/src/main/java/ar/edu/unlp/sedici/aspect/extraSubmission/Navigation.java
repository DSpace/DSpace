/**
 * Copyright (C) 2011 SeDiCI <info@sedici.unlp.edu.ar>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ar.edu.unlp.sedici.aspect.extraSubmission;


import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.workflow.WorkflowItem;
import org.dspace.xmlworkflow.WorkflowFactory;
import org.dspace.xmlworkflow.XmlWorkflowManager;
import org.dspace.xmlworkflow.state.Workflow;
import org.dspace.xmlworkflow.storedcomponents.ClaimedTask;
import org.dspace.xmlworkflow.storedcomponents.PoolTask;
import org.dspace.xmlworkflow.storedcomponents.XmlWorkflowItem;
import org.xml.sax.SAXException;


/**
 * Aspect para agregar temas relacionados al submit y workflow
 */
public class Navigation  extends AbstractDSpaceTransformer implements CacheableProcessingComponent
{
    /** Language Strings */
    
    private static final Message T_context_edit_item =
            message("xmlui.administrative.Navigation.context_edit_item_workflow");
    
    private static final Message T_context_in_edit_item =
            message("xmlui.administrative.Navigation.context_in_edit_item_workflow");

    public void addOptions(Options options) throws SAXException, WingException,
    UIException, SQLException, IOException, AuthorizeException
{
    
    	List context = options.addList("context");
    	
        // Agregamos el menu para editar un item desde el workflow si el item no esta ya en workflow
		DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
		if (dso != null && dso.getType() == Constants.ITEM) {
			Item item = (Item) dso;
			try {			    
				XmlWorkflowItem workflowItem=XmlWorkflowManager.GetWorkflowItem(this.context, item);
				if (item.canEdit()) {
					//si el usuario no puede editar el item, no hace nada, sino entra por aca
					if (workflowItem!=null){
						//si existe un workflowitem para este item entra por aca
						java.util.List<ClaimedTask> ct = ClaimedTask.find(this.context, workflowItem);
						if (ct.isEmpty()){
							//si la tarea de edicion no fue tomada por nadie, le proveo la posibilidad de tomarla
							context.addItem().addXref(contextPath+"/handle/"+item.getHandle()+"/edit_item_metadata", T_context_edit_item);
						} else {
							//si la tarea fue tomada, debo verificar que sea el propio usuario el encargado de ella
				            ClaimedTask claimedT=ct.get(0);
				            String stepID = claimedT.getStepID();
				            
				            String actionID = claimedT.getActionID();
				            int workflowID = workflowItem.getID();
				            
							EPerson usuario=EPerson.find(this.context, claimedT.getOwnerID());
							
							if (usuario.getID()==this.context.getCurrentUser().getID()){
								//la tarea es del usuario, le proveo el link a la edición de metadatos
								context.addItem().addXref(contextPath+"/handle/"+item.getHandle()+"/workflow_edit_metadata?workflowID=X"+workflowID+"&stepID="+stepID+"&actionID="+actionID, T_context_edit_item);
							} else {
								//la tarea fue tomada por otro usuario, notifico quien es el encargado
								context.addItem(T_context_in_edit_item.parameterize(usuario.getFullName()));
							}
						}							
					} else {
						//si no existe un workflowitem para este item provee la posibilidad de ingresarlo al workflow
						context.addItem().addXref(contextPath+"/handle/"+item.getHandle()+"/edit_item_metadata", T_context_edit_item);
					}					
				} 
			} catch (Exception e) {
				// TODO: handle exception
			}
			
		}
        
    }

    /**
     * Generate the unique caching key.
     * This key must be unique inside the space of this component.
     */
    public Serializable getKey() {
        try {
            Request request = ObjectModelHelper.getRequest(objectModel);
            String key = request.getScheme() + request.getServerName() + request.getServerPort() + request.getSitemapURI() + request.getQueryString();

            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
            if (dso != null)
            {
                key += "-" + dso.getHandle();
            }

            return HashUtil.hash(key);
        }
        catch (SQLException sqle)
        {
            // Ignore all errors and just return that the component is not cachable.
            return "0";
        }
    }

        /**
     * Generate the cache validity object.
     *
     * The cache is always valid.
     */
    public SourceValidity getValidity() {
        return NOPValidity.SHARED_INSTANCE;
    }
}

