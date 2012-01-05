package ar.edu.unlp.sedici.dspace.authority;

import java.util.ArrayList;
import java.util.List;

import ar.edu.unlp.sedici.sedici2003.model.JerarquiasTermino;

public class SeDiCI2003Jerarquia extends SeDiCI2003Hierarchy {

	@Override
	protected List<Object> getSeDiCI2003HierarchyElements(String text, String[] parents, boolean includeChilds, int start, int limit) {
		List<JerarquiasTermino> resultados = JerarquiasTermino.findAll(text, parents, includeChilds, start, limit);
		return new ArrayList<Object>(resultados);
	}

	@Override
	protected String getSeDiCI2003HierarchyElementLabel(String key) {
		JerarquiasTermino t = JerarquiasTermino.findJerarquiasTermino(key);
		return t.getNombreEs();
	}

	@Override
	protected String getAuthority(Object entity) {
		return ((JerarquiasTermino) entity).getId();
	}

	@Override
	protected String getLabel(Object entity) {
		return ((JerarquiasTermino) entity).getNombreEs();
	}

	@Override
	protected String getValue(Object entity) {
		return ((JerarquiasTermino) entity).getNombreEs();
	}

}
