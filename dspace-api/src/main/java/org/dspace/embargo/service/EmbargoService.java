/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.embargo.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCDate;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

/**
 * Public interface to the embargo subsystem.
 * <p>
 * Configuration properties: (with examples)
 * {@code
 *   # DC metadata field to hold the user-supplied embargo terms
 *   embargo.field.terms = dc.embargo.terms
 *   # DC metadata field to hold computed "lift date" of embargo
 *   embargo.field.lift = dc.date.available
 *   # String to indicate indefinite (forever) embargo in terms
 *   embargo.terms.open = Indefinite
 *   # implementation of embargo setter plugin
 *   plugin.single.org.dspace.embargo.EmbargoSetter = edu.my.Setter
 *   # implementation of embargo lifter plugin
 *   plugin.single.org.dspace.embargo.EmbargoLifter = edu.my.Lifter
 * }
 * @author Larry Stone
 * @author Richard Rodgers
 */
public interface EmbargoService {

    /** Special date signalling an Item is to be embargoed forever.
     ** The actual date is the first day of the year 10,000 UTC.
     **/
    public static final DCDate FOREVER = new DCDate("10000-01-01");


    /**
     * Put an Item under embargo until the specified lift date.
     * Calls EmbargoSetter plugin to adjust Item access control policies.
     *
     * @param context the DSpace context
     * @param item the item to embargo
     */
    public void setEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException;

    /**
     * Get the embargo lift date for an Item, if any.  This looks for the
     * metadata field configured to hold embargo terms, and gives it
     * to the EmbargoSetter plugin's method to interpret it into
     * an absolute timestamp.  This is intended to be called at the time
     * the Item is installed into the archive.
     * <p>
     * Note that the plugin is *always* called, in case it gets its cue for
     * the embargo date from sources other than, or in addition to, the
     * specified field.
     *
     * @param context the DSpace context
     * @param item the item to embargo
     * @return lift date on which the embargo is to be lifted, or null if none
     */
    public DCDate getEmbargoTermsAsDate(Context context, Item item)
        throws SQLException, AuthorizeException;

    /**
     * Lift the embargo on an item which is assumed to be under embargo.
     * Call the plugin to manage permissions in its own way, then delete
     * the administrative metadata fields that dictated embargo date.
     *
     * @param context the DSpace context
     * @param item the item on which to lift the embargo
     */
    public void liftEmbargo(Context context, Item item)
        throws SQLException, AuthorizeException, IOException;

    public void checkEmbargo(Context context, Item item) throws SQLException, IOException, AuthorizeException;

    public List<MetadataValue> getLiftMetadata(Context context, Item item);

    public Iterator<Item> findItemsByLiftMetadata(Context context) throws SQLException, IOException, AuthorizeException;
}
