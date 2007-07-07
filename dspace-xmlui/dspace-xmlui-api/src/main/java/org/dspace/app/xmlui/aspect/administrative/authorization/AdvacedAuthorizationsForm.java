/*
 * AuthorizationMain.java
 *
 * Version: $Revision: 1.0 $
 *
 * Date: $Date: 2006/07/13 23:20:54 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.xmlui.aspect.administrative.authorization;

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
import org.dspace.content.Collection;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;

/**
 * @author Alexey Maslov
 */
public class AdvacedAuthorizationsForm extends AbstractDSpaceTransformer   
{	
	private static final Message T_title = 
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.title");
	private static final Message T_trail =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.trail");
	private static final Message T_authorize_trail =
		message("xmlui.administrative.authorization.general.authorize_trail");
	
	private static final Message T_main_head =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.main_head");
	private static final Message T_main_para =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.main_para");
	
	private static final Message T_actions_groupSentence =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.actions_groupSentence");
    private static final Message T_actions_actionSentence =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.actions_actionSentence");
    private static final Message T_actions_resourceSentence =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.actions_resourceSentence");
    private static final Message T_actions_collectionSentence =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.actions_collectionSentence");
    
	private static final Message T_actions_policyGroup =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.actions_policyGroup");
    private static final Message T_actions_policyAction =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.actions_policyAction");
    private static final Message T_actions_policyResource =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.actions_policyResource");
    private static final Message T_actions_policyCollections =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.actions_policyCollections");
    
    private static final Message T_submit_add =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.submit_add");
    private static final Message T_submit_remove_all =
		message("xmlui.administrative.authorization.AdvacedAuthorizationsForm.submit_remove_all");
    private static final Message T_submit_return =
		message("xmlui.general.return");
	
	private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");
	
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/authorize", T_authorize_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
		
	public void addBody(Body body) throws WingException, SQLException 
	{
		Division main = body.addInteractiveDivision("advanced-authorization",contextPath+"/admin/authorize",Division.METHOD_POST,"primary administrative authorization");
		main.setHead(T_main_head);
		main.addPara(T_main_para);		
		
		
		List actionsList = main.addList("actions","form");
        
		// For all of the selected groups...
		actionsList.addItem().addContent(T_actions_groupSentence);
        actionsList.addLabel(T_actions_policyGroup);
        Select groupSelect = actionsList.addItem().addSelect("group_id");
        groupSelect.setMultiple(true);
        groupSelect.setSize(15);
        for (Group group : Group.findAll(context, Group.NAME))
       		groupSelect.addOption(false, group.getID(), group.getName());
        
        // Grant the ability to perform the following action...
        actionsList.addItem().addContent(T_actions_actionSentence);
        actionsList.addLabel(T_actions_policyAction);
        Select actionSelect = actionsList.addItem().addSelect("action_id");
        for( int i = 0; i < Constants.actionText.length; i++ )
            actionSelect.addOption(i, Constants.actionText[i]);
        
        // For all following object types...
        actionsList.addItem().addContent(T_actions_resourceSentence);
        actionsList.addLabel(T_actions_policyResource);
        Select resourceSelect = actionsList.addItem().addSelect("resource_id");
        resourceSelect.addOption(true, Constants.ITEM, "item");
        resourceSelect.addOption(false, Constants.BITSTREAM, "bitstream");
        
        // Across the following collections...
        actionsList.addItem().addContent(T_actions_collectionSentence);
        actionsList.addLabel(T_actions_policyCollections);
        Select collectionsSelect = actionsList.addItem().addSelect("collection_id");
        collectionsSelect.setMultiple(true);
        collectionsSelect.setSize(15);
        for (Collection collection : Collection.findAll(context))
        	collectionsSelect.addOption(false, collection.getID(), collection.getMetadata("name"));
        
        
    	Para buttons = main.addPara();
    	buttons.addButton("submit_add").setValue(T_submit_add);
    	buttons.addButton("submit_remove_all").setValue(T_submit_remove_all);
    	buttons.addButton("submit_return").setValue(T_submit_return);
    	
		main.addHidden("administrative-continue").setValue(knot.getId());
   }
}
