/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.submission.submit;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.dspace.app.xmlui.aspect.submission.AbstractSubmissionStep;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Hidden;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.handle.HandleManager;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.xml.sax.SAXException;

/**
 * Allow the user to select a collection they wish to submit an item to, this
 * step is sort-of but not officialy part of the item submission processes.
 * Normaly a user will have selected a collection to submit too by going to the
 * collection's page, but if that was invalid or the user came directly from the
 * mydspace page then this step is given.
 *
 * @author Scott Phillips
 * @author Tim Donohue (updated for Configurable Submission)
 * modified for LINDAT/CLARIN
 * @author Michal Josífko (updated for Community selection)
 */
public class SelectCollectionStep extends AbstractSubmissionStep
{

    /** Language Strings */
    protected static final Message T_head = message("xmlui.Submission.submit.SelectCollection.head");

    protected static final Message T_select_collection_head = message("xmlui.Submission.submit.SelectCollection.select_collection_head");

    protected static final Message T_select_collection_help = message("xmlui.Submission.submit.SelectCollection.select_collection_help");

    protected static final Message T_collection = message("xmlui.Submission.submit.SelectCollection.collection");

    protected static final Message T_collection_help = message("xmlui.Submission.submit.SelectCollection.collection_help");

    protected static final Message T_collection_default = message("xmlui.Submission.submit.SelectCollection.collection_default");

    protected static final Message T_select_community_head = message("xmlui.Submission.submit.SelectCollection.select_community_head");

    protected static final Message T_select_community_help = message("xmlui.Submission.submit.SelectCollection.select_community_help");

    protected static final Message T_community = message("xmlui.Submission.submit.SelectCollection.community");

    protected static final Message T_single_collection_help = message("xmlui.Submission.submit.SelectCollection.single_collection_help");

    protected static final Message T_submit_next = message("xmlui.Submission.general.submission.next");

    private class CommunityComparator implements Comparator<Community>
    {
        @Override
        public int compare(Community o1, Community o2)
        {
            return o1.getName().compareTo(o2.getName());
        }
    }

    private class CollectionComparator implements Comparator<Collection>
    {
        @Override
        public int compare(Collection o1, Collection o2)
        {
            return o1.getName().compareTo(o2.getName());
        }
    }

    public SelectCollectionStep()
    {
        this.requireHandle = true;
    }

    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, SQLException, IOException, AuthorizeException
    {
        super.addPageMeta(pageMeta);
        pageMeta.addMetadata("title").addContent(T_submission_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrail().addContent(T_submission_trail);
        pageMeta.addMetadata("include-library", "select-collection");
    }

    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Collection[] collections; // List of possible collections.
        String actionURL = contextPath + "/submit/" + knot.getId()
                + ".continue";
        DSpaceObject dso = HandleManager.resolveToObject(context, handle);

        if (dso instanceof Community)
        {
            collections = Collection.findAuthorized(context, ((Community) dso),
                    Constants.ADD);
        }
        else if (dso instanceof Collection)
        {
            collections = new Collection[] { (Collection) dso };
        }
        else
        {
            collections = Collection.findAuthorized(context, null,
                    Constants.ADD);
        }

        // Create hierarchical structure of authorized communities and
        // collections
        Map<Community, java.util.List<Collection>> communitiesMap = createCommunitiesMap(collections);

        // Transform the hierarchical structure of authorized communities and
        // collections
        // to JASON, so that we can use it later in client side javascript
        JSONObject jsonModel = createJSONModel(communitiesMap);

        // Basic form with a drop down list of all the collections
        // you can submit too.
        Division div = body.addInteractiveDivision("select-collection",
                actionURL, Division.METHOD_POST, "primary submission");

        // Set the head of the page
        div.setHead(T_submission_head);

        // First store the hierarchical structure of authorized communities and
        // collections in a hidden input, so that it is available on the client side
        Hidden modelHidden = div.addHidden("communities-model");
        modelHidden.setValue(jsonModel.toJSONString());

        if (collections != null && collections.length > 0)
        {
            Division communitiesAndCollectionsDiv = div.addDivision("select-community-and-collection-div");

            if (collections.length == 1)
            {
                Division collectionsDiv = communitiesAndCollectionsDiv.addDivision("single-collection");

                Hidden collection = collectionsDiv.addHidden("handle");
                collection.setValue(collections[0].getHandle());

                collectionsDiv.addPara(null, "alert alert-info").addContent(
                        T_single_collection_help);

                Button submit = communitiesAndCollectionsDiv.addDivision("control-group","control-group").addPara().addButton("submit");
                submit.setValue(T_submit_next);
            }
            else
            {
                // Communities

                Division communitiesDiv = communitiesAndCollectionsDiv.addDivision("select-community-div","hidden");

                communitiesDiv.setHead(T_select_community_head);

                communitiesDiv.addPara(null, "alert alert-info").addContent(
                        T_select_community_help);

                communitiesDiv.addDivision("communities-list");

                Division collectionsDiv = communitiesAndCollectionsDiv.addDivision("select-collection-div");

                collectionsDiv.setHead(T_select_collection_head);

                collectionsDiv.addPara(null, "alert alert-info").addContent(
                        T_select_collection_help);

                Select collectionSelect = collectionsDiv.addPara().addSelect(
                        "handle");
                collectionSelect.setLabel(T_collection);
                collectionSelect.setHelp(T_collection_help);

                collectionSelect.addOption("", T_collection_default);

                JSONArray communitiesJa = (JSONArray) jsonModel
                        .get("communities");

                for (int i = 0; i < communitiesJa.size(); i++)
                {
                    JSONObject communityJo = (JSONObject) communitiesJa.get(i);
                    JSONArray collectionsJa = (JSONArray) communityJo
                            .get("collections");

                    for (int j = 0; j < collectionsJa.size(); j++)
                    {
                        JSONObject collectionJo = (JSONObject) collectionsJa
                                .get(j);

                        collectionSelect.addOption(
                                (String) collectionJo.get("handle"),
                                (String) collectionJo.get("name"));
                    }
                }

                Button submit = communitiesAndCollectionsDiv.addDivision("control-group","control-group").addPara().addButton("submit");
                submit.setValue(T_submit_next);
            }

        }
        else
        {
            div.addDivision("notice", "")
                    .addDivision("failure", "alert alert-error")
                    .addPara(
                            "No Collections found. Please contact the administrator!");
        }
    }

    /**
     * Transforms hierarchical structure of communities and collections into
     * JSON array of JASON objects
     *
     * @param model
     *            The hierarchical structure of communities and collections as
     *            created by createModel
     * @return The JSON array of JSON objects of communities and collections
     */
    private JSONObject createJSONModel(
            Map<Community, java.util.List<Collection>> communitiesMap)
    {
        JSONObject modelJo = new JSONObject();

        JSONArray communitiesJa = new JSONArray();

        Community[] communities = communitiesMap.keySet().toArray(
                new Community[communitiesMap.size()]);
        Arrays.sort(communities, new CommunityComparator());

        for (Community community : communities)
        {
            String communityLogoURL = "";
            if (community.getLogo() != null)
            {
                communityLogoURL = contextPath + "/bitstream/id/"
                        + community.getLogo().getID() + "/community_logo_"
                        + community.getID();
            }
            JSONObject communityJo = new JSONObject();
            communityJo.put("id", community.getID());
            communityJo.put("name", community.getName());
            communityJo.put("handle", community.getHandle());
            communityJo.put("logoURL", communityLogoURL);
            communityJo.put("shortDescription",
                    community.getMetadata("short_description"));

            Collection[] communityCollections = communitiesMap
                    .get(community)
                    .toArray(
                            new Collection[communitiesMap.get(community).size()]);
            Arrays.sort(communityCollections, new CollectionComparator());

            JSONArray collectionsJa = new JSONArray();
            for (Collection collection : communityCollections)
            {
                String collectionLogoURL = "";
                if (collection.getLogo() != null)
                {
                    collectionLogoURL = contextPath + "/bitstream/id/"
                            + collection.getLogo().getID()
                            + "/collection_logo_" + collection.getID();
                }
                JSONObject collectionJo = new JSONObject();
                collectionJo.put("id", collection.getID());
                collectionJo.put("name", collection.getName());
                collectionJo.put("handle", collection.getHandle());
                collectionJo.put("logoURL", collectionLogoURL);
                collectionJo.put("shortDescription",
                        collection.getMetadata("short_description"));
                collectionsJa.add(collectionJo);
            }

            communityJo.put("collections", collectionsJa);
            communitiesJa.add(communityJo);
        }

        modelJo.put("communities", communitiesJa);

        return modelJo;
    }

    /**
     * Creates the hierarchical structure of communities and collections
     *
     * @param collections
     *            The list of collections
     * @return The hierarchical structure of communities and collections
     * @throws SQLException
     */

    private Map<Community, java.util.List<Collection>> createCommunitiesMap(
            Collection[] collections) throws SQLException
    {
        Map<Community, java.util.List<Collection>> model = new HashMap<Community, java.util.List<Collection>>();

        for (Collection collection : collections)
        {
            Community[] communities = collection.getCommunities();
            for (Community community : communities)
            {
                if (model.containsKey(community))
                {
                    java.util.List<Collection> communityCollections = (java.util.List<Collection>) model
                            .get(community);
                    communityCollections.add(collection);
                }
                else
                {
                    java.util.List<Collection> communityCollections = new ArrayList<Collection>();
                    communityCollections.add(collection);
                    model.put(community, communityCollections);
                }
            }
        }

        return model;
    }

    /**
     * Each submission step must define its own information to be reviewed
     * during the final Review/Verify Step in the submission process.
     * <P>
     * The information to review should be tacked onto the passed in List
     * object.
     * <P>
     * NOTE: To remain consistent across all Steps, you should first add a
     * sub-List object (with this step's name as the heading), by using a call
     * to reviewList.addList(). This sublist is the list you return from this
     * method!
     *
     * @param reviewList
     *            The List to which all reviewable information should be added
     * @return The new sub-List object created by this step, which contains all
     *         the reviewable information. If this step has nothing to review,
     *         then return null!
     */
    public List addReviewSection(List reviewList) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        // Currently, the selecting a Collection is not reviewable in DSpace,
        // since it cannot be changed easily after creating the item
        return null;
    }

    /**
     * Recycle
     */
    public void recycle()
    {
        this.handle = null;
        super.recycle();
    }
}
