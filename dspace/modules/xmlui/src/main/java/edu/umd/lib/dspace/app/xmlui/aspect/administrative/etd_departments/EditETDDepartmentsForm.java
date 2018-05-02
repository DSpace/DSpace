/*
 * Copyright (c) 2009 The University of Maryland. All Rights Reserved.
 *
 */
package edu.umd.lib.dspace.app.xmlui.aspect.administrative.etd_departments;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.content.Collection;
import org.dspace.content.EtdUnit;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.EtdUnitService;

import edu.umd.lib.dspace.app.xmlui.aspect.administrative.FlowETDDepartmentUtils;

/**
 * Present the user with the etd_department's current state. The user may select
 * to change the etd_department's name, OR search for new Collections to add, OR
 * select current collections for removal.
 *
 * @author Alexey Maslov
 * @author Scott Phillips
 */
public class EditETDDepartmentsForm extends AbstractDSpaceTransformer
{
    /** Language Strings */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_etd_department_trail = message("xmlui.administrative.etd_departments.general.etd_department_trail");

    private static final Message T_title = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.title");
    private static final Message T_trail = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.trail");

    private static final Message T_main_head = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.main_head");
    private static final Message T_label_name = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.label_name");
    private static final Message T_label_search = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.label_search");
    private static final Message T_submit_search_collections = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.submit_search_collections");
    private static final Message T_no_results = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.no_results");
    private static final Message T_main_head_new = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.main_head_new");
    private static final Message T_member = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.member");

    private static final Message T_submit_clear = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.submit_clear");
    private static final Message T_submit_save = message("xmlui.general.save");
    private static final Message T_submit_cancel = message("xmlui.general.cancel");

    private static final Message T_pending = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.pending");
    private static final Message T_pending_warn = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.pending_warn");
    private static final Message T_submit_add = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.submit_add");
    private static final Message T_submit_remove = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.submit_remove");

    // Collections Search
    private static final Message T_collections_column1 = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.collections_column1");
    private static final Message T_collections_column2 = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.collections_column2");

    // Members
    private static final Message T_members_head = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.members_head");
    private static final Message T_members_column1 = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.members_column1");
    private static final Message T_members_column2 = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.members_column2");
    private static final Message T_members_pending = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.members_pending");
    private static final Message T_members_none = message("xmlui.administrative.etd_departments.EditETDDepartmentForm.members_none");

    // How many results to show on a page.
    private static final int RESULTS_PER_PAGE = 5;

    /** The maximum size of a collection name allowed */
    private static final int MAX_COLLECTION_NAME = 15;
    
    private static EtdUnitService etdunitService = ContentServiceFactory.getInstance().getEtdUnitService();
    
    private static CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();

    @Override
    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(contextPath + "/admin/etd_departments",
                T_etd_department_trail);
        pageMeta.addTrail().addContent(T_trail);
    }

    @Override
    public void addBody(Body body) throws WingException, SQLException
    {
        // Find the department in question
        String etd_departmentID = parameters.getParameter(
                "etd_departmentID", null);
        String currentName = decodeFromURL(parameters.getParameter(
                "etd_departmentName", null));
        if (currentName == null || currentName.length() == 0)
        {
            currentName = FlowETDDepartmentUtils.getName(context,
                    UUID.fromString(etd_departmentID));
        }

        EtdUnit etd_department = null;
        if (StringUtils.isNotBlank(etd_departmentID))
        {
            etd_department = etdunitService.find(context, UUID.fromString(etd_departmentID));
        }

        // Get list of member collections from url
        List<UUID> memberCollectionIDs = new ArrayList<UUID>();
        String memberCollectionIDsString = parameters.getParameter(
                "memberCollectionIDs", null);
        if (memberCollectionIDsString != null)
        {
            for (String id : memberCollectionIDsString.split(","))
            {
                if (id.length() > 0)
                {
                    memberCollectionIDs.add(UUID.fromString(id));
                }
            }
        }

        String highlightCollectionIDString = parameters.getParameter("highlightCollectionID", null);
        UUID highlightCollectionID = null;
        if (StringUtils.isNotBlank(highlightCollectionIDString)) {
            highlightCollectionID = UUID.fromString(highlightCollectionIDString);
        }

        // Get search parameters
        String query = decodeFromURL(parameters.getParameter("query", null));
        int page = parameters.getParameterAsInteger("page", 0);

        // Get any errors
        String errorString = parameters.getParameter("errors", null);
        List<String> errors = new ArrayList<String>();
        if (errorString != null)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
            }
        }

        // DIVISION: etd_department-edit
        Division main = body.addInteractiveDivision("etd_department-edit",
                contextPath + "/admin/etd_departments", Division.METHOD_POST,
                "primary administrative etd_departments");
        if (etd_department == null)
        {
            main.setHead(T_main_head_new);
        }
        else
        {
            main.setHead(T_main_head.parameterize(etd_department.getName(),
                    etd_departmentID));
        }

        // DIVISION: etd_department-actions
        Division actions = main.addDivision("etd_department-edit-actions");
        Para etd_departmentName = actions.addPara();
        etd_departmentName.addContent(T_label_name);
        Text etd_departmentText = etd_departmentName
                .addText("etd_department_name");
        etd_departmentText.setValue(currentName);

        if (errors.contains("etd_department_name")
                || errors.contains("etd_department_name_duplicate"))
        {
            etd_departmentText.addError("");
        }

        Para searchBoxes = actions.addPara();
        searchBoxes.addContent(T_label_search);
        Text queryField = searchBoxes.addText("query");
        queryField.setValue(query);
        queryField.setSize(MAX_COLLECTION_NAME);
        searchBoxes.addButton("submit_search_collection").setValue(
                T_submit_search_collections);

        if (query != null)
        {
            searchBoxes.addButton("submit_clear").setValue(T_submit_clear);
            addCollectionsSearch(main, query, page, etd_department,
                    memberCollectionIDs);
        }

        boolean changes = false;
        if (etd_department != null)
        {
            changes = addMemberList(main, etd_department, memberCollectionIDs, highlightCollectionID);
        }

        Para buttons = main.addPara();
        buttons.addButton("submit_save").setValue(T_submit_save);
        buttons.addButton("submit_cancel").setValue(T_submit_cancel);

        if (changes)
        {
            main.addPara().addHighlight("warn").addContent(T_pending_warn);
        }

        main.addHidden("administrative-continue").setValue(knot.getId());
    }

    /**
     * Search for collections to add to this etd_department.
     */
    private void addCollectionsSearch(Division div, String query, int page,
            EtdUnit etd_department, List<UUID> memberCollectionIDs)
                    throws SQLException, WingException
    {
        List<Collection> collections = collectionService.search(context, query, page
                * RESULTS_PER_PAGE, RESULTS_PER_PAGE);
        int resultCount = collectionService.searchResultCount(context, query);

        Division results = div.addDivision("results");

        if (resultCount > RESULTS_PER_PAGE)
        {
            // If there are enough results then paginate the results
            String baseURL = contextPath
                    + "/admin/etd_departments?administrative-continue="
                    + knot.getId();
            int firstIndex = page * RESULTS_PER_PAGE + 1;
            int lastIndex = page * RESULTS_PER_PAGE + collections.size();

            String nextURL = null, prevURL = null;
            if (page < (resultCount / RESULTS_PER_PAGE))
            {
                nextURL = baseURL + "&page=" + (page + 1);
            }
            if (page > 0)
            {
                prevURL = baseURL + "&page=" + (page - 1);
            }

            results.setSimplePagination(resultCount, firstIndex, lastIndex,
                    prevURL, nextURL);
        }

        /* Set up a table with search results (if there are any). */
        Table table = results.addTable("group-edit-search-collections",
                collections.size() + 1, 2);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_collections_column1);
        header.addCell().addContent(T_collections_column2);

        for (Collection collection : collections)
        {
            String collectionID = String.valueOf(collection.getID());
            String name = collection.getName();

            Row collectionData = table.addRow();

            collectionData.addCell().addContent(collection.getID().toString());
            collectionData.addCell().addContent(name);

            // check if they are already a member of the group
            if (memberCollectionIDs.contains(collection.getID()))
            {
                // Check if they really members or just pending members
                if (etd_department != null
                        && etd_department.isMember(collection))
                {
                    collectionData.addCellContent(T_member);
                }
                else
                {
                    collectionData.addCell().addHighlight("warn")
                    .addContent(T_pending);
                }
            }
            else
            {
                collectionData.addCell()
                .addButton("submit_add_collection_" + collectionID)
                .setValue(T_submit_add);
            }
        }

        if (collections.isEmpty())
        {
            table.addRow().addCell(1, 2).addContent(T_no_results);
        }
    }

    /**
     * Add a table with all the current etd_department's member collections to
     * the specified division.
     *
     * @throws SQLException
     */
    private boolean addMemberList(Division div, EtdUnit parent,
            List<UUID> memberCollectionIDs, UUID highlightCollectionID)
                    throws WingException, SQLException
    {
        // Flag to remember if there are any pending changes.
        boolean changes = false;

        Division members = div.addDivision("deparment-edit-members");
        members.setHead(T_members_head);

        Table table = members.addTable("etd_department-edit-members-table",
                memberCollectionIDs.size() + 1, 2);

        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_members_column1);
        header.addCell().addContent(T_members_column2);

        // get all member collections, pend or actual
        // the cast is correct
        List<UUID> allMemberCollectionIDs = new ArrayList<UUID>(
                memberCollectionIDs);

        for (Collection collection : parent.getCollections())
        {
            if (!allMemberCollectionIDs.contains(collection.getID()))
            {
                allMemberCollectionIDs.add(collection.getID());
            }
        }
        // Sort them to a consistent ordering
        Collections.sort(allMemberCollectionIDs);

        for (UUID collectionID : allMemberCollectionIDs)
        {
            Collection collection = collectionService.find(context, collectionID);
            boolean highlight = (collection.getID().equals(highlightCollectionID));
            boolean pendingAddition = !parent.isMember(collection);
            boolean pendingRemoval = !memberCollectionIDs
                    .contains(collectionID);
            addMemberRow(table, collection, highlight, pendingAddition,
                    pendingRemoval);

            if (pendingAddition || pendingRemoval)
            {
                changes = true;
            }
        }

        if (allMemberCollectionIDs.size() <= 0)
        {
            table.addRow().addCell(1, 2).addContent(T_members_none);
        }

        return changes;
    }

    /**
     * Add a single member row for collections.
     *
     * @param table
     *            The table to add a row too.
     * @param collection
     *            The collection being displayed
     * @param highlight
     *            Should this collection be highlighted?
     * @param pendingAddition
     *            Is this collection pending addition?
     * @param pendingRemoval
     *            Is this collection pending removal?
     */
    private void addMemberRow(Table table, Collection collection,
            boolean highlight, boolean pendingAddition, boolean pendingRemoval)
                    throws WingException, SQLException
    {
        String fullName = collection.getName();

        Row collectionData = table.addRow(null, null, highlight ? "highlight"
                : null);

        collectionData.addCell().addContent(collection.getID().toString());

        Cell nameCell = collectionData.addCell();
        nameCell.addContent(fullName);
        if (pendingAddition)
        {
            nameCell.addContent(" ");
            nameCell.addHighlight("warn").addContent(T_members_pending);
        }

        if (pendingRemoval)
        {
            collectionData.addCell().addHighlight("warn").addContent(T_pending);
        }
        else
        {
            collectionData
            .addCell()
            .addButton("submit_remove_collection_" + collection.getID())
            .setValue(T_submit_remove);
        }
    }
}