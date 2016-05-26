/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;

/**
 * A class representing the Button input control. The button input control
 * allows the user to activate a form submit, where the form information is sent
 * back to the server.
 *
 * @author Scott Phillips
 */
public class Button extends Field
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
     * @throws org.dspace.app.xmlui.wing.WingException on error.
     */
    protected Button(WingContext context, String name, String rend)
            throws WingException
    {
        super(context, name, Field.TYPE_BUTTON, rend);

        params = new Params(context);
    }
    
    /**
     * Set the button's label, removing any previous label's
     * 
     * @return A button label's value.
     * @throws org.dspace.app.xmlui.wing.WingException on error.
     */
    public Value setValue() throws WingException
    {
        removeValueOfType(Value.TYPE_RAW);
        Value value = new Value(context, Value.TYPE_RAW);
        values.add(value);
        return value;
    }

    /**
     * Set the button's label, removing any previous label's
     * 
     * @param characters
     *            (May be null) The button's label as a string.
     * @throws org.dspace.app.xmlui.wing.WingException on error.
     */
    public void setValue(String characters) throws WingException
    {
        Value value = this.setValue();
        value.addContent(characters);
    }

    /**
     * Set the button's label, removing any previous labels.
     * 
     * @param translated
     *            (Required) A key into the i18n catalogue for translation into
     *            the user's preferred language.
     * @throws org.dspace.app.xmlui.wing.WingException on error.
     */
    public void setValue(Message translated) throws WingException
    {
        Value value = this.setValue();
        value.addContent(translated);
    }
}
