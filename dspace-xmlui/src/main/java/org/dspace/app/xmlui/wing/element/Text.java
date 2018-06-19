/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

/**
 * A class representing a text input control. The text input control allows the
 * user to enter one-line of text.
 *
 * @author Scott Phillips
 */

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;

public class Text extends Field
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
    protected Text(WingContext context, String name, String rend)
            throws WingException
    {
        super(context, name, Field.TYPE_TEXT, rend);
        this.params = new Params(context);
    }

    /**
     * Set the size of the text field.
     *
     * @param size
     *            (May be zero for no defined value) he default size for a
     *            field.
     */
    public void setSize(int size)
    {
        this.params.setSize(size);
    }

    /**
     * Set the size and maximum size of the text field.
     *
     * @param size
     *            (May be zero for no defined value) he default size for a
     *            field.
     * @param maxLength
     *            (May be zero for no defined value) The maximum length that the
     *            theme should accept for form input.
     */
    public void setSize(int size, int maxLength)
    {
        this.params.setSize(size);
        this.params.setMaxLength(maxLength);
    }
    
    /**
     * Enable the add operation for this field. When this is enabled the
     * front end will add a button to add more items to the field.
     *
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void enableAddOperation() throws WingException
    {
        this.params.enableAddOperation();
    }

    /**
     * Enable the delete operation for this field. When this is enabled then
     * the front end will provide a way for the user to select fields (probably
     * checkboxes) along with a submit button to delete the selected fields.
     *
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void enableDeleteOperation()throws WingException
    {
        this.params.enableDeleteOperation();
    }
    
    
    /** ******************************************************************** */
    /** Raw Values * */
    /** ******************************************************************** */

    /**
     * Set the raw value of the field removing any previous raw values.
     * @return the new value.
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
    
    /**
     * Set the authority value of the field removing any previous authority values.
     * Initialized to an empty value.
     * @return the new value.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Value setAuthorityValue() throws WingException
    {
        return setAuthorityValue("", "UNSET");
    }
    
    /**
     * Set the authority value of the field removing any previous authority values.
     * Initialized to an empty value.
     * @param characters new value.
     * @param confidence confidence in this value
     * @return the new value.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Value setAuthorityValue(String characters, String confidence) throws WingException
    {
        this.removeValueOfType(Value.TYPE_AUTHORITY);
        Value value = new Value(context, Value.TYPE_AUTHORITY, confidence);
        value.addContent(characters);
        values.add(value);
        return value;
    }
    
    /**
     * Add a field instance
     * @return instance
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Instance addInstance() throws WingException
    {
        Instance instance = new Instance(context);
        instances.add(instance);
        return instance;
    }
}
