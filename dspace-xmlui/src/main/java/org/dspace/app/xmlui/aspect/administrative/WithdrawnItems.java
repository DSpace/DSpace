/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.DSpaceValidity;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.utils.RequestUtils;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.ReferenceSet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.browse.BrowseEngine;
import org.dspace.browse.BrowseException;
import org.dspace.browse.BrowseIndex;
import org.dspace.browse.BrowseInfo;
import org.dspace.browse.BrowseItem;
import org.dspace.browse.BrowserScope;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.xml.sax.SAXException;

/**
 * Implements all the browse functionality (browse by title, subject, authors,
 * etc.) The types of browse available are configurable by the implementor. See
 * dspace.cfg and documentation for instructions on how to configure.
 *
 * based on class by Graham Triggs
 * modified for LINDAT/CLARIN
 */
public class WithdrawnItems extends AbstractDSpaceTransformer implements
        CacheableProcessingComponent
{
    private static final Logger log = Logger.getLogger(WithdrawnItems.class);

    /**
     * Static Messages for common text
     */
    private static final Message T_dspace_home = message("xmlui.general.dspace_home");

    private static final Message T_sort_by = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.sort_by");

    private static final Message T_order = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.order");

    private static final Message T_no_results= message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.no_results");

    private static final Message T_rpp = message("xmlui.ArtifactBrowser.ConfigurableBrowse.general.rpp");

    private static final Message T_order_asc = message("xmlui.ArtifactBrowser.ConfigurableBrowse.order.asc");

    private static final Message T_order_desc = message("xmlui.ArtifactBrowser.ConfigurableBrowse.order.desc");

    private static final Message T_head1_none = message("xmlui.ArtifactBrowser.AbstractSearch.head1_none");
    
    private static final String WITHDRAWN_URL_BASE = "withdrawn";

    /**
     * The options for results per page
     */
    private static final int[] RESULTS_PER_PAGE_PROGRESSION = {5,10,20,40,60,80,100};

    /** Cached validity object */
    private SourceValidity validity;

    /** Cached UI parameters, results and messages */
    private BrowseParams userParams;

    private BrowseInfo browseInfo;

    @Override
    public void setup(SourceResolver resolver, Map objectModel, String src, Parameters parameters) throws ProcessingException, SAXException, IOException {
        super.setup(resolver, objectModel, src, parameters);

        //Verify if we have received valid parameters
        try {
            getUserParams();
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
        catch (SQLException e)
        {
            log.error("Database error", e);
        }
        catch (UIException e)
        {
            log.error("UI error", e);
        }

        return "0";
    }

    public SourceValidity getValidity()
    {
        if (validity == null)
        {
            try
            {
                DSpaceValidity newValidity = new DSpaceValidity();
                DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

                if (dso != null)
                {
                    newValidity.add(dso);
                }

                BrowseInfo info = getBrowseInfo();
                newValidity.add("total:"+info.getTotal());
                newValidity.add("start:"+info.getStart());

                    // Add the browse items to the validity
                    for (BrowseItem item : (java.util.List<BrowseItem>) info.getResults())
                    {
                        newValidity.add(item);
                    }

                validity = newValidity.complete();
            }
            catch (SQLException e)
            {
                log.error("Database error", e);
            }
            catch (UIException e)
            {
                log.error("UI error", e);
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
        BrowseInfo info = getBrowseInfo();

        pageMeta.addMetadata("title").addContent(message("xmlui.administrative.Navigation.administrative_withdrawn"));

        DSpaceObject dso = HandleUtil.obtainHandle(objectModel);

        pageMeta.addTrailLink(contextPath + "/", T_dspace_home);

        pageMeta.addTrail().addContent(message("xmlui.administrative.Navigation.administrative_withdrawn"));
    }

    /**
     * Add the browse-title division.
     */
    public void addBody(Body body) throws SAXException, WingException, UIException, SQLException,
            IOException, AuthorizeException
    {
        BrowseParams params = null;

	    params = getUserParams();

	    BrowseInfo info = getBrowseInfo();

        String type = "withdrawn";

        // Build the DRI Body
        Division div = body.addDivision("browse-by-" + type, "primary");

        div.setHead(message("xmlui.administrative.Navigation.administrative_withdrawn"));

        // Build the sort and display controls
        addBrowseControls(div, info, params);

        // This div will hold the browsing results
        Division results = div.addDivision("browse-by-" + type + "-results", "primary");

        // If there are items to browse, add the pagination
        int itemsTotal = info.getTotal();
                
        if (itemsTotal > 0) {
        	int firstItemIndex = browseInfo.getOverallPosition()+1;
        	int lastItemIndex = browseInfo.getOverallPosition()+browseInfo.getResultCount();        	
            int currentPage = (int) (firstItemIndex/browseInfo.getResultsPerPage())+1;
            int pagesTotal = (int) Math.ceil((double)itemsTotal / browseInfo.getResultsPerPage());
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("page", "{pageNum}");
            String pageURLMask = generateURL(parameters);
        	results.setHead(T_head1_none.parameterize(firstItemIndex,lastItemIndex, itemsTotal));
        	results.setMaskedPagination(itemsTotal,  firstItemIndex, lastItemIndex, currentPage, pagesTotal, pageURLMask);

			// Reference all the browsed items
	        ReferenceSet referenceSet = results.addReferenceSet("browse-by-" + type, ReferenceSet.TYPE_SUMMARY_LIST, type, null);

	        // Add the items to the browse results
	        for (BrowseItem item : (java.util.List<BrowseItem>) info.getResults())
	        {
		        referenceSet.addReference(item);
	        }        
		}
        else
        {
            results.addPara(T_no_results);
        }
    }

    /**
     * Recycle
     */
    public void recycle()
    {
        this.validity = null;
        this.userParams = null;
        this.browseInfo = null;
        super.recycle();
    }



    /*
     * @param info
     * @param params
     * @throws WingException
     */
    private void addBrowseControls(Division div, BrowseInfo info, BrowseParams params)
            throws WingException
    {
        // Prepare a Map of query parameters required for all links
        Map<String, String> parameters = new HashMap<String, String>();

        parameters.putAll(params.getCommonParameters());

        Division searchControlsGear = div.addDivision("masked-page-control").addDivision("search-controls-gear", "controls-gear-wrapper");
        List sortList = searchControlsGear.addList("sort-options", List.TYPE_SIMPLE, "gear-selection");

        boolean first = true;
        try
	    {
	        // Create a drop down of the different sort columns available
	        Set<SortOption> sortOptions = SortOption.getSortOptions();

	        // Only generate the list if we have multiple columns
	        if (sortOptions.size() > 1)
	        {
                	first = false;
                    sortList.addItem("sort-head", "gear-head first").addContent(T_sort_by);
			        List sortByOptions = sortList.addList("sort-selections");

	            for (SortOption so : sortOptions)
	            {
                        if (so.isVisible())
                        {
					        boolean selected = so.equals(info.getSortOption());
					        parameters.put("sort_by",so.getNumber()+"");
                            sortByOptions.addItem(null,null).addXref(generateURL(parameters), message("xmlui.ArtifactBrowser.ConfigurableBrowse.sort_by." + so.getName()),"gear-option" + (selected ? " gear-option-selected" : ""));					        
                        }
	            }
	        }
	    }
	    catch (SortException se)
	    {
	        throw new WingException("Unable to get sort options", se);
	    }

        parameters.remove("sort_by");

	    // Create a control to changing ascending / descending order
        sortList.addItem("order-head", "gear-head" + (first? " first":"")).addContent(T_order);
        List ordOptions = sortList.addList("order-selections");
        boolean asc = SortOption.ASCENDING.equals(params.scope.getOrder());
  
    	parameters.put("order",SortOption.ASCENDING);
        ordOptions.addItem(null,null).addXref(generateURL(parameters),T_order_asc, "gear-option" + (asc? " gear-option-selected":""));
    	parameters.put("order",SortOption.DESCENDING);
        ordOptions.addItem(null,null).addXref(generateURL(parameters),T_order_desc, "gear-option" + (!asc? " gear-option-selected":""));

        parameters.remove("order");

        //Add the rows per page
        sortList.addItem("rpp-head", "gear-head").addContent(T_rpp);
        List rppOptions = sortList.addList("rpp-selections");
        for (int i : RESULTS_PER_PAGE_PROGRESSION)
        {
    		parameters.put("page", 1+"");
        	parameters.put("rpp", Integer.toString(i));
            rppOptions.addItem(null, null).addXref(generateURL(parameters), Integer.toString(i), "gear-option" + (i == browseInfo.getResultsPerPage() ? " gear-option-selected" : ""));
        }    
        }


    protected String getParameterSortBy() {
        String s = ObjectModelHelper.getRequest(objectModel).getParameter("sort_by");
        return s != null ? s : null;
        }

    protected String getParameterGroup() {
        String s = ObjectModelHelper.getRequest(objectModel).getParameter("group_by");
        return s != null ? s : "none";
    }

    protected String getParameterOrder() {
        return ObjectModelHelper.getRequest(objectModel).getParameter("order");
    }

    protected String getParameterScope() {
        return ObjectModelHelper.getRequest(objectModel).getParameter("scope");
        }


    private String  generateURL(Map<String, String> parameters) throws UIException {
    	if (parameters.get("page") == null)
        {
            parameters.put("page", encodeForURL(String.valueOf(getParameterPage())));
        }
    	if (parameters.get(BrowseParams.ORDER) == null){
    			parameters.put(BrowseParams.ORDER, encodeForURL(getParameterOrder()));
    	}
    	if (parameters.get(BrowseParams.TYPE)==null){
    		String type = getParameterType();
    		if(type!=null)
    			parameters.put(BrowseParams.TYPE, encodeForURL(type));
    	}
    	if (parameters.get("sort_by")==null){
    		String sort = getParameterSort();
    		if(sort!=null)
    			parameters.put("sort_by", encodeForURL(sort));
    	}
    	if (parameters.get(BrowseParams.RESULTS_PER_PAGE)==null){
    		parameters.put(BrowseParams.RESULTS_PER_PAGE, encodeForURL(String.valueOf(getParameterRpp())));
    	}
    	//??other defaults
        return super.generateURL(WITHDRAWN_URL_BASE, parameters);
    }

    private int getParameterPage() {
        try {
            int ret = Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter("page"));
            if(ret<=0){
            	return 1;
            }
            else return ret;
        }
        catch (Exception e) {
            return 1;
        }
    } 
        
    private String getParameterType(){
    	String type = ObjectModelHelper.getRequest(objectModel).getParameter(BrowseParams.TYPE);
    	return type;
    }
    private String getParameterSort(){
    	String sort = ObjectModelHelper.getRequest(objectModel).getParameter("sort_by");
    	return sort;
    }
    
    private int getParameterRpp() {
        try {
            int ret = Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter(BrowseParams.RESULTS_PER_PAGE));
            if(ret<=0){
            	return 20;
            }
            else return ret;
        }
        catch (Exception e) {
            return 20;
        }
    } 
    
    /**
     * Get the query parameters supplied to the browse.
     *
     * @return
     * @throws SQLException
     * @throws UIException
     */
    private BrowseParams getUserParams() throws SQLException, UIException
    {
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
            int    sortBy = RequestUtils.getIntParameter(request, BrowseParams.SORT_BY);

            BrowseIndex bi = BrowseIndex.getWithdrawnBrowseIndex();

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

            params.scope.setBrowseIndex(bi);
            params.scope.setSortBy(sortBy);

            params.scope.setJumpToItem(RequestUtils.getIntParameter(request, BrowseParams.JUMPTO_ITEM));
            params.scope.setOrder(request.getParameter(BrowseParams.ORDER));
            params.scope.setResultsPerPage(RequestUtils.getIntParameter(request, BrowseParams.RESULTS_PER_PAGE));            
            params.scope.setOffset((getParameterPage()-1)*params.scope.getResultsPerPage());
            params.scope.setStartsWith(request.getParameter(BrowseParams.STARTS_WITH));
            String filterValue = request.getParameter(BrowseParams.FILTER_VALUE[0]);
            if (filterValue == null)
            {
                filterValue = request.getParameter(BrowseParams.FILTER_VALUE[1]);
            }
            else
            {
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
                if ((params.month != null) && !"-1".equals(params.month)
                        && !"".equals(params.month))
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
        BrowseParams params = getUserParams();

	    try
        {
            // Create a new browse engine, and perform the browse
            BrowseEngine be = new BrowseEngine(context);
            this.browseInfo = be.browse(params.scope);

            // figure out the setting for author list truncation
            if (params.etAl < 0)
            {
                // there is no limit, or the UI says to use the default
                int etAl = ConfigurationManager.getIntProperty("webui.browse.author-limit");
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
     * Is this browse sorted by date?
     * @param info
     * @return
     */
    private boolean isSortedByDate(BrowseInfo info)
    {
        return info.getSortOption().isDate() ||
            (info.getBrowseIndex().isDate() && info.getSortOption().isDefault());
    }

    /**
     * Returns a list of the filter queries for use in rendering pages, creating page more urls, ....
     * @return an array containing the filter queries
     */
    protected String[] getParameterFilterQueries(){
        try {
            return ObjectModelHelper.getRequest(objectModel).getParameterValues("fq");
            }
        catch (Exception e) {
            return null;
            }
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

        if (scope.getFilterValue() != null)
        {
            paramMap.put(scope.getAuthorityValue() != null?
                    BrowseParams.FILTER_VALUE[1]:BrowseParams.FILTER_VALUE[0],
                    AbstractDSpaceTransformer.encodeForURL(
                    scope.getFilterValue()));
        }

        if (scope.getFilterValueLang() != null)
        {
            paramMap.put(BrowseParams.FILTER_VALUE_LANG, AbstractDSpaceTransformer.encodeForURL(
                    scope.getFilterValueLang()));
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
        paramMap.put(BrowseParams.ORDER, AbstractDSpaceTransformer.encodeForURL(this.scope.getOrder()));
        paramMap.put(BrowseParams.RESULTS_PER_PAGE, Integer
                .toString(this.scope.getResultsPerPage()));
        paramMap.put(BrowseParams.ETAL, Integer.toString(this.etAl));

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
            key += "-" + scope.getResultsPerPage();
            key += "-" + scope.getSortBy();
            key += "-" + scope.getSortOption().getNumber();
            key += "-" + scope.getOrder();
            key += "-" + scope.getOffset();
            key += "-" + scope.getJumpToItem();
            key += "-" + scope.getFilterValue();
            key += "-" + scope.getFilterValueLang();
            key += "-" + scope.getJumpToValue();
            key += "-" + scope.getJumpToValueLang();
            key += "-" + etAl;

            return key;
        }
        catch (BrowseException e)
        {
            return null; // ignore exception and return no key
        }
    }
};

