/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;

import cz.cuni.mff.ufal.dspace.IOUtils;

/**
 * Create new version of existing archived submission. This action is used by the 
 * submission page, the user may check archived submission 
 * and when he clicks the add new version button this action 
 * will create new workflow item with metadata fields populated with
 * values of the archived item. 
 * 
 * @author Michal Josífko
 * modified for LINDAT/CLARIN
 */
public class AddNewVersionAction extends AbstractAction
{

    private final static int SUBMISSION_THIRD_STEP = 3;
    
    /**
     * Create new version of existing archived submission
     * 
     * @param redirector
     * @param resolver
     * @param objectModel
     *            Cocoon's object model
     * @param source
     * @param parameters
     */
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
        
        Context context = ContextUtil.obtainContext(objectModel);
        Request request = ObjectModelHelper.getRequest(objectModel);
        
        WorkspaceItem firstWorkspaceItem = null;
        String redirectURL = null;
        
    	String[] itemIDs = request.getParameterValues("itemID");
    	
    	if (itemIDs != null)
    	{
        	for (String itemID : itemIDs)
        	{        	    
    			Item item = Item.find(context, Integer.valueOf(itemID));
    			WorkspaceItem workspaceItem = WorkspaceItem.createByItem(context, item);
    			fixMetadata(workspaceItem, item);
    			if(firstWorkspaceItem == null)
    			{
    			    firstWorkspaceItem = workspaceItem;
    			}    		
        	}
        	context.commit();
        	redirectURL = request.getContextPath()+"/submit?workspaceID="+firstWorkspaceItem.getID();
    	}
    	else {
    	    redirectURL = request.getContextPath()+"/submissions";
    	}
    	
    	// redirect to submission
        final HttpServletResponse httpResponse = (HttpServletResponse) objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
        httpResponse.sendRedirect(redirectURL);
    
        return null;
    }
    
    /**
     * Fixes metadata of the newly created workspace item based on existing item
     * I.e. appends distinguishing mark to the title (date), 
     * clears dc.identifier.uri, adds note about the base item  
     * 
     * @param workspaceItem New workspace item
     * @param baseItem The base item for this workspace item 
     * @throws IOException 
     * @throws AuthorizeException 
     * @throws SQLException 
     */
    private void fixMetadata(WorkspaceItem workspaceItem, Item baseItem) throws SQLException, AuthorizeException, IOException 
    {
        Item item = workspaceItem.getItem();
        
        // add distinguishing mark (date) to the title
        Metadatum[] md = item.getMetadata("dc", "title", Item.ANY, Item.ANY);
        item.clearMetadata("dc", "title", Item.ANY, Item.ANY);
        for (int n = 0; n < md.length; n++)
        {                  
            if (n == 0) {
                md[n].value = String.format("%s (%s)", md[n].value, IOUtils.today_string());                
            }
            item.addMetadata(md[n].schema, md[n].element, md[n].qualifier, md[n].language,
                    md[n].value);
        } 
        
        // clear dc.identifier.uri - will be created upon installation to the repository
        item.clearMetadata("dc", "identifier", "uri", Item.ANY);
        
        // add note about the base item         
        String note = String.format("This item was created as a new version of '%s' (%s)", baseItem.getName(), baseItem.getHandle());        
        item.addMetadata("local", "submission", "note", Item.ANY, note);
        
        item.update();        
        workspaceItem.update();    
    }
    
}
