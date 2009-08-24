/*
 * ToggleCollectionHarvestingForm.java
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
package org.dspace.app.xmlui.aspect.administrative.collection;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.administrative.FlowContainerUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Option;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Collection;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.OAIHarvester;
import org.dspace.core.ConfigurationManager;


/**
 * The form displayed for collections that do not have any harvesting settings active
 * (i.e. do not have a row in the harvest_instance table)
 * 
 * @author Alexey Maslov
 */
public class ToggleCollectionHarvestingForm extends AbstractDSpaceTransformer   
{
	/** Language Strings */
	private static final Message T_dspace_home =
        message("xmlui.general.dspace_home");
	
	private static final Message T_collection_trail = message("xmlui.administrative.collection.general.collection_trail");
	private static final Message T_options_metadata = message("xmlui.administrative.collection.general.options_metadata");	
	private static final Message T_options_roles = message("xmlui.administrative.collection.general.options_roles");
	private static final Message T_main_head = message("xmlui.administrative.collection.EditCollectionMetadataForm.main_head");
	
	private static final Message T_options_harvest = message("xmlui.administrative.collection.GeneralCollectionHarvestingForm.options_harvest");
	private static final Message T_title = message("xmlui.administrative.collection.GeneralCollectionHarvestingForm.title");
	private static final Message T_trail = message("xmlui.administrative.collection.GeneralCollectionHarvestingForm.trail");
	
	private static final Message T_label_source = message("xmlui.administrative.collection.GeneralCollectionHarvestingForm.label_source");
	private static final Message T_source_normal = message("xmlui.administrative.collection.GeneralCollectionHarvestingForm.source_normal");
	private static final Message T_source_harvested = message("xmlui.administrative.collection.GeneralCollectionHarvestingForm.source_harvested");

	private static final Message T_submit_return = message("xmlui.general.return");
	private static final Message T_submit_save = message("xmlui.administrative.collection.GeneralCollectionHarvestingForm.submit_save");
	
	
	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_collection_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
		int collectionID = parameters.getParameterAsInteger("collectionID", -1);
		Collection thisCollection = Collection.find(context, collectionID);

		// This should always be null; it's an error condition for this tranformer to be called when
		// a harvest instance exists for this collection
		HarvestedCollection hc = HarvestedCollection.find(context, collectionID);
		String baseURL = contextPath + "/admin/collection?administrative-continue=" + knot.getId();
		
		// DIVISION: main
	    Division main = body.addInteractiveDivision("collection-harvesting-setup",contextPath+"/admin/collection",Division.METHOD_MULTIPART,"primary administrative collection");
	    main.setHead(T_main_head.parameterize(thisCollection.getMetadata("name")));   
	    
	    List options = main.addList("options",List.TYPE_SIMPLE,"horizontal");
	    options.addItem().addXref(baseURL+"&submit_metadata",T_options_metadata);
	    options.addItem().addXref(baseURL+"&submit_roles",T_options_roles);
	    options.addItem().addHighlight("bold").addXref(baseURL+"&submit_harvesting",T_options_harvest);
	    
	    
	    // The top-level, all-setting, countent source radio button
	    List harvestSource = main.addList("harvestSource", "form");
	    
	    harvestSource.addLabel(T_label_source);
	    Radio source = harvestSource.addItem().addRadio("source");
    	source.addOption(hc == null, "source_normal", T_source_normal);
    	source.addOption(hc != null, "source_harvested", T_source_harvested);
   
		Para buttonList = main.addPara();
	    buttonList.addButton("submit_save").setValue(T_submit_save);
	    buttonList.addButton("submit_return").setValue(T_submit_return);
	    
    	main.addHidden("administrative-continue").setValue(knot.getId());
    }
	
}
