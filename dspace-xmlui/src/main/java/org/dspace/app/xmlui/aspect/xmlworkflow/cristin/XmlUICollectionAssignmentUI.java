package org.dspace.app.xmlui.aspect.xmlworkflow.cristin;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.app.xmlui.aspect.xmlworkflow.AbstractXMLUIAction;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Constants;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Stack;
import java.util.UUID;

/**
 * <p>XMLUI Action to provide a Collection assignment workflow stage</p>
 *
 * <p><strong>Configuration</strong></p>
 *
 * <p>In the spring/xmlui/workflow-actions-xmlui.xml definitions file, add a new bean:</p>
 */
public class XmlUICollectionAssignmentUI extends AbstractXMLUIAction {



    @Override
    public void addBody(Body body)
            throws SAXException, WingException, SQLException, IOException, AuthorizeException {
        Item item = workflowItem.getItem();
        Collection collection = workflowItem.getCollection();
        Request request = ObjectModelHelper.getRequest(objectModel);
        String actionURL = contextPath + "/handle/" + collection.getHandle() + "/xmlworkflow";

        Division div = body.addInteractiveDivision("perform-task", actionURL, Division.METHOD_POST, "primary workflow");
        div.setHead("Assign the Item's Collections");
        div.addPara("Select collections to which the item will be mapped");

        // add the item meta, and also the hidden field required to make the full item display work
        addWorkflowItemInformation(div, item, request);
        div.addHidden("workflowID").setValue(workflowItem.getID());
        div.addHidden("stepID").setValue(request.getParameter("stepID"));
        div.addHidden("actionID").setValue(request.getParameter("actionID"));

        div.addPara("The item is being submitted to the collection:" + collection.getName() + " (" + collection.getHandle() + ")");
        div.addPara("Select collections to add the item to:");
        CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
        TreeNode root = buildTree(communityService.findAllTop(context));
        java.util.List<Collection> existingCollections = item.getCollections();
        java.util.List<UUID> existingIDs = new ArrayList<>();
        for (Collection ec : existingCollections) {
            existingIDs.add(ec.getID());
        }


        java.util.List<TreeNode> rootNodes = root.getChildrenOfType(Constants.COMMUNITY);

        Table comcols = div.addTable("comcol", 1, 1);
        Row tr = comcols.addRow();
        tr.addCell(Cell.ROLE_HEADER).addContent("Community/Collection");
        tr.addCell(Cell.ROLE_HEADER).addContent("Mapped?");
        for (TreeNode node : rootNodes) {
            buildList(comcols, node, collection, existingIDs);
        }

        Table table = div.addTable("workflow-actions", 1, 1);

        // finish the workflow stage
        Row row = table.addRow();
        row.addCellContent("Save progress and finish this workflow stage");
        row.addCell().addButton("submit_finished").setValue("Save and Finish");

        // Reject item
        row = table.addRow();
        row.addCellContent("Save progress, but leave the item in your task list");
        row.addCell().addButton("submit_save").setValue("Save");

        // Everyone can just cancel
        row = table.addRow();
        row.addCell(0, 2).addButton("submit_leave").setValue("Return without saving");

        div.addHidden("submission-continue").setValue(knot.getId());
    }

    public void buildList(Table table, TreeNode node, Collection submissionCollection, java.util.List<UUID> existingIDs) throws WingException {
        DSpaceObject dso = node.getDSO();

        Row tr = table.addRow();
        tr.addCell(Cell.ROLE_HEADER).addContent(this.getIndent(node) + dso.getName());
        tr.addCell().addContent("");

        // Add all the sub-collections;
        java.util.List<TreeNode> collectionNodes = node.getChildrenOfType(Constants.COLLECTION);
        if (collectionNodes != null && collectionNodes.size() > 0) {
            for (TreeNode collectionNode : collectionNodes) {
                UUID nodeID = collectionNode.getDSO().getID();
                tr = table.addRow();
                tr.addCell().addContent(this.getIndent(collectionNode) + collectionNode.getDSO().getName());
                if (nodeID != submissionCollection.getID()) {
                    CheckBox cb = tr.addCell().addCheckBox("mapped_collection_" + nodeID.toString());
                    cb.addOption(existingIDs.contains(nodeID), nodeID.toString());
                }
            }
        }

        // Add all the sub-communities
        java.util.List<TreeNode> communityNodes = node.getChildrenOfType(Constants.COMMUNITY);
        if (communityNodes != null && communityNodes.size() > 0) {
            for (TreeNode communityNode : communityNodes) {
                buildList(table, communityNode, submissionCollection, existingIDs);
            }
        }
    }

    private String getIndent(TreeNode node) {
        StringBuilder indent = new StringBuilder();
        for (int i = 1; i < node.getLevel(); i++) {
            indent.append("--");
        }
        return indent + " ";
    }

    public void buildReferenceSet(ReferenceSet referenceSet, TreeNode node) throws WingException {
        DSpaceObject dso = node.getDSO();

        Reference objectInclude = referenceSet.addReference(dso);

        // Add all the sub-collections;
        java.util.List<TreeNode> collectionNodes = node.getChildrenOfType(Constants.COLLECTION);
        if (collectionNodes != null && collectionNodes.size() > 0) {
            ReferenceSet collectionSet = objectInclude.addReferenceSet(ReferenceSet.TYPE_SUMMARY_LIST);

            for (TreeNode collectionNode : collectionNodes) {
                collectionSet.addReference(collectionNode.getDSO());
            }
        }

        // Add all the sub-communities
        java.util.List<TreeNode> communityNodes = node.getChildrenOfType(Constants.COMMUNITY);
        if (communityNodes != null && communityNodes.size() > 0) {
            ReferenceSet communitySet = objectInclude.addReferenceSet(ReferenceSet.TYPE_SUMMARY_LIST);

            for (TreeNode communityNode : communityNodes) {
                buildReferenceSet(communitySet, communityNode);
            }
        }
    }

    /**
     * construct a tree structure of communities and collections. The results
     * of this hierarchy are cached so calling it multiple times is acceptable.
     *
     * @param communities The root level communities
     * @return A root level node.
     */
    private TreeNode buildTree(java.util.List<Community> communities) {
        int maxDepth = 10;

        TreeNode newRoot = new TreeNode();

        // Setup for breath-first traversal
        Stack<TreeNode> stack = new Stack<>();

        for (Community community : communities) {
            stack.push(newRoot.addChild(community));
        }

        while (!stack.empty()) {
            TreeNode node = stack.pop();

            // Short circuit if we have reached our max depth.
            if (node.getLevel() >= maxDepth) {
                continue;
            }

            // Only communities nodes are pushed on the stack.
            Community community = (Community) node.getDSO();

            for (Community subcommunity : community.getSubcommunities()) {
                stack.push(node.addChild(subcommunity));
            }

            // Add any collections to the document.
            for (Collection collection : community.getCollections()) {
                node.addChild(collection);
            }

        }

        return newRoot;
    }

    /**
     * Private class to represent the tree structure of communities & collections.
     */
    protected static class TreeNode {
        /**
         * The object this node represents
         */
        private DSpaceObject dso;

        /**
         * The level in the hierarchy that this node is at.
         */
        private int level;

        /**
         * All children of this node
         */
        private java.util.List<TreeNode> children = new ArrayList<TreeNode>();

        /**
         * Construct a new root level node
         */
        public TreeNode() {
            // Root level node is add the zero level.
            this.level = 0;
        }

        /**
         * @return The DSpaceObject this node represents
         */
        public DSpaceObject getDSO() {
            return this.dso;
        }

        /**
         * Add a child DSpaceObject
         *
         * @param dso The child
         * @return A new TreeNode object attached to the tree structure.
         */
        public TreeNode addChild(DSpaceObject dso) {
            TreeNode child = new TreeNode();
            child.dso = dso;
            child.level = this.level + 1;
            children.add(child);
            return child;
        }

        /**
         * @return The current level in the hierarchy of this node.
         */
        public int getLevel() {
            return this.level;
        }

        /**
         * @return All children
         */
        public java.util.List<TreeNode> getChildren() {
            return children;
        }

        /**
         * @return All children of the given @type.
         */
        public java.util.List<TreeNode> getChildrenOfType(int type) {
            java.util.List<TreeNode> results = new ArrayList<>();
            for (TreeNode node : children) {
                if (node.dso.getType() == type) {
                    results.add(node);
                }
            }
            return results;
        }
    }

}
