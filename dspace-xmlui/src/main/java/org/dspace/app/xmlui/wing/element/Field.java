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

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * A class representing an an abstract input control (which is just a fancy name
 * for a field :) )
 *
 * <p>The field element is a container for all information necessary to create a
 * form field. The required "type" attribute determines the type of the field,
 * while the children tags carry the information on how to build it. Fields can
 * only occur in divisions of type "interactive".
 *
 * <p>There are several types of possible fields and each of these field types
 * determine the appropriate parameters on the parameter object. This is the
 * only place in the schema where this design pattern is used. It limits the
 * proliferation of elements, such as a special element for textarea, select
 * lists, text fields etc... as HTML does. It also forces us to treat all fields
 * the same.
 *
 * <ul>
 * <li>text: A single-line text input control.</li>
 *
 * <li>textarea: A multi-line text input control.</li>
 *
 * <li>password: A single-line text input control where the input text is rendered
 * in such a way as to hide the characters from the user.</li>
 *
 * <li>hidden: An input control that is not rendered on the screen and hidden from
 * the user.</li>
 *
 * <li>button: A button input control that when activated by the user will submit
 * the form, including all the fields, back to the server for processing.</li>
 *
 * <li>checkbox: A boolean input control which may be toggled by the user. A
 * checkbox may have several fields which share the same name and each of those
 * fields may be toggled independently. This is distinct from a radio button
 * where only one field may be toggled.</li>
 *
 * <li>file: An input control that allows the user to select files to be submitted
 * with the form. Note that a form which uses a file field must use the
 * multipart method.</li>
 *
 * <li>radio: A boolean input control which may be toggled by the user. Multiple
 * radio button fields may share the same name. When this occurs only one field
 * may be selected to be true. This is distinct from a checkbox where multiple
 * fields may be toggled.</li>
 *
 * <li>select: A menu input control which allows the user to select from a list of
 * available options.</li>
 *
 * <li>composite: A combination of multiple fields into one input control.</li>
 * </ul>
 *
 * @author Scott Phillips
 */
public abstract class Field extends AbstractWingElement implements
        StructuralElement
{
    /** The name of the field element */
    public static final String E_FIELD = "field";

    /** The name of the field type attribute */
    public static final String A_FIELD_TYPE = "type";

    /** The name of the disabled attribute */
    public static final String A_DISABLED = "disabled";

    /** The name of the required attribute */
    public static final String A_REQUIRED = "required";
    
    
    /** The possible field types */
    public static final String TYPE_BUTTON = "button";

    public static final String TYPE_CHECKBOX = "checkbox";

    public static final String TYPE_FILE = "file";

    public static final String TYPE_HIDDEN = "hidden";

    public static final String TYPE_PASSWORD = "password";

    public static final String TYPE_RADIO = "radio";

    public static final String TYPE_SELECT = "select";

    public static final String TYPE_TEXT = "text";

    public static final String TYPE_TEXTAREA = "textarea";
    
    public static final String TYPE_COMPOSITE = "composite";
    
    /** All the possible field types collected into one array. */
    public static final String[] TYPES = { TYPE_BUTTON, TYPE_CHECKBOX,
            TYPE_FILE, TYPE_HIDDEN, TYPE_PASSWORD, TYPE_RADIO, TYPE_SELECT,
            TYPE_TEXT, TYPE_TEXTAREA, TYPE_COMPOSITE };
    
    /** Possible field behavioral operations */
    public static final String OPERATION_ADD = "add";

    public static final String OPERATION_DELETE = "delete";
    
    public static final String[] OPERATIONS = {OPERATION_ADD, OPERATION_DELETE};
    
    /** The field's name */
    protected String name;

    /** The type of field, see TYPES above */
    protected String type;

    /** Whether this field is disabled */
    protected boolean disabled;

    /** Whether this field is required */
    protected boolean required;

    /** Any special rendering instructions */
    protected String rend;

    /** Additional field parameters */
    protected Params params;

    /** The fields Label */
    protected Label label;
    
    /** Help instructions for this field */
    protected Help help;
    
    /** Error instructions for this field */
    protected List<Error> errors = new ArrayList<>();
    
    /** All sub fields contained within a composite field */
    protected List<Field> fields = new ArrayList<>();
    
    /** The value of this field */
    protected List<Option> options = new ArrayList<>();
    
    /** The value of this field */
    protected List<Value> values = new ArrayList<>();
    
    /** The set of stored values */
    protected List<Instance> instances = new ArrayList<>();

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
     * @param type
     *            (Required) Specify the type of field, must be one of the field
     *            types defined in the static variable TYPES.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected Field(WingContext context, String name, String type,
            String rend) throws WingException
    {
        super(context);
        require(name, "The 'name' parameter is required for all fields.");
        require(type, "The 'type' parameter is required for all fields.");
        restrict(
                type,
                TYPES,
                "The 'type' parameter must be one of these values: 'button', 'checkbox', 'file', 'hidden', 'password', 'radio', 'select', 'text', 'textarea'.");

        this.name = name;
        this.type = type;
        this.disabled = false;
        this.required = false;
        this.rend = rend;
    }

    /** Parameters available on all fields */

    /**
     * Set this field as required.
     */
    public void setRequired()
    {
        this.required = true;
    }

    /**
     * Set this field to either be required or not required as determined by the
     * required parameter.
     *
     * @param required
     *            Determine if the field is required or not.
     */
    public void setRequired(boolean required)
    {
        this.required = required;
    }

    /**
     * Set this field to be disabled.
     *
     */
    public void setDisabled()
    {
        this.disabled = true;
    }

    /**
     * Set this field to either be disabled or enabled as determined by the
     * disabled parameter.
     *
     * @param disabled
     *            Determine if the field is required or not.
     */
    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    /**
     * Set this field to be authority-controlled.
     *
     */
    public void setAuthorityControlled()
    {
        this.params.setAuthorityControlled(true);
    }

    /**
     * Set this field to be authority-controlled.
     *
     * @param value true if field is authority-controlled.
     */
    public void setAuthorityControlled(boolean value)
    {
        this.params.setAuthorityControlled(value);
    }

    /**
     * Set this field as authority_required.
     */
    public void setAuthorityRequired()
    {
        this.params.setAuthorityRequired();
    }

    /**
     * Set this field to either be required or not required as determined by the
     * required parameter.
     *
     * @param value
     *          Determine if the authority control is required or not on this field.
     */
    public void setAuthorityRequired(boolean value)
    {
        this.params.setAuthorityRequired(value);
    }

    /**
     * Set the field's autofocus attribute, an HTML5 feature.
     * Valid input values to enable autofocus are: autofocus, and empty string.
     * @param value To enable autofocus: "autofocus" or ""
     */
    public void setAutofocus(String value)
    {
        this.params.setAutofocus(value);
    }

    /**
     * Declare that this field has a value to be chosen from a choice
     * provider (see ChoicesManager).  Value is a metadata field key
     * naming the field whose choices to use.
     *
     * @param fieldKey pre-determined metadata field key
     */
    public void setChoices(String fieldKey)
    {
        params.setChoices(fieldKey);
    }

    /**
     * Set the kind of UI presentation requested for this choice, e.g.
     * select vs. suggest.  Value must match one of the PRESENTATIONS.
     *
     * @param value pre-determined metadata field key
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setChoicesPresentation(String value)
        throws WingException
    {
        params.setChoicesPresentation(value);
    }

    /**
     * Set whether set of choices is "closed" to just the values
     * in menu, or allows arbitrary user-supplied values.
     *
     * @param value true if it is closed
     */
    public void setChoicesClosed(boolean value)
    {
        params.setChoicesClosed(value);
    }
    public void setChoicesClosed()
    {
        params.setChoicesClosed();
    }

    /** ******************************************************************** */
    /** Help * */
    /** ******************************************************************** */

    /**
     * The help element provides help instructions to assist the user in using
     * this field.
     *
     * @return a new Help.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Help setHelp() throws WingException
    {
        this.help = new Help(context);
        return this.help;
    }

    /**
     * The help element provides help instructions to assist the user in using
     * this field.
     *
     * @param characters
     *            (May be null) Direct content or a dictionary tag to be
     *            inserted into the element.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setHelp(String characters) throws WingException
    {
        this.help = new Help(context);
        this.help.addContent(characters);
    }
    
    /**
     * The help element provides help instructions to assist the user in using
     * this field.
     *
     * @param message
     *            (Required) A key into the i18n catalogue for translation into
     *            the user's preferred language.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setHelp(Message message) throws WingException
    {
        this.help = new Help(context);
        this.help.addContent(message);
    }

    /** ******************************************************************** */
    /** Errors * */
    /** ******************************************************************** */

    /**
     * The error elements denotes that the fields value is invalid for the given
     * context. The message contained within the error message will provide
     * assistance to the user in correcting the problem.
     *
     * @return the new Error.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Error addError() throws WingException
    {
        Error error = new Error(context);
        errors.add(error);
        return error;
    }

    /**
     * The error elements denotes that the fields value is invalid for the given
     * context. The message contained within the error message will provide
     * assistance to the user in correcting the problem.
     *
     * @param characters
     *            (May be null) Direct content or a dictionary tag to be
     *            inserted into the element.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addError(String characters) throws WingException
    {
        Error error = new Error(context);
        error.addContent(characters);
        errors.add(error);
    }

    /**
     * The error elements denotes that the fields value is invalid for the given
     * context. The message contained within the error message will provide
     * assistance to the user in correcting the problem.
     *
     * @param message
     *            (Required) A key into the i18n catalogue for translation into
     *            the user's preferred language.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addError(Message message) throws WingException
    {
        Error error = new Error(context);
        error.addContent(message);
        errors.add(error);
    }
    
    /** ******************************************************************** */
    /** Label * */
    /** ******************************************************************** */

    /**
     * The help element provides help instructions to assist the user in using
     * this field.
     *
     * @return the new Label.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Label setLabel() throws WingException
    {
        this.label = new Label(context,null,null);
        return this.label;
    }

    /**
     * The help element provides help instructions to assist the user in using
     * this field.
     *
     * @param characters
     *            (May be null) Direct content or a dictionary tag to be
     *            inserted into the element.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setLabel(String characters) throws WingException
    {
        this.label = new Label(context,null,null);
        this.label.addContent(characters);
    }
    
    /**
     * The help element provides help instructions to assist the user in using
     * this field.
     *
     * @param message
     *            (Required) A key into the i18n catalogue for translation into
     *            the user's preferred language.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setLabel(Message message) throws WingException
    {
        this.label = new Label(context,null,null);
        this.label.addContent(message);
    }
    
    
    /**
     * Private function to remove all values of a particular type.
     *
     * @param removeType
     *            The type to be removed.
     */
    protected void removeValueOfType(String removeType)
    {
        List<Value> found = new ArrayList<>();
        for (Value value : values)
        {
            if (value.getType().equals(removeType))
            {
                found.add(value);
            }
        }

        for (Value remove : found)
        {
            values.remove(remove);
            remove.dispose();
        }
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
        AttributeMap attributes = new AttributeMap();

        attributes.put(A_NAME, this.name);
        attributes.put(A_ID, this.context.generateID(E_FIELD, this.name));
        attributes.put(A_FIELD_TYPE, this.type);
        if (this.disabled)
        {
            attributes.put(A_DISABLED, this.disabled);
        }
        if (this.required)
        {
            attributes.put(A_REQUIRED, this.required);
        }
        if (this.rend != null)
        {
            attributes.put(A_RENDER, this.rend);
        }

        startElement(contentHandler, namespaces, E_FIELD, attributes);

        if (params != null)
        {
            params.toSAX(contentHandler, lexicalHandler, namespaces);
        }

        if (label != null)
        {
            label.toSAX(contentHandler, lexicalHandler, namespaces);
        }
        
        if (help != null)
        {
            help.toSAX(contentHandler, lexicalHandler, namespaces);
        }

        for (Error error : errors)
        {
            error.toSAX(contentHandler, lexicalHandler, namespaces);
        }
        
        for (Field field : fields)
        {
            field.toSAX(contentHandler, lexicalHandler, namespaces);
        }

        for (Option option : options)
        {
            option.toSAX(contentHandler, lexicalHandler, namespaces);
        }
        
        for (Value value : values)
        {
            value.toSAX(contentHandler, lexicalHandler, namespaces);
        }
        
        for (Instance instance : instances)
        {
            instance.toSAX(contentHandler, lexicalHandler, namespaces);
        }

        endElement(contentHandler, namespaces, E_FIELD);
    }

    /**
     * Dispose
     */
    @Override
    public void dispose()
    {
        if (params != null)
        {
            params.dispose();
        }

        if (label != null)
        {
            label.dispose();
        }
        
        if (help != null)
        {
            help.dispose();
        }

        if (errors != null)
        {
            for (Error error : errors)
            {
                error.dispose();
            }
            errors.clear();
        }

        if (fields != null)
        {
            for (Field field : fields)
            {
                field.dispose();
            }
            fields.clear();
        }
        
        if (options != null)
        {
            for (Option option : options)
            {
                option.dispose();
            }
            options.clear();
        }
        
        if (values != null)
        {
            for (Value value : values)
            {
                value.dispose();
            }
            values.clear();
        }

        if (instances != null)
        {
            for (Instance instance : instances)
            {
                instance.dispose();
            }
            instances.clear();
        }

        params = null;
        label = null;
        help = null;
        errors = null;
        fields = null;
        options = null;
        values = null;
        instances = null;
        super.dispose();
    }
}
