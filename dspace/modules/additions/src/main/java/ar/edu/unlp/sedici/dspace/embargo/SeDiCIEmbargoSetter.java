package ar.edu.unlp.sedici.dspace.embargo;

import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.embargo.EmbargoManager;
import org.dspace.license.CreativeCommons;

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
		Metadatum docTypes[] = item.getMetadataByMetadataString(typeMetadata);
		
		if (item.getMetadataByMetadataString("sedici2003.identifier").length != 0){
			//Es un doc importado
			Metadatum embargosViejos[] = item.getMetadataByMetadataString("sedici2003.fecha-hora-creacion");
			if (embargosViejos.length > 0){
				try{
        			return new DCDate(sediciDatetimeFormat.parse(embargosViejos[0].value)).toDate();
        		}catch(ParseException e){
        			log.warn("Error de parseo de fecha al procesar (sedici2003.fecha-hora-creacion)==("+embargosViejos[0].value+") del documento importado con id "+item.getMetadataByMetadataString("sedici2003.identifier")[0]);
                } 
			}
			
			throw new IllegalArgumentException("No se pudo procesar la fecha de creacion (sedici2003.fecha-creacion) del documento importado con id "+item.getMetadataByMetadataString("sedici2003.identifier")[0].value);
		}
		
		if (docTypes.length == 0){
			log.info("No se encontro un type para el doc "+item.getHandle());
			return null;
		}
		if (!TYPE_TESIS.equalsIgnoreCase(docTypes[0].value)){
			log.trace("El doc "+item.getHandle()+" no es una tesis, no busco el campo "+exposureDateMetadata);
			return null;
		}
		
		Metadatum exposureDates[] = item.getMetadataByMetadataString(exposureDateMetadata);
		if (exposureDates.length == 0){
			log.trace("No se encontro un campo "+exposureDateMetadata+" para el doc "+item.getHandle());
			return null;
		}
		
		return new DCDate(exposureDates[0].value).toDate();
	}
	
    /**
     * Enforce embargo by turning off all read access to bitstreams in
     * this Item.
     *
     * @param context the DSpace context
     * @param item the item to embargo
     */
    public void setEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException, IOException
    {
    	super.setEmbargo(context, item);
        item.clearMetadata("dc", "date", "available", Item.ANY);
        item.addMetadata("dc", "date", "available", null, item.getMetadataByMetadataString("dc.date.accessioned")[0].value);
        log.info("Set dc.date.available on Item "+item.getHandle());
        item.update();
    }

}
