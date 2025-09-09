/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.app.ldn.NotifyPatternToTrigger;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Service interface class for the {@link NotifyPatternToTrigger} object.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public interface NotifyPatternToTriggerService {

    /**
     * find all notify patterns to be triggered
     *
     * @param context the context
     * @return all notify patterns to be trigger
     * @throws SQLException if database error
     */
    public List<NotifyPatternToTrigger> findAll(Context context) throws SQLException;

    /**
     * find list of Notify Patterns To be Triggered by item
     *
     * @param context the context
     * @param item the item of NotifyPatternToTrigger
     * @return the matched NotifyPatternToTrigger list by item
     * @throws SQLException if database error
     */
    public List<NotifyPatternToTrigger> findByItem(Context context, Item item)
        throws SQLException;

    /**
     * find list of Notify Patterns To be Triggered by item and pattern
     *
     * @param context the context
     * @param item the item of NotifyPatternToTrigger
     * @param pattern the pattern of NotifyPatternToTrigger
     *
     * @return the matched NotifyPatternToTrigger list by item and pattern
     * @throws SQLException if database error
     */
    public List<NotifyPatternToTrigger> findByItemAndPattern(Context context, Item item, String pattern)
        throws SQLException;

    /**
     * create new notifyPatternToTrigger
     *
     * @param context the context
     * @return the created NotifyPatternToTrigger
     * @throws SQLException if database error
     */
    public NotifyPatternToTrigger create(Context context) throws SQLException;

    /**
     * update the provided notifyPatternToTrigger
     *
     * @param context the context
     * @param notifyPatternToTrigger the notifyPatternToTrigger
     * @throws SQLException if database error
     */
    public void update(Context context, NotifyPatternToTrigger notifyPatternToTrigger) throws SQLException;

    /**
     * delete the provided notifyPatternToTrigger
     *
     * @param context the context
     * @param notifyPatternToTrigger the notifyPatternToTrigger
     * @throws SQLException if database error
     */
    public void delete(Context context, NotifyPatternToTrigger notifyPatternToTrigger) throws SQLException;

}
