/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.util.HashUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.*;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.*;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.*;

/**
 * Implements all the browse functionality (browse by title, subject, authors,
 * etc.) The types of browse available are configurable by the implementor. See
 * dspace.cfg and documentation for instructions on how to configure.
 *
 * @author Graham Triggs
 */
public class ConfigurableBrowse extends AbstractDSpaceTransformer implements
        CacheableProcessingComponent
{
    private static final Logger log = Logger.getLogger(ConfigurableBrowse.class);

    /**
     * Static Messages for common text
     */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_go = message("xmlui.general.go");

    private static final Message T_update = message("xmlui.general.update");

    private static final Message T_choose_month = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.choose_month");

    private static final Message T_choose_year = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.choose_year");

    private static final Message T_jump_year = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.jump_year");

    private static final Message T_jump_year_help = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.jump_year_help");

    private static final Message T_jump_select = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.jump_select");

    private static final Message T_starts_with = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.starts_with");

    private static final Message T_starts_with_help = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.starts_with_help");

    private static final Message T_sort_by = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.sort_by");

    private static final Message T_order = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.order");

    private static final Message T_no_results= message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.no_results");

    private static final Message T_rpp = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.rpp");

    private static final Message T_order_asc = message("xmlui.ArtifactBrowser.ConfigurableBrowse.order.asc");

    private static final Message T_order_desc = message("xmlui.ArtifactBrowser.ConfigurableBrowse.order.desc");

    private static final String BROWSE_URL_BASE = "browse";

    /**
     * These variables dictate when the drop down list of years is to break from
     * 1 year increments, to 5 year increments, to 10 year increments, and
     * finally to stop.
     */
    private static final int ONE_YEAR_LIMIT = 10;

    private static final int FIVE_YEAR_LIMIT = 30;

    private static final int TEN_YEAR_LIMIT = 100;

    /** The options for results per page */
    private static final int[] RESULTS_PER_PAGE_PROGRESSION = {5,10,20,40,60,80,100};
    private int currentOffset = 0;
    private String currentOrder;
    /** Cached validity object */
    private SourceValidity validity;

    /** Cached UI parameters, results and messages */
    private BrowseParams userParams;

    private BrowseInfo browseInfo;

    private Message titleMessage = null;
    private Message trailMessage = null;

    protected ChoiceAuthorityService choicheAuthorityService = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, parameters);

        //Verify if we have received valid parameters
        try {
            getUserParams();
        } catch (ResourceNotFoundException e) {
            throw new BadRequestException("Invalid parameters");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } catch (UIException e) {
            throw new RuntimeException(e);
        }
    }

    public Serializable getKey()
    {
        try
        {
            BrowseParams params = getUserParams();
            
            String key = params.getKey();
            
            if (key != null)
            {
                DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
                if (dso != null)
                {
                    key += "-" + dso.getHandle();
                }

                return HashUtil.hash(key);
            }
        }
        catch (RuntimeException re)
        {
            throw re;    
        }
        catch (Exception e)
        {
            return "0";
        }
        
        return "0";
    }

    public SourceValidity getValidity()
    {
        if (validity == null)
        {
            try
            {
                Context.Mode originalMode = context.getCurrentMode();
                context.setMode(Context.Mode.READ_ONLY);

                DSpaceValidity validity = new DSpaceValidity();
                DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

                if (dso != null)
                {
                    validity.add(context, dso);
                }
                
                BrowseInfo info = getBrowseInfo();
                validity.add("total:"+info.getTotal());
                validity.add("start:"+info.getStart());
                
                // Are we browsing items, or unique metadata?
                if (isItemBrowse(info))
                {
                    // Add the browse items to the validity
                    for (Item item : (java.util.List<Item>) info.getResults())
                    {
                        validity.add(context, item);
                    }
                }
                else
                {
                    // Add the metadata to the validity
                    for (String[] singleEntry : browseInfo.getStringResults())
                    {
                        validity.add(StringUtils.join(singleEntry,"#"));
                    }
                }

                this.validity =  validity.complete();

                context.setMode(originalMode);
            }
            catch (RuntimeException re)
            {
                throw re;
            }
            catch (Exception e)
            {
                return null;
            }

            if (this.validity != null)
            {
                log.info(LogManager.getHeader(context, "browse", this.validity.toString()));
            }
        }
        
        return this.validity;
    }

    /**
     * Add Page metadata.
     */
    public void addPageMeta(PageMeta pageMeta) throws SAXException, WingException, UIException,
            SQLException, IOException, AuthorizeException
    {
        Context.Mode originalMode = context.getCurrentMode();
        context.setMode(Context.Mode.READ_ONLY);

        BrowseInfo info = getBrowseInfo();

        pageMeta.addMetadata("title").addContent(getTitleMessage(info));

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
        if (dso != null)
        {
            HandleUtil.buildHandleTrail(context, dso, pageMeta, contextPath, true);
        }

        pageMeta.addTrail().addContent(getTrailMessage(info));

        context.setMode(originalMode);
    }

    /**
     * Add the browse-title division.
     */
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException,
            IOException, AuthorizeException
    {
        Context.Mode originalMode = context.getCurrentMode();
        context.setMode(Context.Mode.READ_ONLY);

        BrowseParams params = null;

        try {
            params = getUserParams();
        } catch (ResourceNotFoundException e) {
           HttpServletResponse response = (HttpServletResponse)objectModel
		.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
	    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        BrowseInfo info = getBrowseInfo();
        currentOrder = params.scope.getOrder();
        if(info == null)
        {
            HttpServletResponse response = (HttpServletResponse)objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
 	        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String type = info.getBrowseIndex().getName();

        // Build the DRI Body
        Division div = body.addDivision("browse-by-" + type, "primary");

        div.setHead(getTitleMessage(info));

        // Build the internal navigation (jump lists)
        addBrowseJumpNavigation(div, info, params);
        
        // Build the sort and display controls
        addBrowseControls(div, info, params);

        // This div will hold the browsing results
        Division results = div.addDivision("browse-by-" + type + "-results", "primary");

        // If there are items to browse, add the pagination
        int itemsTotal = info.getTotal();
        currentOffset=info.getOffset();

        if (itemsTotal > 0)
        {
            //results.setSimplePagination(itemsTotal, firstItemIndex, lastItemIndex, previousPage, nextPage)
            results.setSimplePagination(itemsTotal, browseInfo.getOverallPosition() + 1,
                    browseInfo.getOverallPosition() + browseInfo.getResultCount(), getPreviousPageURL(
                            params, info), getNextPageURL(params, info));

            // Reference all the browsed items
            ReferenceSet referenceSet = results.addReferenceSet("browse-by-" + type,
                    ReferenceSet.TYPE_SUMMARY_LIST, type, null);

            // Are we browsing items, or unique metadata?
            if (isItemBrowse(info))
            {
                // Add the items to the browse results
                for (Item item : (java.util.List<Item>) info.getResults())
                {
                    referenceSet.addReference(item);
                }
            }
            else    // browsing a list of unique metadata entries
            {
                // Create a table for the results
                Table singleTable = results.addTable("browse-by-" + type + "-results",
                        browseInfo.getResultCount() + 1, 1);
            
                // Add the column heading
                singleTable.addRow(Row.ROLE_HEADER).addCell().addContent(
                        message("xmlui.ArtifactBrowser.ConfigurableBrowse." + type + ".column_heading"));

                // Iterate each result
                for (String[] singleEntry : browseInfo.getStringResults())
                {
                    // Create a Map of the query parameters for the link
                    Map<String, String> queryParams = new HashMap<String, String>();
                    queryParams.put(BrowseParams.TYPE, encodeForURL(type));
                    if (singleEntry[1] != null)
                    {
                        queryParams.put(BrowseParams.FILTER_VALUE[1], encodeForURL(
                            singleEntry[1]));
                    }
                    else
                    {
                        queryParams.put(BrowseParams.FILTER_VALUE[0], encodeForURL(
                            singleEntry[0]));
                    }

                    // Create an entry in the table, and a linked entry
                    Cell cell = singleTable.addRow().addCell();
                    cell.addXref(super.generateURL(BROWSE_URL_BASE, queryParams),
                          singleEntry[0]);
                    if (StringUtils.isNotEmpty(singleEntry[2]))
                    {
                        cell.addContent(" ["+singleEntry[2]+"]");
                    }
                }  
            }
        }
        else 
        {
            results.addPara(T_no_results);
        }

        context.setMode(originalMode);
    }

    /**
     * Recycle
     */
    public void recycle()
    {
        this.validity = null;
        this.userParams = null;
        this.browseInfo = null;
        this.titleMessage = null;
        this.trailMessage = null;
        super.recycle();
    }

    /**
     * Makes the jump-list navigation for the results
     *
     * @param div
     * @param info
     * @param params
     * @throws WingException
     */
    private void addBrowseJumpNavigation(Division div, BrowseInfo info, BrowseParams params)
            throws WingException
    {
        // Prepare a Map of query parameters required for all links
        Map<String, String> queryParamsGET = new HashMap<String, String>();
        queryParamsGET.putAll(params.getCommonParametersEncoded());
        queryParamsGET.putAll(params.getControlParameters());

        Map<String, String> queryParamsPOST = new HashMap<String, String>();
        queryParamsPOST.putAll(params.getCommonParameters());
        queryParamsPOST.putAll(params.getControlParameters());

        // Navigation aid (really this is a poor version of pagination)
        Division jump = div.addInteractiveDivision("browse-navigation", BROWSE_URL_BASE,
                Division.METHOD_POST, "secondary navigation");

        // Add all the query parameters as hidden fields on the form
        for (Map.Entry<String, String> param : queryParamsPOST.entrySet())
        {
            jump.addHidden(param.getKey()).setValue(param.getValue());
        }

        // If this is a date based browse, render the date navigation
        if (isSortedByDate(info))
        {
            Para jumpForm = jump.addPara();

            // Create a select list to choose a month
            jumpForm.addContent(T_jump_select);
            Select month = jumpForm.addSelect(BrowseParams.MONTH);
            month.addOption(false, "-1", T_choose_month);
            for (int i = 1; i <= 12; i++)
            {
                month.addOption(false, String.valueOf(i), DCDate.getMonthName(i, Locale
                        .getDefault()));
            }

            // Create a select list to choose a year
            Select year = jumpForm.addSelect(BrowseParams.YEAR);
            year.addOption(false, "-1", T_choose_year);
            int currentYear = DCDate.getCurrent().getYear();
            int i = currentYear;
            
            // Calculate where to move from 1, 5 to 10 year jumps
            int oneYearBreak = ((currentYear - ONE_YEAR_LIMIT) / 5) * 5;
            int fiveYearBreak = ((currentYear - FIVE_YEAR_LIMIT) / 10) * 10;
            int tenYearBreak = (currentYear - TEN_YEAR_LIMIT);
            do
            {
                year.addOption(false, String.valueOf(i), String.valueOf(i));

                if (i <= fiveYearBreak)
                {
                    i -= 10;
                }
                else if (i <= oneYearBreak)
                {
                    i -= 5;
                }
                else
                {
                    i--;
                }
            }
            while (i > tenYearBreak);

            // Create a free text entry box for the year
            jumpForm = jump.addPara();
            jumpForm.addContent(T_jump_year);
            jumpForm.addText(BrowseParams.STARTS_WITH).setHelp(T_jump_year_help);
            
            jumpForm.addButton("submit").setValue(T_go);
        }
        else
        {
            // Create a clickable list of the alphabet
            List jumpList = jump.addList("jump-list", List.TYPE_SIMPLE, "alphabet");
            
            // browse params for each letter are all the query params
            // WITHOUT the second-stage browse value, and add STARTS_WITH.
            Map<String, String> letterQuery = new HashMap<String, String>(queryParamsGET);
            for (String valueKey : BrowseParams.FILTER_VALUE)
            {
                letterQuery.remove(valueKey);
            }
            letterQuery.put(BrowseParams.STARTS_WITH, "0");
            jumpList.addItemXref(super.generateURL(BROWSE_URL_BASE, letterQuery), "0-9");
            
            for (char c = 'A'; c <= 'Z'; c++)
            {
                letterQuery.put(BrowseParams.STARTS_WITH, Character.toString(c));
                jumpList.addItemXref(super.generateURL(BROWSE_URL_BASE, letterQuery), Character
                        .toString(c));
            }

            // Create a free text field for the initial characters
            Para jumpForm = jump.addPara();
            jumpForm.addContent(T_starts_with);
            jumpForm.addText(BrowseParams.STARTS_WITH).setHelp(T_starts_with_help);
            
            jumpForm.addButton("submit").setValue(T_go);
        }
    }

    /**
     * Add the controls to changing sorting and display options.
     *
     * @param div
     * @param info
     * @param params
     * @throws WingException
     */
    private void addBrowseControls(Division div, BrowseInfo info, BrowseParams params)
            throws WingException
    {
        // Prepare a Map of query parameters required for all links
        Map<String, String> queryParams = new HashMap<String, String>();

        queryParams.putAll(params.getCommonParameters());

        Division controls = div.addInteractiveDivision("browse-controls", BROWSE_URL_BASE+(StringUtils.contains(BROWSE_URL_BASE,"?")?"&resetOffset=true":"?resetOffset=true"),
                Division.METHOD_POST, "browse controls");

        // Add all the query parameters as hidden fields on the form
        for (Map.Entry<String, String> param : queryParams.entrySet())
        {
            controls.addHidden(param.getKey()).setValue(param.getValue());
        }

        Para controlsForm = controls.addPara();

        // If we are browsing a list of items
        if (isItemBrowse(info)) //  && info.isSecondLevel()
        {
            try
            {
                // Create a drop down of the different sort columns available
                Set<SortOption> sortOptions = SortOption.getSortOptions();
                
                // Only generate the list if we have multiple columns
                if (sortOptions.size() > 1)
                {
                    controlsForm.addContent(T_sort_by);
                    Select sortSelect = controlsForm.addSelect(BrowseParams.SORT_BY);
    
                    for (SortOption so : sortOptions)
                    {
                        if (so.isVisible())
                        {
                            sortSelect.addOption(so.equals(info.getSortOption()), so.getNumber(),
                                    message("xmlui.ArtifactBrowser.ConfigurableBrowse.sort_by." + so.getName()));
                        }
                    }
                }
            }
            catch (SortException se)
            {
                throw new WingException("Unable to get sort options", se);
            }
        }

        // Create a control to changing ascending / descending order
        controlsForm.addContent(T_order);
        Select orderSelect = controlsForm.addSelect(BrowseParams.ORDER);
        orderSelect.addOption("ASC".equals(params.scope.getOrder()), "ASC", T_order_asc);
        orderSelect.addOption("DESC".equals(params.scope.getOrder()), "DESC", T_order_desc);

        // Create a control for the number of records to display
        controlsForm.addContent(T_rpp);
        Select rppSelect = controlsForm.addSelect(BrowseParams.RESULTS_PER_PAGE);
        
        for (int i : RESULTS_PER_PAGE_PROGRESSION)
        {
            rppSelect.addOption((i == info.getResultsPerPage()), i, Integer.toString(i));
 
        }

        // Create a control for the number of authors per item to display
        // FIXME This is currently disabled, as the supporting functionality
        // is not currently present in xmlui
        //if (isItemBrowse(info))
        //{
        //    controlsForm.addContent(T_etal);
        //    Select etalSelect = controlsForm.addSelect(BrowseParams.ETAL);
        //
        //    etalSelect.addOption((info.getEtAl() < 0), 0, T_etal_all);
        //    etalSelect.addOption(1 == info.getEtAl(), 1, Integer.toString(1));
        //
        //    for (int i = 5; i <= 50; i += 5)
        //    {
        //        etalSelect.addOption(i == info.getEtAl(), i, Integer.toString(i));
        //    }
        //}

        controlsForm.addButton("update").setValue(T_update);
    }

    /**
     * The URL query string of of the previous page.
     *
     * Note: the query string does not start with a "?" or "&" those need to be
     * added as appropriate by the caller.
     */
    private String getPreviousPageURL(BrowseParams params, BrowseInfo info) throws SQLException,
            UIException
    {
        // Don't create a previous page link if this is the first page
        if (info.isFirst())
        {
            return null;
        }

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.putAll(params.getCommonParametersEncoded());
        parameters.putAll(params.getControlParameters());

        if (info.hasPrevPage())
        {
            parameters.put(BrowseParams.OFFSET, encodeForURL(String.valueOf(info.getPrevOffset())));
        }

        return super.generateURL(BROWSE_URL_BASE, parameters);

    }

    /**
     * The URL query string of of the next page.
     *
     * Note: the query string does not start with a "?" or "&" those need to be
     * added as appropriate by the caller.
     */
    private String getNextPageURL(BrowseParams params, BrowseInfo info) throws SQLException,
            UIException
    {
        // Don't create a next page link if this is the last page
        if (info.isLast())
        {
            return null;
        }

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.putAll(params.getCommonParametersEncoded());
        parameters.putAll(params.getControlParameters());

        if (info.hasNextPage())
        {
            parameters.put(BrowseParams.OFFSET, encodeForURL(String.valueOf(info.getNextOffset())));
        }

        return super.generateURL(BROWSE_URL_BASE, parameters);
    }

    /**
     * Get the query parameters supplied to the browse.
     *
     * @return
     * @throws SQLException
     * @throws UIException
     */
    private BrowseParams getUserParams() throws SQLException, UIException, ResourceNotFoundException, IllegalArgumentException {

        if (this.userParams != null)
        {
            return this.userParams;
        }

        Context context = ContextUtil.obtainContext(objectModel);
        Request request = ObjectModelHelper.getRequest(objectModel);

        BrowseParams params = new BrowseParams();

        params.month = request.getParameter(BrowseParams.MONTH);
        params.year = request.getParameter(BrowseParams.YEAR);
        params.etAl = RequestUtils.getIntParameter(request, BrowseParams.ETAL);

        params.scope = new BrowserScope(context);

        // Are we in a community or collection?
        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
        if (dso instanceof Community)
        {
            params.scope.setCommunity((Community) dso);
        }
        if (dso instanceof Collection)
        {
            params.scope.setCollection((Collection) dso);
        }

        try
        {
            String type   = request.getParameter(BrowseParams.TYPE);
            int    sortBy = RequestUtils.getIntParameter(request, BrowseParams.SORT_BY);

            if(!request.getParameters().containsKey("type"))
            {
                // default to first browse index.
                String defaultBrowseIndex = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("webui.browse.index.1");
                if(defaultBrowseIndex != null)
                {
                    type = defaultBrowseIndex.split(":")[0];
                }
            }

            BrowseIndex bi = BrowseIndex.getBrowseIndex(type);
            if (bi == null)
            {
                throw new ResourceNotFoundException("Browse index " + type + " not found");
            }
            
            // If we don't have a sort column
            if (sortBy == -1)
            {
                // Get the default one
                SortOption so = bi.getSortOption();
                if (so != null)                             
                {
                    sortBy = so.getNumber();
                }
            }
            else if (bi.isItemIndex() && !bi.isInternalIndex())
            {
                try
                {
                    // If a default sort option is specified by the index, but it isn't
                    // the same as sort option requested, attempt to find an index that
                    // is configured to use that sort by default
                    // This is so that we can then highlight the correct option in the navigation
                    SortOption bso = bi.getSortOption();
                    SortOption so = SortOption.getSortOption(sortBy);
                    if ( bso != null && !bso.equals(so))
                    {
                        BrowseIndex newBi = BrowseIndex.getBrowseIndex(so);
                        if (newBi != null)
                        {
                            bi   = newBi;
                            type = bi.getName();
                        }
                    }
                }
                catch (SortException se)
                {
                    throw new UIException("Unable to get sort options", se);
                }
            }
            
            params.scope.setBrowseIndex(bi);
            params.scope.setSortBy(sortBy);
            
            params.scope.setJumpToItem(RequestUtils.getIntParameter(request, BrowseParams.JUMPTO_ITEM));
            params.scope.setOrder(request.getParameter(BrowseParams.ORDER));
            updateOffset(request, params);
            params.scope.setResultsPerPage(RequestUtils.getIntParameter(request, BrowseParams.RESULTS_PER_PAGE));
            params.scope.setStartsWith(decodeFromURL(request.getParameter(BrowseParams.STARTS_WITH)));
            String filterValue = request.getParameter(BrowseParams.FILTER_VALUE[0]);
            if (filterValue == null)
            {
                filterValue = request.getParameter(BrowseParams.FILTER_VALUE[1]);
                params.scope.setAuthorityValue(filterValue);
            }
            
            params.scope.setFilterValue(filterValue);
            params.scope.setJumpToValue(decodeFromURL(request.getParameter(BrowseParams.JUMPTO_VALUE)));
            params.scope.setJumpToValueLang(decodeFromURL(request.getParameter(BrowseParams.JUMPTO_VALUE_LANG)));
            params.scope.setFilterValueLang(decodeFromURL(request.getParameter(BrowseParams.FILTER_VALUE_LANG)));

            // Filtering to a value implies this is a second level browse
            if (params.scope.getFilterValue() != null)
            {
                params.scope.setBrowseLevel(1);
            }

            // if year and perhaps month have been selected, we translate these
            // into "startsWith"
            // if startsWith has already been defined then it is overwritten
            if (params.year != null && !"".equals(params.year) && !"-1".equals(params.year))
            {
                String startsWith = params.year;
                if ((params.month != null) && !"-1".equals(params.month) && !"".equals(params.month))
                {
                    // subtract 1 from the month, so the match works
                    // appropriately
                    if ("ASC".equals(params.scope.getOrder()))
                    {
                        params.month = Integer.toString((Integer.parseInt(params.month) - 1));
                    }

                    // They've selected a month as well
                    if (params.month.length() == 1)
                    {
                        // Ensure double-digit month number
                        params.month = "0" + params.month;
                    }

                    startsWith = params.year + "-" + params.month;

                    if ("ASC".equals(params.scope.getOrder()))
                    {
                        startsWith = startsWith + "-32";
                    }
                }

                params.scope.setStartsWith(startsWith);
            }
        }
        catch (BrowseException bex)
        {
            throw new UIException("Unable to create browse parameters", bex);
        }

        this.userParams = params;
        return params;
    }

    private void updateOffset(Request request, BrowseParams params) {
        int configuredOffset=-1;
        boolean retainOffset = false;
        if (request.getParameters().containsKey("update")) {
            configuredOffset = currentOffset;
            retainOffset=true;
        }
        int offset = RequestUtils.getIntParameter(request, BrowseParams.OFFSET);
        params.scope.setOffset(offset > 0 ? offset : 0);
        if (retainOffset) {
            params.scope.setOffset(configuredOffset);
        }
    }

    /**
     * Get the results of the browse. If the results haven't been generated yet,
     * then this will perform the browse.
     *
     * @return
     * @throws SQLException
     * @throws UIException
     */
    private BrowseInfo getBrowseInfo() throws SQLException, UIException
    {
        if (this.browseInfo != null)
        {
            return this.browseInfo;
        }

        Context context = ContextUtil.obtainContext(objectModel);

        // Get the parameters we will use for the browse
        // (this includes a browse scope)
        BrowseParams params = null;
        try {
            params = getUserParams();
        } catch (ResourceNotFoundException e) {
            return null;
        }
        String paramsOrder = params.scope.getOrder();
        boolean orderingUpdated = !StringUtils.equals(currentOrder, paramsOrder);
        if (orderingUpdated) {
            if (ObjectModelHelper.getRequest(objectModel).getParameters().containsKey("resetOffset")) {
                params.scope.setOffset(0);
            }
        }
        try
        {
            // Create a new browse engine, and perform the browse
            BrowseEngine be = new BrowseEngine(context);
            this.browseInfo = be.browse(params.scope);

            // figure out the setting for author list truncation
            if (params.etAl < 0)
            {
                // there is no limit, or the UI says to use the default
                int etAl = DSpaceServicesFactory.getInstance().getConfigurationService().getIntProperty("webui.browse.author-limit");
                if (etAl != 0)
                {
                    this.browseInfo.setEtAl(etAl);
                }

            }
            else if (params.etAl == 0) // 0 is the user setting for unlimited
            {
                this.browseInfo.setEtAl(-1); // but -1 is the application
                // setting for unlimited
            }
            else
            // if the user has set a limit
            {
                this.browseInfo.setEtAl(params.etAl);
            }
        }
        catch (BrowseException bex)
        {
            throw new UIException("Unable to process browse", bex);
        }

        return this.browseInfo;
    }

    /**
     * Is this a browse on a list of items, or unique metadata values?
     *
     * @param info
     * @return
     */
    private boolean isItemBrowse(BrowseInfo info)
    {
        return info.getBrowseIndex().isItemIndex() || info.isSecondLevel();
    }
    
    /**
     * Is this browse sorted by date?
     * @param info
     * @return
     */
    private boolean isSortedByDate(BrowseInfo info)
    {
        return info.getSortOption().isDate() ||
            (info.getBrowseIndex().isDate() && info.getSortOption().isDefault());
    }

    private Message getTitleMessage(BrowseInfo info)
    {
        if (titleMessage == null)
        {
            BrowseIndex bix = info.getBrowseIndex();

            // For a second level browse (ie. items for author),
            // get the value we are focussing on (ie. author).
            // (empty string if none).
            String value = "";
            if (info.hasValue())
            {
                if (bix.isAuthorityIndex())
                {
                    String fk = bix.getMetadata(0).replace(".", "_");
                    value = "\""+choicheAuthorityService.getLabel(fk, info.getValue(), null)+"\"";
                }
                else
                {
                    value = "\"" + info.getValue() + "\"";
                }
            }

            // Get the name of any scoping element (collection / community)
            String scopeName = "";
            
            if (info.getBrowseContainer() != null)
            {
                scopeName = info.getBrowseContainer().getName();
            }
            else
            {
                scopeName = "";
            }
            
            if (bix.isMetadataIndex())
            {
                titleMessage = message("xmlui.ArtifactBrowser.ConfigurableBrowse.title.metadata." + bix.getName())
                        .parameterize(scopeName, value);
            }
            else if (info.getSortOption() != null)
            {
                titleMessage = message("xmlui.ArtifactBrowser.ConfigurableBrowse.title.item." + info.getSortOption().getName())
                        .parameterize(scopeName, value);
            }
            else
            {
                titleMessage = message("xmlui.ArtifactBrowser.ConfigurableBrowse.title.item." + bix.getSortOption().getName())
                        .parameterize(scopeName, value);
            }
        }
        
        return titleMessage;
    }

    private Message getTrailMessage(BrowseInfo info)
    {
        if (trailMessage == null)
        {
            BrowseIndex bix = info.getBrowseIndex();

            // Get the name of any scoping element (collection / community)
            String scopeName = "";
            
            if (info.getBrowseContainer() != null)
            {
                scopeName = info.getBrowseContainer().getName();
            }
            else
            {
                scopeName = "";
            }

            if (bix.isMetadataIndex())
            {
                trailMessage = message("xmlui.ArtifactBrowser.ConfigurableBrowse.trail.metadata." + bix.getName())
                        .parameterize(scopeName);
            }
            else if (info.getSortOption() != null)
            {
                trailMessage = message("xmlui.ArtifactBrowser.ConfigurableBrowse.trail.item." + info.getSortOption().getName())
                        .parameterize(scopeName);
            }
            else
            {
                trailMessage = message("xmlui.ArtifactBrowser.ConfigurableBrowse.trail.item." + bix.getSortOption().getName())
                        .parameterize(scopeName);
            }
        }
        
        return trailMessage;
    }
}

/*
 * Helper class to track browse parameters
 */
class BrowseParams
{
    String month;

    String year;

    int etAl;

    BrowserScope scope;

    static final String MONTH = "month";

    static final String YEAR = "year";

    static final String ETAL = "etal";

    static final String TYPE = "type";

    static final String JUMPTO_ITEM = "focus";

    static final String JUMPTO_VALUE = "vfocus";

    static final String JUMPTO_VALUE_LANG = "vfocus_lang";

    static final String ORDER = "order";

    static final String OFFSET = "offset";

    static final String RESULTS_PER_PAGE = "rpp";

    static final String SORT_BY = "sort_by";

    static final String STARTS_WITH = "starts_with";

    static final String[] FILTER_VALUE = new String[]{"value","authority"};

    static final String FILTER_VALUE_LANG = "value_lang";

    /*
     * Creates a map of the browse options common to all pages (type / value /
     * value language)
     */
    Map<String, String> getCommonParameters() throws UIException
    {
        Map<String, String> paramMap = new HashMap<String, String>();

        paramMap.put(BrowseParams.TYPE, scope.getBrowseIndex().getName());

        if (scope.getFilterValue() != null)
        {
            paramMap.put(scope.getAuthorityValue() != null?
                    BrowseParams.FILTER_VALUE[1]:BrowseParams.FILTER_VALUE[0], scope.getFilterValue());
        }
        if(StringUtils.isNotBlank(scope.getStartsWith())){
            paramMap.put(STARTS_WITH,scope.getStartsWith());
        }
        if (scope.getFilterValueLang() != null)
        {
            paramMap.put(BrowseParams.FILTER_VALUE_LANG, scope.getFilterValueLang());
        }

        return paramMap;
    }

    Map<String, String> getCommonParametersEncoded() throws UIException
    {
        Map<String, String> paramMap = getCommonParameters();
        Map<String, String> encodedParamMap = new HashMap<String, String>();

        for (Map.Entry<String, String> param : paramMap.entrySet())
        {
            encodedParamMap.put(param.getKey(), AbstractDSpaceTransformer.encodeForURL(param.getValue()));
        }

        return encodedParamMap;
    }


    /*
     * Creates a Map of the browse control options (sort by / ordering / results
     * per page / authors per item)
     */
    Map<String, String> getControlParameters() throws UIException
    {
        Map<String, String> paramMap = new HashMap<String, String>();

        paramMap.put(BrowseParams.SORT_BY, Integer.toString(this.scope.getSortBy()));
        paramMap
                .put(BrowseParams.ORDER, AbstractDSpaceTransformer.encodeForURL(this.scope.getOrder()));
        paramMap.put(BrowseParams.RESULTS_PER_PAGE, Integer
                .toString(this.scope.getResultsPerPage()));
        paramMap.put(BrowseParams.ETAL, Integer.toString(this.etAl));
        if (this.scope.hasStartsWith()) {
            paramMap.put(BrowseParams.STARTS_WITH, this.scope.getStartsWith());
        }

        return paramMap;
    }
    
    String getKey()
    {
        try
        {
            String key = "";
            
            key += "-" + scope.getBrowseIndex().getName();
            key += "-" + scope.getBrowseLevel();
            key += "-" + scope.getStartsWith();
            key += "-" + scope.getOrder();
            key += "-" + scope.getResultsPerPage();
            key += "-" + scope.getSortBy();
            key += "-" + scope.getSortOption().getNumber();
            key += "-" + scope.getOffset();
            key += "-" + scope.getJumpToItem();
            key += "-" + scope.getFilterValue();
            key += "-" + scope.getFilterValueLang();
            key += "-" + scope.getJumpToValue();
            key += "-" + scope.getJumpToValueLang();
            key += "-" + etAl;
    
            return key;
        }
        catch (RuntimeException re)
        {
            throw re;
        }
        catch (Exception e)
        {
            return null; // ignore exception and return no key
        }
    }
};
