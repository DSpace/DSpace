package ar.edu.unlp.sedici.dspace.embargo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Context;
import org.dspace.embargo.DefaultEmbargoSetter;
import org.dspace.embargo.EmbargoManager;

/**
 * Plugin implementation of the embargo setting function. The parseTerms()
 * method expects a terms expression that evaluates to a fixed number of days. 
 * It should be configured using value-pairs under input-forms.xml
 * 
 * Note: this class silently ommits past embargos 
 */
public class DaysEmbargoSetter extends DefaultEmbargoSetter {

	private static Logger log = Logger.getLogger(DaysEmbargoSetter.class);

	public DaysEmbargoSetter() {
		super();

	    log.info("Se inicialza correctamente el EmbargoSetter del módulo de embargo. ");
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

		if (item.getMetadataByMetadataString("sedici.embargo.liftDate").length != 0){
			Metadatum liftDates[] = item.getMetadataByMetadataString("sedici.embargo.liftDate");
			return new DCDate(liftDates[0].value);
		}
		if (terms == null || "".equals(terms.trim())) {
			log.trace("No se aplica embargo sobre el doc "+item.getHandle()+" dado que no hay un terms definido como metadato");
			return null;
		}

		if (termsOpen.equals(terms)) {
			return EmbargoManager.FOREVER;
		}
		
		//termProps.getProperty(terms)
		Long days = Long.parseLong(terms);
		if (days == 0){
			log.trace("No se aplica embargo sobre el doc "+item.getHandle()+" dado que no hay un terms definido como metadato");
			return null;
		}
		
		Date embargoStartDate = this.getEmbargoStartDate(item);

		if (embargoStartDate ==null){
			log.trace("No hay una fecha de embargo calculada, para el doc "+item.getHandle()+", se usa el dia de hoy");
			embargoStartDate  = new Date();
		}
//		}else if (System.currentTimeMillis() < embargoStartDate.getTime()){
//			log.info("La fecha calculada para el inicio de embargo del doc "+item.getHandle()+" es mayor a NOW(), se usa el dia de hoy");
//			embargoStartDate  = new Date();
//		}
			
		long liftTime = embargoStartDate.getTime()+ (days * 24 * 60 * 60 * 1000);
		
		if (System.currentTimeMillis() < liftTime) {
			// el embargo sigue vigente, se le pone embargo
			log.info("Se aplica un embargo sobre el doc "+item.getHandle()+", finalizará en el ("+embargoStartDate+" + "+days+" dias) = ("+") = ("+new Date(liftTime).toString()+")");
			return new DCDate(new Date(liftTime));
		}else{
			log.info("Se ignora el embargo solicitado sobre el doc "+item.getHandle()+" porque cae en el pasado ("+new Date(liftTime).toString()+")");
			return null;
		}
		
	}

	protected Date getEmbargoStartDate(Item item) {
		return new Date();
	}
}
