/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.embargo;

import java.sql.SQLException;
import java.util.Date;
import java.util.Properties;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Plugin implementation of the embargo setting function. The parseTerms()
 * method performs a look-up to a table that relates a terms expression
 * to a fixed number of days. Table constructed from a dspace.cfg property
 * with syntax:
 *
 * {@code embargo.terms.days = 90 days:90,1 year:365,2 years:730}
 *
 * That is, an arbitrary, comma separated, list of {@code <terms>:<days>} where {@code <terms>}
 * can be any string and {@code <days>} must be a positive integer.
 * All the {@code <terms>} fields should be defined in a 'value-pairs' element,
 * and the field configured as the embargo terms should employ a drop-down using
 * that element in input_forms.xml, if user submission is desired.
 * 
 * @author Richard Rodgers
 */
public class DayTableEmbargoSetter extends DefaultEmbargoSetter
{
    public DayTableEmbargoSetter() {
        super();
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
    @Override
    public DCDate parseTerms(Context context, Item item, String terms)
        throws SQLException, AuthorizeException {

        String termsOpen = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("embargo.terms.open");
        Properties termProps = getTermProperties();

    	if (terms != null) {
            if (termsOpen.equals(terms)) {
                return EmbargoServiceImpl.FOREVER;
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

    /**
     * Get term properties from configuration
     * @return Properties
     */
    private Properties getTermProperties()
    {
        Properties termProps = new Properties();

        String terms[] = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("embargo.terms.days");

        if (terms != null) {
            for (String term : terms) {
                String[] parts = term.trim().split(":");
                termProps.setProperty(parts[0].trim(), parts[1].trim());
            }
        }

        return termProps;
    }
}
