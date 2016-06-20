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
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.service.HarvestedCollectionService;


/**
 * This is the other form that deals with harvesting. This one comes up when the collection is
 * edited with the harvesting options set and verified. Allows two actions: "import" and "reingest",
 * as well as "change", which takes the user to the other harvesting form. 
 * @author Alexey Maslov
 */
public class EditCollectionHarvestingForm extends AbstractDSpaceTransformer   
{
	/** Language Strings */
	private static final Message T_dspace_home = message("xmlui.general.dspace_home");
	
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
		
	private static final Message T_main_settings_head = message("xmlui.administrative.collection.EditCollectionHarvestingForm.main_settings_head");
	private static final Message T_label_oai_provider = message("xmlui.administrative.collection.EditCollectionHarvestingForm.label_oai_provider");
	private static final Message T_label_setid = message("xmlui.administrative.collection.EditCollectionHarvestingForm.label_setid");
	private static final Message T_label_metadata_format = message("xmlui.administrative.collection.EditCollectionHarvestingForm.label_metadata_format");
	
	private static final Message T_label_harvest_level = message("xmlui.administrative.collection.EditCollectionHarvestingForm.label_harvest_level");
	private static final Message T_label_harvest_result = message("xmlui.administrative.collection.EditCollectionHarvestingForm.label_harvest_result");
	private static final Message T_harvest_result_new = message("xmlui.administrative.collection.EditCollectionHarvestingForm.harvest_result_new");
	
	private static final Message T_label_harvest_status = message("xmlui.administrative.collection.EditCollectionHarvestingForm.label_harvest_status");
	private static final Message T_harvest_status_ready = message("xmlui.administrative.collection.EditCollectionHarvestingForm.harvest_status_ready");
	private static final Message T_harvest_status_busy = message("xmlui.administrative.collection.EditCollectionHarvestingForm.harvest_status_busy");
	private static final Message T_harvest_status_queued = message("xmlui.administrative.collection.EditCollectionHarvestingForm.harvest_status_queued");
	private static final Message T_harvest_status_oai_error = message("xmlui.administrative.collection.EditCollectionHarvestingForm.harvest_status_oai_error");
	private static final Message T_harvest_status_unknown_error = message("xmlui.administrative.collection.EditCollectionHarvestingForm.harvest_status_unknown_error");
	
	private static final Message T_option_md_only = message("xmlui.administrative.collection.EditCollectionHarvestingForm.option_md_only");
	private static final Message T_option_md_and_ref = message("xmlui.administrative.collection.EditCollectionHarvestingForm.option_md_and_ref");
	private static final Message T_option_md_and_bs = message("xmlui.administrative.collection.EditCollectionHarvestingForm.option_md_and_bs");

	private static final Message T_submit_change_settings = message("xmlui.administrative.collection.EditCollectionHarvestingForm.submit_change_settings");
	private static final Message T_submit_import_now = message("xmlui.administrative.collection.EditCollectionHarvestingForm.submit_import_now");
	private static final Message T_submit_reimport_collection = message("xmlui.administrative.collection.EditCollectionHarvestingForm.submit_reimport_collection");
	
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
		HarvestedCollection hc = harvestedCollectionService.find(context, thisCollection);
				
		String baseURL = contextPath + "/admin/collection?administrative-continue=" + knot.getId();
		
		String oaiProviderValue = hc.getOaiSource();
		String oaiSetIdValue = hc.getOaiSetId();
		String metadataFormatValue = hc.getHarvestMetadataConfig();
		int harvestLevelValue = hc.getHarvestType();
		int harvestStatusValue = hc.getHarvestStatus();
					    
		// DIVISION: main
	    Division main = body.addInteractiveDivision("collection-harvesting-edit",contextPath+"/admin/collection",Division.METHOD_MULTIPART,"primary administrative collection");
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
    	source.addOption(false, "source_normal", T_source_normal);      // was hc == null - always false
    	source.addOption(true, "source_harvested", T_source_harvested); // was hc != null - always true
	    
	    List settings = main.addList("harvestSettings", "form");
	    settings.setHead(T_main_settings_head);
	    
	    settings.addLabel(T_label_oai_provider);
	    settings.addItem(oaiProviderValue);
	    
	    settings.addLabel(T_label_setid);
	    settings.addItem(oaiSetIdValue);

	    // The big complex way of getting to our metadata
	    settings.addLabel(T_label_metadata_format);
    
	    String key = "oai.harvester.metadataformats." + metadataFormatValue;
	    String metadataString = (DSpaceServicesFactory.getInstance().getConfigurationService().getProperty(key));

	    String displayName;
    	if (metadataString.indexOf(',') != -1)
        {
            displayName = metadataString.substring(metadataString.indexOf(',') + 1);
        }
    	else
        {
            displayName = metadataFormatValue + "(" + metadataString + ")";
        }
    	    	
    	settings.addItem(displayName);
    	
    	settings.addLabel(T_label_harvest_level);
    	Item harvestLevel = settings.addItem();
    	switch (harvestLevelValue) {
			case HarvestedCollection.TYPE_DMD: harvestLevel.addContent(T_option_md_only); break;
			case HarvestedCollection.TYPE_DMDREF: harvestLevel.addContent(T_option_md_and_ref); break;
    		default: harvestLevel.addContent(T_option_md_and_bs); break;
    	}
	        	    	
        /* Results of the last harvesting cycle */
        if (harvestLevelValue > 0) {
        	settings.addLabel(T_label_harvest_result);
        	Item harvestResult = settings.addItem();
        	if (hc.getHarvestMessage() != null) {
        		harvestResult.addContent(hc.getHarvestMessage() + " on " + hc.getHarvestStartTime());
        	}
        	else {
        		harvestResult.addContent(T_harvest_result_new);
        	}
        }
        
        /* Current status */
        settings.addLabel(T_label_harvest_status);
    	Item harvestStatus = settings.addItem();
        switch(harvestStatusValue) {
	        case HarvestedCollection.STATUS_READY: harvestStatus.addContent(T_harvest_status_ready); break;
	        case HarvestedCollection.STATUS_BUSY: harvestStatus.addContent(T_harvest_status_busy); break;
	        case HarvestedCollection.STATUS_QUEUED: harvestStatus.addContent(T_harvest_status_queued); break;
	        case HarvestedCollection.STATUS_OAI_ERROR: harvestStatus.addContent(T_harvest_status_oai_error); break;
	        case HarvestedCollection.STATUS_UNKNOWN_ERROR: harvestStatus.addContent(T_harvest_status_unknown_error); break;
        }
	    	    
        settings.addLabel();
	    Item harvestButtons = settings.addItem();
	    harvestButtons.addButton("submit_change").setValue(T_submit_change_settings);
	    harvestButtons.addButton("submit_import_now").setValue(T_submit_import_now);
	    harvestButtons.addButton("submit_reimport").setValue(T_submit_reimport_collection);
	      
		Para buttonList = main.addPara();
	    buttonList.addButton("submit_save").setValue(T_submit_save);
	    buttonList.addButton("submit_return").setValue(T_submit_return);
	    
    	main.addHidden("administrative-continue").setValue(knot.getId());
    }
	
}
