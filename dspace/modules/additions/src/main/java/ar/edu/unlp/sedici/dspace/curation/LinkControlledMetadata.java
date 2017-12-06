package ar.edu.unlp.sedici.dspace.curation;

import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
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
public class LinkControlledMetadata extends AbstractCurationTask {

	private Pattern pattern = Pattern.compile("^(.*):::(.*)$");
	
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
            
            // Obtenemos los metadatos y los recorremos
            //FIXME Sería mas eficiente levantar la configuración y testear solo aquellos metadatos que sean controlados
            processSchema(item, "sedici");
            processSchema(item, "thesis");
            processSchema(item, "eprints");
            processSchema(item, "mods");
            
            report("------------------------------------------------------------");
            
            try {
				item.update();
			} catch (Exception e) {
				e.printStackTrace();
				setResult("Exception en Item "+item.getID()+": ["+e.getClass().getName()+"]"+e.getMessage());
				return Curator.CURATE_ERROR;
			}
            
            setResult("Item "+item.getID()+" actualizado");
            return Curator.CURATE_SUCCESS;
            
        } else {
           setResult("Omitido por no ser Item");
           return Curator.CURATE_SKIP;
        }
	}
	
	private void processSchema(Item item, String schema) {
        Metadatum[] metadata = item.getMetadata(schema, Item.ANY, Item.ANY, Item.ANY);
        item.clearMetadata(schema, Item.ANY, Item.ANY, Item.ANY);
        for (Metadatum m : metadata) {
        	Matcher matcher = pattern.matcher(m.value);
        	if(matcher.matches()) {
        		String old_value = m.value;
        		
        		m.authority = matcher.group(1);
        		m.value = matcher.group(2);
        		
        		if("0".equals( m.authority )) {
        			m.authority = null;
        			m.confidence = Choices.CF_NOTFOUND;
        		} else {
        			m.confidence = Choices.CF_ACCEPTED;
        		}
        		report("["+m.schema+"."+m.element+"."+m.qualifier+"] "+old_value+" ==> ("+m.authority+") "+m.value);
        	}

        	// Insertamos el metadato actualizado
        	item.addMetadata(m.schema, m.element, m.qualifier, m.language, m.value, m.authority, m.confidence);
        }
	}

}
