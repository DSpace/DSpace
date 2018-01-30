/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

public interface IViewer {
	public final String BITSTREAM_SCHEMA = "bitstream";
	public final String VIEWER_ELEMENT = "viewer";
	public final String PROVIDER_QUALIFIER = "provider";
	
	public final String[] METADATA_PROVIDER = new String[]{BITSTREAM_SCHEMA, VIEWER_ELEMENT, PROVIDER_QUALIFIER};
	public final String METADATA_STRING_PROVIDER = BITSTREAM_SCHEMA + "." + VIEWER_ELEMENT + "."+ PROVIDER_QUALIFIER;
	
	public final String MASTER_ELEMENT = "master";
	public final String HIDENOTPRIMARY_QUALIFIER = "hidenotprimary";
	public final String[] METADATA_MASTER = new String[]{BITSTREAM_SCHEMA, MASTER_ELEMENT, null};
	public final String METADATA_STRING_MASTER = BITSTREAM_SCHEMA + "." + MASTER_ELEMENT;
	public final String STOP_DOWNLOAD = "nodownload";
	public final String METADATA_STRING_HIDENOTPRIMARY = BITSTREAM_SCHEMA + "." + VIEWER_ELEMENT + "." + HIDENOTPRIMARY_QUALIFIER;
}