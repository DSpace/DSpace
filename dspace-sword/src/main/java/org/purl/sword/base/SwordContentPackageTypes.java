/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.purl.sword.base;


import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 *
 * @author Neil Taylor (nst@aber.ac.uk)
 */
public class SwordContentPackageTypes {

    private static Logger log = Logger.getLogger(SwordContentPackageTypes.class);

    private static Properties types;

    static
    {
        // static constructor to attempt to load the properties file
        try
        {
            types = new Properties();
            InputStream stream = SwordContentPackageTypes.class.getClassLoader().getResourceAsStream("swordContentPackageTypes.properties");
            if( stream != null )
            {
                types.loadFromXML(stream);
            }
        }
        catch(Exception ex)
        {
            log.error("Unable to load sword types property file: " + ex.getMessage());
        }
    }

    public SwordContentPackageTypes()
    {
    }

    private static SwordContentPackageTypes instance;

    public static SwordContentPackageTypes instance()
    {
       if( instance == null )
       {
           instance = new SwordContentPackageTypes();
       }
       return instance;
    }

    public boolean isValidType(String uri)
    {
        return types.containsKey(uri);
    }

    public boolean isEmpty()
    {
        return types.isEmpty();
    }

    /**
     *
     */
    public Enumeration elements()
    {

        return types.elements();
    }
    
    public Enumeration keys()
    {
        return types.keys();
    }



}
