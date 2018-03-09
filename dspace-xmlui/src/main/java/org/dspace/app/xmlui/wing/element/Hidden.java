/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

/**
 * A class representing a hidden input control. The hidden input control is
 * never displayed to the user but is passed back to the server when the user
 * submits a form.
 * 
 * @author Scott Phillips
 */

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;

public class Hidden extends Field
{

    /**
     * Construct a new field.
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * 
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
    protected Hidden(WingContext context, String name, String rend)
            throws WingException
    {
        super(context, name, Field.TYPE_HIDDEN, rend);
        this.params = new Params(context);
    }

    /** ******************************************************************** */
    /** Values * */
    /** ******************************************************************** */

    /**
     * Set the raw value of the field removing any previous raw values.
     * @return a new Value.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Value setValue() throws WingException
    {
        removeValueOfType(Value.TYPE_RAW);
        Value value = new Value(context, Value.TYPE_RAW);
        values.add(value);
        return value;
    }

    /**
     * Set the raw value of the field removing any previous raw values.
     * 
     * @param characters
     *            (May be null) Field value as a string
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setValue(String characters) throws WingException
    {
        Value value = this.setValue();
        value.addContent(characters);
    }

    /**
     * Set the raw value of the field removing any previous raw values.
     * 
     * @param integer
     *            Field value as an integer
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setValue(int integer) throws WingException
    {
    	setValue(String.valueOf(integer));
    }
    
    /**
     * Set the raw value of the field removing any previous raw values.
     * 
     * @param message
     *            (Required) A key into the i18n catalogue for translation into
     *            the user's preferred language.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setValue(Message message) throws WingException
    {
        Value value = this.setValue();
        value.addContent(message);
    }
}
