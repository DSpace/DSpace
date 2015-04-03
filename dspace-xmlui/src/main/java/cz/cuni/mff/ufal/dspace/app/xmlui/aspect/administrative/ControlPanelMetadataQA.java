/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.ConfigurationManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.dspace.storage.rdbms.TableRowIterator;

@SuppressWarnings("deprecation")
public class ControlPanelMetadataQA extends AbstractControlPanelTab {
	
	private static Logger log = Logger.getLogger(ControlPanelMetadataQA.class);

    private static int max_outliers = 10;

	@Override
	public void addBody(Map objectModel, Division div) throws WingException, SQLException, AuthorizeException, IOException 
	{
		// header
		Division div_main = div.addDivision("metadata_qa", "well");
		
		// process updates
		String updated_field = null;
        Request request = ObjectModelHelper.getRequest(objectModel);
        Pattern updateParamPattern = Pattern.compile("^update_(.*)_(\\d+)$");
        for ( Object obj : request.getParameters().entrySet() )
        {
            Map.Entry me = (Map.Entry)obj;
            String key = (String)me.getKey();
            Matcher updateParamMatcher = updateParamPattern.matcher(key);
            if(updateParamMatcher.find() && request.getParameter(key) != null)
            {
                String field = updateParamMatcher.group(1);
                String id = updateParamMatcher.group(2);                
                String old_val = request.getParameter(String.format("original_%s_%s", field, id));
                String new_val = request.getParameter(String.format("new_%s_%s", field, id));
                div_main.addPara("alert", "alert alert-info").addContent(
                                String.format("Updating field [%s] from [%s] to [%s]!", field, old_val, new_val));
                String output = update_metadata_field(div_main, field, old_val, new_val);
                div_main.addPara("alert", "alert alert-info replace_br linkify").addContent(output);
                updated_field = field;
            }
        }
		
		//
		String meta_field = show_meta();
		if ( null == meta_field ) {
		    add_config(div_main);
		    add_metaqa_form(div_main, updated_field);
		}else {
            add_metaqa_form(div_main, meta_field);
		    add_metaqa_values(div_main, meta_field);
		}
    }
	
	private ItemIterator get_items_with_metadata_field(String schema, String element, String qualifier)
        throws SQLException, AuthorizeException
	{
        MetadataSchema mds = MetadataSchema.find(context, schema);
            if (mds == null) {
                throw new IllegalArgumentException("No such metadata schema: " + schema);
            }
        MetadataField mdf = MetadataField.findByElement(context, mds.getSchemaID(), element, qualifier);
            if (mdf == null) {
                throw new IllegalArgumentException(
                        "No such metadata field: schema=" + schema + ", element=" + element + ", qualifier=" + qualifier);
            }

        String query = "SELECT DISTINCT(item.item_id) FROM metadatavalue,item WHERE "+
                       "item.item_id = metadatavalue.resource_id AND metadata_field_id = ? AND item.in_archive='1'";
        java.util.List<Integer> itemids = new ArrayList<Integer>();
        TableRowIterator rows = DatabaseManager.queryTable(context, "item", query, mdf.getFieldID());
        while ( rows.hasNext() ) {
            TableRow row = rows.next();
            itemids.add( row.getIntColumn("item_id"));
        }
        return new ItemIterator(context, itemids);
	}

	// processing and UI
	//
	//
	private void add_metaqa_values(Division div_main, String meta_field) 
	                throws SQLException, AuthorizeException, IOException, WingException
	{
	    String[] fields = meta_field.split("\\.");
        String schema = null;
        String element = null;
        String qualifier = null;
            if ( 2 > fields.length || 3 < fields.length ) 
            {
                String err_msg = String.format("The selected field in MetadataQA CP is invalid [%s]!", 
                                meta_field);
                log.warn( err_msg );
                div_main.addPara("warn", "alert alert-danger").addContent(err_msg);
                return;
            }
        schema = fields[0];
        element = fields[1];
        if ( fields.length > 2 ) {
            qualifier = fields[2];
        }
	    
        Map<String, Integer> hm = new HashMap<String, Integer>();
        // loop through all items with that metadata
        ItemIterator itr = get_items_with_metadata_field(schema, element, qualifier);
            if ( !itr.hasNext() ) 
            {
                div_main.addPara("warn", "alert alert-info").addContent(
                    String.format("No items with field [%s].", meta_field));
                return;
            }

        // count form before real values - see show_counts
        List count_form = div_main.addList("metadata-values", null,  "well well-white");

        // show values
        int count = show_values(div_main, itr, schema, element, qualifier, hm);
        // show counts
        show_counts(count_form, hm, count, meta_field);
        // show outliers
        show_outliers(div_main, hm);
	}
	
	@SuppressWarnings("deprecation")
    private int show_values(Division div_main, 
	                ItemIterator itr, String schema, String element, String qualifier,
	                Map<String, Integer> hm) throws WingException, SQLException
	{
	    int max_to_process = ConfigurationManager.getIntProperty("lr", "lr.metaqa.max.processed");
	    
        // values
        List form = div_main.addList("metadata-values", List.TYPE_GLOSS,  "well well-white");
        form.setHead("Item metadata association");
        int count = 0;
        Item item = null;
        while (itr.hasNext())
        {
            item = itr.next();
            ++count;
                if ( max_to_process < count ) {
                    div_main.addPara("warn", "alert alert-info").addContent(
                        String.format("Max items [%d] processed, stopping...", max_to_process));
                    break;
                }
                
            org.dspace.app.xmlui.wing.element.Item row = form.addItem(null, "linkify");
            row.addXref(String.format("%s%s", 
                            ConfigurationManager.getProperty("handle.canonical.prefix"),
                            item.getHandle()), item.getHandle(), "label label-important");
            
            Metadatum[] values = item.getMetadata(schema, element, qualifier, Item.ANY);
            Set<String> done = new HashSet<String>();
            for ( Metadatum dc : values ) 
            {
                String value = dc.value;
                    // should not happen
                    if ( value == null ) {
                        log.warn( String.format("NULL in dc.value(s) of %s.%s.%s",
                                        dc.schema,
                                        dc.element != null ? dc.element : "null",
                                        dc.qualifier != null ? dc.qualifier : "null") );
                        value = "<<null>>";
                    }
                    if ( value.length() == 0 ) {
                        value = "<<empty>>";
                    }
                // done with other language?
                if ( done.contains(value) ) {
                    continue;
                }
                int cnt = hm.containsKey(value) ? hm.get(value) : 0;
                hm.put(value, cnt + 1);
                done.add(value);
            }
            
            for ( Metadatum dc : values ) {
                row.addHighlight("badge badge-info").addContent(dc.value);
            }
        }
        itr.close();	
        return count;
	}
	
	@SuppressWarnings("deprecation")
    private String update_metadata_field(Division div_main, String meta_field, String old_val, String new_val) 
	                throws SQLException, AuthorizeException, IOException, WingException
	{
	    StringBuilder sb = new StringBuilder();
        String[] fields = meta_field.split("\\.");
        String schema = null;
        String element = null;
        String qualifier = null;
        
        schema = fields[0];
        element = fields[1];
        if ( fields.length > 2 ) {
            qualifier = fields[2];
        }

        ItemIterator itr = get_items_with_metadata_field(schema, element, qualifier);
            if ( !itr.hasNext() ) 
            {
                div_main.addPara("warn", "alert alert-info").addContent(
                    String.format("No items with field [%s].", meta_field));
                return "No item has this metadata field!";
            }
	    
        Item item = null;
        while (itr.hasNext())
        {
            item = itr.next();
            boolean changed = false;
            Metadatum[] values = item.getMetadata(schema, element, qualifier, Item.ANY);
            for ( Metadatum dc : values ) {
                if ( dc.value.equals(old_val) ) {
                    // change it
                    dc.value = new_val;
                    String handle_url = String.format("%s%s", 
                        ConfigurationManager.getProperty("handle.canonical.prefix"), item.getHandle());
                    sb.append(String.format("Item [%s] updated.\n", handle_url));
                    changed = true;
                }
            }
            // update the metadata properly - first clear, than add
            if ( changed ) {
                item.clearMetadata(schema, element, qualifier, Item.ANY);
                for ( Metadatum dc : values ) {
                    item.addMetadata(schema, element, qualifier, dc.language, dc.value);
                }
                item.store_provenance_info("Item was updated in CP MetadataQA", context.getCurrentUser());
                item.update();
            }
        }
        
        context.commit();
        log.warn( sb.toString() );
        return sb.toString();
	}

	
	private void show_counts(List form, Map<String, Integer> hm, int count, String meta_field) 
throws WingException
	{
        // show counts
        hm = sortByValue(hm);
        form.setHead(String.format("Occurrences in items", count));
        int id = 1;
        for (Map.Entry<String, Integer> i : hm.entrySet())
        {
            form.addLabel(i.getKey());
            org.dspace.app.xmlui.wing.element.Item row = form.addItem();
            row.addContent(String.format("%d / %d", i.getValue(), count));
            row.addHidden(String.format("original_%s_%d", meta_field,id)).setValue(i.getKey());
            row.addText(String.format("new_%s_%d", meta_field,id)).setValue(i.getKey());
            row.addButton(String.format("update_%s_%d", meta_field,id), "btn btn-repository").setValue("Update");
            id++;
        }
	}
	
    private void show_outliers(Division div_main, Map<String, Integer> hm)
    {
        try {
            if ( 2 * max_outliers + 1 > hm.size() ) {
                return;
            }
            String[] arr = hm.keySet().toArray(new String[0]);
            Arrays.sort(arr, new Comparator<String>() {
                                public int compare(String s1,String s2) {
                                    return s1.length() - s2.length();
                                }
                            });
            List form = div_main.addList("metadata-outliers", List.TYPE_GLOSS,  "well well-white");
            form.setHead("Metadata value outliers - too short");
            for ( int i = 0; i < max_outliers; ++i ) {
                form.addItem().addContent(arr[i]);
            }
            form = div_main.addList("metadata-outliers", List.TYPE_GLOSS,  "well well-white");
            form.setHead("Metadata value outliers - too long");
            for ( int i = 1; i <= max_outliers; ++i ) {
                form.addItem().addContent(arr[arr.length - i]);
            }
        } catch (WingException e) {
        }
    }

	// UI
	//
	//

	private String show_meta() {
        Request request = ObjectModelHelper.getRequest(objectModel);
        if ( null == request.getParameter("submit_metaqa") ) {
            return null;
        }
        return request.getParameter("metaqa_field");
	}

	private void add_metaqa_form(Division div_main) throws WingException, SQLException 
    {
	    add_metaqa_form( div_main, null );
    }

	private void add_metaqa_form(Division div_main, String selected_value) throws WingException, SQLException 
	{
        List form = div_main.addList("metadata-field", null,  "well well-white");
        form.setHead("Metadata QA");
        form.addLabel("Process up to");
        form.addItem().addContent(ConfigurationManager.getProperty("lr", "lr.metaqa.max.processed"));

        form.addLabel("Select metadata field");
        Select addName = form.addItem().addSelect("metaqa_field", "form-control");
        addName.setSize(20);
        MetadataField[] fields = MetadataField.findAll(context);
        for (MetadataField field : fields)
        {
                MetadataSchema schema = MetadataSchema.find(context, field.getSchemaID());
                String name = schema.getName() + "." + field.getElement();
                if (field.getQualifier() != null) {
                    name += "." + field.getQualifier();
                }
                if ( selected_value != null ) {
                    addName.addOption(name.equals(selected_value), name, name);
                }else {
                    addName.addOption(name, name);
                }
        }
        
        form.addLabel(" ");
        form.addItem().addButton( "submit_metaqa", "btn btn-repository" ).setValue(
            "Show metadata values");
	}
	
	private void add_config(Division div_main) throws WingException 
	{
        String auto_on_s = ConfigurationManager.getProperty("lr", "lr.autocomplete.on");
        boolean auto_on = auto_on_s != null && auto_on_s.equals("true");

        List l = div_main.addList("metadata_qa", null, "well well-light");
        l.setHead("Autocomplete mappings");
        l.addLabel("Turned on/off");
        l.addItem("on-off", auto_on ? "alert alert-success" : "alert alert-error").addContent(
            auto_on ? "on" : "off");
        
        l.addLabel("SOLR JSON url mapping");
        String solr_url = ConfigurationManager.getProperty("lr", "lr.autocomplete.solr.url");
        if ( null != solr_url ) {
            l.addItemXref(solr_url, solr_url);
        }

        l.addLabel("SOLR JSON for e.g., subject_dc");
        if ( null != solr_url ) {
            l.addItemXref(solr_url + "subject_dc", solr_url + "subject_dc");
        }

        String base_url = ConfigurationManager.getProperty("dspace.baseUrl");
        l.addLabel("SOLR admin (must be tunneled)");
        l.addItemXref(base_url + "/solr", solr_url + "/solr");
	}
	
	// taken from http://stackoverflow.com/questions/109383/how-to-sort-a-mapkey-value-on-the-values-in-java
	// - should be reimplemented for generic types
	//
	@SuppressWarnings("unchecked")
    static Map sortByValue(Map<String, Integer> map) {
	     java.util.List list = new LinkedList(map.entrySet());
	     Collections.sort(list, new Comparator() {
	          public int compare(Object o1, Object o2) {
	               return ((Comparable) ((Map.Entry) (o2)).getValue()).compareTo(((Map.Entry) (o1)).getValue());
	          }
	     });

	    Map result = new LinkedHashMap();
	    for (Iterator it = list.iterator(); it.hasNext();) {
	        Map.Entry entry = (Map.Entry)it.next();
	        result.put(entry.getKey(), entry.getValue());
	    }
	    return result;
	} 	
}
