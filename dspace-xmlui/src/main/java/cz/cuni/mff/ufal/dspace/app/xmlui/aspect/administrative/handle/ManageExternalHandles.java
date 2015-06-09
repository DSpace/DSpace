/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.handle;

import java.sql.SQLException;
import java.util.*;

import cz.cuni.mff.ufal.dspace.PIDServiceEPICv2;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.core.ConfigurationManager;
import org.dspace.handle.HandleManager;

/**
 */

public class ManageExternalHandles extends AbstractDSpaceTransformer {

	/** Language strings */
	private static final Message T_dspace_home = 
			message("xmlui.general.dspace_home");
	private static final Message T_head1_none = 
			message("xmlui.ArtifactBrowser.AbstractSearch.head1_none");
	private static final Message T_title =
			message("xmlui.administrative.handle.ManageExternalHandles.title");
	private static final Message T_head =
			message("xmlui.administrative.handle.ManageExternalHandles.head");
	private static final Message T_trail =
			message("xmlui.administrative.handle.ManageExternalHandles.trail");
	private static final Message T_new_external_handle =
			message("xmlui.administrative.handle.ManageExternalHandles.new_external_handle");
	private static final Message T_delete_handle =
			message("xmlui.administrative.handle.ManageExternalHandles.delete_handle");
	private static final Message T_edit_handle =
			message("xmlui.administrative.handle.ManageExternalHandles.edit_handle");
	private static final Message T_list_head =
			message("xmlui.administrative.handle.ManageExternalHandles.list_head");
	private static final Message T_list_help=
			message("xmlui.administrative.handle.ManageExternalHandles.list_help");
	private static final Message T_list_filtered_help=
			message("xmlui.administrative.handle.ManageExternalHandles.list_filtered_help");
	private static final Message T_no_results=
			message("xmlui.administrative.handle.ManageExternalHandles.para_no_results");
	private static final Message T_search_url=
			message("xmlui.administrative.handle.ManageExternalHandles.button_search_url");
	private static final Message T_search_pid=
			message("xmlui.administrative.handle.ManageExternalHandles.button_search_pid");
	private static final Message T_handle =
			message("xmlui.administrative.handle.general.handle");
	private static final Message T_url =
			message("xmlui.administrative.handle.general.url");
	private static final Message T_return =
			message("xmlui.general.return");

	private Request request = null;
	private static Logger log = Logger
			.getLogger(ManageExternalHandles.class);
	
	private static final String PAGE_NUM_PLACEHOLDER = "{pageNum}";
	private static final String PAGE_KEY = "page";
	private static final int DEFAULT_PAGE = 1;
	private static final int[] RESULTS_PER_PAGE_PROGRESSION = { 5, 10, 20, 40, 60, 80, 100 };
	private static final String RESULTS_PER_PAGE_KEY = "rpp";
	private static final int DEFAULT_RESULTS_PER_PAGE = 10;
	public static final String HANDLES_URL_BASE = "handles";
	public static final String EDIT_EXTERNAL = "edit_external";
	

	public void addPageMeta(PageMeta pageMeta) throws WingException {
		pageMeta.addMetadata("title").addContent(T_title);
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(null, T_trail);
		pageMeta.addMetadata("include-library", "handles");
	}

	@Override
	public void addBody(Body body) throws WingException,
			SQLException {

		// Page creation
		Division div = body.addDivision("main-div");

		// Handle list heading
		div.setHead(T_head);

		// Handle list div
		Division hdiv = div.addDivision("handle-list", "well well-light");

		// Handle list heading
		hdiv.setHead(T_list_head);

		request = ObjectModelHelper.getRequest(objectModel);
		String prefix = parameters.getParameter("prefix", ConfigurationManager.getProperty("handle.prefix"));
		String filterUrl = parameters.getParameter("filter", null);
		PIDServiceEPICv2 pidService = null;
		try{
		    pidService = new PIDServiceEPICv2();
		}catch(Exception e){
		    log.error(e);
		}
		

		// Get parameters
		String pageParam = request.getParameter(PAGE_KEY);
		String resultsPerPageParam = request.getParameter(RESULTS_PER_PAGE_KEY);
		
		// Sanitize parameters
		int page = pageParam == null ? DEFAULT_PAGE : Integer
				.parseInt(pageParam);
		int resultsPerPage = resultsPerPageParam == null ? DEFAULT_RESULTS_PER_PAGE
				: Integer.parseInt(resultsPerPageParam);

		int resultCount = -1;
		try{
			//if we have filter then do a search otherwise list all
			if(StringUtils.isEmpty(filterUrl)) {
				resultCount = pidService.getCount(prefix);
			}else{
				resultCount = pidService.getResultCount(prefix, filterUrl);
			}
		}catch(Exception e){
		    log.error(e);
		}

		if(resultCount < 1){
			//no results
			hdiv.addPara(null, "alert alert-error").addContent(T_no_results);
			Division buttons = hdiv.addInteractiveDivision("buttons", HANDLES_URL_BASE, Division.METHOD_POST, "well");
			buttons.addHidden("administrative-continue").setValue(knot.getId());
			buttons.addPara().addButton("submit_cancel").setValue(T_return);
			return;
		}

		// Calculate
		int totalPages = (int) Math.ceil((double) resultCount / resultsPerPage);

		// Sanitize page parameter
		if (page < 1)
			page = 1;
		else if (page > totalPages)
			page = totalPages;

		// Compute offsets for current page
		int firstIndex = Math.max(0, (page - 1) * resultsPerPage + 1);
		int lastIndex = (page - 1) * resultsPerPage + resultsPerPage;

		// Sanitize lastIndex
		if (lastIndex > resultCount)
			lastIndex = resultCount;

        // Retrieve records
        java.util.List<PIDServiceEPICv2.Handle> handles = new LinkedList<>();
		try
        {
			//if we have filter then do a search otherwise list all
			if(StringUtils.isEmpty(filterUrl)) {
				handles = pidService.list(prefix, "1", resultsPerPage, page);
			}else{
				handles = pidService.findHandles(filterUrl, prefix, "1", resultsPerPage, page);
			}
        }
        catch (Exception e)
        {
            log.error(e);
        }

		// Handle list info
		if(StringUtils.isEmpty(filterUrl)) {
			hdiv.addPara(null, "alert alert-info").addContent(T_list_help.parameterize(prefix));
		}else{
			hdiv.addPara(null, "alert alert-info").addContent(T_list_filtered_help.parameterize(filterUrl, prefix));
		}

		// Number of results
		hdiv.addPara(T_head1_none.parameterize(firstIndex, lastIndex,
				resultCount));

		// Main form with table
		Division hform = hdiv.addInteractiveDivision("handle-list-form",
				HANDLES_URL_BASE, Division.METHOD_POST, "primary administrative");

		// List controls
		addListControls(hform, resultsPerPage);

		// Add pagination
		Division hdivtable = hform.addDivision("handle-list-paginated-div");

		HashMap<String, String> urlParameters = new HashMap<String, String>();

		urlParameters.put(EDIT_EXTERNAL, EDIT_EXTERNAL);
		urlParameters.put("administrative-continue", knot.getId());
		urlParameters.put(PAGE_KEY, PAGE_NUM_PLACEHOLDER);
		urlParameters.put(RESULTS_PER_PAGE_KEY, Integer.toString(resultsPerPage));
		hdivtable.setMaskedPagination(resultCount, firstIndex, lastIndex, page,
				totalPages, generateURL(HANDLES_URL_BASE, urlParameters));

		// Table of handles
		Table htable = hdivtable.addTable("handle-list-table", 1, 3);

		// Table headers
		Row hhead = htable.addRow(Row.ROLE_HEADER);

		hhead.addCellContent("");
		hhead.addCellContent(T_handle);
		hhead.addCellContent(T_url);

		// Table rows
		for (PIDServiceEPICv2.Handle h : handles) {
			Row hrow = htable.addRow(null, Row.ROLE_DATA, null);
			hrow.addCell().addRadio("handle_id")
					.addOption(false, "" + h.getHandle());
            if (h.getHandle() != null && !h.getHandle().isEmpty())
            {
                hrow.addCell().addXref(
                        HandleManager.getCanonicalForm(h.getHandle()),
                        h.getHandle(), "target_blank");
            }
            else
            {
                hrow.addCell().addContent(h.getHandle());
            }
            if (StringUtils.isBlank(h.getUrl()))
            {
                hrow.addCell().addContent(h.getHandle());
            }
            else
            {
                hrow.addCell()
                        .addXref(h.getUrl(), h.getUrl(), "target_blank");
            }
		}

		// Handle list action buttons
		Para hlactions = hform.addPara("handle-list-actions", null);

		hlactions.addButton("submit_add").setValue(T_new_external_handle);
		hlactions.addButton("submit_edit").setValue(T_edit_handle);
		hlactions.addButton("submit_delete").setValue(T_delete_handle);

		//View this menu only on "full" list
		if(StringUtils.isEmpty(filterUrl)) {
			org.dspace.app.xmlui.wing.element.List searchForm = hform.addList("handle-search-form", org.dspace.app.xmlui.wing.element.List.TYPE_FORM);
			Text text = searchForm.addItem().addText("text_search");
			text.setHelp("Enter url or pid suffix and press the appropriate button to perform search");
			searchForm.addItem().addButton("submit_search_url").setValue(T_search_url);
			searchForm.addItem().addButton("submit_search_pid").setValue(T_search_pid);
		}else{
			//add option to return otherwise
			hlactions.addButton("submit_cancel").setValue(T_return);
		}

		// Continuation for cocoon workflow
		hform.addHidden("administrative-continue").setValue(knot.getId());

	}

	private void addListControls(Division div, int resultsPerPage) throws WingException {
		Map<String, String> urlParameters = new HashMap<String, String>();
		urlParameters.put(EDIT_EXTERNAL, EDIT_EXTERNAL);
		urlParameters.put("administrative-continue", knot.getId());
		urlParameters.put(RESULTS_PER_PAGE_KEY,
				Integer.toString(resultsPerPage));

		Division searchControlsGear = div.addDivision("masked-page-control")
				.addDivision("search-controls-gear", "controls-gear-wrapper");
		org.dspace.app.xmlui.wing.element.List sortList = searchControlsGear
				.addList("sort-options",
						org.dspace.app.xmlui.wing.element.List.TYPE_SIMPLE,
						"gear-selection");

		// Create control to change number of results per page
		sortList.addItem("rpp-head", "gear-head").addContent("Results/Page");
		org.dspace.app.xmlui.wing.element.List rppOptions = sortList
				.addList("rpp-selections");
		for (int i : RESULTS_PER_PAGE_PROGRESSION) {
			urlParameters.put(PAGE_KEY, Integer.toString(1));
			urlParameters.put(RESULTS_PER_PAGE_KEY, Integer.toString(i));
			rppOptions.addItem(null, null).addXref(
					generateURL(HANDLES_URL_BASE, urlParameters),
					Integer.toString(i),
					"gear-option"
							+ (i == resultsPerPage ? " gear-option-selected"
									: ""));
		}
	}
}
