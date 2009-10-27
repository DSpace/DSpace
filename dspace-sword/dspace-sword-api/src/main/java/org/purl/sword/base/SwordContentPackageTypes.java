/**
 * Copyright (c) 2007-2009, Aberystwyth University
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 *  - Neither the name of the Centre for Advanced Software and
 *    Intelligent Systems (CASIS) nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
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

    public SwordContentPackageTypes()
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
     * @return
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
