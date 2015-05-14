/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative.handle;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import cz.cuni.mff.ufal.dspace.handle.Handle;
import cz.cuni.mff.ufal.dspace.handle.HandleComparatorFactory;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.sort.SortOption;

/**
 * Manage handles page is the entry point for handle management. From here the
 * user may browse/search a the list of handles, they may also add new external
 * handles or select exiting handles to edit or delete.
 * 
 * @author Michal Josífko
 * modified for LINDAT/CLARIN
 */

public class ManageHandlesMain extends AbstractDSpaceTransformer {

	/** Language strings */
	private static final Message T_dspace_home = 
			message("xmlui.general.dspace_home");
	private static final Message T_head1_none = 
			message("xmlui.ArtifactBrowser.AbstractSearch.head1_none");
	private static final Message T_title =
			message("xmlui.administrative.handle.ManageHandlesMain.title");
	private static final Message T_head =
			message("xmlui.administrative.handle.ManageHandlesMain.head");
	private static final Message T_trail =
			message("xmlui.administrative.handle.ManageHandlesMain.trail");
	private static final Message T_new_external_handle =
			message("xmlui.administrative.handle.ManageHandlesMain.new_external_handle");
	private static final Message T_delete_handle =
			message("xmlui.administrative.handle.ManageHandlesMain.delete_handle");
	private static final Message T_edit_handle =
			message("xmlui.administrative.handle.ManageHandlesMain.edit_handle");
	private static final Message T_global_actions_head =
			message("xmlui.administrative.handle.ManageHandlesMain.global_actions_head");
	private static final Message T_global_actions_help =
			message("xmlui.administrative.handle.ManageHandlesMain.global_actions_help");
	private static final Message T_change_handle_prefix =
			message("xmlui.administrative.handle.ManageHandlesMain.change_handle_prefix");
	private static final Message T_list_head =
			message("xmlui.administrative.handle.ManageHandlesMain.list_head");
	private static final Message T_list_help=
			message("xmlui.administrative.handle.ManageHandlesMain.list_help");
	private static final Message T_yes =
			message("xmlui.administrative.handle.general.yes");
	private static final Message T_no =
			message("xmlui.administrative.handle.general.no");
	private static final Message T_handle =
			message("xmlui.administrative.handle.general.handle");
	private static final Message T_internal =
			message("xmlui.administrative.handle.general.internal");
	private static final Message T_url =
			message("xmlui.administrative.handle.general.url");
	private static final Message T_resource_type =
			message("xmlui.administrative.handle.general.resource_type");	
	private static final Message T_resource_id =
			message("xmlui.administrative.handle.general.resource_id");

	private Request request = null;
	private static Logger log = Logger
			.getLogger(ManageHandlesMain.class);
	
	private static final String PAGE_NUM_PLACEHOLDER = "{pageNum}";
	private static final String PAGE_KEY = "page";
	private static final int DEFAULT_PAGE = 1;
	private static final int[] RESULTS_PER_PAGE_PROGRESSION = { 5, 10, 20, 40, 60, 80, 100 };
	private static final String RESULTS_PER_PAGE_KEY = "rpp";
	private static final int DEFAULT_RESULTS_PER_PAGE = 10;
	private static final String[] SORT_BY_VALUES = { "id", "handle", "resource_id" };
	private static final String SORT_BY_KEY = "sort_by";
	private static final String DEFAULT_SORT_BY = "id";
	private static final String[] ORDER_VALUES = { SortOption.ASCENDING, SortOption.DESCENDING };
	private static final String ORDER_KEY = "order";
	private static final String DEFAULT_ORDER = SortOption.ASCENDING;
	public static final String HANDLES_URL_BASE = "handles";
	

	public void addPageMeta(PageMeta pageMeta) throws WingException {
		pageMeta.addMetadata("title").addContent(T_title);
		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		pageMeta.addTrailLink(null, T_trail);
		pageMeta.addMetadata("include-library", "handles");
	}

	@Override
	public void addBody(Body body) throws WingException,
			SQLException {

		request = ObjectModelHelper.getRequest(objectModel);
		Context context = new Context();

		// Get parameters
		String pageParam = request.getParameter(PAGE_KEY);
		String resultsPerPageParam = request.getParameter(RESULTS_PER_PAGE_KEY);
		String sortParam = request.getParameter(SORT_BY_KEY);
		String orderParam = request.getParameter(ORDER_KEY);
		
		String errorString = parameters.getParameter("errors",null);
		ArrayList<String> errors = new ArrayList<String>();
		if (errorString != null)
        {
            for (String error : errorString.split(","))
            {
                errors.add(error);
            }
        }

		// Sanitize parameters
		String sort = Arrays.asList(SORT_BY_VALUES).contains(sortParam) ? sortParam
				: DEFAULT_SORT_BY;
		String order = Arrays.asList(ORDER_VALUES).contains(orderParam) ? orderParam
				: DEFAULT_ORDER;
		int page = pageParam == null ? DEFAULT_PAGE : Integer
				.parseInt(pageParam);
		int resultsPerPage = resultsPerPageParam == null ? DEFAULT_RESULTS_PER_PAGE
				: Integer.parseInt(resultsPerPageParam);

		// Retrieve records
		java.util.List<Handle> handles = Handle.findAll(
			context);
		sortHandles(handles, sort, order);

		// Calculate
		int resultCount = handles.size();
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

		// Page creation		
		Division div = body.addDivision("main-div");
		
		// Handle list heading
		div.setHead(T_head);

		// Handle list div
		Division hdiv = div.addDivision("handle-list", "well well-light");

		// Handle list heading
		hdiv.setHead(T_list_head);

		// Handle list info
		hdiv.addPara(null, "alert alert-info").addContent(T_list_help);					

		// Number of results
		hdiv.addPara(T_head1_none.parameterize(firstIndex, lastIndex,
				resultCount));

		// Main form with table
		Division hform = hdiv.addInteractiveDivision("handle-list-form",
				HANDLES_URL_BASE, Division.METHOD_POST, "primary administrative");

		// List controls
		addListControls(hform, sort, order, resultsPerPage);

		// Add pagination
		Division hdivtable = hform.addDivision("handle-list-paginated-div");

		HashMap<String, String> urlParameters = new HashMap<String, String>();

		urlParameters.put(PAGE_KEY, PAGE_NUM_PLACEHOLDER);
		urlParameters.put(RESULTS_PER_PAGE_KEY, Integer.toString(resultsPerPage));
		hdivtable.setMaskedPagination(resultCount, firstIndex, lastIndex, page,
				totalPages, generateURL(HANDLES_URL_BASE, urlParameters));

		// Table of handles
		Table htable = hdivtable.addTable("handle-list-table", 1, 6);

		// Table headers
		Row hhead = htable.addRow(Row.ROLE_HEADER);

		hhead.addCellContent("");
		hhead.addCellContent(T_handle);
		hhead.addCellContent(T_internal);
		hhead.addCellContent(T_url);
		hhead.addCellContent(T_resource_type);
		hhead.addCellContent(T_resource_id);

		// Table rows
		for (int i = firstIndex - 1; i < lastIndex && i < handles.size(); i++) {
			if ( i < 0 ) {
				break;
			}
			Handle h = handles.get(i);
			Row hrow = htable.addRow(null, Row.ROLE_DATA, null);
			hrow.addCell().addRadio("handle_id")
					.addOption(false, "" + h.getID());
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
            hrow.addCell().addContent(h.isInternalResource() ? T_yes : T_no);
            if (h.getHandle() != null && !h.getHandle().isEmpty())
            {
                String resolvedURL = HandleManager.resolveToURL(context,
                        h.getHandle());
                hrow.addCell()
                        .addXref(resolvedURL, resolvedURL, "target_blank");
            }
            else
            {
                hrow.addCell().addContent(h.getHandle());
            }
			String resourceType = h.getResourceTypeID() < 0 ? null : Constants.typeText[h.getResourceTypeID()]; 
			hrow.addCell().addContent(resourceType);
			String resourceID = h.getResourceID() < 0 ? null : String.valueOf(h.getResourceID());
			hrow.addCell().addContent(resourceID); 
		}

		// Handle list action buttons
		Para hlactions = hform.addPara("handle-list-actions", null);

		hlactions.addHidden("administrative-continue").setValue(knot.getId());
		hlactions.addButton("submit_add").setValue(T_new_external_handle);
		hlactions.addButton("submit_edit").setValue(T_edit_handle);
		hlactions.addButton("submit_delete").setValue(T_delete_handle);

		
		
		// Global actions div
		Division hgdiv = div.addDivision("handle-global");

		// Global actions heading
		hgdiv.setHead(T_global_actions_head);

		// Global actions info
		hgdiv.addPara(null, "alert alert-info")
				.addContent(T_global_actions_help);
		
		Division hgform = hgdiv.addInteractiveDivision("handle-global-actions-form",
				HANDLES_URL_BASE, Division.METHOD_POST, "primary administrative");

		// Global action buttons
		Para hgactions = hgform.addPara("handle-global-actions", "");

		hgactions.addButton("submit_change_prefix").setValue(
				T_change_handle_prefix);

		// Close database connection
		context.complete();

		// Continuation for cocoon workflow
		hform.addHidden("administrative-continue").setValue(knot.getId());
		hgform.addHidden("administrative-continue").setValue(knot.getId());

	}

	private void addListControls(Division div, String sort, String order,
			int resultsPerPage) throws WingException {
		Map<String, String> urlParameters = new HashMap<String, String>();
		urlParameters.put(SORT_BY_KEY, sort);
		urlParameters.put(ORDER_KEY, order);
		urlParameters.put(RESULTS_PER_PAGE_KEY,
				Integer.toString(resultsPerPage));

		Division searchControlsGear = div.addDivision("masked-page-control")
				.addDivision("search-controls-gear", "controls-gear-wrapper");
		org.dspace.app.xmlui.wing.element.List sortList = searchControlsGear
				.addList("sort-options",
						org.dspace.app.xmlui.wing.element.List.TYPE_SIMPLE,
						"gear-selection");

		// Create control to change column for sorting
		sortList.addItem("sort-head", "gear-head first").addContent("Sort by");
		org.dspace.app.xmlui.wing.element.List sortByOptions = sortList
				.addList("sort-selections");

		for (String value : SORT_BY_VALUES) {
			urlParameters.put(SORT_BY_KEY, value);
			sortByOptions.addItem(null, null).addXref(
					generateURL(HANDLES_URL_BASE, urlParameters),
					value,
					"gear-option"
							+ (value.equals(sort) ? " gear-option-selected"
									: ""));
		}
		urlParameters.put(SORT_BY_KEY, sort);

		// Create control to change ascending / descending order
		sortList.addItem("order-head", "gear-head").addContent("Order By");
		org.dspace.app.xmlui.wing.element.List ordOptions = sortList
				.addList("order-selections");

		for (String value : ORDER_VALUES) {
			urlParameters.put(ORDER_KEY, value);
			ordOptions.addItem(null, null).addXref(
					generateURL(HANDLES_URL_BASE, urlParameters),
					value,
					"gear-option"
							+ (value.equals(order) ? " gear-option-selected"
									: ""));
		}
		urlParameters.put(ORDER_KEY, order);

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
		urlParameters.put(RESULTS_PER_PAGE_KEY,
				Integer.toString(resultsPerPage));

	}

	private void sortHandles(java.util.List<Handle> handles, final String sort,
			final String order) {
		if (sort.equals("id")) {
			Collections.sort(handles,
					HandleComparatorFactory.createComparatorByID(order));
		} else if (sort.equals("handle")) {
			Collections.sort(handles,
					HandleComparatorFactory.createComparatorByHandle(order));
		} else if (sort.equals("resource_id")) {
			Collections
					.sort(handles, HandleComparatorFactory
							.createComparatorByResourceID(order));
		}
	}
}
