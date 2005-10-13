/******************************************************************************
 * DSPACE DIDL MODULE UUID GENERATOR
 * AUTHOR
 *        Los Alamos National Laboratory
 *        Research Library
 *        Digital Library Research & Prototyping Team
 *        Henry Jerez
 *        2004, 2005
 *CONTACT
 *   proto@gws.lanl.gov
 *VERSION
 *  Beta 1
 *  date 07/26/2005 
 * ACKNOWLEDGMENT
 *    Development of this code is part of the aDORe repository project by the Research Library of the Los Alamos National Laboratory.
* BASED ON:
 *Implementation of UUID version 4 (the one that uses random/pseudo-random
 * numbers) 
 * By: Ashraf Amrou
 * Old Dominion University
 * Aug 14, 2003
 *****************************************************************************/
package org.dspace.app.didl;

import java.security.SecureRandom;
import java.util.Random;
/**
 * Factory class for generating UUID version 4. All what this class does is
 * creating UUID version 4 objects using crypto-quality random numbers.
 *
 **/
public final class UUIDFactory{
    
    /**
     * Random number generator
     **/
    private java.util.Random rand = null;
    /**
     *  an instance
     **/
    private static UUIDFactory generator = new UUIDFactory();
    ///////////////////////////////////////////////////    
    /**
     * private constructor (Singleton class)
     **/
    private UUIDFactory() {
        // crypto-quality random number generator
        rand = new SecureRandom();
    }
    ///////////////////////////////////////////////////
    /**
     * Customers of this class call this method to generete new UUID objects
     **/
    public synchronized static UUID generateUUID(){
        return new UUID(generator.rand.nextLong(),generator.rand.nextLong());
    }
    ///////////////////////////////////////////////////
}
