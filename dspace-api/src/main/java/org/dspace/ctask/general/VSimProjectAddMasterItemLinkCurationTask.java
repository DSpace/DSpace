/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
 * VSimProjectAddMasterItemLinkCurationTask is a task that adds a link back to the project master item to the collections created by the ProjectMasterInit script
 *
 * @author hardyoyo
 */
@Distributive
public class VSimProjectAddMasterItemLinkCurationTask extends AbstractCurationTask
{
/** log4j category */
    private static final Logger log = Logger.getLogger(VSimProjectCurationTask.class);

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
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


    // If this dso is an ITEM, proceed
    vsimInit:

          try {

          DSpaceObject projectMastersDSO = handleService.resolveToObject(Curator.curationContext(), projectMasterCollectionHandle);
          Collection projectMastersCollection = (Collection) projectMastersDSO;

          // *ONLY* KEEP GOING IF THIS ITEM IS A PROJECT MASTER, OTHERWISE *STOP*!!
          if (!itemService.isIn(item, projectMastersCollection)) {
              break vsimInit;
          }

              // Get All requried MetadataValues, all are returned as lists, use .get(0).getValue() to return the first value, like strings,
              // use the usual list stuff to manage multiple values

              // get the handle to this master item, we'll need it
              String itemId = item.getHandle();
              log.info("VSimProjectAddMasterItemLinkCurationTask: processing master item at handle: " + itemId);


              // the following are used to find the containers to which we need to add the itemId
              List<MetadataValue> mvVsimRelationModels = itemService.getMetadata(item, "vsim", "relation", "models", Item.ANY);
              List<MetadataValue> mvVsimRelationArchives = itemService.getMetadata(item, "vsim", "relation", "archives", Item.ANY);
              List<MetadataValue> mvVsimRelationSubmissions = itemService.getMetadata(item, "vsim", "relation", "submissions", Item.ANY);

              // grab each container object using the handles above
              // -- the HandleService has resolveToObject(Context context, String handle) which goes from handle to dso
              // -- NOTE: this is a generic dso, not a typed dso (i.e. not a community or collection object)
              // -- just look at the code starting around line 103, there's an example there
              DSpaceObject projectCollModelsDSO = handleService.resolveToObject(Curator.curationContext(), mvVsimRelationModels.get(0).getValue());
              Collection projectCollModels = (Collection) projectCollModelsDSO;
              DSpaceObject projectCollArchivesDSO = handleService.resolveToObject(Curator.curationContext(), mvVsimRelationArchives.get(0).getValue());
              Collection projectCollArchives = (Collection) projectCollArchivesDSO;
              DSpaceObject projectCollSubmissionsDSO = handleService.resolveToObject(Curator.curationContext(), mvVsimRelationSubmissions.get(0).getValue());
              Collection projectCollSubmissions = (Collection) projectCollSubmissionsDSO;

              // set the link back to the project master item for each container object we grabbed above
              log.info("VSimProjectAddMasterItemLinkCurationTask:  - adding vsim.relation.projectMaster to projectCollModels at handle: " + projectCollModels.getHandle());
              collectionService.addMetadata(Curator.curationContext(), projectCollModels, "vsim", "relation", "projectMaster", null, itemId);
              log.info("VSimProjectAddMasterItemLinkCurationTask:  - adding vsim.relation.projectMaster to projectCollArchives at handle: " + projectCollArchives.getHandle());
              collectionService.addMetadata(Curator.curationContext(), projectCollArchives, "vsim", "relation", "projectMaster", null, itemId);
              log.info("VSimProjectAddMasterItemLinkCurationTask:  - adding vsim.relation.projectMaster to projectCollSubmissions at handle: " + projectCollSubmissions.getHandle());
              collectionService.addMetadata(Curator.curationContext(), projectCollSubmissions, "vsim", "relation", "projectMaster", null, itemId);

              // now write all that metadata with a set of updates
              log.info("VSimProjectAddMasterItemLinkCurationTask: Writing changes to all three project collections for master item at handle: " + itemId);
              collectionService.update(Curator.curationContext(), projectCollModels);
              collectionService.update(Curator.curationContext(), projectCollArchives);
              collectionService.update(Curator.curationContext(), projectCollSubmissions);

              // set the success flag and add a line to the result report
              // KEEP THIS AT THE END OF THE SCRIPT

              status = Curator.CURATE_SUCCESS;
              result = "VSim Project Master item links intialized based on " + itemId;

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
