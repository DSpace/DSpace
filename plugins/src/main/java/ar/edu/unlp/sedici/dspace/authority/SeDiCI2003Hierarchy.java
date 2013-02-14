package ar.edu.unlp.sedici.dspace.authority;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.content.authority.Choice;
import org.dspace.core.ConfigurationManager;

public abstract class SeDiCI2003Hierarchy extends SeDiCI2003AuthorityProvider {
	// Los tipos posibles son jearquia y tesauro
	public static String JERARQUIAS_CONFIG_PROPERTY_FILTER = "sedici.choices.jerarquias.filter";
	public static String JERARQUIAS_CONFIG_PROPERTY_INCLUDE_CHILDS = "sedici.choices.jerarquias.include_childs";
	public static String JERARQUIAS_CONFIG_PROPERTY_INCLUDE_SELF = "sedici.choices.jerarquias.include_self";
	
	@Override
	protected List<Choice> findSeDiCI2003Entities(String field, String text, int start, int limit, ChoiceFactory choiceFactory) {
		// Reemplazamos los _ en el field por puntos (.)
		field = field.replace('_', '.');
		
		// Obtenemos la configuracion para el field y preparamos los filtros
		Map<String, String> props = getAuthorityConfig(field);

		// Si no especico si se incluyen los hijos, por defecto no se incluyen
		boolean includeChilds = false;
		if(props.containsKey(JERARQUIAS_CONFIG_PROPERTY_INCLUDE_CHILDS))
			includeChilds = Boolean.parseBoolean( props.get(JERARQUIAS_CONFIG_PROPERTY_INCLUDE_CHILDS) );

		boolean includeSelf = false;
		if(props.containsKey(JERARQUIAS_CONFIG_PROPERTY_INCLUDE_SELF))
			includeSelf = Boolean.parseBoolean( props.get(JERARQUIAS_CONFIG_PROPERTY_INCLUDE_SELF) );

		
		if(!props.containsKey(JERARQUIAS_CONFIG_PROPERTY_FILTER))
			throw new IllegalArgumentException("No se ha definido la propiedad "+JERARQUIAS_CONFIG_PROPERTY_FILTER+" para el campo "+field);
		
		String[] filter = props.get(JERARQUIAS_CONFIG_PROPERTY_FILTER).split(",");
		
		// Delegamos la generacion de la consulta
		List<Object> entities = getSeDiCI2003HierarchyElements(text, filter, includeChilds, includeSelf, start, limit);
		
		// Generamos los Choices
		List<Choice> choices = new ArrayList<Choice>(entities.size());
		for (Object entity : entities) {
			choices.add( choiceFactory.createChoice(getAuthority(entity), getValue(entity), getLabel(entity)) );
		}

		return choices;
	}
	
    protected int findSeDiCI2003EntitiesCount(String field, String text){
    	return 0;
    }

	
	/**
	 * Accede a la configuración y retorna la lista de IDs padres que se usarán para filtrar de la tabla de jerarquias
	 */
	protected Map<String, String> getAuthorityConfig(String field) {
		Map<String, String> props = new HashMap<String, String>();
		props.put(JERARQUIAS_CONFIG_PROPERTY_FILTER, ConfigurationManager.getProperty( JERARQUIAS_CONFIG_PROPERTY_FILTER + "." + field ));
		props.put(JERARQUIAS_CONFIG_PROPERTY_INCLUDE_CHILDS, ConfigurationManager.getProperty( JERARQUIAS_CONFIG_PROPERTY_INCLUDE_CHILDS + "." + field ));
		props.put(JERARQUIAS_CONFIG_PROPERTY_INCLUDE_SELF, ConfigurationManager.getProperty( JERARQUIAS_CONFIG_PROPERTY_INCLUDE_SELF + "." + field ));
		
		return props;
	}
	
	
	/**
	 * Retorna el conjunto de Entities de SeDiCI2003 según los filtros indicados
	 * @return
	 */
	protected abstract List<Object> getSeDiCI2003HierarchyElements(String text, String[] parents, boolean includeChilds, boolean includeSelf, int start, int limit);
	
	protected abstract String getAuthority(Object entity);
	
	protected abstract String getLabel(Object entity);
	
	protected abstract String getValue(Object entity);
	
}
