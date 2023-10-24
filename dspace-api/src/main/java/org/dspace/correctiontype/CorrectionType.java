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

/**
 * Interface class that model the CorrectionType.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public interface CorrectionType {

    public String getId();

    public String getTopic();

    public boolean isRequiredRelatedItem();

    public boolean isAllowed(Context context, Item targetItem) throws AuthorizeException, SQLException;

    public boolean isAllowed(Context context, Item targetItem, Item relatedItem) throws AuthorizeException,SQLException;

    public QAEvent createCorrection(Context context, Item targetItem);

    public QAEvent createCorrection(Context context, Item targetItem, Item relatedItem);

}
