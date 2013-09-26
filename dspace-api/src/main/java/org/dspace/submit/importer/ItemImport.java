/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
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