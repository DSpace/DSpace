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
// TODO:

package org.dspace.ctask.general;

import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.MetadataSchema;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.core.Constants;
import org.dspace.curate.Curator;
import org.apache.log4j.Logger;

import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;

import java.sql.SQLException;
import java.io.IOException;

public class VSimProjectCurationTask extends AbstractCurationTask
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(VSimProjectCurationTask.class);

    protected CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    protected CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    protected int status = Curator.CURATE_UNSET;
    protected String result = null;

    // TODO: We need a few DSpace group objects for AuthZ purposes: Admins, ContentCreators, Anonymous; keep them handy

    /**
     * Perform the curation task upon passed DSO
     *
     * @param dso the DSpace object
     * @throws IOException if IO error
     */

    @Override
    public int perform(DSpaceObject dso) throws IOException
    {

    int status = Curator.CURATE_SKIP;

		if (dso.getType() == Constants.ITEM)
        {
          try {

              // Get All requried MetadataValues, all are returned as lists, use .get(0).getValue() to return the first value, like strings,
              // use the usual list stuff to manage multiple values
              Item item = (Item)dso;
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

              // TODO: MAKE THIS IDEMPOTENT!!! unfortunately, there does not seem to be an easy way to grab a collection by its handle (see line 112 below)
              // if there is no link to the project community in this projectMaster item's metadata -- disabled for now, hjp
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

              // TODO: set the logo for the community, if possible, use projectCommunity.setLogo(Bitstream logo)
              // TODO: before we can do that, we need to find the Bitstream logo on this Project master item
              // TODO: set the admins for this community, use setAdmins(Group admins) <- we need a Group object that matches the Content Creators group


              // TODO add a link to the top level community as metadata for this project master Item (use vsim.relation.community)
              itemService.addMetadata(Curator.curationContext(), item, "vsim", "relation", "community", Item.ANY, projectCommunityHandle);

              // if there is no link to the project models collection in this item's metadata, create a models collection in this project's TLC and add a link to the models collection as metadata for this project master item
              Collection projectCollModels = collectionService.create(Curator.curationContext(), projectCommunity);
              if ( CollectionUtils.isNotEmpty(mvDcTitle) ) {
                collectionService.addMetadata(Curator.curationContext(), projectCollModels, MetadataSchema.DC_SCHEMA, "title", null, null, "Models: " + mvDcTitle.get(0).getValue());
                collectionService.addMetadata(Curator.curationContext(), projectCollModels, MetadataSchema.DC_SCHEMA, "description", null, null, "Collection description for Models: " + mvDcTitle.get(0).getValue());
                collectionService.addMetadata(Curator.curationContext(), projectCollModels, MetadataSchema.DC_SCHEMA, "description", "abstract", null, "Collection short_description for Models: " + mvDcTitle.get(0).getValue());
                collectionService.addMetadata(Curator.curationContext(), projectCollModels, MetadataSchema.DC_SCHEMA, "description", "tableofcontents", null, "Collection sidebar for Models: " + mvDcTitle.get(0).getValue());
                collectionService.addMetadata(Curator.curationContext(), projectCollModels, MetadataSchema.DC_SCHEMA, "rights", null, null, mvDcRights.get(0).getValue());
              }
              // write this collection
              collectionService.update(Curator.curationContext(), projectCollModels);
              // add a link to this collection to the item
              itemService.addMetadata(Curator.curationContext(), item, "vsim", "relation", "models", Item.ANY, projectCollModels.getHandle() );
              itemService.update(Curator.curationContext(), item);

              // if there is no link to the project archives collection in this item's metadata, create an archives collection in this project's TLC and add a link to the archives collection as metadata for this project master item
              Collection projectCollArchives = collectionService.create(Curator.curationContext(), projectCommunity);
              if ( CollectionUtils.isNotEmpty(mvDcTitle) ) {
                collectionService.addMetadata(Curator.curationContext(), projectCollArchives, MetadataSchema.DC_SCHEMA, "title", null, null, "Archives: " + mvDcTitle.get(0).getValue());
                collectionService.addMetadata(Curator.curationContext(), projectCollArchives, MetadataSchema.DC_SCHEMA, "description", null, null, "Collection description for Archives: " + mvDcTitle.get(0).getValue());
                collectionService.addMetadata(Curator.curationContext(), projectCollArchives, MetadataSchema.DC_SCHEMA, "description", "abstract", null, "Collection short_description for Archives: " + mvDcTitle.get(0).getValue());
                collectionService.addMetadata(Curator.curationContext(), projectCollArchives, MetadataSchema.DC_SCHEMA, "description", "tableofcontents", null, "Collection sidebar for Archives: " + mvDcTitle.get(0).getValue());
                collectionService.addMetadata(Curator.curationContext(), projectCollArchives, MetadataSchema.DC_SCHEMA, "rights", null, null, mvDcRights.get(0).getValue());
              }
              // write this collection
              collectionService.update(Curator.curationContext(), projectCollArchives);
              // add a link to this collection to the item
              itemService.addMetadata(Curator.curationContext(), item, "vsim", "relation", "archives", Item.ANY, projectCollArchives.getHandle() );
              itemService.update(Curator.curationContext(), item);

              // if there is no link to the project submissions collection in this item's metadata, create a submissions collection in this project's TLC and add a link to the submissions collection as metadata for this project master item
              Collection projectCollSubmissions = collectionService.create(Curator.curationContext(), projectCommunity);
              if ( CollectionUtils.isNotEmpty(mvDcTitle) ) {
                collectionService.addMetadata(Curator.curationContext(), projectCollSubmissions, MetadataSchema.DC_SCHEMA, "title", null, null, "Submissions: " + mvDcTitle.get(0).getValue());
                collectionService.addMetadata(Curator.curationContext(), projectCollSubmissions, MetadataSchema.DC_SCHEMA, "description", null, null, "Collection description for Submissions: " + mvDcTitle.get(0).getValue());
                collectionService.addMetadata(Curator.curationContext(), projectCollSubmissions, MetadataSchema.DC_SCHEMA, "description", "abstract", null, "Collection short_description for Submissions: " + mvDcTitle.get(0).getValue());
                collectionService.addMetadata(Curator.curationContext(), projectCollSubmissions, MetadataSchema.DC_SCHEMA, "description", "tableofcontents", null, "Collection sidebar for Submissions: " + mvDcTitle.get(0).getValue());
                collectionService.addMetadata(Curator.curationContext(), projectCollSubmissions, MetadataSchema.DC_SCHEMA, "rights", null, null, mvDcRights.get(0).getValue());
              }
              // write this collection
              collectionService.update(Curator.curationContext(), projectCollSubmissions);
              // add a link to this collection to the item
              itemService.addMetadata(Curator.curationContext(), item, "vsim", "relation", "submissions", Item.ANY, projectCollSubmissions.getHandle() );
              itemService.update(Curator.curationContext(), item);

              // update the projectCommunity (just to be safe)
              communityService.update(Curator.curationContext(), projectCommunity);


              // be sure to write the changed item metadata (just in case we've missed something along the way)
              itemService.update(Curator.curationContext(), item);


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


}
