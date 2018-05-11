/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Instance represents multiple value instances of a field.
 *
 *
 * @author Scott Phillips
 */

public class Instance extends Container
{
    /** The name of the field instance element */
    public static final String E_INSTANCE = "instance";

    /**
     * Construct a new field value, when used in a multiple value context
     *
     * @param context
     *            (Required) The context this element is contained in.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected Instance(WingContext context) throws WingException
    {
        super(context);
    }

    
    /** ******************************************************************** */
    /** Values * */
    /** ******************************************************************** */

    /**
     * Set the raw value of the field removing any previous raw values.
     * @return the new Value.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Value setValue() throws WingException
    {
        this.removeValueOfType(Value.TYPE_RAW);
        Value value = new Value(context, Value.TYPE_RAW);
        contents.add(value);
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
     * Set the raw value of the field removing any previous raw values. This
     * will set the field as either checked or unchecked. This should only be
     * used on checkbox or radio button fields.
     *
     * @param checked
     *            (Required) Whether the checkbox is checked or not.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setValue(boolean checked) throws WingException
    {
        this.removeValueOfType(Value.TYPE_RAW);
        Value value = new Value(context, Value.TYPE_RAW,checked);
        contents.add(value);
    }
    
    /**
     * Set the authority value of the field removing any previous authority values.
     * Initialized to an empty value.
     * @return the new authority Value.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
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
     * @param confidence measure of confidence.
     * @return the new Value.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Value setAuthorityValue(String characters, String confidence) throws WingException
    {
        this.removeValueOfType(Value.TYPE_AUTHORITY);
        Value value = new Value(context, Value.TYPE_AUTHORITY, confidence);
        value.addContent(characters);
        contents.add(value);
        return value;
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
        contents.add(value);
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
        setOptionSelected(String.valueOf(returnValue));
    }
    
    /** ******************************************************************** */
    /** Interpreted Values * */
    /** ******************************************************************** */

    /**
     * Set the interpreted value of the field removing any previous interpreted
     * values.
     * @return the new Value.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Value setInterpretedValue() throws WingException
    {
        removeValueOfType(Value.TYPE_INTERPRETED);
        Value value = new Value(context, Value.TYPE_INTERPRETED);
        contents.add(value);
        return value;
    }

    /**
     * Set the interpreted value of the field removing any previous interpreted
     * values.
     *
     * @param characters
     *            (May be null) Field value as a string
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setInterpretedValue(String characters) throws WingException
    {
        Value value = this.setInterpretedValue();
        value.addContent(characters);
    }

    /**
     * Set the interpreted value of the field removing any previous interpreted
     * values.
     *
     * @param message
     *            (Required) A key into the i18n catalogue for translation into
     *            the user's preferred language.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setInterpretedValue(Message message) throws WingException
    {
        Value value = this.setInterpretedValue();
        value.addContent(message);
    }
    
    
    /** ******************************************************************** */
    /** Special Values * */
    /** ******************************************************************** */
    
    /**
     * Add an option value, there may be many of these. These values reference
     * an option already added to the field.
     *
     * @param option
     *            (Required) The return value of the selected option.
     * @return the new Value.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Value addOptionValue(String option) throws WingException
    {
        Value value = new Value(context, Value.TYPE_OPTION, option);
        contents.add(value);
        return value;
    }
    
    /**
     * Set the checkbox (or radio) value of this field. This is a parameter
     * whether the field is selected or not along with the return string that
     * should be used with this parameter.
     *
     * @param checked
     *            (Required) determine if the value is selected or not.
     * @param characters
     *            (may be null) The returned value for this field, if selected.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setCheckedValue(boolean checked, String characters) throws WingException
    {
        this.removeValueOfType(Value.TYPE_RAW);
        Value value = new Value(context,Value.TYPE_RAW,checked);
        contents.add(value);
        value.addContent(characters);
    }
    
    
    /**
     * Translate this element and all contained elements into SAX events. The
     * events should be routed to the contentHandler found in the WingContext.
     *
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed too.
     * @param lexicalHandler
     *            (Required) The registered lexicalHandler where lexical events
     *            (such as CDATA, DTD, etc) should be routed too.
     * @param namespaces
     *            (Required) SAX Helper class to keep track of namespaces able
     *            to determine the correct prefix for a given namespace URI.
     * @throws org.xml.sax.SAXException passed through.
     */

    @Override
    public void toSAX(ContentHandler contentHandler,
            LexicalHandler lexicalHandler, NamespaceSupport namespaces)
            throws SAXException
    {
        startElement(contentHandler, namespaces, E_INSTANCE, null);
        super.toSAX(contentHandler, lexicalHandler, namespaces);
        endElement(contentHandler, namespaces, E_INSTANCE);
    }
    
    /**
     * Private function to remove all values of a particular type.
     *
     * @param removeType
     *            The type to be removed.
     */
    private void removeValueOfType(String removeType)
    {
        List<Value> found = new ArrayList<>();
        for (AbstractWingElement awe : contents)
        {
            if (awe instanceof Value)
            {
                Value value = (Value) awe;
                if (value.getType().equals(removeType))
                {
                    found.add(value);
                }
            }
        }
            
        for (Value remove : found)
        {
            contents.remove(remove);
            remove.dispose();
        }
    }
    
}
