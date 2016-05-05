/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

/**
 * A class representing a radio input control. Radio input controls allow the
 * user to select one value among many. If the user selects one value then all
 * other values are set to off.
 * 
 * @author Scott Phillips
 */

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;

public class Radio extends Field
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
    protected Radio(WingContext context, String name, String rend)
            throws WingException
    {
        super(context, name, Field.TYPE_RADIO, rend);
        this.params = new Params(context);
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

    /**
     * Add an option.
     * 
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @return the new option.
     * @throws org.dspace.app.xmlui.wing.WingException passed through
     */
    public Option addOption(String returnValue)
            throws WingException
    {
        Option option = new Option(context, returnValue);
        options.add(option);

        return option;
    }

    /**
     * Add an option.
     * 
     * @param selected
     *            (Required) Set the field as selected.
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @return the new option.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Option addOption(boolean selected, String returnValue)
            throws WingException
    {
        if (selected)
        {
            setOptionSelected(returnValue);
        }
        return addOption(returnValue);
    }
    
    /**
     * Add an option.
     * 
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param characters
     *            (Required) The text to set as the visible option.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addOption(String returnValue, String characters) throws WingException
    {
        Option option = this.addOption(returnValue);
        option.addContent(characters);
    }
    
    /**
     * Add an option.
     * 
     * @param selected
     *            (Required) Set the field as selected.
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param characters
     *            (Required) The text to set as the visible option.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addOption(boolean selected,String returnValue, String characters) throws WingException
    {
        if (selected)
        {
            setOptionSelected(returnValue);
        }
        addOption(returnValue,characters);
    }
    
    /**
     * Add an option.
     * 
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param characters
     *            (Required) The text to set as the visible option.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addOption(int returnValue, String characters) throws WingException
    {
        Option option = this.addOption(String.valueOf(returnValue));
        option.addContent(characters);
    }
    
    /**
     * Add an option.
     * 
     * @param selected
     *            (Required) Set the field as selected.
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param characters
     *            (Required) The text to set as the visible option.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addOption(boolean selected, int returnValue, String characters) throws WingException
    {
        if (selected)
        {
            setOptionSelected(returnValue);
        }
        addOption(returnValue,characters);
    }
    
    /**
     * Add an option.
     * 
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param message
     *            (Required) The translated text to set as the visible option.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addOption(String returnValue, Message message) throws WingException
    {
        Option option = this.addOption(returnValue);
        option.addContent(message);
    }
    
    /**
     * Add an option.
     * 
     * @param selected
     *            (Required) Set the field as selected.
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param message
     *            (Required) The translated text to set as the visible option.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addOption(boolean selected, String returnValue, Message message) throws WingException
    {
        if (selected)
        {
            setOptionSelected(returnValue);
        }
        addOption(returnValue,message);
    }
    
    /**
     * Add an option.
     * 
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param message
     *            (Required) The translated text to set as the visible option.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addOption(int returnValue, Message message) throws WingException
    {
        Option option = this.addOption(String.valueOf(returnValue));
        option.addContent(message);
    }
    
    /**
     * Add an option.
     * 
     * @param selected
     *            (Required) Set the field as selected.
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param message
     *            (Required) The translated text to set as the visible option.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addOption(boolean selected, int returnValue, Message message) throws WingException
    {
        if (selected)
        {
            setOptionSelected(returnValue);
        }
        addOption(returnValue,message);
    }
    
    /**
     * Set the given option as selected.
     * 
     * @param returnValue
     *            (Required) The return value of the option to be selected.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setOptionSelected(String returnValue) throws WingException
    {
        Value value = new Value(context,Value.TYPE_OPTION,returnValue);
        values.add(value);
    }
    
    /**
     * Set the given option as selected.
     * 
     * @param returnValue
     *            (Required) The return value of the option to be selected.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setOptionSelected(int returnValue) throws WingException
    {
        Value value = new Value(context,Value.TYPE_OPTION,String.valueOf(returnValue));
        values.add(value);
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
