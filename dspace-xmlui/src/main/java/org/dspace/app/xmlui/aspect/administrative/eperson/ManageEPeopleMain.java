/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.eperson;

import java.sql.SQLException;

import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.CheckBox;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Highlight;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.eperson.EPerson;

/**
 * The manage epeople page is the starting point page for managing 
 * epeople. From here the user is able to browse or search for epeople, 
 * once identified the user can selected them for deletion by selecting 
 * the checkboxes and clicking delete or click their name to edit the 
 * eperson.
 * 
 * based on class by Alexey Maslov and Scott Phillips
 * modified for LINDAT/CLARIN
 */
public class ManageEPeopleMain extends AbstractDSpaceTransformer   
{	

    /** Language Strings */
    private static final Message T_title = 
		message("xmlui.administrative.eperson.ManageEPeopleMain.title");
	
    private static final Message T_eperson_trail =
		message("xmlui.administrative.eperson.general.epeople_trail");
	
    private static final Message T_main_head =
		message("xmlui.administrative.eperson.ManageEPeopleMain.main_head");
	
    private static final Message T_actions_head =
		message("xmlui.administrative.eperson.ManageEPeopleMain.actions_head");
	
    private static final Message T_actions_create =
		message("xmlui.administrative.eperson.ManageEPeopleMain.actions_create");
	
    private static final Message T_actions_create_link =
		message("xmlui.administrative.eperson.ManageEPeopleMain.actions_create_link");
	
    private static final Message T_actions_browse =
		message("xmlui.administrative.eperson.ManageEPeopleMain.actions_browse");
	
    private static final Message T_actions_browse_link =
		message("xmlui.administrative.eperson.ManageEPeopleMain.actions_browse_link");
	
    private static final Message T_actions_search =
		message("xmlui.administrative.eperson.ManageEPeopleMain.actions_search");
	
    private static final Message T_search_help =
		message("xmlui.administrative.eperson.ManageEPeopleMain.search_help");
	
    private static final Message T_dspace_home =
		message("xmlui.general.dspace_home");
	
    private static final Message T_go =
		message("xmlui.general.go");

    private static final Message T_head1_none =
            message("xmlui.ArtifactBrowser.AbstractSearch.head1_none");

    private static final Message T_search_head =
		message("xmlui.administrative.eperson.ManageEPeopleMain.search_head");

    private static final Message T_search_column1 =
		message("xmlui.administrative.eperson.ManageEPeopleMain.search_column1");
	
    private static final Message T_search_column2 =
		message("xmlui.administrative.eperson.ManageEPeopleMain.search_column2");

    private static final Message T_search_column3 =
		message("xmlui.administrative.eperson.ManageEPeopleMain.search_column3");

    private static final Message T_search_column4 =
		message("xmlui.administrative.eperson.ManageEPeopleMain.search_column4");

    private static final Message T_submit_delete =
		message("xmlui.administrative.eperson.ManageEPeopleMain.submit_delete");

    private static final Message T_no_results =
		message("xmlui.administrative.eperson.ManageEPeopleMain.no_results");

    /**
     * The total number of entries to show on a page
     */
    private static final int PAGE_SIZE = 30;


    public void addPageMeta(PageMeta pageMeta) throws WingException
    {
        pageMeta.addMetadata("title").addContent(T_title);
        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        pageMeta.addTrailLink(null,T_eperson_trail);
    }


    public void addBody(Body body) throws WingException, SQLException 
    {
        /* Get and setup our parameters */
        int page = parameters.getParameterAsInteger("page", 1);
        if(page <= 0)
        	page = 1;
                
        int highlightID   = parameters.getParameterAsInteger("highlightID",-1);
        String query      = decodeFromURL(parameters.getParameter("query",null));
        String baseURL    = contextPath+"/admin/epeople?administrative-continue="+knot.getId();
        int resultCount   = EPerson.searchResultCount(context, query);	
        EPerson[] epeople = EPerson.search(context, query, (page-1)*PAGE_SIZE, PAGE_SIZE, "eperson_id");


        // DIVISION: eperson-main
        Division main = body.addInteractiveDivision("epeople-main", contextPath
                + "/admin/epeople", Division.METHOD_POST,
                "primary administrative eperson");
        main.setHead(T_main_head);

        // DIVISION: eperson-actions
        Division actions = main.addDivision("epeople-actions");
        actions.setHead(T_actions_head);

        List actionsList = actions.addList("actions");
        actionsList.addLabel(T_actions_create);
        actionsList.addItemXref(baseURL+"&submit_add", T_actions_create_link);
        actionsList.addLabel(T_actions_browse);
        actionsList.addItemXref(baseURL+"&query&submit_search", 
        		T_actions_browse_link);

        actionsList.addLabel(T_actions_search);
        org.dspace.app.xmlui.wing.element.Item actionItem = actionsList.addItem();
        Text queryField = actionItem.addText("query");
        queryField.setAutofocus("autofocus");
        if (query != null)
        {
            queryField.setValue(query);
        }
        queryField.setHelp(T_search_help);
        actionItem.addButton("submit_search").setValue(T_go);

        // DIVISION: eperson-search
        Division search = main.addDivision("eperson-search");
		search.setHead(T_search_head);

        int firstIndex = (page-1)*PAGE_SIZE+1; 
        int lastIndex = (page-1)*PAGE_SIZE + PAGE_SIZE;
        if(lastIndex > resultCount) lastIndex = resultCount;
        int totalPages = (int)Math.ceil((double)resultCount / PAGE_SIZE); 

		search.setHead(T_head1_none.parameterize(firstIndex, lastIndex, resultCount));
		search.setMaskedPagination(resultCount, firstIndex, lastIndex, page, totalPages, baseURL+"&page={pageNum}");

        Table table = search.addTable("eperson-search-table", epeople.length + 1, 5);
        Row header = table.addRow(Row.ROLE_HEADER);
        header.addCell().addContent(T_search_column1);
        header.addCell().addContent(T_search_column2);
        header.addCell().addContent(T_search_column3);
        header.addCell().addContent(T_search_column4);
        header.addCell().addContent("More information");

        CheckBox selectEPerson; 
        for (EPerson person : epeople)
        {
            String epersonID = String.valueOf(person.getID());
            String fullName = person.getFullName();
            String email = person.getEmail();
            String url = baseURL+"&submit_edit&epersonID="+epersonID;
            java.util.List<String> deleteConstraints = person.getDeleteConstraints();

            Row row;
            if (person.getID() == highlightID)
            {
                // This is a highlighted eperson
                row = table.addRow(null, null, "highlight");
            }
            else
            {
                row = table.addRow();
            }

            if (deleteConstraints != null && deleteConstraints.size() > 0)
            {
                row.addCell().addHighlight("fa fa-ban text-error").addContent(" ");
            } else {
            selectEPerson = row.addCell().addCheckBox("select_eperson");
            selectEPerson.setLabel(epersonID);
            selectEPerson.addOption(epersonID);
            }

            row.addCellContent(epersonID);
            row.addCell().addXref(url, fullName);
            row.addCell().addXref(url, email);

            // more details
        	Cell c = row.addCell();
        	
        	Highlight line1 = c.addHighlight("container-fluid");
        	
        	if(person.canLogIn()) {
        		line1.addHighlight("label label-success").addContent("Can login");
        	} else {
        		line1.addHighlight("label label-important").addContent("Cannot login");
        	}
        	
        	if(person.getPasswordHash()==null) {
        		line1.addHighlight("label label-warning").addContent("Password not set");
        	} else {
        		line1.addHighlight("label label-success").addContent("Password set");
        	}
        	
        	if(person.getLoggedIn()==null) {
        		line1.addHighlight("label label-warning").addContent("Not logged in yet");
        	} else {
        		line1.addHighlight("label label-info").addContent("Last login: " + person.getLoggedIn());
        	}
        	
        	if ( deleteConstraints != null && deleteConstraints.size() > 0 ) {
        		Highlight line2 = c.addHighlight("container-fluid");
        		line2.addHighlight("label label-important").addContent("Cannot Delete");        		
            	for ( String s : deleteConstraints ) {
            	    line2.addHighlight("label label-info").addContent(s);
            	}
        	}
        	

    		Highlight line3 = c.addHighlight("container-fluid");
        	
        	if(person.canEditSubmissionMetadata()){
        		line3.addHighlight("label label-info").addContent("Edits metadata");
        	}

        	if(person.getNetid()==null) {
        		line3.addHighlight("label label-warning").addContent("netid: " + person.getNetid());
        	} else {
        		line3.addHighlight("text-info").addContent("netid: " + person.getNetid());
        	}
        	
        }

        if (epeople.length <= 0) 
        {
            Cell cell = table.addRow().addCell(1, 4);
            cell.addHighlight("italic").addContent(T_no_results);
        }
        else 
        {
            search.addPara().addButton("submit_delete").setValue(T_submit_delete);
        }

        main.addHidden("administrative-continue").setValue(knot.getId());

   }
}
