/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.collection;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.UUID;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.OAIHarvester;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.service.HarvestedCollectionService;


/**
 * Presents the user (in this case an administrator over the collection) with the
 * form to edit that collection's metadata, logo, and item template.
 * @author Alexey Maslov
 */
public class SetupCollectionHarvestingForm extends AbstractDSpaceTransformer   
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
		
	private static final Message T_main_settings_head = message("xmlui.administrative.collection.SetupCollectionHarvestingForm.main_settings_head");
	private static final Message T_options_head = message("xmlui.administrative.collection.SetupCollectionHarvestingForm.options_head");
	
	private static final Message T_label_oai_provider = message("xmlui.administrative.collection.SetupCollectionHarvestingForm.label_oai_provider");
	private static final Message T_label_setid = message("xmlui.administrative.collection.SetupCollectionHarvestingForm.label_setid");
	private static final Message T_label_metadata_format = message("xmlui.administrative.collection.SetupCollectionHarvestingForm.label_metadata_format");
	
	private static final Message T_help_oaiurl = message("xmlui.administrative.collection.SetupCollectionHarvestingForm.help_oaiurl");
	private static final Message T_error_oaiurl = message("xmlui.administrative.collection.SetupCollectionHarvestingForm.error_oaiurl");
	private static final Message T_help_oaisetid = message("xmlui.administrative.collection.SetupCollectionHarvestingForm.help_oaisetid");
	private static final Message T_error_oaisetid = message("xmlui.administrative.collection.SetupCollectionHarvestingForm.error_oaisetid");
	
	private static final Message T_label_harvest_level = message("xmlui.administrative.collection.SetupCollectionHarvestingForm.label_harvest_level");
	
	private static final Message T_option_md_only = message("xmlui.administrative.collection.SetupCollectionHarvestingForm.option_md_only");
	private static final Message T_option_md_and_ref = message("xmlui.administrative.collection.SetupCollectionHarvestingForm.option_md_and_ref");
	private static final Message T_option_md_and_bs = message("xmlui.administrative.collection.SetupCollectionHarvestingForm.option_md_and_bs");

	private static final Message T_submit_test = message("xmlui.administrative.collection.SetupCollectionHarvestingForm.submit_test"); 

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
		Request request = ObjectModelHelper.getRequest(objectModel);
		
		HarvestedCollection hc = harvestedCollectionService.find(context, thisCollection);
		String baseURL = contextPath + "/admin/collection?administrative-continue=" + knot.getId();
		
		String errorString = parameters.getParameter("errors",null);
		String[] errors = errorString.split(",");
		HashMap<String,String> errorMap = new HashMap<String,String>();
		for (String error : errors) {
			//System.out.println(errorString);
			String[] errorPieces = error.split(":",2);
			
			if (errorPieces.length > 1)
            {
                errorMap.put(errorPieces[0], errorPieces[1]);
            }
			else
            {
                errorMap.put(errorPieces[0], errorPieces[0]);
            }
		}
		
		
		String oaiProviderValue;
		String oaiSetIdValue;
		String metadataFormatValue;
		int harvestLevelValue;
				
		if (hc != null && request.getParameter("submit_test") == null) {
			oaiProviderValue = hc.getOaiSource();
			oaiSetIdValue = hc.getOaiSetId();
			metadataFormatValue = hc.getHarvestMetadataConfig();
			harvestLevelValue = hc.getHarvestType();			
		}
		else {
			oaiProviderValue = parameters.getParameter("oaiProviderValue", "");
			oaiSetIdValue = parameters.getParameter("oaiSetAll", "");
            if(!"all".equals(oaiSetIdValue))
            {
                oaiSetIdValue = parameters.getParameter("oaiSetIdValue", null);
            }
			metadataFormatValue = parameters.getParameter("metadataFormatValue", "");
			String harvestLevelString = parameters.getParameter("harvestLevelValue","0");
			if (harvestLevelString.length() == 0)
            {
                harvestLevelValue = 0;
            }
			else
            {
                harvestLevelValue = Integer.parseInt(harvestLevelString);
            }
		}
		
		// DIVISION: main
	    Division main = body.addInteractiveDivision("collection-harvesting-setup",contextPath+"/admin/collection",Division.METHOD_MULTIPART,"primary administrative collection");
	    main.setHead(T_main_head.parameterize(thisCollection.getName()));
	    
	    List options = main.addList("options",List.TYPE_SIMPLE,"horizontal");
	    options.addItem().addXref(baseURL+"&submit_metadata",T_options_metadata);
	    options.addItem().addXref(baseURL+"&submit_roles",T_options_roles);
	    options.addItem().addHighlight("bold").addXref(baseURL+"&submit_harvesting",T_options_harvest);
            options.addItem().addXref(baseURL+"&submit_curate",T_options_curate);
	    
	    
	    // The top-level, all-setting, countent source radio button
	    List harvestSource = main.addList("harvestSource", "form");
	    
	    harvestSource.addLabel(T_label_source);
	    Radio source = harvestSource.addItem().addRadio("source");
    	source.addOption(hc == null || harvestLevelValue == -1, "source_normal", T_source_normal);
    	source.addOption(hc != null, "source_harvested", T_source_harvested);
	    
	    List settings = main.addList("harvestSettings", "form");
	    settings.setHead(T_main_settings_head);
	    
	    settings.addLabel(T_label_oai_provider);
	    Text oaiProvider = settings.addItem().addText("oai_provider");
	    oaiProvider.setSize(40);
	    oaiProvider.setValue(oaiProviderValue);
	    oaiProvider.setHelp(T_help_oaiurl);
	    
	    if (errorMap.containsKey(OAIHarvester.OAI_ADDRESS_ERROR)) {
	    	oaiProvider.addError(errorMap.get(OAIHarvester.OAI_ADDRESS_ERROR));
	    }
	    if (errorMap.containsKey("oai_provider")) {
	    	oaiProvider.addError(T_error_oaiurl);
	    	//oaiProvider.addError("You must provide a set id of the target collection.");
	    }
	    
	    settings.addLabel(T_label_setid);
        Composite oaiSetComp = settings.addItem().addComposite("oai-set-comp");
        Radio oaiSetSettingRadio = oaiSetComp.addRadio("oai-set-setting");
        oaiSetSettingRadio.addOption("all".equals(oaiSetIdValue) || oaiSetIdValue == null, "all", "All sets");
        oaiSetSettingRadio.addOption(!"all".equals(oaiSetIdValue) && oaiSetIdValue != null, "specific", "Specific sets");

        Text oaiSetId = oaiSetComp.addText("oai_setid");
	    oaiSetId.setSize(40);
        if(!"all".equals(oaiSetIdValue) && oaiSetIdValue != null)
        {
            oaiSetId.setValue(oaiSetIdValue);
        }
	    oaiSetId.setHelp(T_help_oaisetid);
	    if (errorMap.containsKey(OAIHarvester.OAI_SET_ERROR)) {
	    	oaiSetId.addError(errorMap.get(OAIHarvester.OAI_SET_ERROR));
	    }
	    if (errorMap.containsKey("oai_setid")) {
	    	oaiSetId.addError(T_error_oaisetid);
	    }
	    
	    settings.addLabel(T_label_metadata_format);
	    Select metadataFormat = settings.addItem().addSelect("metadata_format");
	    if (errorMap.containsKey(OAIHarvester.OAI_ORE_ERROR)) {	    	
	    	metadataFormat.addError(errorMap.get(OAIHarvester.OAI_ORE_ERROR));
	    }
		if (errorMap.containsKey(OAIHarvester.OAI_DMD_ERROR)) {
	    	metadataFormat.addError(errorMap.get(OAIHarvester.OAI_DMD_ERROR));
	    }
	
	
	    // Add an entry for each instance of ingestion crosswalks configured for harvesting 
        String metaString = "oai.harvester.metadataformats.";
        Enumeration pe = Collections.enumeration(DSpaceServicesFactory.getInstance().getConfigurationService().getPropertyKeys("oai"));
        while (pe.hasMoreElements())
        {
            String key = (String)pe.nextElement();
            if (key.startsWith(metaString)) {
            	String metadataString = (DSpaceServicesFactory.getInstance().getConfigurationService().getProperty(key));
            	String metadataKey = key.substring(metaString.length());
            	String displayName;

            	if (metadataString.indexOf(',') != -1)
                {
                    displayName = metadataString.substring(metadataString.indexOf(',') + 1);
                }
            	else
                {
                    displayName = metadataKey + "(" + metadataString + ")";
                }
            	
            	metadataFormat.addOption(metadataKey.equalsIgnoreCase(metadataFormatValue), metadataKey, displayName);
            }
        }
        
        
        settings.addLabel();
	    Item harvestButtons = settings.addItem();
	    harvestButtons.addButton("submit_test").setValue(T_submit_test);
	    
	    // Various non-critical harvesting options 
	    //Division optionsDiv = main.addDivision("collection-harvesting-options","secondary");
	    //optionsDiv.setHead(T_options_head);
	    
	    List harvestOptions = main.addList("harvestOptions", "form");
	    harvestOptions.setHead(T_options_head);
	    
	    harvestOptions.addLabel(T_label_harvest_level);
	    Radio harvestLevel = harvestOptions.addItem().addRadio("harvest_level");
	    harvestLevel.addOption(harvestLevelValue == 1, 1, T_option_md_only);
	    harvestLevel.addOption(harvestLevelValue == 2, 2, T_option_md_and_ref);
	    harvestLevel.addOption(harvestLevelValue != 1 && harvestLevelValue != 2, 3, T_option_md_and_bs);
	    
		Para buttonList = main.addPara();
	    buttonList.addButton("submit_save").setValue(T_submit_save);
	    buttonList.addButton("submit_return").setValue(T_submit_return);
	    
    	main.addHidden("administrative-continue").setValue(knot.getId());
    }
	
}
