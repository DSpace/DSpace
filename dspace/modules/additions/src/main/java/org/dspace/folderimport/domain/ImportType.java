package org.dspace.folderimport.domain;

import java.util.EnumSet;

/**
 * Registra tipos possíveis de importação
 * @author Márcio Ribeiro Gurgel do Amaral
 *
 */
public enum ImportType {
	

	TEST(1, "jsp.dspace-admin.foldermetadataimport.type.test", "jsp.dspace-admin.foldermetadataimport.success.test"),
	WORKFLOW(2, "jsp.dspace-admin.foldermetadataimport.type.workflow", "jsp.dspace-admin.foldermetadataimport.success.workflow"),
	IMPORT(3, "jsp.dspace-admin.foldermetadataimport.type.import", "jsp.dspace-admin.foldermetadataimport.success");
	
	private Integer id;
	private String key;
	private String successMessageKey;

	private ImportType(Integer id, String key, String successMessageKey) {
		this.id = id;
		this.key = key;
		this.successMessageKey = successMessageKey;
	}

	/**
	 * Recupera instância de enum tendo como base (de busca) o <i>ID</i>
	 * @param id Parâmetro-base para busca
	 * @return Instância encontrada
	 */
	public static ImportType getById(Integer id) {
		
		for(ImportType importType : EnumSet.allOf(ImportType.class)) {
			
			if(importType.getId().equals(id)) {
				
				return importType;
			}
		}
		return null;
	}
	
	public String getSuccessMessageKey() {
		return successMessageKey;
	}

	public Integer getId() {
		return id;
	}

	public String getKey() {
		return key;
	}
	
}