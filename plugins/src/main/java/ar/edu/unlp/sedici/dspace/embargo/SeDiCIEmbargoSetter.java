package ar.edu.unlp.sedici.dspace.embargo;

import java.util.Date;

import org.apache.log4j.Logger;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;

/**

 */
public class SeDiCIEmbargoSetter extends DaysEmbargoSetter {

	private static Logger log = Logger.getLogger(SeDiCIEmbargoSetter.class);

	private static String exposureDateMetadata = "sedici.date.exposure";
	private static String typeMetadata = "dc.type";
	private static String TYPE_TESIS= "tesis";

	public SeDiCIEmbargoSetter() {
		super();
	    log.info("Se inicialza correctamente el SeDiCIEmbargoSetter del módulo de embargo");
	}

	protected Date getEmbargoStartDate(Item item) {
		DCValue docTypes[] = item.getMetadata(typeMetadata);
		
		if (docTypes.length == 0){
			log.info("No se encontro un type para el doc "+item.getHandle());
			return null;
		}
		if (!TYPE_TESIS.equalsIgnoreCase(docTypes[0].value)){
			log.trace("El doc "+item.getHandle()+" no es una tesis, no busco el campo "+exposureDateMetadata);
			return null;
		}
		
		DCValue exposureDates[] = item.getMetadata(exposureDateMetadata);
		if (exposureDates.length == 0){
			log.trace("No se encontro un campo "+exposureDateMetadata+" para el doc "+item.getHandle());
			return null;
		}
		
		return new DCDate(exposureDates[0].value).toDate();
	}
}
