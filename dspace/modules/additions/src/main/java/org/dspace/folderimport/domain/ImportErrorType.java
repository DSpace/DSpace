package org.dspace.folderimport.domain;

/**
 * Register possible values for error on metadata import
 * @author Márcio Ribeiro Gurgel do Amaral (marcio.rga@gmail.com)
 *
 */
public enum ImportErrorType {

	IMPORT("jsp.dspace-admin.foldermetadataerror.type.import"),
	EXPORT("jsp.dspace-admin.foldermetadataerror.type.export");
	
	private String i18nKey;

	private ImportErrorType(String i18nKey) {
		this.i18nKey = i18nKey;
	}

	public String getI18nKey() {
		return i18nKey;
	}
}
