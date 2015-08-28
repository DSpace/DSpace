/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.collection;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.service.HarvestedCollectionService;


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
        private static final Message T_options_curate = message("xmlui.administrative.collection.general.options_curate");
	private static final Message T_main_head = message("xmlui.administrative.collection.EditCollectionMetadataForm.main_head");
	
	private static final Message T_options_harvest = message("xmlui.administrative.collection.GeneralCollectionHarvestingForm.options_harvest");
	private static final Message T_title = message("xmlui.administrative.collection.GeneralCollectionHarvestingForm.title");
	private static final Message T_trail = message("xmlui.administrative.collection.GeneralCollectionHarvestingForm.trail");
	
	private static final Message T_label_source = message("xmlui.administrative.collection.GeneralCollectionHarvestingForm.label_source");
	private static final Message T_source_normal = message("xmlui.administrative.collection.GeneralCollectionHarvestingForm.source_normal");
	private static final Message T_source_harvested = message("xmlui.administrative.collection.GeneralCollectionHarvestingForm.source_harvested");

	private static final Message T_submit_return = message("xmlui.general.return");
	private static final Message T_submit_save = message("xmlui.administrative.collection.GeneralCollectionHarvestingForm.submit_save");

	protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
	protected HarvestedCollectionService harvestedCollectionService = HarvestServiceFactory.getInstance().getHarvestedCollectionService();

	public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_collection_trail);
        pageMeta.addTrail().addContent(T_trail);
    }
	
	
	public void addBody(Body body) throws WingException, SQLException, AuthorizeException
	{
		UUID collectionID = UUID.fromString(parameters.getParameter("collectionID", null));
		Collection thisCollection = collectionService.find(context, collectionID);

		// This should always be null; it's an error condition for this tranformer to be called when
		// a harvest instance exists for this collection
		HarvestedCollection hc = harvestedCollectionService.find(context, thisCollection);
		String baseURL = contextPath + "/admin/collection?administrative-continue=" + knot.getId();
		
		// DIVISION: main
	    Division main = body.addInteractiveDivision("collection-harvesting-setup",contextPath+"/admin/collection",Division.METHOD_MULTIPART,"primary administrative collection");
	    main.setHead(T_main_head.parameterize(collectionService.getMetadata(thisCollection, "name")));
	    
	    List options = main.addList("options",List.TYPE_SIMPLE,"horizontal");
	    options.addItem().addXref(baseURL+"&submit_metadata",T_options_metadata);
	    options.addItem().addXref(baseURL+"&submit_roles",T_options_roles);
	    options.addItem().addHighlight("bold").addXref(baseURL+"&submit_harvesting",T_options_harvest);
            options.addItem().addXref(baseURL+"&submit_curate",T_options_curate);
	    
	    
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
