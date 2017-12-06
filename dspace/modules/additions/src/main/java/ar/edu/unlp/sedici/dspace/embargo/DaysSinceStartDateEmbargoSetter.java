package ar.edu.unlp.sedici.dspace.embargo;

import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * Plugin implementation of the embargo setting function. The parseTerms()
 * method performs a look-up to a table that relates a terms expression to a
 * fixed number of days. Table constructed from a dspace.cfg property with
 * syntax:
 * 
 * embargo.terms.days = 90 days:90,1 year:365,2 years:730
 * 
 * 
 * If field named embargo.field.startDate (dc.date.created by default) is empty , NOW is used
 * as relative Date in order to calculate the end of embargo period (lift)
 */
public class DaysSinceStartDateEmbargoSetter extends DaysEmbargoSetter {

    private static String startDate_schema = null;
    private static String startDate_element = null;
    private static String startDate_qualifier = null;

	private static Logger log = Logger.getLogger(DaysSinceStartDateEmbargoSetter.class);

//	private Properties termProps = new Properties();

	private static String startDate = "dc.date.issued";

	public DaysSinceStartDateEmbargoSetter() {
		super();
//		
//        String terms = ConfigurationManager.getProperty("embargo.terms.days");
//        if (terms != null && terms.length() > 0) {
//            for (String term : terms.split(",")) {
//                String[] parts = term.trim().split(":");
//                termProps.setProperty(parts[0].trim(), parts[1].trim());
//            }
//        }
//        
        //
		String cfg_startDate = ConfigurationManager.getProperty("embargo.field.startDate");
		
		if (cfg_startDate != null && !"".equals(cfg_startDate.trim()))
		{
			startDate=cfg_startDate;
		}
	    String sa[] = startDate.split("\\.", 3);
	    startDate_schema=sa[0];
	    startDate_element=sa.length > 1 ? sa[1] : null;
	    startDate_qualifier=sa.length > 2 ? sa[2] : null;
	    
	    if (startDate_element == null)
	    	throw new IllegalStateException(
				 "Invalid DSpace configuration property 'embargo.field.startDate' for EmbargoSetter, it should be like dc.element.qualifier or at least dc.element");

	    Context context;
		try {
			context = new Context();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
		
		try {
			MetadataSchema schema = MetadataSchema.find(context, startDate_schema);
			if (schema == null) 
				throw new IllegalStateException("Unknown MetadataSchema '"+startDate_schema+"' on DSpace configuration property 'embargo.field.startDate' ("+startDate+")");
			MetadataField field = MetadataField.findByElement(context, schema.getSchemaID(), startDate_element, startDate_qualifier);
			if (field == null) 
				throw new IllegalStateException("Unknown MetadataField '"+startDate+"' on DSpace configuration property 'embargo.field.startDate'");
			context.complete();
		} catch (SQLException e) {
			e.printStackTrace();
			context.abort();
			throw new IllegalStateException(e);
		}
	    
	    log.info("Se inicialza correctamente el EmbargoSetter del módulo de embargo. Se usa el campo ("+startDate+") como Fecha de inicio de los embargos.");
	}

	protected Date getEmbargoStartDate(Item item) {
		Metadatum embargoStartDates[] = item.getMetadataByMetadataString(startDate);
		if (embargoStartDates.length > 0)
			return new DCDate(embargoStartDates[0].value).toDate();
		else 
			return null;
	}
}
