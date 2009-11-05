/*
 * Copyright (c) 2004-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
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
    public synchronized static UUID generateUUID()
    {
        return new UUID(generator.rand.nextLong(),generator.rand.nextLong());
    }
}