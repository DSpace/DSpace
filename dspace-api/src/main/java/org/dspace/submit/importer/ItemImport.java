package org.dspace.submit.importer;
/*
 *  Per ogni item che viene importato viene effettuato
 *  il set del metadato dc.identifier.source e il nome 
 *  della classe istanziata.
 *  Tutti gli _importer estendono questa classe.
 */
public interface ItemImport {

	public void setSource(String source);

	public String getSource();

	public void setRecord(String record);

	public String getRecord();
}