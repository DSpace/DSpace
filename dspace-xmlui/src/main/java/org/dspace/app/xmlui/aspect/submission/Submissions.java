/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.submission;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.SupervisedItem;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.xml.sax.SAXException;

/**
 * @author Scott Phillips
 */
public class Submissions extends AbstractDSpaceTransformer
{
	/** General Language Strings */
    protected static final Message T_title = 
        message("xmlui.Submission.Submissions.title");
    protected static final Message T_dspace_home = 
        message("xmlui.general.dspace_home"); 
    protected static final Message T_trail = 
        message("xmlui.Submission.Submissions.trail");
    protected static final Message T_head = 
        message("xmlui.Submission.Submissions.head");
    protected static final Message T_untitled = 
        message("xmlui.Submission.Submissions.untitled");
    protected static final Message T_email = 
        message("xmlui.Submission.Submissions.email");

    // used by the unfinished submissions section
    protected static final Message T_s_head1 = 
        message("xmlui.Submission.Submissions.submit_head1"); 
    protected static final Message T_s_info1a = 
        message("xmlui.Submission.Submissions.submit_info1a"); 
    protected static final Message T_s_info1b = 
        message("xmlui.Submission.Submissions.submit_info1b"); 
    protected static final Message T_s_info1c = 
        message("xmlui.Submission.Submissions.submit_info1c"); 
    protected static final Message T_s_head2 = 
        message("xmlui.Submission.Submissions.submit_head2"); 
    protected static final Message T_s_info2a = 
        message("xmlui.Submission.Submissions.submit_info2a"); 
    protected static final Message T_s_info2b = 
        message("xmlui.Submission.Submissions.submit_info2b"); 
    protected static final Message T_s_info2c = 
        message("xmlui.Submission.Submissions.submit_info2c"); 
    protected static final Message T_s_column1 = 
        message("xmlui.Submission.Submissions.submit_column1"); 
    protected static final Message T_s_column2 = 
        message("xmlui.Submission.Submissions.submit_column2"); 
    protected static final Message T_s_column3 = 
        message("xmlui.Submission.Submissions.submit_column3"); 
    protected static final Message T_s_column4 = 
        message("xmlui.Submission.Submissions.submit_column4"); 
    protected static final Message T_s_head3 = 
        message("xmlui.Submission.Submissions.submit_head3"); 
    protected static final Message T_s_info3 = 
        message("xmlui.Submission.Submissions.submit_info3"); 
    protected static final Message T_s_head4 = 
        message("xmlui.Submission.Submissions.submit_head4"); 
    protected static final Message T_s_submit_remove = 
        message("xmlui.Submission.Submissions.submit_submit_remove"); 

    // Used in the completed submissions section
    protected static final Message T_c_head =
            message("xmlui.Submission.Submissions.completed.head");
    protected static final Message T_c_info =
            message("xmlui.Submission.Submissions.completed.info");
    protected static final Message T_c_column1 =
            message("xmlui.Submission.Submissions.completed.column1");
    protected static final Message T_c_column2 =
            message("xmlui.Submission.Submissions.completed.column2");
    protected static final Message T_c_column3 =
            message("xmlui.Submission.Submissions.completed.column3");
    protected static final Message T_c_limit =
            message("xmlui.Submission.Submissions.completed.limit");
    protected static final Message T_c_displayall =
            message("xmlui.Submission.Submissions.completed.displayall");

    @Override
    public void addPageMeta(PageMeta pageMeta) throws SAXException,
	WingException, UIException, SQLException, IOException,
	AuthorizeException
	{
            pageMeta.addMetadata("title").addContent(T_title);

            pageMeta.addTrailLink(contextPath + "/",T_dspace_home);
            pageMeta.addTrailLink(null,T_trail);
	}

    @Override
    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        boolean displayAll = false;
        //This param decides whether we display all of the user's previous
        // submissions, or just a portion of them
        if (request.getParameter("all") != null)
        {
            displayAll=true;
        }

        Division div = body.addInteractiveDivision("submissions", contextPath+"/submissions", Division.METHOD_POST,"primary");
        div.setHead(T_head);

//        this.addWorkflowTasksDiv(div);
        this.addUnfinishedSubmissions(div);
//        this.addSubmissionsInWorkflowDiv(div);
        this.addPreviousSubmissions(div, displayAll);
    }

    /**
     * If the user has any workflow tasks, either assigned to them or in an 
     * available pool of tasks, then build two tables listing each of these queues.
     * 
     * If the user doesn't have any workflows then don't do anything.
     * 
     * @param division The division to add the two queues too.
     */
    private void addWorkflowTasksDiv(Division division) throws SQLException, WingException, AuthorizeException, IOException {
    	division.addDivision("workflow-tasks");
        }

    /**
     * There are two options:  the user has some unfinished submissions 
     * or the user does not.
     * 
     * If the user does not, then we just display a simple paragraph 
     * explaining that the user may submit new items to dspace.
     * 
     * If the user does have unfinished submissions then a table is 
     * presented listing all the unfinished submissions that this user has.
     * 
     */
    private void addUnfinishedSubmissions(Division division) throws SQLException, WingException
    {

        // User's WorkflowItems
    	WorkspaceItem[] unfinishedItems = WorkspaceItem.findByEPerson(context,context.getCurrentUser());
    	SupervisedItem[] supervisedItems = SupervisedItem.findbyEPerson(context, context.getCurrentUser());

    	if (unfinishedItems.length <= 0 && supervisedItems.length <= 0)
    	{
            Collection[] collections = Collection.findAuthorizedOptimized(context, Constants.ADD);

            if (collections.length > 0)
            {
                Division start = division.addDivision("start-submision");
                start.setHead(T_s_head1);
                Para p = start.addPara();
                p.addContent(T_s_info1a);
                p.addXref(contextPath+"/submit",T_s_info1b);
                Para secondP = start.addPara();
                secondP.addContent(T_s_info1c);
                return;
            }
    	}

    	Division unfinished = division.addDivision("unfinished-submisions");
    	unfinished.setHead(T_s_head2);
    	Para p = unfinished.addPara();
    	p.addContent(T_s_info2a);
    	p.addHighlight("bold").addXref(contextPath+"/submit",T_s_info2b);
    	p.addContent(T_s_info2c);

    	// Calculate the number of rows.
    	// Each list pluss the top header and bottom row for the button.
    	int rows = unfinishedItems.length + supervisedItems.length + 2;
    	if (supervisedItems.length > 0 && unfinishedItems.length > 0)
        {
            rows++; // Authoring heading row
        }
    	if (supervisedItems.length > 0)
        {
            rows++; // Supervising heading row
        }

    	Table table = unfinished.addTable("unfinished-submissions",rows,5);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCellContent(T_s_column1);
        header.addCellContent(T_s_column2);
        header.addCellContent(T_s_column3);
        header.addCellContent(T_s_column4);

        if (supervisedItems.length > 0 && unfinishedItems.length > 0)
        {
            header = table.addRow();
            header.addCell(null,Cell.ROLE_HEADER,0,5,null).addContent(T_s_head3);
        }

        if (unfinishedItems.length > 0)
        {
            for (WorkspaceItem workspaceItem : unfinishedItems) 
            {
                Metadatum[] titles = workspaceItem.getItem().getDC("title", null, Item.ANY);
                EPerson submitterEPerson = workspaceItem.getItem().getSubmitter();

                int workspaceItemID = workspaceItem.getID();
                String url = contextPath+"/submit?workspaceID="+workspaceItemID;
                String submitterName = submitterEPerson.getFullName();
                String submitterEmail = submitterEPerson.getEmail();
                String collectionName = workspaceItem.getCollection().getMetadata("name");

                Row row = table.addRow(Row.ROLE_DATA);
                CheckBox remove = row.addCell().addCheckBox("workspaceID");
                remove.setLabel("remove");
                remove.addOption(workspaceItemID);

                if (titles.length > 0)
                {
                    String displayTitle = titles[0].value;
                    if (displayTitle.length() > 50)
                        displayTitle = displayTitle.substring(0, 50) + " ...";
                    row.addCell().addXref(url,displayTitle);
                }
                else
                    row.addCell().addXref(url,T_untitled);
                row.addCell().addXref(url,collectionName);
                Cell cell = row.addCell();
                cell.addContent(T_email);
                cell.addXref("mailto:"+submitterEmail,submitterName);
            }
        } 
        else
        {
            header = table.addRow();
            header.addCell(0,5).addHighlight("italic").addContent(T_s_info3);
        }

        if (supervisedItems.length > 0)
        {
            header = table.addRow();
            header.addCell(null,Cell.ROLE_HEADER,0,5,null).addContent(T_s_head4);
        }

        for (WorkspaceItem workspaceItem : supervisedItems) 
        {

            Metadatum[] titles = workspaceItem.getItem().getDC("title", null, Item.ANY);
            EPerson submitterEPerson = workspaceItem.getItem().getSubmitter();

            int workspaceItemID = workspaceItem.getID();
            String url = contextPath+"/submit?workspaceID="+workspaceItemID;
            String submitterName = submitterEPerson.getFullName();
            String submitterEmail = submitterEPerson.getEmail();
            String collectionName = workspaceItem.getCollection().getMetadata("name");

            Row row = table.addRow(Row.ROLE_DATA);
            CheckBox selected = row.addCell().addCheckBox("workspaceID");
            selected.setLabel("select");
            selected.addOption(workspaceItemID);

            if (titles.length > 0)
            {
                String displayTitle = titles[0].value;
                if (displayTitle.length() > 50)
                {
                    displayTitle = displayTitle.substring(0, 50) + " ...";
                }
                row.addCell().addXref(url,displayTitle);
            }
            else
            {
                row.addCell().addXref(url, T_untitled);
            }
            row.addCell().addXref(url,collectionName);
            Cell cell = row.addCell();
            cell.addContent(T_email);
            cell.addXref("mailto:"+submitterEmail,submitterName);
        }

        header = table.addRow();
        Cell lastCell = header.addCell(0,5);
        if (unfinishedItems.length > 0 || supervisedItems.length > 0)
        {
            lastCell.addButton("submit_submissions_remove").setValue(T_s_submit_remove);
        }
    }

    /**
     * This section lists all the submissions that this user has submitted which are currently under review.
     * 
     * If the user has none, this nothing is displayed.
     */
    private void addSubmissionsInWorkflowDiv(Division division)
            throws SQLException, WingException, AuthorizeException, IOException
    {
        division.addDivision("submissions-inprogress");
    }

    /**
     * Show the user's completed submissions.
     * 
     * If the user has no completed submissions, display nothing.
     * If 'displayAll' is true, then display all user's archived submissions.
     * Otherwise, default to only displaying 50 archived submissions.
     * 
     * @param division div to put archived submissions in
     * @param displayAll whether to display all or just a limited number.
     */
    private void addPreviousSubmissions(Division division, boolean displayAll)
            throws SQLException,WingException
    {
        // Turn the iterator into a list (to get size info, in order to put in a table)
        List subList = new LinkedList();

        Integer limit;

        if(displayAll) {
            limit = -1;
        } else {
            //Set a default limit of 50
            limit = 50;
        }
        ItemIterator subs = Item.findBySubmitterDateSorted(context, context.getCurrentUser(), limit);

        //NOTE: notice we are adding each item to this list in *reverse* order...
        // this is a very basic attempt at making more recent submissions float 
        // up to the top of the list (findBySubmitter() doesn't guarrantee
        // chronological order, but tends to return older items near top of the list)
        try
        {
            while (subs.hasNext())
            {
                subList.add(subs.next());
            }
        }
        finally
        {
            if (subs != null)
                subs.close();
        }

        // No tasks, so don't show the table.
        if (!(subList.size() > 0))
            return;

        Division completedSubmissions = division.addDivision("completed-submissions");
        completedSubmissions.setHead(T_c_head);
        completedSubmissions.addPara(T_c_info);

        // Create table, headers
        Table table = completedSubmissions.addTable("completed-submissions",subList.size() + 2,3);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCellContent(T_c_column1); // ISSUE DATE
        header.addCellContent(T_c_column2); // ITEM TITLE (LINKED)
        header.addCellContent(T_c_column3); // COLLECTION NAME (LINKED)

        //Limit to showing just 50 archived submissions, unless overridden
        //(This is a saftey measure for Admins who may have submitted 
        // thousands of items under their account via bulk ingest tools, etc.)
        int count = 0;

        // Populate table
        Iterator i = subList.iterator();
        while(i.hasNext())
        {
            count++;
            //exit loop if we've gone over our limit of submissions to display
            if(count>limit && !displayAll)
                break;

            Item published = (Item) i.next();
            String collUrl = contextPath+"/handle/"+published.getOwningCollection().getHandle();
            String itemUrl = contextPath+"/handle/"+published.getHandle();
            Metadatum[] titles = published.getMetadata("dc", "title", null, Item.ANY);
            String collectionName = published.getOwningCollection().getMetadata("name");
            Metadatum[] ingestDate = published.getMetadata("dc", "date", "accessioned", Item.ANY);

            Row row = table.addRow();

            // Item accession date
            if (ingestDate != null && ingestDate.length > 0 &&
                ingestDate[0].value != null)
            {
                String displayDate = ingestDate[0].value.substring(0,10);
                Cell cellDate = row.addCell();
                cellDate.addContent(displayDate);
            }
            else //if no accession date add an empty cell (shouldn't happen, but just in case)
                row.addCell().addContent("");

            // The item description
            if (titles != null && titles.length > 0 &&
                titles[0].value != null)
            {
                String displayTitle = titles[0].value;
                if (displayTitle.length() > 50)
                    displayTitle = displayTitle.substring(0,50)+ " ...";
                row.addCell().addXref(itemUrl,displayTitle);
            }
            else
                row.addCell().addXref(itemUrl,T_untitled);

            // Owning Collection
            row.addCell().addXref(collUrl,collectionName);
        }//end while

        //Display limit text & link to allow user to override this default limit
        if(!displayAll && count == limit)
        {
            Para limitedList = completedSubmissions.addPara();
            limitedList.addContent(T_c_limit);
            limitedList.addXref(contextPath + "/submissions?all", T_c_displayall);
        }    
    }

}
