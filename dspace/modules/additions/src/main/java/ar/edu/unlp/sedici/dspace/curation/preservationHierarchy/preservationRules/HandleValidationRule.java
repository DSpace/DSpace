package ar.edu.unlp.sedici.dspace.curation.preservationHierarchy.preservationRules;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.content.Item;
import org.dspace.content.Metadatum;

import ar.edu.unlp.sedici.dspace.curation.preservationHierarchy.Reporter;


/**
 * @author terru
 * Toma el handle del item y evalúa:
 * 		que el handle exista 
 * 		que no sea 123456789
 * 		que sea igual al item.getHandle();
 */

public class HandleValidationRule extends Rule {
	/* (non-Javadoc)
	 * @see ar.edu.unlp.sedici.dspace.curation.preservationHierarchy.preservationRules.Rule#evaluate(org.dspace.content.Item)
	 * Extiende la clase abstracta, para poder validar Handle
	 */
	@Override
	public int evaluate(Item item, Reporter reporter) {
		Metadatum[] dcHandle = item.getMetadataByMetadataString("dc.identifier.uri");
		String handle = item.getHandle();
		if(dcHandle.length == 0){
		String Msg = "El ítem: "+item.getID()+" fue evaluado con 0 porque no posee Handle";
		reporter.addToItemReport(Msg);
		return 0;
		}
		Pattern pattern = Pattern.compile("\\d+/\\d+");
		Matcher matcher = pattern.matcher(dcHandle[0].value);
		matcher.find();
		String handleValue = matcher.group();
		if(handleValue.equals("123456789")){
			String Msg = "El item: "+item.getID()+" fue evaluado con 0 porque su Handle es el predefinido para Dspace";
			reporter.addToItemReport(Msg);
			return 0;
		}
		if(!handleValue.equals(handle)){
			String Msg = "El item: "+item.getID()+" fue evaluado con 0 porque su handle es diferente al que debería tener\n";
			Msg += "El handle del item hallado para la comparación fue :"+handleValue;
			reporter.addToItemReport(Msg);
			return 0;
		}
		String Msg = "El Handle del item: "+item.getID()+" es válido, el item fue evaluado con 1";
		reporter.addToItemReport(Msg);
		return 1;
	}
}
