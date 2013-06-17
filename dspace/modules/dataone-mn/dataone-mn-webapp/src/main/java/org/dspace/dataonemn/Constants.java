package org.dspace.dataonemn;

public interface Constants {

	public static final String DRYAD_CROSSWALK = "DRYAD-V3-1";

	public static final String DEFAULT_CHECKSUM_ALGO = "MD5";

        // MD5 checksum of /dev/null, for defaults:
        public static final String DEFAULT_CHECKSUM = "d41d8cd98f00b204e9800998ecf8427e";
	
        public static final String DRYAD_ADMIN = "admin@datadryad.org";

	// Namespaces...
	public static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";

	public static final String D1_TYPES_NAMESPACE = "http://ns.dataone.org/service/types/v1";
	
	public static final String DC_TERMS_NAMESPACE = "http://purl.org/dc/terms/";
	
	public static final String DRYAD_NAMESPACE = "http://datadryad.org/profile/v3.1";
	
	public static final String MN_NODE_NAME = "http://datadryad.org/mn/";
        
        public static final String ORE_NAMESPACE = "http://www.openarchives.org/ore/terms";

	// and related schemas...
	public static final String XSD_LOCATION = "http://ns.dataone.org/service/types/v1 http://ns.dataone.org/service/types/v1";
	
}
