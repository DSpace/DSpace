package ar.edu.unlp.sedici.dspace.embargo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.dspace.content.DCDate;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.curate.Curator;

/**

 */
public class SeDiCIEmbargoSetter extends DaysEmbargoSetter {

	private static Logger log = Logger.getLogger(SeDiCIEmbargoSetter.class);
	private static SimpleDateFormat sediciDatetimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private static SimpleDateFormat sediciDateFormat = new SimpleDateFormat("yyyy-MM-dd");

	private static String exposureDateMetadata = "sedici.date.exposure";
	private static String typeMetadata = "dc.type";
	private static String TYPE_TESIS= "tesis";

	public SeDiCIEmbargoSetter() {
		super();
	    log.info("Se inicialza correctamente el SeDiCIEmbargoSetter del módulo de embargo");
	}

	protected Date getEmbargoStartDate(Item item) {
		DCValue docTypes[] = item.getMetadata(typeMetadata);
		
		if (item.getMetadata("sedici2003.identifier").length != 0){
			//Es un doc importado
			DCValue embargosViejos[] = item.getMetadata("sedici2003.fecha-hora-creacion");
			if (embargosViejos.length > 0){
				try{
        			return new DCDate(sediciDatetimeFormat.parse(embargosViejos[0].value)).toDate();
        		}catch(ParseException e){
        			log.warn("Error de parseo de fecha al procesar (sedici2003.fecha-hora-creacion)==("+embargosViejos[0].value+") del documento importado con id "+item.getMetadata("sedici2003.identifier")[0]);
                } 
			}
			
			throw new IllegalArgumentException("No se pudo procesar la fecha de creacion (sedici2003.fecha-creacion) del documento importado con id "+item.getMetadata("sedici2003.identifier")[0].value);
		}
		
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
