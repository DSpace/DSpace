/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.correctiontype;

import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.QAEvent;
import org.dspace.core.Context;
import org.dspace.qaevent.service.dto.QAMessageDTO;

/**
 * Interface class that model the CorrectionType.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public interface CorrectionType {

    /**
     * Retrieves the unique identifier associated to the CorrectionType.
     */
    public String getId();

    /**
     * Retrieves the topic associated with the to the CorrectionType.
     */
    public String getTopic();

    /**
     * Checks whether the CorrectionType required related item.
     */
    public boolean isRequiredRelatedItem();

    /**
     * Checks whether target item is allowed for current CorrectionType
     * 
     * @param context              Current DSpace session
     * @param targetItem           Target item
     * @throws AuthorizeException  if authorize error
     * @throws SQLException        if there's a database problem
     */
    public boolean isAllowed(Context context, Item targetItem) throws AuthorizeException, SQLException;

    /**
     * Checks whether target item and related item are allowed for current CorrectionType
     * 
     * @param context               Current DSpace session
     * @param targetItem            Target item
     * @param relatedItem           Related item
     * @throws AuthorizeException   if authorize error
     * @throws SQLException         if there's a database problem
     */
    public boolean isAllowed(Context context, Item targetItem, Item relatedItem) throws AuthorizeException,SQLException;

    /**
     * Creates a QAEvent for a specific target item.
     * 
     * @param context      Current DSpace session
     * @param targetItem   Target item
     * @param reason       Reason
     * @return             QAEvent
     */
    public QAEvent createCorrection(Context context, Item targetItem, QAMessageDTO reason);

    /**
     *  Creates a QAEvent for a target item and related item.
     * @param context      Current DSpace session
     * @param targetItem   Target item
     * @param relatedItem  Related item
     * @param reason       Reason
     * @return             QAEvent
     */
    public QAEvent createCorrection(Context context, Item targetItem, Item relatedItem, QAMessageDTO reason);

}
