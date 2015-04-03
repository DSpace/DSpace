/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.app.xmlui.aspect.administrative;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.content.Metadatum;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;

@SuppressWarnings("deprecation")
public class ControlPanelMetadata extends AbstractControlPanelTab {
	
	private static Logger log = Logger.getLogger(ControlPanelMetadata.class);

	@Override
	public void addBody(Map objectModel, Division div) throws WingException, SQLException 
	{
		// header
        Division div_main = div.addDivision("metadata_isreplaced_items", "well well-light");
        String dc = Item.metadata_isreplacedby;
        create_table( div_main, String.format("IS REPLACED BY ITEMS (%s)", dc), dc );
        
        div_main = div.addDivision("metadata_relation_items", "well well-light");
        dc = "dc.relation." + Item.ANY;
        create_table( div_main, String.format("RELATION (%s)", dc), dc );

        div_main = div.addDivision("metadata_hidden_items", "well well-light");
        dc = "local.hidden";
        create_table( div_main, String.format("HIDDEN ITEM (%s)", dc), dc, "!false" );
	}
	
	// show all values
    private Table create_table(Division div, String title, String metadata) throws WingException {
        return create_table( div, title, metadata, null);
    }
    
	private Table create_table(Division div, String title, String metadata, String show_only_if_value) throws WingException
	{
        div.setHead(title);
		String[] metadata_parts = metadata.split("\\.");
        
		// use only schema.element
		if ( metadata_parts.length == 2 ) 
		{
			return create_table( 
			    div, metadata_parts[0], metadata_parts[1], null, show_only_if_value );

		// use only schema.element.qualifier
		}else if ( metadata_parts.length == 3 ) 
		{
		    // if we use * iterate through all relations
		    if ( metadata_parts[2].equals(Item.ANY) ) 
		    {
		        Table table = div.addTable("specific-regexp-metadata", 1, 3);
                for ( MetadataField field : metadata_fields(
                          "http://dublincore.org/documents/dcmi-terms/", metadata_parts[1] ) ) {
                    table = create_table( div, "dc", field.getElement(), field.getQualifier(), table, show_only_if_value ); 
                }
                return null;
                
            // use one metadata field
		    }else {
		        return create_table( div, metadata_parts[0], metadata_parts[1], metadata_parts[2], show_only_if_value );
		    }
		    
		}else {
			log.warn( String.format(
					"Invalid metadata supplied, expecting schema.element.qualifier got [%s]", 
					metadata));
		}
		return null;
		
	}
	
	private MetadataField[] metadata_fields( String schema_namespace, String element ) {
	    MetadataSchema schema;
        try {
            schema = MetadataSchema.findByNamespace(
                            context, schema_namespace);
            MetadataField[] fields = MetadataField.findAllInSchema(
                            context, schema.getSchemaID());
            List<MetadataField> metas = new ArrayList<MetadataField>();
            for ( MetadataField field : fields ) 
            {
                if ( field.getElement().equals(element) ) {
                    metas.add(field);
                }
            }
            return metas.toArray(new MetadataField[metas.size()]);
        
        } catch (SQLException e) {
        }
        return new MetadataField[0];
	}

    private Table create_table( Division div, String schema, String element, String qualifier, String show_only_if_value) throws WingException 
    {
        return create_table( div, schema, element, qualifier, null, show_only_if_value );
    }
	
	private Table create_table(
	                Division div, 
	                String schema, 
	                String element, 
	                String qualifier, 
	                Table in_table,
	                String show_only_if_value) throws WingException 
	{
	    boolean show_relation = in_table != null;
	    Table wftable = in_table != null ? 
	                        in_table : div.addTable("specific-metadata", 1, 3);
        ItemIterator item_iter = null;
		try {
			item_iter = Item.findByMetadataField(
				context, schema, element, qualifier, Item.ANY, false);

			if ( item_iter != null )
			{
				if (!item_iter.hasNext() ) {
				    if ( !show_relation ) {
				        wftable.setHead( "No items found" );
				    }
				}else {
				    Row wfhead = wftable.addRow(Row.ROLE_HEADER);
					// table items - because of GUI not all columns could be shown
					wfhead.addCellContent("#");
					wfhead.addCellContent("NAME");
					if ( show_relation ) {
	                    wfhead.addCellContent("relation");
					}
					wfhead.addCellContent("VALUE");
					
					int i = 0;
					while ( item_iter.hasNext() )
					{
						
						Item item = item_iter.next();
						String handle = item.getHandle();
                        Metadatum[] values = item.getMetadata(schema, element, qualifier, Item.ANY);
                        for ( Metadatum dcval : values ) {
                            // if only specific values should be shown
                            if ( show_only_if_value != null ) 
                            {
                                if ( show_only_if_value.startsWith("!") ) {
                                    if ( show_only_if_value.substring(1).equals(dcval.value) ) {
                                        continue;
                                    }
                                }else if (!show_only_if_value.equals(dcval.value) ) {
                                    continue;
                                }
                            }
                            Row wsrow = wftable.addRow(Row.ROLE_DATA);
                            wsrow.addCell().addContent( String.valueOf(i + 1) );
                            wsrow.addCell().addXref( 
    							String.format( "%s/handle/%s", contextPath, handle != null ? handle : "null" ), 
    							item.getName() );
    	                    if ( show_relation ) {
    	                        wsrow.addCell().addContent(
    	                            String.format("%s.%s.%s", dcval.schema, dcval.element, dcval.qualifier ) );
    	                    }
    						wsrow.addCell().addXref( dcval.value, dcval.value );
    						i++;
                        }
					}
				}
			}
		}catch( IllegalArgumentException e1 ) {
			div.addPara( "No items - " + e1.getMessage() );
		}catch( Exception e2 ) {
			div.addPara( "Exception - " + e2.toString() );
		}
		
		return wftable;
	}
}





