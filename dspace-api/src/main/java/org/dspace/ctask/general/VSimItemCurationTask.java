/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

 /*
TODO: refactor this script to follow this example:
https://github.com/DSpace/DSpace/blob/ea642d6c9289d96b37b5de3bb7a4863ec48eaa9c/dspace-api/src/main/java/org/dspace/ctask/general/ProfileFormats.java
*/

package org.dspace.ctask.general;

import java.util.List;
import org.apache.commons.lang.StringUtils;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Collection;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Distributive;

import org.apache.log4j.Logger;

import org.dspace.services.factory.DSpaceServicesFactory;

import org.dspace.content.MetadataValue;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;

import java.sql.SQLException;
import java.io.IOException;

/**
 * VSimItemCurationTask is a task that copies important vsim.relation metadata from a project master to an item
 *
 * @author hardyoyo
 */
@Distributive
public class VSimItemCurationTask extends AbstractCurationTask
{
/** log4j category */
    private static final Logger log = Logger.getLogger(VSimProjectCurationTask.class);

    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected int status = Curator.CURATE_UNSET;
    protected String result = null;

    /**
     * Perform the curation task upon passed DSO
     *
     * @param dso the DSpace object
     * @throws IOException if IO error
     * @throws SQLException if SQL error
     */

    @Override
    public int perform(DSpaceObject dso) throws IOException
    {
         distribute(dso);
         return Curator.CURATE_SUCCESS;
    }

    @Override
    protected void performItem(Item item) throws IOException
    {

    int status = Curator.CURATE_SKIP;

    // read some configuration settings
    //reference: ConfigurationService info: https://wiki.duraspace.org/display/DSPACE/DSpace+Spring+Services+Tutorial#DSpaceSpringServicesTutorial-DSpaceConfigurationService
    String projectMasterCollectionHandle = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("vsim.project.master.collection.handle");

    // if the projectMasterCollectionHandle value isn't set, use a default
    if (StringUtils.isEmpty(projectMasterCollectionHandle))
      {
        projectMasterCollectionHandle = "20.500.11930/1015"; // <-- that better be a collection object on that handle
      }

    vsimItem:
          try {

          DSpaceObject projectMastersDSO = handleService.resolveToObject(Curator.curationContext(), projectMasterCollectionHandle);
          Collection projectMastersCollection = (Collection) projectMastersDSO;

          // grab the handle for this item, we'll need it later
          String itemId = item.getHandle();

          // IF THIS ITEM IS A PROJECT MASTER, *STOP*!! OTHERWISE, CONTINUE...
          if (itemService.isIn(item, projectMastersCollection)) {
              break vsimItem;
          }

              log.info("VSimItemCurationTask: processing item at handle: " + itemId);

              // TODO: find the corresponding project master item for all items in this collection
              // first, grab the collection object for this item
              List<Collection> thisItemCollection = itemService.getCollectionsNotLinked(Curator.curationContext(), item);

              // then grab the vsim.relation.projectMaster metadata

              // assume that the first collection returned above is the one we want (there should only be one)
              // then copy the collection links from the projectMaster item to this item
              List<MetadataValue> mvProjectMaster = collectionService.getMetadata(thisItemCollection.get(0), "vsim", "relation", "projectMaster", Item.ANY);

              String projectMasterHandle = mvProjectMaster.get(0).getValue();

              log.info("VSimItemCurationTask: found the corresponding projectMaster handle: " + projectMasterHandle);

              // get the collection links from the project master item
              DSpaceObject projectMasterDSO = handleService.resolveToObject(Curator.curationContext(), projectMasterHandle);
              Item projectMasterItem = (Item) projectMasterDSO;

              List<MetadataValue> mvVsimMasterRelationModels = itemService.getMetadata(projectMasterItem, "vsim", "relation", "models", Item.ANY);
              List<MetadataValue> mvVsimMasterRelationArchives = itemService.getMetadata(projectMasterItem, "vsim", "relation", "archives", Item.ANY);
              List<MetadataValue> mvVsimMasterRelationSubmissions = itemService.getMetadata(projectMasterItem, "vsim", "relation", "submissions", Item.ANY);


              // set the relation values to the projectMaster values gathered above
              log.info("VSimItemCurationTask:  - adding vsim.relation.models: " + mvVsimMasterRelationModels.get(0).getValue());
              itemService.addMetadata(Curator.curationContext(), item, "vsim", "relation", "models", Item.ANY, mvVsimMasterRelationModels.get(0).getValue());

              log.info("VSimItemCurationTask:  - adding vsim.relation.archives: " + mvVsimMasterRelationArchives.get(0).getValue());
              itemService.addMetadata(Curator.curationContext(), item, "vsim", "relation", "archives", Item.ANY, mvVsimMasterRelationArchives.get(0).getValue());

              log.info("VSimItemCurationTask:  - adding vsim.relation.submissions: " + mvVsimMasterRelationSubmissions.get(0).getValue());
              itemService.addMetadata(Curator.curationContext(), item, "vsim", "relation", "submissions", Item.ANY, mvVsimMasterRelationSubmissions.get(0).getValue());

              // update the itemService to write the values we just set
              log.info("VSimItemCurationTask: writing changes to item at handle: " + itemId);
              itemService.update(Curator.curationContext(), item);

              // set the success flag and add a line to the result report
              // KEEP THIS AT THE END OF THE SCRIPT

              status = Curator.CURATE_SUCCESS;
              result = "VSim standard item " + itemId + " updated with metatdata from project master " + projectMasterHandle;

            // catch any exceptions
            } catch (AuthorizeException authE) {
        		log.error("caught exception: " + authE);
        		status = Curator.CURATE_FAIL;
           	} catch (SQLException sqlE) {
        		log.error("caught exception: " + sqlE);
           	}

              setResult(result);
              report(result);

    }

}
