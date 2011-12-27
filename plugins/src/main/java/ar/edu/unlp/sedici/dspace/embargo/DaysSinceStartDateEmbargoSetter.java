package ar.edu.unlp.sedici.dspace.embargo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.embargo.DayTableEmbargoSetter;
import org.dspace.embargo.EmbargoManager;

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
public class DaysSinceStartDateEmbargoSetter extends DayTableEmbargoSetter {

    private static String startDate_schema = null;
    private static String startDate_element = null;
    private static String startDate_qualifier = null;
    
//    item.clearMetadata(lift_schema, lift_element, lift_qualifier, Item.ANY);
//  item.addMetadata(lift_schema, lift_element, lift_qualifier, null, slift);
//    

    
	private static Logger log = Logger.getLogger(EmbargoManager.class);

	private Properties termProps = new Properties();

	private static String startDate = "dc.date.created";

	public DaysSinceStartDateEmbargoSetter() {
		super();

		String cfg_startDate = ConfigurationManager.getProperty("embargo.field.startDate");
		
		if (cfg_startDate  != null || "".equals(startDate.trim()))
			throw new IllegalStateException(
					"Missing required DSpace configuration property 'embargo.field.startDate' for EmbargoManager, check your configuration file.");
		
	    String sa[] = startDate.split("\\.", 3);
	    startDate_schema=sa[0];
	    startDate_element=sa.length > 1 ? sa[1] : null;
	    startDate_qualifier=sa.length > 2 ? sa[2] : null;
	    
	    if (startDate_element == null)
	    	throw new IllegalStateException(
				 "Invalid DSpace configuration property 'embargo.field.startDate' for EmbargoManager, it should be like dc.element.qualifier or at least dc.element");

//	    MetadataField.findByElement(Context context, int schemaID,
//	            String element, String qualifier)
	}

	/**
	 * Parse the terms into a definite date. Only terms expressions processed
	 * are those defined in 'embargo.terms.days' configuration property.
	 * 
	 * @param context
	 *            the DSpace context
	 * @param item
	 *            the item to embargo
	 * @param terms
	 *            los dias de embargo
	 * @return parsed date in DCDate format
	 */
	public DCDate parseTerms(Context context, Item item, String terms)
			throws SQLException, AuthorizeException, IOException {
		Date embargoStartDate = this.checkEmbargoStartDate(item);

		if (terms != null) {
			if (termsOpen.equals(terms)) {
				return EmbargoManager.FOREVER;
			}
			
			String days = termProps.getProperty(terms);
			if (days != null && days.length() > 0) {
				long lift = embargoStartDate.getTime()
						+ (Long.parseLong(days) * 24 * 60 * 60 * 1000);
				if (System.currentTimeMillis() < lift) {
					// el embargo sigue vigente, se le pone embargo
					return new DCDate(new Date(lift));
				}else{
					log.debug("Se ignora el embargo del doc "+item.getHandle()+" porque cae en el pasado ("+new Date(lift).toString()+")");
				}
			}
		}
		return null;
	}

	private Date checkEmbargoStartDate(Item item) {
		DCValue embargoStartDates[] = item.getMetadata(startDate);
		DCDate embargoStartDate;
		if (embargoStartDates.length > 1){
			embargoStartDate = new DCDate(embargoStartDates[0].value);
		}else{
			// Si no hay un campo inicial para el embargo, uso el current
			log.debug("No se encontro una fecha para el inicio de embargo ("+startDate+") del doc "+item.getHandle()+" , se usa el dia de hoy");
			embargoStartDate  = new DCDate(new Date());
//			item.clearMetadata(lift_schema, lift_element, lift_qualifier, Item.ANY);
		    item.addMetadata(startDate_schema, startDate_element, startDate_qualifier, null, embargoStartDate.toString());
		}
		
		return embargoStartDate.toDate();
	}
}
