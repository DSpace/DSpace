package ar.edu.unlp.sedici.dspace.authority;

import java.util.ArrayList;
import java.util.List;

import ar.edu.unlp.sedici.sedici2003.model.TesaurosTermino;

public class SeDiCI2003Tesauro extends SeDiCI2003Hierarchy {

	@Override
	/*
	 * Nota: includeSelf se ignora este campo porque no tiene sentido el campo en tesauro
	 * */
	protected List<Object> getSeDiCI2003HierarchyElements(String text, String[] parents, boolean includeChilds, boolean includeSelf, int start, int limit) {
		List<TesaurosTermino> resultados = TesaurosTermino.findAll(text, parents, includeChilds, start, limit);
		return new ArrayList<Object>(resultados);
	}

	@Override
	protected String getSeDiCI2003EntityLabel(String field, String key) {
		TesaurosTermino t = TesaurosTermino.findTesaurosTermino(key);
		if (t == null){
			this.reportMissingAuthorityKey(field, key);
			return key;
		}else{
			return t.getNombreEs();
		}
	}

	@Override
	protected String getAuthority(Object entity) {
		return ((TesaurosTermino) entity).getId();
	}

	@Override
	protected String getLabel(Object entity) {
		String separador = "::";
		String camino = TesaurosTermino.getCamino((TesaurosTermino)entity, separador);
		return ((TesaurosTermino) entity).getNombreEs() + " - " + camino + separador + ((TesaurosTermino) entity).getNombreEs();
	}

	@Override
	protected String getValue(Object entity) {
		return ((TesaurosTermino) entity).getNombreEs();
	}

}
