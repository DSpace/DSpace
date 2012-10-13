/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

/**
 * A class representing a text area input control. The text area control enables
 * to user to enter multiple lines of text.
 *
 * @author Scott Phillips
 */

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;

public class TextArea extends Field
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
     */
    protected TextArea(WingContext context, String name, String rend)
            throws WingException
    {
        super(context, name, Field.TYPE_TEXTAREA, rend);
        this.params = new Params(context);
    }

    /**
     * Set the size of the text area.
     *
     * @param rows
     *            (May be zero for no defined value) The default number of rows
     *            that the text area should span.
     * @param cols
     *            (May be zero for no defined value) The default number of
     *            columns that the text area should span.
     */
    public void setSize(int rows, int cols)
    {
        this.params.setRows(rows);
        this.params.setCols(cols);
    }

    /**
     * Set the maximum length of the field.
     *
     * @param maxLength
     *            (May be zero for no defined value) The maximum length that the
     *            theme should accept for form input.
     */
    public void setMaxLength(int maxLength)
    {
        this.params.setMaxLength(maxLength);
    }
    
    /**
     * Enable the add operation for this field. When this is enabled the
     * front end will add a button to add more items to the field.
     *
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
     */
    public void setValue(Message message) throws WingException
    {
        Value value = this.setValue();
        value.addContent(message);
    }
    
    /**
     * Set the authority value of the field removing any previous authority values.
     * Initialized to an empty value.
     */
    public Value setAuthorityValue() throws WingException
    {
        return setAuthorityValue("", "UNSET");
    }
    /**
     * Set the authority value of the field removing any previous authority values.
     *
     * @param characters
     *            (May be null) Field value as a string
     * @param confidence symbolic confidence value
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
     */
    public Instance addInstance() throws WingException
    {
        Instance instance = new Instance(context);
        instances.add(instance);
        return instance;
    }
}
