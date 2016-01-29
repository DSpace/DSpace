/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.importexport;

public interface IBulkChangeNested extends IBulkChange {

	String getParentSourceID();

	String getParentSourceRef();

	String getParentCrisID();

}
