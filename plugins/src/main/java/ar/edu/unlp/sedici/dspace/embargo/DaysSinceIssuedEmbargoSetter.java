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
 * If embargo.field.startDate (dc.date.issued by default) is empty , NOW is used
 * as relative Date in order to calculate the end of embargo period (lift)
 */
public class DaysSinceIssuedEmbargoSetter extends DayTableEmbargoSetter {

	private static Logger log = Logger.getLogger(EmbargoManager.class);

	private Properties termProps = new Properties();

	private static String startDate = null;// dc.date.issued

	public DaysSinceIssuedEmbargoSetter() {
		super();

		startDate = ConfigurationManager.getProperty("embargo.field.startDate");
		if (startDate == null || "".equals(startDate.trim()))
			throw new IllegalStateException(
					"Missing required DSpace configuration property 'embargo.field.startDate' for EmbargoManager, check your configuration file.");
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
		if (terms != null) {
			if (termsOpen.equals(terms)) {
				return EmbargoManager.FOREVER;
			}
			String days = termProps.getProperty(terms);
			if (days != null && days.length() > 0) {
				long lift = this.getEmbargoStartDate(item).getTime()
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

	private Date getEmbargoStartDate(Item item) {
		DCValue embargoStartDate[] = item.getMetadata(startDate);

		if (embargoStartDate.length > 1)
			return new DCDate(embargoStartDate[0].value).toDate();

		// Si no hay un campo inicial para el embargo, uso el current
		log.debug("No se encontro una fecha para el inicio de embargo ("+startDate+") del doc "+item.getHandle()+" , se usa el dia de hoy");
		return new Date();
	}
}
