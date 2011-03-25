/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.didl;

import java.security.SecureRandom;
import java.util.Random;
/**
 * Factory class for generating UUID version 4. All what this class does is
 * creating UUID version 4 objects using crypto-quality random numbers.
 * 
 * Development of this code was part of the aDORe repository project by the
 * Research Library of the Los Alamos National Laboratory.
 * 
 * This code is based on the implementation of UUID version 4 (the one that
 * uses random/pseudo-random numbers by Ashraf Amrou of the Old Dominion University
 * (Aug 14, 2003)
 *
 **/
public final class UUIDFactory
{	
    /** Random number generator */
    private Random rand = null;
    
    /**	an instance	 */
    private static UUIDFactory generator = new UUIDFactory();
        
    /** private constructor (Singleton class) */
    private UUIDFactory()
    {
        // crypto-quality random number generator
        rand = new SecureRandom();
    }
    
    /**
     * 
     * Customers of this class call this method to generete new UUID objects
     * 
     * @return a new UUID object
     * 
     **/
    public static synchronized UUID generateUUID()
    {
        return new UUID(generator.rand.nextLong(),generator.rand.nextLong());
    }
}