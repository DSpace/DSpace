/**
 * $Id: ItemViewer.java 4707 2010-01-19 09:17:47Z mdiggory $
 * $URL: https://scm.dspace.org/svn/repo/modules/dspace-discovery/trunk/block/src/main/java/org/dspace/app/xmlui/aspect/discovery/ItemViewer.java $
 * *************************************************************************
 * Copyright (c) 2002-2009, DuraSpace.  All rights reserved
 * Licensed under the DuraSpace License.
 *
 * A copy of the DuraSpace License has been included in this
 * distribution and is available at: http://scm.dspace.org/svn/repo/licenses/LICENSE.txt
 */

package org.datadryad.dspace.xmlui.aspect.browse;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.DisseminationCrosswalk;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.LogManager;
import org.dspace.core.PluginManager;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.IdentifierNotFoundException;
import org.dspace.identifier.IdentifierNotResolvableException;
import org.dspace.utils.DSpace;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.VersioningService;
import org.dspace.workflow.ClaimedTask;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;
import org.jdom.Element;
import org.jdom.output.XMLOutputter;
import org.xml.sax.SAXException;

/**
 * Display a single item.
 *
 * @author Scott Phillips
 * @author Kevin S. Clarke
 */
public class ItemViewer extends AbstractDSpaceTransformer implements
        CacheableProcessingComponent {
    private static final Logger log = Logger.getLogger(ItemViewer.class);

    /**
     * Language strings
     */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_trail = message("xmlui.ArtifactBrowser.ItemViewer.trail");

    private static final Message T_show_simple = message("xmlui.ArtifactBrowser.ItemViewer.show_simple");

    private static final Message T_show_full = message("xmlui.ArtifactBrowser.ItemViewer.show_full");

    private static final Message T_head_parent_collections = message("xmlui.ArtifactBrowser.ItemViewer.head_parent_collections");

    private static final Message T_withdrawn = message("xmlui.DryadItemSummary.withdrawn");
    private static final Message T_in_workflow = message("xmlui.DryadItemSummary.in_workflow");

    private static final Message T_not_current_version = message("xmlui.DryadItemSummary.notCurrentVersion");
    private static final Message T_most_current_version = message("xmlui.DryadItemSummary.mostCurrentVersion");

    private static final Message T_head_has_part = message("xmlui.ArtifactBrowser.ItemViewer.head_hasPart");
    private static final Message T_head_is_part_of = message("xmlui.ArtifactBrowser.ItemViewer.head_isPartOf");

    private static final Message T_version_in_submission = message("xmlui.ItemViewer.versionInSubmission");
    private static final Message T_go_to_submission_page = message("xmlui.ItemViewer.goToSubmissionPage");
    private static final Message T_version_in_workflow = message("xmlui.ItemViewer.versionInWorkflow");


    private List<Item> dataFiles = new ArrayList<Item>();


    /**
     * Cached validity object
     */
    private SourceValidity validity = null;

    /**
     * XHTML crosswalk instance
     */
    private DisseminationCrosswalk xHTMLHeadCrosswalk = null;

    /**
     * Generate the unique caching key. This key must be unique inside the space
     * of this component.
     */
    public Serializable getKey() {
        try {
            DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

            if (dso == null) return "0"; // no item, something is wrong.

            return HashUtil.hash(dso.getHandle() + "full:"
                    + showFullItem(objectModel));
        } catch (SQLException sqle) {
            // Ignore all errors and just return that the component is not
            // cachable.
            return "0";
        }
    }

    /**
     * Generate the cache validity object.
     * <p/>
     * The validity object will include the item being viewed, along with all
     * bundles & bitstreams.
     */
    public SourceValidity getValidity() {
        DSpaceObject dso = null;

        if (this.validity == null) {
            try {
                dso = HandleUtil.obtainHandle(objectModel);

                DSpaceValidity validity = new DSpaceValidity();
                if (dso instanceof Item) {
                    Item item = (Item) dso;
                    retrieveDataFiles(item);

                    for (Item i : dataFiles) {
                        validity.add(i);
                    }
                }
                validity.add(dso);

                this.validity = validity.complete();
            } catch (Exception e) {
                log.error("Exception: getValidity()", e);
            }

            // add log message that we are viewing the item
            // done here, as the serialization may not occur if the cache is
            // valid
            log.info(LogManager.getHeader(context, "view_item", "handle="
                    + (dso == null ? "" : dso.getHandle())));
        }
        return this.validity;
    }


    /**
     * Add the item's title and trail links to the page's metadata.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException {
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Item)) return;
        Item item = (Item) dso;

        // Set the page title
        String title = getItemTitle(item);

        if (title != null) pageMeta.addMetadata("title").addContent(title);
        else pageMeta.addMetadata("title").addContent(item.getHandle());

        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        HandleUtil.buildHandleTrail(item, pageMeta, contextPath);
        pageMeta.addTrail().addContent(T_trail);

        // Find out whether our theme should be localized
        String localize = ConfigurationManager.getProperty("dryad.localize");

        if (localize != null && localize.equals("true")) {
            pageMeta.addMetadata("dryad", "localize").addContent("true");
        }

        // Data file metadata included on data package items (integrated view)
        for (DCValue metadata : item.getMetadata("dc.relation.haspart")) {
            int skip = 0;
            String id;

            if (metadata.value.startsWith("http://hdl.")) {
                skip = 22;
            } else if (metadata.value.indexOf("/handle/") != -1) {
                skip = metadata.value.indexOf("/handle/") + 8;
            }
            // else DOI, stick with skip == 0

            id = metadata.value.substring(skip); // skip host name

            if (id.startsWith("doi:") || id.startsWith("http://dx.doi.org/")) {
                if (id.startsWith("http://dx.doi.org/")) {
                    id = id.substring("http://dx.doi.org/".length());

                    // service with resolve with or without the "doi:" prepended
                    if (!id.startsWith("doi:")) {
                        id = "doi:" + id;
                    }
                }

                DOIIdentifierProvider doiService = new DSpace().getSingletonService(DOIIdentifierProvider.class);
                Item file = null;
                try {
                    file = (Item) doiService.resolve(context, id, new String[]{});
                } catch (IdentifierNotFoundException e) {
                    // just keep going
                } catch (IdentifierNotResolvableException e) {
                    // just keep going
                }

                if (file != null) {
                    String fileTitle = getItemTitle(file);

                    if (fileTitle != null) {
                        pageMeta.addMetadata("dryad", "fileTitle").addContent(metadata.value + "|" + fileTitle);
                    } else {
                        pageMeta.addMetadata("dryad", "fileTitle").addContent(metadata.value);
                    }
                } else {
                    log.warn("Didn't find a DOI from internal db for: " + id);
                }
            } else {
                Item file = (Item) HandleManager.resolveToObject(context, id);
                String fileTitle = getItemTitle(file);

                if (fileTitle != null) {
                    pageMeta.addMetadata("dryad", "fileTitle").addContent(metadata.value + "|" + fileTitle);
                } else {
                    pageMeta.addMetadata("dryad", "fileTitle").addContent(metadata.value);
                }
            }
        }

        // Data package metadata included on data file items
        for (DCValue metadata : item.getMetadata("dc.relation.ispartof")) {
            int skip = 0;

            if (metadata.value.startsWith("http://hdl.")) {
                skip = 22;
            } else if (metadata.value.indexOf("/handle/") != -1) {
                skip = metadata.value.indexOf("/handle/") + 8;
            } else {
                // if doi, leave as is and we'll process differently below
            }

            String id = metadata.value.substring(skip); // skip host name
            Item pkg = null;

            if (id.startsWith("doi:")) {
                DOIIdentifierProvider doiService = new DSpace().getSingletonService(DOIIdentifierProvider.class);
                try {
                    pkg = (Item) doiService.resolve(context, id, new String[]{});
                } catch (IdentifierNotFoundException e) {
                    // just keep going
                } catch (IdentifierNotResolvableException e) {
                    // just keep going
                }
            } else {
                pkg = (Item) HandleManager.resolveToObject(context, id);
            }

            StringBuilder buffer = new StringBuilder();
            boolean identifierSet = false;
            DCValue[] values;
            String date;

            if (pkg != null) {
                String pkgTitle = getItemTitle(pkg).trim();
                String author;

                for (DCValue pkgMeta : pkg
                        .getMetadata("dc.identifier.citation")) {
                    pageMeta.addMetadata("citation", "article").addContent(
                            pkgMeta.value);
                }

                buffer.append(parseName(pkg
                        .getMetadata("dc.contributor.author")));
                buffer.append(parseName(pkg.getMetadata("dc.creator")));
                buffer.append(parseName(pkg.getMetadata("dc.contributor")));

                author = buffer.toString().trim();
                author = author.endsWith(",") ? author.substring(0, author
                        .length() - 1) : author;

                pageMeta.addMetadata("authors", "package").addContent(
                        author + " ");
                pageMeta.addMetadata("title", "package").addContent(
                        pkgTitle.endsWith(".") ? pkgTitle + " " : pkgTitle
                                + ". ");

                if ((values = pkg.getMetadata("dc.date.issued")).length > 0) {
                    pageMeta.addMetadata("dateIssued", "package").addContent(
                            "(" + values[0].value.substring(0, 4) + ")");
                }

                if ((values = pkg.getMetadata("dc.relation.isreferencedby")).length != 0) {
                    pageMeta.addMetadata("identifier", "article").addContent(
                            values[0].value);
                }

                if ((values = pkg.getMetadata("prism.publicationName")).length != 0) {
                    pageMeta.addMetadata("publicationName").addContent(
                            values[0].value);
                }

                if ((values = pkg.getMetadata("dc.identifier")).length != 0) {
                    for (DCValue value : values) {
                        if (value.value.startsWith("doi:")) {
                            pageMeta.addMetadata("identifier", "package")
                                    .addContent(value.value);
                        }
                    }
                } else if ((values = pkg.getMetadata("dc.identifier.uri")).length != 0) {
                    for (DCValue value : values) {
                        if (value.value.startsWith("doi:")) {
                            pageMeta.addMetadata("identifier", "package")
                                    .addContent(value.value);
                            identifierSet = true;
                        }
                    }

                    if (!identifierSet) {
                        for (DCValue value : values) {
                            if (value.value.startsWith("http://dx.doi.org/")) {
                                pageMeta.addMetadata("identifier", "package")
                                        .addContent(value.value.substring(18));
                                identifierSet = true;
                            }
                        }
                    }

                    if (!identifierSet) {
                        for (DCValue value : values) {
                            if (value.value.startsWith("hdl:")) {
                                pageMeta.addMetadata("identifier", "package")
                                        .addContent(value.value);
                                identifierSet = true;
                            }
                        }
                    }

                    if (!identifierSet) {
                        for (DCValue value : values) {
                            if (value.value
                                    .startsWith("http://hdl.handle.net/")) {
                                pageMeta.addMetadata("identifier", "package")
                                        .addContent(value.value.substring(22));
                            }
                        }
                    }
                }
            }
        }
        /**
         * TODO: We can use the trail here to reference parent Article and/or
         * original search links
         */

        // Metadata for <head> element
        if (xHTMLHeadCrosswalk == null) {
            xHTMLHeadCrosswalk = (DisseminationCrosswalk) PluginManager
                    .getNamedPlugin(DisseminationCrosswalk.class,
                            "XHTML_HEAD_ITEM");
        }

        // Produce <meta> elements for header from crosswalk
        try {
            List l = xHTMLHeadCrosswalk.disseminateList(item);
            StringWriter sw = new StringWriter();

            XMLOutputter xmlo = new XMLOutputter();
            for (int i = 0; i < l.size(); i++) {
                Element e = (Element) l.get(i);
                // FIXME: we unset the Namespace so it's not printed.
                // This is fairly yucky, but means the same crosswalk should
                // work for Manakin as well as the JSP-based UI.
                e.setNamespace(null);
                xmlo.output(e, sw);
            }
            pageMeta.addMetadata("xhtml_head_item").addContent(sw.toString());
        } catch (CrosswalkException ce) {
            // TODO: Is this the right exception class?
            throw new WingException(ce);
        }
    }

    /**
     * Display a single item
     */
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException {

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (!(dso instanceof Item)) return;
        Item item = (Item) dso;

        // Build the item viewer division.
        Division division = body.addDivision("item-view", "primary");
        String title = getItemTitle(item);
        if (title != null) division.setHead(title);
        else division.setHead(item.getHandle());


        // Adding message for withdrawn or workflow item
        addWarningMessage(item, division);


        Para showfullPara = division.addPara(null,
                "item-view-toggle item-view-toggle-top");

        if (showFullItem(objectModel)) {
            String link = contextPath + "/handle/" + item.getHandle();
            showfullPara.addXref(link).addContent(T_show_simple);
        } else {
            String link = contextPath + "/handle/" + item.getHandle()
                    + "?show=full";
            showfullPara.addXref(link).addContent(T_show_full);
        }

        ReferenceSet referenceSet;
        if (showFullItem(objectModel)) {
            referenceSet = division.addReferenceSet("collection-viewer", ReferenceSet.TYPE_DETAIL_VIEW);
        } else {
            referenceSet = division.addReferenceSet("collection-viewer", ReferenceSet.TYPE_SUMMARY_VIEW);
        }

        // Reference the actual Item referenceSet.addReference(item);

        /*
           * reference any isPartOf items to create listing...
           */
        DOIIdentifierProvider dis = new DSpace().getSingletonService(DOIIdentifierProvider.class);
        org.dspace.app.xmlui.wing.element.Reference itemRef = referenceSet.addReference(item);

        if (item.getMetadata("dc.relation.haspart").length > 0) {
            ReferenceSet hasParts;
            hasParts = itemRef.addReferenceSet("embeddedView", null, "hasPart");
            hasParts.setHead(T_head_has_part);

            if (dataFiles.size() == 0) retrieveDataFiles(item);

            for (Item obj : dataFiles) {
                hasParts.addReference(obj);
            }
        }

        ReferenceSet appearsInclude = itemRef.addReferenceSet(ReferenceSet.TYPE_DETAIL_LIST, null, "hierarchy");
        appearsInclude.setHead(T_head_parent_collections);

        //Reference all collections the item appears in.
        for (Collection collection : item.getCollections()) {
            appearsInclude.addReference(collection);
        }
    }


    private void retrieveDataFiles(Item item) throws SQLException {
        DOIIdentifierProvider dis = new DSpace().getSingletonService(DOIIdentifierProvider.class);

        if (item.getMetadata("dc.relation.haspart").length > 0) {
            dataFiles = new ArrayList<Item>();
            for (DCValue value : item.getMetadata("dc.relation.haspart")) {

                DSpaceObject obj = null;
                try {
                    obj = dis.resolve(context, value.value);
                } catch (IdentifierNotFoundException e) {
                    // just keep going
                } catch (IdentifierNotResolvableException e) {
                    // just keep going
                }
                if (obj != null) dataFiles.add((Item) obj);
            }
        }
    }

    private void addWarningMessage(Item item, Division division) throws WingException, SQLException, AuthorizeException, IOException {
        Message message = null;

        // Add Withdrawn Message
        if (item.isWithdrawn()) {
            Division div = division.addDivision("notice", "notice");
            Para p = div.addPara();
            p.addContent(T_withdrawn);
            return;
        }


        log.error("Item ADDING MESSAGE!!!!!!");

        // Add Versioning Message
        VersioningService versioningService = new DSpace().getSingletonService(VersioningService.class);
        VersionHistory history = versioningService.findVersionHistory(context, item.getID());
        if (history != null && !history.isLastVersion(history.getVersion(item))) {

            // if last version is not archived:
            //     a. If the version is in the submission Queue of the current EPerson or the current person is an administrator: show a link to the version "you have a version of this item in your Submission Queue."
            //     b  If the version is in workflow review and the current user is the submitter of the item or an administrator: show message T_version_in_workflow
            //     c. If the current user in not in the cases previous cases (a,b) and the version doesn't have a next archived version don't show any message, if it does show  NOT_CURRENT_VERSION
            Item lastestItemVersion = history.getLatestVersion().getItem();
            if (!lastestItemVersion.isArchived()) {
                if (isCurrentEpersonItemOwner(lastestItemVersion) || item.canEdit()) {
                    WorkspaceItem wsi = WorkspaceItem.findByItemId(context, lastestItemVersion.getID());
                    if (wsi != null) {
                        String link = "/submit-overview?workspaceID=" + wsi.getID();
                        addMessage(division, T_version_in_submission, link, T_go_to_submission_page);
                        return;
                    }
                    WorkflowItem wfi = WorkflowItem.findByItemId(context, lastestItemVersion.getID());
                    if (wfi != null) {
                        addMessage(division, T_version_in_workflow, null, null);
                        return;
                    }
                } else {
                    Version latestArchived = findLastArchivedVersion(history);
                    // next version is archived. Display: NOT_CURRENT_VERSION
                    if (latestArchived.getItem().getID() != item.getID())
                        addMessage(division, T_not_current_version, getItemURL(latestArchived.getItem()), T_most_current_version);
                }

            }
            // Latest Version archived
            else {
                Version latestArchived = findLastArchivedVersion(history);
                // next version is archived. Display: NOT_CURRENT_VERSION
                if (latestArchived.getItem().getID() != item.getID())
                    addMessage(division, T_not_current_version, getItemURL(latestArchived.getItem()), T_most_current_version);

                    // add IN_WORKFLOW Message
                else {
                    WorkflowItem wfi = WorkflowItem.findByItemId(context, lastestItemVersion.getID());
                    if (wfi != null) {
                        List<ClaimedTask> claimedTasks = ClaimedTask.findByWorkflowId(context, wfi.getID());
                        if (claimedTasks.get(0).getStepID().equals("dryadAcceptEditReject")  && claimedTasks.get(0).getActionID().equals("dryadAcceptEditRejectAction")) {
                            addMessage(division, T_in_workflow, null, null);
                        }
                    }
                }

            }
            return;
        }
        // add IN_WORKFLOW Message
        else {
            WorkflowItem wfi = WorkflowItem.findByItemId(context, item.getID());
            log.error("Item ADDING MESSAGE WORKFLOW: " + wfi);
            if (wfi != null) {
                //List<ClaimedTask> claimedTasks = ClaimedTask.findByWorkflowId(context, wfi.getID());
                //if(claimedTasks.size() > 0){
                    //if (claimedTasks.get(0).getStepID().equals("dryadAcceptEditReject")  && claimedTasks.get(0).getActionID().equals("dryadAcceptEditRejectAction")) {
                DCValue[] values = item.getMetadata("workflow.step.reviewerKey");

                log.error("Item ADDING MESSAGE REVIEW KEY: " + values);
                log.error("Item ADDING MESSAGE REVIEW KEY: " + values.length);
                if(values!=null && values.length > 0){
                    addMessage(division, T_in_workflow, null, null);
                }
            }
        }


    }


    /**
     * Determine if the full item should be referenced or just a summary.
     */
    public static boolean showFullItem(Map objectModel) {
        Request request = ObjectModelHelper.getRequest(objectModel);
        String show = request.getParameter("show");

        if (show != null && show.length() > 0) return true;
        return false;
    }

    /**
     * Obtain the item's title.
     */
    public static String getItemTitle(Item item) {
        DCValue[] titles = item.getDC("title", Item.ANY, Item.ANY);

        String title;
        if (titles != null && titles.length > 0) title = titles[0].value;
        else title = null;
        return title;
    }

    /**
     * Recycle
     */
    public void recycle() {
        this.validity = null;
        super.recycle();
    }

    private String parseName(DCValue[] aMetadata) {
        StringBuilder buffer = new StringBuilder();
        int position = 0;

        for (DCValue metadata : aMetadata) {

            if (metadata.value.indexOf(",") != -1) {
                String[] parts = metadata.value.split(",");

                if (parts.length > 1) {
                    StringTokenizer tokenizer = new StringTokenizer(parts[1], ". ");

                    buffer.append(parts[0]).append(" ");

                    while (tokenizer.hasMoreTokens()) {
                        buffer.append(tokenizer.nextToken().charAt(0));
                    }
                }
            } else {
                // now the minority case (as we clean up the data)
                String[] parts = metadata.value.split("\\s+|\\.");
                String author = parts[parts.length - 1].replace("\\s+|\\.", "");
                char ch;

                buffer.append(author).append(" ");

                for (int index = 0; index < parts.length - 1; index++) {
                    if (parts[index].length() > 0) {
                        ch = parts[index].replace("\\s+|\\.", "").charAt(0);
                        buffer.append(ch);
                    }
                }
            }

            if (++position < aMetadata.length) {
                if (aMetadata.length > 2) {
                    buffer.append(", ");
                } else {
                    buffer.append(" and ");
                }
            }
        }

        return buffer.length() > 0 ? buffer.toString() : "";
    }


    private String getItemURL(Item item) throws WingException {
        DCValue[] identifiers = item.getMetadata("dc", "identifier", null, Item.ANY);
        String itemIdentifier = null;
        if (identifiers != null && identifiers.length > 0)
            itemIdentifier = identifiers[0].value;

        if (itemIdentifier != null)
            return ConfigurationManager.getProperty("dspace.baseUrl") + "/resource/" + itemIdentifier;

        return ConfigurationManager.getProperty("dspace.baseUrl") + "/handle/" + item.getHandle();

    }


    private Version findLastArchivedVersion(VersionHistory history) {
        if (history != null) {
            Version version = history.getLatestVersion();
            if (version.getItem().isArchived())
                return version;

            return history.getPrevious(version);
        }
        return null;
    }

    private boolean isCurrentEpersonItemOwner(Item item) throws SQLException {
        if (eperson == null) return false;

        if (item.getSubmitter().getID() == eperson.getID())
            return true;
        return false;
    }


    private void addMessage(Division main, Message message, String link, Message linkMessage) throws WingException {
        Division div = main.addDivision("notice", "notice");
        Para p = div.addPara();
        p.addContent(message);
        p.addXref(link, linkMessage);

    }
}


