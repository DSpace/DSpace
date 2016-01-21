/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.controlpanel;

import java.sql.SQLException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.harvest.HarvestScheduler;
import org.dspace.harvest.HarvestedCollection;
import org.dspace.harvest.factory.HarvestServiceFactory;
import org.dspace.harvest.service.HarvestedCollectionService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Control panel tab that controls the OAI harvester.
 * Based on the original ControlPanel class by Jay Paz and Scott Phillips
 * @author LINDAT/CLARIN dev team (http://lindat.cz)
 **/
public class ControlPanelHarvestingTab extends AbstractControlPanelTab
{

    private static final Message T_harvest_scheduler_head = message("xmlui.administrative.ControlPanel.harvest_scheduler_head");

    private static final Message T_harvest_label_status = message("xmlui.administrative.ControlPanel.harvest_label_status");

    private static final Message T_harvest_status_refresh = message("xmlui.administrative.ControlPanel.harvest_status_refresh");

    private static final Message T_harvest_label_actions = message("xmlui.administrative.ControlPanel.harvest_label_actions");

    private static final Message T_harvest_submit_start = message("xmlui.administrative.ControlPanel.harvest_submit_start");

    private static final Message T_harvest_submit_reset = message("xmlui.administrative.ControlPanel.harvest_submit_reset");

    private static final Message T_harvest_submit_resume = message("xmlui.administrative.ControlPanel.harvest_submit_resume");

    private static final Message T_harvest_submit_pause = message("xmlui.administrative.ControlPanel.harvest_submit_pause");

    private static final Message T_harvest_submit_stop = message("xmlui.administrative.ControlPanel.harvest_submit_stop");

    private static final Message T_harvest_label_collections = message("xmlui.administrative.ControlPanel.harvest_label_collections");

    private static final Message T_harvest_label_active = message("xmlui.administrative.ControlPanel.harvest_label_active");

    private static final Message T_harvest_label_queued = message("xmlui.administrative.ControlPanel.harvest_label_queued");

    private static final Message T_harvest_label_oai_errors = message("xmlui.administrative.ControlPanel.harvest_label_oai_errors");

    private static final Message T_harvest_label_internal_errors = message("xmlui.administrative.ControlPanel.harvest_label_internal_errors");

    private static final Message T_harvest_head_generator_settings = message("xmlui.administrative.ControlPanel.harvest_head_generator_settings");

    private static final Message T_harvest_label_oai_url = message("xmlui.administrative.ControlPanel.harvest_label_oai_url");

    private static final Message T_harvest_label_oai_source = message("xmlui.administrative.ControlPanel.harvest_label_oai_source");

    private static final Message T_harvest_head_harvester_settings = message("xmlui.administrative.ControlPanel.harvest_head_harvester_settings");

    protected HarvestedCollectionService harvestedCollectionService = HarvestServiceFactory.getInstance().getHarvestedCollectionService();

    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Override
    public void addBody(Map objectModel, Division div) throws WingException,
            SQLException
    {
        List harvesterControls = div.addList("oai-harvester-controls",
                List.TYPE_FORM);
        harvesterControls.setHead(T_harvest_scheduler_head);
        harvesterControls.addLabel(T_harvest_label_status);
        Item status = harvesterControls.addItem();
        status.addContent(HarvestScheduler.getStatus());
        status.addXref(this.web_link, T_harvest_status_refresh);

        harvesterControls.addLabel(T_harvest_label_actions);
        Item actionsItem = harvesterControls.addItem();
        if (HarvestScheduler
                .hasStatus(HarvestScheduler.HARVESTER_STATUS_STOPPED))
        {
            actionsItem.addButton("submit_harvest_start").setValue(
                    T_harvest_submit_start);
            actionsItem.addButton("submit_harvest_reset").setValue(
                    T_harvest_submit_reset);
        }
        if (HarvestScheduler
                .hasStatus(HarvestScheduler.HARVESTER_STATUS_PAUSED))
        {
            actionsItem.addButton("submit_harvest_resume").setValue(
                    T_harvest_submit_resume);
        }
        if (HarvestScheduler
                .hasStatus(HarvestScheduler.HARVESTER_STATUS_RUNNING)
                || HarvestScheduler
                        .hasStatus(HarvestScheduler.HARVESTER_STATUS_SLEEPING))
        {
            actionsItem.addButton("submit_harvest_pause").setValue(
                    T_harvest_submit_pause);
        }
        if (!HarvestScheduler
                .hasStatus(HarvestScheduler.HARVESTER_STATUS_STOPPED))
        {
            actionsItem.addButton("submit_harvest_stop").setValue(
                    T_harvest_submit_stop);
        }

        // Can be retrieved via
        // "{context-path}/admin/collection?collectionID={id}"
        String baseURL = contextPath + "/admin/collection?collectionID=";

        harvesterControls.addLabel(T_harvest_label_collections);
        Item allCollectionsItem = harvesterControls.addItem();
        java.util.List<HarvestedCollection> allCollections = harvestedCollectionService.findAll(context);
        for (HarvestedCollection oaiCollection : allCollections)
        {
            allCollectionsItem.addXref(baseURL + oaiCollection.getCollection().getID(),
                    oaiCollection.getCollection().getName());
        }
        harvesterControls.addLabel(T_harvest_label_active);
        Item busyCollectionsItem = harvesterControls.addItem();
        java.util.List<HarvestedCollection> busyCollections = harvestedCollectionService
                .findByStatus(context, HarvestedCollection.STATUS_BUSY);
        for (HarvestedCollection busyCollection : busyCollections)
        {
            busyCollectionsItem.addXref(baseURL + busyCollection.getCollection().getID(),
                    busyCollection.getCollection().getName());
        }
        harvesterControls.addLabel(T_harvest_label_queued);
        Item queuedCollectionsItem = harvesterControls.addItem();
        java.util.List<HarvestedCollection> queuedCollections = harvestedCollectionService
                .findByStatus(context, HarvestedCollection.STATUS_QUEUED);
        for (HarvestedCollection queuedCollection : queuedCollections)
        {
            queuedCollectionsItem.addXref(baseURL + queuedCollection.getCollection().getID(),
                    queuedCollection.getCollection().getName());
        }
        harvesterControls.addLabel(T_harvest_label_oai_errors);
        Item oaiErrorsItem = harvesterControls.addItem();
        java.util.List<HarvestedCollection> oaiErrors = harvestedCollectionService.findByStatus(
                context, HarvestedCollection.STATUS_OAI_ERROR);
        for (HarvestedCollection oaiError : oaiErrors)
        {
            oaiErrorsItem.addXref(baseURL + oaiError.getCollection().getID(), oaiError.getCollection().getName());
        }
        harvesterControls.addLabel(T_harvest_label_internal_errors);
        Item internalErrorsItem = harvesterControls.addItem();
        java.util.List<HarvestedCollection> internalErrors = harvestedCollectionService
                .findByStatus(context, HarvestedCollection.STATUS_UNKNOWN_ERROR);
        for (HarvestedCollection internalError : internalErrors)
        {
            internalErrorsItem.addXref(baseURL + internalError.getCollection().getID(),
                    internalError.getCollection().getName());
        }

        // OAI Generator settings
        List generatorSettings = div.addList("oai-generator-settings");
        generatorSettings.setHead(T_harvest_head_generator_settings);

        generatorSettings.addLabel(T_harvest_label_oai_url);
        String oaiUrl = configurationService.getProperty("oai.url");
        if (!StringUtils.isEmpty(oaiUrl))
        {
            generatorSettings.addItemXref(oaiUrl, oaiUrl);
        }

        generatorSettings.addLabel(T_harvest_label_oai_source);
        String oaiAuthoritativeSource = configurationService.getProperty("oai.ore.authoritative.source");
        if (!StringUtils.isEmpty(oaiAuthoritativeSource))
        {
            generatorSettings.addItem(oaiAuthoritativeSource);
        }
        else
        {
            generatorSettings.addItem("oai");
        }

        // OAI Harvester settings (just iterate over all the values that start
        // with "harvester")
        List harvesterSettings = div.addList("oai-harvester-settings");
        harvesterSettings.setHead(T_harvest_head_harvester_settings);

        java.util.List<String> harvesterKeys = configurationService.getPropertyKeys("harvester");
        for(String key : harvesterKeys)
        {
                harvesterSettings.addLabel(key);
                harvesterSettings.addItem(configurationService.getProperty(key)
                        + " ");
        }
    }

}
