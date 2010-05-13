/*
 * DayTableEmbargoSetter.java
 *
 * Version: $Revision: 1.1 $
 *
 * Date: $Date: 2009/07/16 01:52:59 $
 *
 * Copyright (c) 2002-2010, The DSpace Foundation.  All rights reserved.
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
package org.dspace.embargo;

import java.sql.SQLException;
import java.io.IOException;
import java.util.Date;
import java.util.Properties;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;

/**
 * Plugin implementation of the embargo setting function. The parseTerms()
 * method performs a look-up to a table that relates a terms expression
 * to a fixed number of days. Table constructed from a dspace.cfg property
 * with syntax:
 *
 * embargo.terms.days = 90 days:90,1 year:365,2 years:730
 *
 * That is, an arbitrary, comma separated, list of <terms>:<days> where <terms>
 * can be any string and <days> must be a positive integer.
 * All the <terms> fields should be defined in a 'value-pairs' element,
 * and the field configured as the embargo terms should employ a drop-down using
 * that element in input_forms.xml, if user submission is desired.
 * 
 * @author Richard Rodgers
 */
public class DayTableEmbargoSetter extends DefaultEmbargoSetter
{
    private Properties termProps = new Properties();
	
    public DayTableEmbargoSetter() {
        super();
        // load properties
        String terms = ConfigurationManager.getProperty("embargo.terms.days");
        if (terms != null && terms.length() > 0) {
            for (String term : terms.split(",")) {
                String[] parts = term.trim().split(":");
                termProps.setProperty(parts[0].trim(), parts[1].trim());
            }
        }
    }
    
    /**
     * Parse the terms into a definite date. Only terms expressions processed
     * are those defined in 'embargo.terms.days' configuration property.
     * 
     * @param context the DSpace context
     * @param item the item to embargo
     * @param terms the embargo terms
     * @return parsed date in DCDate format
     */
    public DCDate parseTerms(Context context, Item item, String terms)
        throws SQLException, AuthorizeException, IOException {
    	if (terms != null) {
            if (termsOpen.equals(terms)) {
                return EmbargoManager.FOREVER;
            }
            String days = termProps.getProperty(terms);
            if (days != null && days.length() > 0) {
                long lift = System.currentTimeMillis() + 
                           (Long.parseLong(days) * 24 * 60 * 60 * 1000);
                return new DCDate(new Date(lift));
            }
        }
        return null;
    }
}
