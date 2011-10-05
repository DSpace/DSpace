package org.dspace.dataonemn;

public interface Constants {

	public static final String DRYAD_CROSSWALK = "DRYAD-V3";

	public static final String DEFAULT_CHECKSUM_ALGO = "MD5";
	
	// Namespaces...
	public static final String LIST_OBJECTS_NAMESPACE = "http://dataone.org/service/types/ListObjects/0.1";

	public static final String SYS_META_NAMESPACE = "http://dataone.org/service/types/SystemMetadata/0.6";

	public static final String XSI_NAMESPACE = "http://www.w3.org/2001/XMLSchema-instance";

	public static final String MN_SERVICE_TYPES_NAMESPACE = "http://dataone.org/service/types/0.5.1";
	
	public static final String DC_TERMS_NAMESPACE = "http://purl.org/dc/terms/";
	
	public static final String DRYAD_NAMESPACE = "http://purl.org/dryad/terms/";
	
	public static final String MN_NODE_NAME = "http://datadryad.org/mn/";

	// and related schemas...
	public static final String XSD_LOCATION = "http://dataone.org/service/types/SystemMetadata/0.6 https://repository.dataone.org/software/cicore/trunk/d1_schemas/systemmetadata.xsd";
}
