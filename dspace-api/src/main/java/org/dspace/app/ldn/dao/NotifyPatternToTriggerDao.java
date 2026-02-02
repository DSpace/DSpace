/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.dao;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.ldn.NotifyPatternToTrigger;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.GenericDAO;

/**
 * This is the Data Access Object for the {@link NotifyPatternToTrigger} object
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public interface NotifyPatternToTriggerDao extends GenericDAO<NotifyPatternToTrigger> {

    /**
     * find the NotifyPatternToTrigger matched with the provided item
     *
     * @param context the context
     * @param item the item
     * @return the NotifyPatternToTrigger matched the provided item
     * @throws SQLException if database error
     */
    public List<NotifyPatternToTrigger> findByItem(Context context, Item item) throws SQLException;

    /**
     * find the NotifyPatternToTrigger matched with the provided
     * item and pattern
     *
     * @param context the context
     * @param item the item
     * @param pattern the pattern
     * @return the NotifyPatternToTrigger matched the provided
     * item and pattern
     * @throws SQLException if database error
     */
    public List<NotifyPatternToTrigger> findByItemAndPattern(Context context, Item item, String pattern)
        throws SQLException;

}
