package ar.edu.unlp.sedici.dspace.xoai.filters;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.xoai.data.DSpaceDatabaseItem;
import org.dspace.xoai.data.DSpaceItem;
import org.dspace.xoai.exceptions.InvalidMetadataFieldException;
import org.dspace.xoai.filter.results.SolrFilterResult;
import org.dspace.xoai.filter.results.DatabaseFilterResult;

import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Element.Field;

/*
 * @author sedici.unlp.edu.ar - UNLP
 * This class extends the BundleOriginalFilter. Its add a new condition that make this filter
 * less restrictive. This filter ask about the existence of one of two conditions: 
 * 		-exists any bundle original
 * 		-exists an electronic location (sedici.identifier.uri)
 * If one of the 2 conditions is true, then makes the filter be true too.
 */
public class DspaceExistsBundleOriginalOrElectronicLocationFilter extends DspaceExistsBundleOriginalFilter{
	
	/*
	 * @since April 2014
	 */
		private static Logger log = LogManager
	            .getLogger(DspaceExistsBundleOriginalOrElectronicLocationFilter.class);
	/*
	 * "sedici.identifier.uri" represents the external location of a bitstreams of 
	 * an item out of the repository.
	 */
	public static final String solrQueryField = "sedici.identifier.uri";
	
	public DatabaseFilterResult buildDatabaseQuery(Context context){
		try{
			
			String schema="sedici", element="identifier", qualifier = "uri";
			
			DatabaseFilterResult filter = super.buildDatabaseQuery(context);
			String whereBundleOriginal = filter.getQuery();
			ArrayList<Object> params = (ArrayList<Object>)filter.getParameters();
			MetadataSchema schemaID = MetadataSchema.find(context, schema);
			params.add(MetadataField.findByElement(context, schemaID.getSchemaID(), "identifier", qualifier));
			return new DatabaseFilterResult(whereBundleOriginal + " OR " + 
						"EXISTS (SELECT tmp.* FROM metadatavalue tmp WHERE tmp.item_id=i.item_id AND tmp.metadata_field_id=?)",
                    params);
		}
        catch (SQLException e)
        {
            log.error(e.getMessage(), e);
        }
        return new DatabaseFilterResult();
	}
	
	public SolrFilterResult buildSolrQuery(){
		String bundleOriginalQueryFilter = DspaceExistsBundleOriginalFilter.solrQueryField;
		String electronicLocationQueryFilter = solrQueryField;
		return new SolrFilterResult("metadata."+ bundleOriginalQueryFilter + ":[* TO *] OR " +
								    "metadata." + electronicLocationQueryFilter + ":[* TO *]");
	}
	
	public boolean isShown(DSpaceItem item){
		return super.isShown(item) || (item.getMetadata(solrQueryField).size() > 0);
	}
	
	
}
