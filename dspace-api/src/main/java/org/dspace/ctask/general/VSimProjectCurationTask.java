/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

 /*
THE PLAN:
[x] get the project metadata from the Project Master Item
what fields are we going to use for the links we will be checking below?
we talked about using dc.relation.ispartof and dc.relation.requires, but that's not expressive enough for what we need
we need four new fields: vsim.relation.community, vsim.relation.models, vsim.relation.archives, vsim.relation.submissions
ALL/some of these links *can* be added to the dc fields, too, but that's not really important to us right now.
We need to add them to fields we can use to also recall the values in this script
*/

// TODO: make this whole thing Idempotent (see below for notes, around line 109)

package org.dspace.ctask.general;

import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;

import org.apache.commons.io.FilenameUtils;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.MetadataSchema;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.core.Constants;
import org.dspace.curate.Curator;

import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;

import org.apache.log4j.Logger;

import org.dspace.services.factory.DSpaceServicesFactory;

import java.io.File;

import org.dspace.content.MetadataValue;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;

import java.io.InputStream;
import java.io.FileInputStream;

import java.sql.SQLException;
import java.io.IOException;

/**
 * VSimProjectCurationTask is a task that initializes a VSim Project structure, based on the metadata entered in a VSim Project Master item
 *
 * @author hardyoyo
 */
public class VSimProjectCurationTask extends AbstractCurationTask
{
/** log4j category */
    private static final Logger log = Logger.getLogger(VSimProjectCurationTask.class);

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected int status = Curator.CURATE_UNSET;
    protected String result = null;

    private static final int digitsPerLevel = 2;
    private static final int directoryLevels = 3;

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

    int status = Curator.CURATE_SKIP;

    // read some configuration settings
    //reference: ConfigurationService info: https://wiki.duraspace.org/display/DSPACE/DSpace+Spring+Services+Tutorial#DSpaceSpringServicesTutorial-DSpaceConfigurationService
    String projectMasterCollectionHandle = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("vsim.project.master.collection.handle");
    String assetstoreDir = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("assetstore.dir");

    // if the projectMasterCollectionHandle value isn't set, use a default
    if (StringUtils.isEmpty(projectMasterCollectionHandle))
      {
        projectMasterCollectionHandle = "20.500.11930/1015"; // <-- that better be a collection object on that handle
      }


    // If this dso is an ITEM, proceed
    vsimInit:
		if (dso.getType() == Constants.ITEM)
        {
          try {

          DSpaceObject projectMastersDSO = handleService.resolveToObject(Curator.curationContext(), projectMasterCollectionHandle);
          Collection projectMastersCollection = (Collection) projectMastersDSO;

          Item item = (Item)dso;

          // *ONLY* KEEP GOING IF THIS ITEM IS A PROJECT MASTER, OTHERWISE *STOP*!!
          if (!itemService.isIn(item, projectMastersCollection)) {
              break vsimInit;
          }




              // Get All requried MetadataValues, all are returned as lists, use .get(0).getValue() to return the first value, like strings,
              // use the usual list stuff to manage multiple values


              String itemId = item.getHandle();
              List<MetadataValue> mvDcTitle = itemService.getMetadata(item, "dc", "title", Item.ANY, Item.ANY);
              List<MetadataValue> mvDcDescriptionAbstract = itemService.getMetadata(item, "dc", "description", "abstract", Item.ANY);
              List<MetadataValue> mvDcDescription = itemService.getMetadata(item, "dc", "description", Item.ANY, Item.ANY);
              List<MetadataValue> mvDcRelation = itemService.getMetadata(item, "dc", "relation", Item.ANY, Item.ANY);
              List<MetadataValue> mvDcDateCreated = itemService.getMetadata(item, "dc", "date", "created", Item.ANY);
              List<MetadataValue> mvDcDescriptionSponsorship = itemService.getMetadata(item, "dc", "description", "sponsorship", Item.ANY);
              List<MetadataValue> mvDcCoverageSpatial = itemService.getMetadata(item, "dc", "coverage", "spatial", Item.ANY);
              List<MetadataValue> mvDcCoverageTemporal = itemService.getMetadata(item, "dc", "coverage", "temporal", Item.ANY);
              List<MetadataValue> mvDcContributorAuthor = itemService.getMetadata(item, "dc", "", "contributor", "author", Item.ANY);
              List<MetadataValue> mvDcContributor = itemService.getMetadata(item, "dc", "", "contributor", Item.ANY, Item.ANY);
              List<MetadataValue> mvDcContributorAdvisor = itemService.getMetadata(item, "dc", "", "contributor", "advisor", Item.ANY);
              List<MetadataValue> mvDcDescriptionURI = itemService.getMetadata(item, "dc", "description", "uri", Item.ANY);
              List<MetadataValue> mvDcDateAvailable = itemService.getMetadata(item, "dc", "date", "available", Item.ANY);
              List<MetadataValue> mvDcRightsHolder = itemService.getMetadata(item, "dc", "rights", "holder", Item.ANY);
              List<MetadataValue> mvDcDateCopyright = itemService.getMetadata(item, "dc", "date", "copyright", Item.ANY);
              List<MetadataValue> mvDcRights = itemService.getMetadata(item, "dc", "rights", Item.ANY, Item.ANY);
              List<MetadataValue> mvDcDateIssued = itemService.getMetadata(item, "dc", "date", "issued", Item.ANY);
              List<MetadataValue> mvVsimResearchObjective = itemService.getMetadata(item, "vsim", "research", "objective", Item.ANY);
              List<MetadataValue> mvVsimAcknowledgements = itemService.getMetadata(item, "vsim", "acknowledgements", Item.ANY, Item.ANY);
              List<MetadataValue> mvVsimBibliography = itemService.getMetadata(item, "vsim", "bibliography", Item.ANY, Item.ANY);
              List<MetadataValue> mvVsimKeywords = itemService.getMetadata(item, "vsim", "keywords", Item.ANY, Item.ANY);
              List<MetadataValue> mvVsimContributorDetails = itemService.getMetadata(item, "vsim", "contributor", "details", Item.ANY);
              List<MetadataValue> mvVsimNews = itemService.getMetadata(item, "vsim", "news", Item.ANY, Item.ANY);

              // the following are used as links and to give this Curation Task idempotency
              List<MetadataValue> mvVsimRelationCommunity = itemService.getMetadata(item, "vsim", "relation", "community", Item.ANY);
              List<MetadataValue> mvVsimRelationModels = itemService.getMetadata(item, "vsim", "relation", "models", Item.ANY);
              List<MetadataValue> mvVsimRelationArchives = itemService.getMetadata(item, "vsim", "relation", "archives", Item.ANY);
              List<MetadataValue> mvVsimRelationSubmissions = itemService.getMetadata(item, "vsim", "relation", "submissions", Item.ANY);

              // TODO: MAKE THIS IDEMPOTENT!!!
              // TODO: grab the projectCommunity in the same way I do on line 80, for projectMasterCollection, using HandleService, feeding it mvVsimRelationCommunity
              // TODO: grab the mvVsimRelationCommunity, confirm it's not null/empty
              // TODO: confirm that the dso for this handle exists and is a community object
              // -- disabled for now, hjp, the below is not fully cooked, it's just a start, do all of the above
              // if ( CollectionUtils.isNotEmpty(mvVsimRelationCommunity) ){
                // create a new top level community for this project
                Community projectCommunity = communityService.create(null, Curator.curationContext());
              //} else {
                // grab the linked projectCommunity by its handle
                //Community projectCommunity = communityService.create(null, Curator.curationContext());
              //}

              // set what metadata we can on this community; this should be safe to re-run over existing communities... in that case the project master
              // will simply overwrite whatever metadata is set on the existing community
              // code is borrowed from the example here: https://github.com/DSpace/DSpace/blob/ea642d6c9289d96b37b5de3bb7a4863ec48eaa9c/dspace-api/src/test/java/org/dspace/content/packager/PackageUtilsTest.java#L79-L80

              // set the title (dc.title)
              if ( CollectionUtils.isNotEmpty(mvDcTitle) ) {
                communityService.addMetadata(Curator.curationContext(), projectCommunity, MetadataSchema.DC_SCHEMA, "title", null, null, mvDcTitle.get(0).getValue());
              }
              // set the description (dc.description)
              // TODO: this will likely require a combination of the following metatdata fields, with a bit of formatting added: dc.description, vsim.acknowledgements, vsim.research.objective
              // TODO: support markup for any of these fields, will probably need to use a markdown library, like es.nitaur.markdown/txtmark or sirthius/pegdown
              if ( CollectionUtils.isNotEmpty(mvDcDescription) ) {
                communityService.addMetadata(Curator.curationContext(), projectCommunity, MetadataSchema.DC_SCHEMA, "description", null, null, mvDcDescription.get(0).getValue());
              }

              // set the short_description (dc.description.abstract)
              if ( CollectionUtils.isNotEmpty(mvDcDescriptionAbstract) ) {
                communityService.addMetadata(Curator.curationContext(), projectCommunity, MetadataSchema.DC_SCHEMA, "description", "abstract", null, mvDcDescriptionAbstract.get(0).getValue());
              }

              // TODO: set the sidebar_text (dc.description.tableofcontents) we'll have to interpolate this from other values, requires discussion and/or thought
              // probably it'll be a link to the project master? leave blank for now, oh, or maybe the bibliography?

              // set the copyright_text (dc.rights)
              if ( CollectionUtils.isNotEmpty(mvDcRights) ) {
                communityService.addMetadata(Curator.curationContext(), projectCommunity, MetadataSchema.DC_SCHEMA, "rights", null, null, mvDcRights.get(0).getValue());
              }

              // finish the update of the projectCommunity metadata (AKA: write!)
              communityService.update(Curator.curationContext(), projectCommunity);

              // snag the projectCommunityhandle, we'll need it
              String projectCommunityHandle = projectCommunity.getHandle();

              // set the logo for the community, if possible, use projectCommunity.setLogo(Bitstream logo)

              // We need a DSpace group object for AuthZ purposes, for ContentCreators, to keep handy
              Group ContentCreatorsGroupObj = groupService.findByName(Curator.curationContext(), "Content Creators");
              Group AnonymousGroupObj = groupService.findByName(Curator.curationContext(), "Anonymous");

              // create the Administrators group we need
              Group projectCommunityAdminGroupObj = communityService.createAdministrators(Curator.curationContext(), projectCommunity);

              // add the ContentCreatorsGroupObj to the group we just created
              groupService.addMember(Curator.curationContext(), projectCommunityAdminGroupObj, ContentCreatorsGroupObj);
              groupService.update(Curator.curationContext(), projectCommunityAdminGroupObj);

              // add a link to the top level community as metadata for this project master Item (use vsim.relation.community)
              itemService.addMetadata(Curator.curationContext(), item, "vsim", "relation", "community", Item.ANY, projectCommunityHandle);

              // if there is no link to the project models collection in this item's metadata, create a models collection in this project's TLC and add a link to the models collection as metadata for this project master item
              Collection projectCollModels = collectionService.create(Curator.curationContext(), projectCommunity);
              if ( CollectionUtils.isNotEmpty(mvDcTitle) ) {
                collectionService.addMetadata(Curator.curationContext(), projectCollModels, MetadataSchema.DC_SCHEMA, "title", null, null, mvDcTitle.get(0).getValue() + ": VSim Files");
                collectionService.addMetadata(Curator.curationContext(), projectCollModels, MetadataSchema.DC_SCHEMA, "description", null, null, "Files specific to VSim, including 3D models, narratives, and embedded resources (e.g., .vsim, .nar, .ere). For the " + mvDcTitle.get(0).getValue() + " project.");
                collectionService.addMetadata(Curator.curationContext(), projectCollModels, MetadataSchema.DC_SCHEMA, "description", "abstract", null, "Files specific to VSim, including 3D models, narratives, and embedded resources (e.g., .vsim, .nar, .ere). For the " + mvDcTitle.get(0).getValue() + " project.");
                collectionService.addMetadata(Curator.curationContext(), projectCollModels, MetadataSchema.DC_SCHEMA, "description", "tableofcontents", null, "Collection sidebar for Models: " + mvDcTitle.get(0).getValue());
                collectionService.addMetadata(Curator.curationContext(), projectCollModels, MetadataSchema.DC_SCHEMA, "rights", null, null, mvDcRights.get(0).getValue());
              }

              // create the Administrators and Submitters groups we need
              Group projectCollModelsAdminGroupObj = collectionService.createAdministrators(Curator.curationContext(), projectCollModels);
              Group projectCollModelsSubmittersGroupObj = collectionService.createSubmitters(Curator.curationContext(), projectCollModels);

              // add the ContentCreatorsGroupObj to the groups we just created
              groupService.addMember(Curator.curationContext(), projectCollModelsAdminGroupObj, ContentCreatorsGroupObj);
              groupService.addMember(Curator.curationContext(), projectCollModelsSubmittersGroupObj, ContentCreatorsGroupObj);
              groupService.update(Curator.curationContext(), projectCollModelsAdminGroupObj);
              groupService.update(Curator.curationContext(), projectCollModelsSubmittersGroupObj);

              // write this collection
              collectionService.update(Curator.curationContext(), projectCollModels);
              // add a link to this collection to the item
              itemService.addMetadata(Curator.curationContext(), item, "vsim", "relation", "models", Item.ANY, projectCollModels.getHandle() );
              itemService.update(Curator.curationContext(), item);

              // if there is no link to the project archives collection in this item's metadata, create an archives collection in this project's TLC and add a link to the archives collection as metadata for this project master item
              Collection projectCollArchives = collectionService.create(Curator.curationContext(), projectCommunity);
              if ( CollectionUtils.isNotEmpty(mvDcTitle) ) {
                collectionService.addMetadata(Curator.curationContext(), projectCollArchives, MetadataSchema.DC_SCHEMA, "title", null, null, mvDcTitle.get(0).getValue() + ": Project Archive");
                collectionService.addMetadata(Curator.curationContext(), projectCollArchives, MetadataSchema.DC_SCHEMA, "description", null, null, "Multimedia files related to the project that provide context for the 3D model (e.g., .pdf, .jpg, .ppt, .csv, etc.). For the " + mvDcTitle.get(0).getValue() + " project.");
                collectionService.addMetadata(Curator.curationContext(), projectCollArchives, MetadataSchema.DC_SCHEMA, "description", "abstract", null, "Multimedia files related to the project that provide context for the 3D model (e.g., .pdf, .jpg, .ppt, .csv, etc.). For the " + mvDcTitle.get(0).getValue() + " project.");
                collectionService.addMetadata(Curator.curationContext(), projectCollArchives, MetadataSchema.DC_SCHEMA, "description", "tableofcontents", null, "Collection sidebar for Archives: " + mvDcTitle.get(0).getValue());
                collectionService.addMetadata(Curator.curationContext(), projectCollArchives, MetadataSchema.DC_SCHEMA, "rights", null, null, mvDcRights.get(0).getValue());
              }

              // create the Administrators and Submitters groups we need
              Group projectCollArchivesAdminGroupObj = collectionService.createAdministrators(Curator.curationContext(), projectCollArchives);
              Group projectCollArchivesSubmittersGroupObj = collectionService.createSubmitters(Curator.curationContext(), projectCollArchives);

              // add the ContentCreatorsGroupObj to the groups we just created
              groupService.addMember(Curator.curationContext(), projectCollArchivesAdminGroupObj, ContentCreatorsGroupObj);
              groupService.addMember(Curator.curationContext(), projectCollArchivesSubmittersGroupObj, ContentCreatorsGroupObj);
              groupService.update(Curator.curationContext(), projectCollArchivesAdminGroupObj);
              groupService.update(Curator.curationContext(), projectCollArchivesSubmittersGroupObj);

              // write this collection
              collectionService.update(Curator.curationContext(), projectCollArchives);
              // add a link to this collection to the item
              itemService.addMetadata(Curator.curationContext(), item, "vsim", "relation", "archives", Item.ANY, projectCollArchives.getHandle() );
              itemService.update(Curator.curationContext(), item);

              // if there is no link to the project submissions collection in this item's metadata, create a submissions collection in this project's TLC and add a link to the submissions collection as metadata for this project master item
              Collection projectCollSubmissions = collectionService.create(Curator.curationContext(), projectCommunity);
              if ( CollectionUtils.isNotEmpty(mvDcTitle) ) {
                collectionService.addMetadata(Curator.curationContext(), projectCollSubmissions, MetadataSchema.DC_SCHEMA, "title", null, null, mvDcTitle.get(0).getValue() + ": User Submissions");
                collectionService.addMetadata(Curator.curationContext(), projectCollSubmissions, MetadataSchema.DC_SCHEMA, "description", null, null, "Multimedia files submitted by users for sharing within the educational and research communities (e.g., narratives created for use in the classroom, or imagery and texts related to the 3D model that are in the public domain). For the " + mvDcTitle.get(0).getValue() + " project.");
                collectionService.addMetadata(Curator.curationContext(), projectCollSubmissions, MetadataSchema.DC_SCHEMA, "description", "abstract", null, "Multimedia files submitted by users for sharing within the educational and research communities (e.g., narratives created for use in the classroom, or imagery and texts related to the 3D model that are in the public domain). For the " + mvDcTitle.get(0).getValue() + " project.");
                collectionService.addMetadata(Curator.curationContext(), projectCollSubmissions, MetadataSchema.DC_SCHEMA, "description", "tableofcontents", null, "Collection sidebar for Submissions: " + mvDcTitle.get(0).getValue());
                collectionService.addMetadata(Curator.curationContext(), projectCollSubmissions, MetadataSchema.DC_SCHEMA, "rights", null, null, mvDcRights.get(0).getValue());
              }

              // create the Administrators and Submitters groups we need
              Group projectCollSubmissionsAdminGroupObj = collectionService.createAdministrators(Curator.curationContext(), projectCollSubmissions);
              Group projectCollSubmissionsSubmittersGroupObj = collectionService.createSubmitters(Curator.curationContext(), projectCollSubmissions);

              // add the ContentCreatorsGroupObj to the admin group we just created, and the AnonymousGroupObj to the submitters group we just created
              groupService.addMember(Curator.curationContext(), projectCollSubmissionsAdminGroupObj, ContentCreatorsGroupObj);
              groupService.addMember(Curator.curationContext(), projectCollSubmissionsSubmittersGroupObj, AnonymousGroupObj);
              groupService.update(Curator.curationContext(), projectCollSubmissionsAdminGroupObj);
              groupService.update(Curator.curationContext(), projectCollSubmissionsSubmittersGroupObj);

              // write this collection
              collectionService.update(Curator.curationContext(), projectCollSubmissions);
              // add a link to this collection to the item
              itemService.addMetadata(Curator.curationContext(), item, "vsim", "relation", "submissions", Item.ANY, projectCollSubmissions.getHandle() );
              itemService.update(Curator.curationContext(), item);

              // update the projectCommunity (just to be safe)
              communityService.update(Curator.curationContext(), projectCommunity);


              // be sure to write the changed item metadata (just in case we've missed something along the way)
              itemService.update(Curator.curationContext(), item);

              // BEGIN: ADD LOGO to Community and Collections ///////////////////////////////////////////////////////////////////////////////
              // Get the list of Bitstreams for this Project Master item
              List<Bitstream> projectMasterBitstreams = itemService.getNonInternalBitstreams(Curator.curationContext(), item);

              // Now do something useful with these bitstreams:
              // Loop through each bistream, find the logo, get the path, send that path to the addlogo method for all generated communities and collections
              // NOTE: this bakes in the assumption that this bitstream lives on the same server, and thus has a file path that this curation script can reference, which is not guaranteed
              // by DSpace. Still, good enough for now, as this assumption works for our current implementation.

              for (Bitstream bitstream : projectMasterBitstreams) {
                  String fileNameWithOutExt = FilenameUtils.removeExtension(bitstream.getName());
                  if ("logo".equals(fileNameWithOutExt)) {
                      // infer the bitstream path by splitting the bitstream internal ID and adding it to the assetstore path
                      // this is kind of dumb, but it's how the bitstore migration code does it
                      String sInternalId = bitstream.getInternalId();
                      String sIntermediatePath = null;
                      sIntermediatePath = getIntermediatePath(sInternalId);
                      StringBuilder bufFilename = new StringBuilder();
                      bufFilename.append(assetstoreDir);
                      bufFilename.append(File.separator);
                      bufFilename.append(sIntermediatePath);
                      bufFilename.append(sInternalId);

                      // make an InputStream for this logo
                      InputStream projectLogoFileStream4Community = new FileInputStream(bufFilename.toString());

                      // load the logo bitstream into the community and collections created above
                      communityService.setLogo(Curator.curationContext(), projectCommunity, projectLogoFileStream4Community);

                      // update of the projectCommunity (AKA: write!)
                      communityService.update(Curator.curationContext(), projectCommunity);

                      // re-open the logoFileStream, the setLogo method closes it
                      InputStream projectLogoFileStream4projectCollModels = new FileInputStream(bufFilename.toString());
                      // load the logo bitstream into collectionService for projectCollModels
                      collectionService.setLogo(Curator.curationContext(), projectCollModels, projectLogoFileStream4projectCollModels);

                      // re-open the logoFileStream, the setLogo method closes it
                      InputStream projectLogoFileStream4projectCollArchives = new FileInputStream(bufFilename.toString());
                      // load the logo bitstream into collectionService for projectCollArchives
                      collectionService.setLogo(Curator.curationContext(), projectCollArchives, projectLogoFileStream4projectCollArchives);

                      // re-open the logoFileStream, the setLogo method closes it
                      InputStream projectLogoFileStream4projectCollSubmissions = new FileInputStream(bufFilename.toString());
                      // load the logo bitstream into collectionService for projectCollSubmissions
                      collectionService.setLogo(Curator.curationContext(), projectCollSubmissions, projectLogoFileStream4projectCollSubmissions);

                      // update each collection via collectionService for projectCollModels, projectCollArchives, projectCollSubmissions
                      collectionService.update(Curator.curationContext(), projectCollModels);
                      collectionService.update(Curator.curationContext(), projectCollArchives);
                      collectionService.update(Curator.curationContext(), projectCollSubmissions);

                  }
              }

              // END: ADD LOGO to Community and Collections ///////////////////////////////////////////////////////////////////////////////




              // set the success flag and add a line to the result report
              // KEEP THIS AT THE END OF THE SCRIPT

              status = Curator.CURATE_SUCCESS;
              result = "VSim Project intialized based on " + itemId + " | title: " + mvDcTitle.get(0).getValue() + " | Project Community: " + projectCommunityHandle;

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

        return status;
    }

    /**
     * Return the intermediate path derived from the internal_id. This method
     * splits the id into groups which become subdirectories.
     *
     * @param iInternalId
     *            The internal_id
     * @return The path based on the id without leading or trailing separators
     */
    protected String getIntermediatePath(String iInternalId) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < directoryLevels; i++) {
            int digits = i * digitsPerLevel;
            if (i > 0) {
                buf.append(File.separator);
            }
            buf.append(iInternalId.substring(digits, digits
                    + digitsPerLevel));
        }
        buf.append(File.separator);
        return buf.toString();
    }

}
