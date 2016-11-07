package org.dspace.app.util;

public interface IViewer {
	public final String BITSTREAM_SCHEMA = "bitstream";
	public final String VIEWER_ELEMENT = "viewer";
	public final String PROVIDER_QUALIFIER = "provider";
	
	public final String[] METADATA_PROVIDER = new String[]{BITSTREAM_SCHEMA, VIEWER_ELEMENT, PROVIDER_QUALIFIER};
	public final String METADATA_STRING_PROVIDER = BITSTREAM_SCHEMA + "." + VIEWER_ELEMENT + "."+ PROVIDER_QUALIFIER;
	
	public final String MASTER_ELEMENT = "master";
	public final String[] METADATA_MASTER = new String[]{BITSTREAM_SCHEMA, MASTER_ELEMENT, null};
	public final String METADATA_STRING_MASTER = BITSTREAM_SCHEMA + "." + MASTER_ELEMENT;
}