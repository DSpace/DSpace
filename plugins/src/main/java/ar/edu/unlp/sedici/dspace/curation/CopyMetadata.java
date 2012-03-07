package ar.edu.unlp.sedici.dspace.curation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.authority.Choices;
import org.dspace.core.Constants;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

/**
 * Recorre los metadatos de cada Item y se fija si su valor contiene el patron determinado en AUTHORITY_SEPARATOR
 * Cuando se detecta este patron, se extrae el ID de la Authority y se guarad en el campo authority,
 * estrableciendo así el vínculo entre el metadato y el vocabulario controlado.
 * El valor del metadato se guarda nuevamente sin el id ni el separador.
 * @author nestor
 *
 */
public class CopyMetadata extends AbstractCurationTask {
	
	@Override
	public int perform(DSpaceObject dso) throws IOException {
		
		// Consideramos solo Items
        if (dso.getType() == Constants.ITEM) {
            Item item = (Item)dso;
            
            try {
				report("Procesando Item "+item.getID()+ " - canEdit: "+item.canEdit());
			} catch (SQLException e) {
				e.printStackTrace();
				setResult("Exception en Item "+item.getID()+": ["+e.getClass().getName()+"]"+e.getMessage());
				return Curator.CURATE_ERROR;
			}
            
            //copio el metadato fecha_hora_creacion
            DCValue[] metadata=item.getMetadata("sedici2003.fecha-hora-creacion");
            Boolean creacion_bool=true;
            Boolean disponibilidad_bool=true;        	
        	if (metadata.length>0){
        		item.clearMetadata("dc", "date", "accessioned", null);
	            // Insertamos el metadato actualizado
	        	item.addMetadata("dc", "date", "accessioned", null, metadata[0].value);
            } else {
            	//en caso de que no exista el metadato en cuestion reportamos esto
            	creacion_bool=false;
            	report("El item no posee el metadato sedici2003.fecha-hora-creacion");
            };
            //copio el metadato fecha_disponiblidad
            metadata=item.getMetadata("sedici2003.fecha-disponibilidad");                   	
            if (metadata.length>0){
            	item.clearMetadata("dc", "date", "available", null);
            	// Insertamos el metadato actualizado 
	        	item.addMetadata("dc", "date", "available", null, metadata[0].value);
            } else {
            	//en caso de que no exista el metadato en cuestion reportamos esto
            	disponibilidad_bool=false;
            	report("El item no posee el metadato sedici2003.fecha-disponibilidad");
            };

            report("------------------------------------------------------------");
            
            //Guardo los cambios
            try {
				item.update();
			} catch (Exception e) {
				e.printStackTrace();
				setResult("Exception en Item "+item.getID()+": ["+e.getClass().getName()+"]"+e.getMessage());
				return Curator.CURATE_ERROR;
			}
            //Si se guardaron ambos metadatos, informo la corrección de la curation, en caso contrario notifico el fallo.
            if(creacion_bool && disponibilidad_bool) {
            	setResult("Item "+item.getID()+" actualizado");
                return Curator.CURATE_SUCCESS;
            } else {
            	String result="El Item "+item.getID()+" no posee los siguientes metadatos:";
            	if (!creacion_bool){
            		result=result+" sedici2003.fecha-hora-creacion";
            	};            	
            	if (!disponibilidad_bool){
            		result=result+" sedici2003.fecha-disponibilidad";
            	}
            	
            	if (creacion_bool || disponibilidad_bool){
            		setResult("Item actualizado con errores. "+result);
            	} else {
            		setResult("Item no modificado. "+result);
            	};
            	
            	return Curator.CURATE_ERROR;
            }
            
            
        } else {
           setResult("Omitido por no ser Item");
           return Curator.CURATE_SKIP;
        }
	}


}
