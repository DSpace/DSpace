/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.DSpaceObject;
import org.dspace.sort.SortOption;

public class ControlPanelBitstreams extends AbstractControlPanelTab {

	final static float MB = 1024.0f * 1024.0f;
	final static int items_per_page = 10;

	/** The options for results per page */
    private static final int[] RESULTS_PER_PAGE_PROGRESSION = {5,10,20,40,60,80,100};
    private static final int RESULTS_PER_PAGE_MAX = 100;

	//
	final static String submit_show_unknown_id = "non-del-unknown";
	final static String select_prefix = "format-"; 
	final static String submit_id = "submit_update";
	final static String from_id = "from-value";

    private static final Message T_head1_none =
            message("xmlui.ArtifactBrowser.AbstractSearch.head1_none");

	@Override
	public void addBody(Map objectModel, Division div) throws WingException, SQLException 
	{
		Division wfdivmain = div.addDivision("unpublished_items", "well well-light");

		// header
		wfdivmain.setHead("BITSTREAMS");		

		
		Bitstream[] bitstreams = getBitstreams();
		
		// param from
		Request request = ObjectModelHelper.getRequest(objectModel);
		
		// show only non-deleted unkown
		boolean show_only_non_deleted_unknown = "Show not deleted unknown".equals(request.getParameter(submit_show_unknown_id));

		ArrayList<Bitstream> bitstreamList = new ArrayList<Bitstream>();

		for(Bitstream b : bitstreams) {
			if(show_only_non_deleted_unknown) {
				if(b.getFormat().getSupportLevel()==BitstreamFormat.UNKNOWN && !b.isDeleted()) {
					bitstreamList.add(b);
				}
			} else {
				bitstreamList.add(b);
			}
		}
		
		String sort_by = getParameterSort();
		if(sort_by.equals("id")) {
			Collections.sort(bitstreamList, new Comparator<Bitstream>() {

				@Override
				public int compare(Bitstream o1, Bitstream o2) {
					String order = getParameterOrder();
					if(order.equals(SortOption.ASCENDING.toString())) {
						return o1.getID() > o2.getID() ? 1 : (o1.getID() < o2.getID() ? -1 : 0);
					} else {
						return o1.getID() > o2.getID() ? -1 : (o1.getID() < o2.getID() ? 1 : 0);
					}
				}
			});
		} else if(sort_by.equals("name")) {
			Collections.sort(bitstreamList, new Comparator<Bitstream>() {

				@Override
				public int compare(Bitstream o1, Bitstream o2) {
					String order = getParameterOrder();
					String o1_name = o1.getName();
					String o2_name = o2.getName();
					
					if(o1_name==null)
						o1_name = "";
					else
						o1_name = o1_name.toLowerCase();
					if(o2_name==null)
						o2_name = ""; 
					else
						o2_name = o2_name.toLowerCase();
					
					if(order.equals(SortOption.ASCENDING.toString())) {
						return o1_name.compareTo(o2_name);
					} else {
						return o2_name.compareTo(o1_name);
					}
				}
			});			
		}

		// Do the updates
		if ( request.getParameter(submit_id) != null ) 
		{
			// do updates
			int changes = 0;
			String exc = "";
			for ( Object obj_key : request.getParameters().keySet() ) 
			{
				try {
					String key = (String)obj_key;
					if ( key.startsWith(select_prefix) ) {
						String value = request.getParameter(key);
						String bitstream_id = key.substring(select_prefix.length());
						int b_id = Integer.valueOf(bitstream_id);
						int f_id = Integer.valueOf(value);
						Bitstream bitstream = Bitstream.find(context, b_id);
						if ( bitstream.getFormat().getID() != f_id ) {
								BitstreamFormat bfn = BitstreamFormat.find(context, f_id);
								if (bfn != null) {
									bitstream.setFormat(bfn);
									bitstream.update();
									changes += 1;
								}
						}
					}
				} catch (Exception e) {
					exc = e.toString();
				}
					
			}
			if ( changes > 0 ) {
				context.commit();
			}

			if ( exc != null && exc.length() > 0 ) {
				div.addPara("exeption", "alert alert-error").addContent("Exception:" + exc);
			}
		}
		

		
		int resultCount = bitstreamList.size();
        int page = getParameterPage();
        if(page <= 0) page = 1;
        int per_page = getParameterRpp();
        
        int firstIndex = (page-1)*per_page+1; 
        int lastIndex = (page-1)*per_page + per_page;
        if(lastIndex > resultCount) lastIndex = resultCount;
        int totalPages = (int)Math.ceil((double)resultCount / per_page);
		
		// table
		Division form = wfdivmain.addInteractiveDivision("edit-bitstream-format", web_link, Division.METHOD_POST, "primary administrative");		

		Division button_div = form.addDivision("button");
		if(show_only_non_deleted_unknown) {
			button_div.addPara().addButton(submit_show_unknown_id).setValue("Show all");
			button_div.addHidden(submit_show_unknown_id).setValue("Show not deleted unknown");
		} else {
			button_div.addPara().addButton(submit_show_unknown_id).setValue("Show not deleted unknown");
			button_div.addHidden(submit_show_unknown_id).setValue("Show all");
		}
		
		web_link = web_link + "&" + submit_show_unknown_id + "=" + request.getParameter(submit_show_unknown_id);
		
		addSearchControls(form);
		
		Division wfdivtable = form.addDivision("paginated-wfdiv");
		wfdivtable.setHead(T_head1_none.parameterize(firstIndex, lastIndex, resultCount));
		HashMap<String, String> parameters = new HashMap<String, String>();
		parameters.put("page", "{pageNum}");
		wfdivtable.setMaskedPagination(resultCount, firstIndex, lastIndex, page, totalPages, generateURL(parameters));

		
		Table wftable = wfdivtable.addTable("workspace_items", 1, 5);
		Row wfhead = wftable.addRow(Row.ROLE_HEADER);
		
		// table items - because of GUI not all columns could be shown
		wfhead.addCellContent("ID");
		wfhead.addCellContent("NAME");
		wfhead.addCellContent("DESCRIPTION");
		//wfhead.addCellContent("CHECKSUM / ALGO");
		wfhead.addCellContent("SIZE (MB)");
		//wfhead.addCellContent("SOURCE");
		wfhead.addCellContent("FORMAT");
		wfhead.addCellContent("DELETED");
		
		
		BitstreamFormat[] bfs = BitstreamFormat.findAll(context);
		for (int i = firstIndex-1; i<lastIndex && i<bitstreamList.size(); i++ ) {		
			Bitstream b = bitstreamList.get(i);
			
			Row wsrow = wftable.addRow(null, Row.ROLE_DATA, b.isDeleted()?"error":"");
			wsrow.addCell().addContent( b.getID() );
			
			DSpaceObject dso = b.getParentObject();
			if ( dso != null ) {
				String handle = dso.getHandle();
				if ( handle != null ) {
					wsrow.addCell().addXref( contextPath + "/handle/" + handle, b.getName() );
				}else {
					dso = null;
				}
			}
			
			if ( dso == null ) {
				wsrow.addCell().addContent( b.getName() );
			}
			
			wsrow.addCell().addContent( b.getDescription() );
			//wsrow.addCell().addContent( 
			//		String.format( "%s/%s", b.getChecksum(), b.getChecksumAlgorithm() ) );
			wsrow.addCell().addContent( String.format( "%.2f MB", (float)b.getSize() / MB ) );
			//wsrow.addCell().addContent( b.getSource() );
			Cell tmp_cell = wsrow.addCell();
			Select formats = tmp_cell.addSelect(select_prefix + b.getID());
			for ( BitstreamFormat bf : bfs )
			{
				boolean selected = bf.getID() == b.getFormat().getID();
				String short_desc = StringUtils.join(bf.getExtensions(), ",");
				if ( short_desc.length() == 0 ) {
					short_desc = bf.getShortDescription();
				}
				final int max_w = 15;
				if ( short_desc.length() > max_w ) {
					short_desc = short_desc.substring(0, max_w) + "...";
				}
				formats.addOption( 
						selected, 
						bf.getID(), 
						short_desc);
			}
			wsrow.addCell().addContent( String.valueOf(b.isDeleted()) );
		}		
		
		Para actions = wfdivtable.addPara(null,"edit-metadata-actions bottom" );
        actions.addButton(submit_id).setValue("update");        
    }
	
	
	private Bitstream[] getBitstreams() throws SQLException {
		return Bitstream.findAll(context);
	}
	
    private void addSearchControls(Division div) throws WingException
    {
        Map<String, String> parameters = new HashMap<String, String>();

        Division searchControlsGear = div.addDivision("masked-page-control").addDivision("search-controls-gear", "controls-gear-wrapper");
        List sortList = searchControlsGear.addList("sort-options", org.dspace.app.xmlui.wing.element.List.TYPE_SIMPLE, "gear-selection");

        sortList.addItem("sort-head", "gear-head first").addContent("Sort by");
        org.dspace.app.xmlui.wing.element.List sortByOptions = sortList.addList("sort-selections");
        
        parameters.put("sort_by", "id");
        sortByOptions.addItem(null,null).addXref(generateURL(parameters), "id","gear-option" + ("id".equals(getParameterSort()) ? " gear-option-selected" : ""));
        parameters.put("sort_by", "name");
        sortByOptions.addItem(null,null).addXref(generateURL(parameters), "name","gear-option" + ("name".equals(getParameterSort()) ? " gear-option-selected" : ""));

        parameters.remove("sort_by");

        // Create a control to changing ascending / descending order
        sortList.addItem("order-head", "gear-head").addContent("Order By");
        org.dspace.app.xmlui.wing.element.List ordOptions = sortList.addList("order-selections");
        boolean asc = SortOption.ASCENDING.equals(getParameterOrder());

    	parameters.put("order",SortOption.ASCENDING);
        ordOptions.addItem(null,null).addXref(generateURL(parameters), SortOption.ASCENDING.toString(), "gear-option" + (asc? " gear-option-selected":""));
    	parameters.put("order",SortOption.DESCENDING);
        ordOptions.addItem(null,null).addXref(generateURL(parameters), SortOption.DESCENDING.toString(), "gear-option" + (!asc? " gear-option-selected":""));

        parameters.remove("order");
        
        sortList.addItem("rpp-head", "gear-head").addContent("Results/Page");
        List rppOptions = sortList.addList("rpp-selections");
        for (int i : RESULTS_PER_PAGE_PROGRESSION)
        {    
    		parameters.put("page", 1+"");
        	parameters.put("rpp", Integer.toString(i));
            rppOptions.addItem(null, null).addXref(generateURL(parameters), Integer.toString(i), "gear-option" + (i == getParameterRpp() ? " gear-option-selected" : ""));
        }    

    }
    
    /**
     * Generate a url to the simple search url.
     */
    private String  generateURL(Map<String, String> parameters) throws UIException {
    	if (parameters.get("page") == null)
        {
            parameters.put("page", encodeForURL(String.valueOf(getParameterPage())));
        }
    	if (parameters.get("order") == null){
    			parameters.put("order", encodeForURL(getParameterOrder()));
    	}
    	if (parameters.get("sort_by")==null){
    		String sort = getParameterSort();
    		if(sort!=null)
    			parameters.put("sort_by", encodeForURL(sort));
    	}
    	if (parameters.get("rpp")==null){
    		parameters.put("rpp", encodeForURL(String.valueOf(getParameterRpp())));
    	}
    	
    	StringBuilder urlBuffer = new StringBuilder();
    	
        for (Map.Entry<String, String> param : parameters.entrySet()) {
        	urlBuffer.append( '&');
        	urlBuffer.append(param.getKey()).append("=").append(param.getValue());
        }
        
        return web_link + urlBuffer.toString();

    }
    
    private String getParameterOrder(){
		String order = ObjectModelHelper.getRequest(objectModel).getParameter("order");
        if(order!=null && (order.equals(SortOption.ASCENDING) || order.equals(SortOption.DESCENDING))){
        	return order;
        }
        return SortOption.ASCENDING;            		
    }
    
    private String getParameterSort(){
    	String sort = ObjectModelHelper.getRequest(objectModel).getParameter("sort_by");
    	if(sort==null) {
    		sort = "id";
    	}
    	return sort;
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

    private int getParameterRpp() {
        try
        {
        	int rpp = Integer.parseInt(ObjectModelHelper.getRequest(objectModel).getParameter("rpp"));
            return rpp<=RESULTS_PER_PAGE_MAX?rpp:RESULTS_PER_PAGE_MAX;
        }
        catch (Exception e)
        {
            return items_per_page;
        }
    }

	
}





