/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.network;

public class ConstantNetwork {
	
	public static final String CONFIG_CONNECTIONS = "network.connection";
	
	public static Integer ENTITY_PLACEHOLDER_RP = -1;
    public static Integer ENTITY_RP = 0;
    public static Integer ENTITY_DEPT = 1;
    
    public static String PREFIX_METADATA_BIBLIOMETRIC_1 = "numberscollaboration_network_";
    public static String PREFIX_METADATA_BIBLIOMETRIC_2 = "maxstrengthcollaboration_network_";
    public static String PREFIX_METADATA_BIBLIOMETRIC_3 = "averagestrengthcollaboration_network_";
    public static String PREFIX_METADATA_BIBLIOMETRIC_4 = "quadraticvariancecollaboration_network_";
    
    public static String PREFIX_METADATA_BIBLIOMETRIC_1_RETRIEVE = "rp_numberscollaboration_network_";
    public static String PREFIX_METADATA_BIBLIOMETRIC_2_RETRIEVE = "rp_maxstrengthcollaboration_network_";
    public static String PREFIX_METADATA_BIBLIOMETRIC_3_RETRIEVE = "rp_averagestrengthcollaboration_network_";
    public static String PREFIX_METADATA_BIBLIOMETRIC_4_RETRIEVE = "rp_quadraticvariancecollaboration_network_";
}
