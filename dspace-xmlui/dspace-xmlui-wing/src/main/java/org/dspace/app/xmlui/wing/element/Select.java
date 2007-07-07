/*
 * Select.java
 *
 * Version: $Revision: 1.2 $
 *
 * Date: $Date: 2006/07/06 17:02:03 $
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.xmlui.wing.element;

/**
 * 
 * A class representing a select input control. The select input control allows
 * the user to select from a list of available options.
 * 
 * @author Scott Phillips
 */

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;

public class Select extends Field
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
    protected Select(WingContext context, String name, String rend)
            throws WingException
    {
        super(context, name, Field.TYPE_SELECT, rend);
        this.params = new Params(context);
    }

    /**
     * Enable the user to select multiple options.
     * 
     */
    public void setMultiple()
    {
        this.params.setMultiple(true);
    }

    /**
     * Set whether the user is able to select multiple options.
     * 
     * @param multiple
     *            (Required) The multiple state.
     */
    public void setMultiple(boolean multiple)
    {
        this.params.setMultiple(multiple);
    }

    /**
     * Set the number of options visible at any one time.
     * 
     * @param size
     *            (Required) The number of options to display.
     */
    public void setSize(int size)
    {
        this.params.setSize(size);
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
    
    
    /**
     * Add a select option.
     * 
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     */
    public Option addOption(String returnValue)
            throws WingException
    {
        Option option = new Option(context, returnValue);
        options.add(option);

        return option;
    }

    /**
     * Add a select option.
     * 
     * @param selected
     *            (Required) Set the field as selected.
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     */
    public Option addOption(boolean selected, String returnValue)
            throws WingException
    {
    	if (selected)
    		setOptionSelected(returnValue);
        return addOption(returnValue);
    }
    
    /**
     * Add a select option.
     * 
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param characters
     *            (Required) The text to set as the visible option.
     */
    public void addOption(String returnValue, String characters) throws WingException
    {
        Option option = this.addOption(returnValue);
        option.addContent(characters);
    }
    
    /**
     * Add a select option.
     * 
     * @param selected
     *            (Required) Set the field as selected.
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param characters
     *            (Required) The text to set as the visible option.
     */
    public void addOption(boolean selected,String returnValue, String characters) throws WingException
    {
    	if (selected)
    		setOptionSelected(returnValue);
        addOption(returnValue,characters);
    }
    
    /**
     * Add a select option.
     * 
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param characters
     *            (Required) The text to set as the visible option.
     */
    public void addOption(int returnValue, String characters) throws WingException
    {
        Option option = this.addOption(String.valueOf(returnValue));
        option.addContent(characters);
    }
    
    /**
     * Add a select option.
     * 
     * @param selected
     *            (Required) Set the field as selected.
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param characters
     *            (Required) The text to set as the visible option.
     */
    public void addOption(boolean selected, int returnValue, String characters) throws WingException
    {
    	if (selected)
    		setOptionSelected(returnValue);
        addOption(returnValue,characters);
    }
    
    /**
     * Add a select option.
     * 
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param message
     *            (Required) The transalted text to set as the visible option.
     */
    public void addOption(String returnValue, Message message) throws WingException
    {
        Option option = this.addOption(returnValue);
        option.addContent(message);
    }
    
    /**
     * Add a select option.
     * 
     * @param selected
     *            (Required) Set the field as selected.
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param message
     *            (Required) The transalted text to set as the visible option.
     */
    public void addOption(boolean selected, String returnValue, Message message) throws WingException
    {
    	if (selected)
    		setOptionSelected(returnValue);
        addOption(returnValue,message);
    }
    
    /**
     * Add a select option.
     * 
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param message
     *            (Required) The transalted text to set as the visible option.
     */
    public void addOption(int returnValue, Message message) throws WingException
    {
        Option option = this.addOption(String.valueOf(returnValue));
        option.addContent(message);
    }
    
    /**
     * Add a select option.
     * 
     * @param selected
     *            (Required) Set the field as selected.
     * @param returnValue
     *            (Required) The value to be passed back if this option is
     *            selected.
     * @param message
     *            (Required) The transalted text to set as the visible option.
     */
    public void addOption(boolean selected, int returnValue, Message message) throws WingException
    {
    	if (selected)
    		setOptionSelected(returnValue);
        addOption(returnValue,message);
    }
    
    /**
     * Set the given option as selected.
     * 
     * @param returnValue
     *            (Required) The return value of the option to be selected.
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
     */
    public void setOptionSelected(int returnValue) throws WingException
    {
        Value value = new Value(context,Value.TYPE_OPTION,String.valueOf(returnValue));
        values.add(value);
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
