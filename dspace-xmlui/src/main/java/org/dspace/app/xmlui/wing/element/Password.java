/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

/**
 * A class representing a password input control. The password control acts just
 * like a text control but the value being typed by the user is hidden from
 * view.
 * 
 * @author Scott Phillips
 */

import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;

public class Password extends Field
{

    /**
     * Construct a new field.
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * @param name
     *            (Required) a non-unique local identifier used to differentiate
     *            the element from its siblings within an interactive division.
     *            This is the name of the field use when data is submitted back
     *            to the server.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected Password(WingContext context, String name, String rend)
            throws WingException
    {
        super(context, name, Field.TYPE_PASSWORD, rend);

        this.params = new Params(context);
    }

    /**
     * Set the size of the password field.
     * 
     * @param size
     *            (Required) The size of the password field.
     */
    public void setSize(int size)
    {
        this.params.setSize(size);
    }

    /**
     * Set the size and maximum length of the field.
     * 
     * @param size
     *            (May be zero for no defined value) The size of the password field.
     * @param maxLength
     *            (May be zero for no defined value) the maximum length of the field.
     */
    public void setSize(int size, int maxLength)
    {
        this.params.setSize(size);
        this.params.setMaxLength(maxLength);
    }
}
