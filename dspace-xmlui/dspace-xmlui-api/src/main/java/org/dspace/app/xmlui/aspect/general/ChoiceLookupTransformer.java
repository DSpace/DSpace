/*
 * ChoiceLookupTransformer.java
 *
 * Version: $Revision: 3705 $
 *
 * Date: $Date: 2009-04-11 13:02:24 -0400 (Sat, 11 Apr 2009) $
 *
 * Copyright (c) 2002-2009, The DSpace Foundation.  All rights reserved.
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
 * - Neither the name of the DSpace Foundation nor the names of its
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

package org.dspace.app.xmlui.aspect.general;

import java.io.IOException;
import java.io.Serializable;
import java.sql.SQLException;

import javax.servlet.http.HttpServletResponse;

import org.dspace.content.authority.ChoiceAuthorityManager;
import org.dspace.content.DCPersonName;
import org.dspace.core.ConfigurationManager;
import org.apache.cocoon.caching.CacheableProcessingComponent;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.util.HashUtil;
import org.apache.excalibur.source.SourceValidity;
import org.apache.excalibur.source.impl.validity.NOPValidity;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.UIException;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingConstants;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Composite;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Hidden;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.Select;
import org.dspace.app.xmlui.wing.element.Text;
import org.dspace.app.xmlui.wing.element.Button;
import org.dspace.authorize.AuthorizeException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import org.apache.log4j.Logger;

/**
 * Create the "lookup" popup window for Choice Control.  It loads a selector
 * via AJAX request, and transfers values (both text and authority/confidence)
 * back to the indicated form fields in the window that launched it.
 * Some necessary logic is in JavaScript, see choice-control.js.
 *
 * Expected Parameters:
 *  field - name of metadata field in "_" notation, eg: dc_contributor_author
 *  value - maybe-partial value of field
 *  formID - the @id of <form> tag in calling window containing the inputs we are to set.
 *  valueInput - @name of input field in DOM for value.
 *  authorityInput - @name of input field in DOM for authority value
 *  isRepeating - true if metadata value can be repeated
 *  isName - true if this is a name value (i.e. last/first boxes)
 *  start - starting index, default 0
 *  limit - maximum values to return, default 0 (none)
 *
 * Configuration Properties:
 *  xmlui.lookup.select.size = 12  (default, entries to show in SELECT widget.)
 *
 * For each FIELD, e.g. dc.contributor.author, these message properties
 * will OVERRIDE the corresponding i18n message catalog entries:
 *  xmlui.lookup.field.FIELD.title = title of lookup page
 *   (e.g. xmlui.lookup.field.dc_contributor_author.title = Author..)
 *  xmlui.lookup.field.FIELD.nonauthority = template for "non-authority" label in options
 *  xmlui.lookup.field.FIELD.help = help message for single input
 *    (NOTE this is still required even for name inputs)
 *  xmlui.lookup.field.FIELD.help.last = help message for last name of Name-oriented input
 *  xmlui.lookup.field.FIELD.help.first = help message for first name of Name-oriented input
 *
 * @author  Larry Stone
 */
public class ChoiceLookupTransformer extends AbstractDSpaceTransformer
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(ChoiceLookupTransformer.class);

    private static final String CONFIG_PREFIX = "xmlui.lookup.field.";

    /** Language Strings */
    private static final String MESSAGE_PREFIX = "xmlui.ChoiceLookupTransformer.";
    private static final Message T_title = message(MESSAGE_PREFIX+"title");
    private static final Message T_add =    message(MESSAGE_PREFIX+"add");
    private static final Message T_accept = message(MESSAGE_PREFIX+"accept");
    private static final Message T_more =   message(MESSAGE_PREFIX+"more");
    private static final Message T_cancel = message(MESSAGE_PREFIX+"cancel");
    private static final Message T_results = message(MESSAGE_PREFIX+"results");
    private static final Message T_fail =    message(MESSAGE_PREFIX+"fail");

    public void addBody(Body body) throws SAXException, WingException,
            UIException, SQLException, IOException, AuthorizeException
    {
        String field = null;
        String value = null;
        String formID = null;
        String confIndicatorID = null;
        boolean isName = false;
        boolean isRepeating = false;
        String valueInput = null;
        String authorityInput = null;
        int start = 0;
        int limit = 0;
        String collection = null;

        // HTTP parameters:
        try
        {
            field = parameters.getParameter("field");
            value = parameters.getParameter("value");
            formID = parameters.getParameter("formID");
            confIndicatorID = parameters.getParameter("confIndicatorID");
            isName = parameters.getParameterAsBoolean("isName", false);
            isRepeating = parameters.getParameterAsBoolean("isRepeating", false);
            valueInput = parameters.getParameter("valueInput");
            authorityInput = parameters.getParameter("authorityInput");
            String sStart = parameters.getParameter("start");
            if (sStart != null)
                start = atoi(sStart);
            String sLimit = parameters.getParameter("limit");
            if (sLimit != null)
                limit = atoi(sLimit);
            collection = parameters.getParameter("collection");
            if (collection == null)
                collection = "-1";
        }
        catch (org.apache.avalon.framework.parameters.ParameterException e)
        {
            throw new UIException("Missing a required parameter",e);
        }

        Division idiv = body.addInteractiveDivision("lookup", "", "get", "popup");
        if (isFieldMessage(field, "title"))
            idiv.setHead(getFieldMessage(field, "title"));
        else
            idiv.setHead(getFieldLabel(field, "title"));
        List fl = idiv.addList("choicesList", "form", "choices-lookup");
        fl.setHead(T_results);

        // the <select> tag, and param values
        Item selectItem = fl.addItem("select", "choices-lookup");
        Select s = selectItem.addSelect("chooser", "choices-lookup");
        s.setSize(ConfigurationManager.getIntProperty("xmlui.lookup.select.size", 12));

        // parameters for javascript
        Hidden h = selectItem.addHidden("paramField");
        h.setValue(field);
        h = selectItem.addHidden("paramValue");
        h.setValue(value);
        h = selectItem.addHidden("paramIsName");
        h.setValue(String.valueOf(isName));
        h = selectItem.addHidden("paramIsRepeating");
        h.setValue(String.valueOf(isRepeating));
        h = selectItem.addHidden("paramValueInput");
        h.setValue(valueInput);
        h = selectItem.addHidden("paramAuthorityInput");
        h.setValue(authorityInput);
        h = selectItem.addHidden("paramStart");
        h.setValue(String.valueOf(start));
        h = selectItem.addHidden("paramLimit");
        h.setValue(String.valueOf(limit));
        h = selectItem.addHidden("paramFormID");
        h.setValue(formID);
        h = selectItem.addHidden("paramConfIndicatorID");
        h.setValue(confIndicatorID);
        h = selectItem.addHidden("paramFail");
        h.setValue(T_fail);
        boolean isClosed = ChoiceAuthorityManager.getManager().isClosed(field);
        h = selectItem.addHidden("paramIsClosed");
        h.setValue(String.valueOf(isClosed));
        h = selectItem.addHidden("paramCollection");
        h.setValue(String.valueOf(collection));
        if (!isClosed)
        {
            h = selectItem.addHidden("paramNonAuthority");
            if (isFieldMessage(field, "nonauthority"))
                h.setValue(getFieldMessage(field, "nonauthority"));
            else
                h.setValue(getFieldLabel(field, "nonauthority"));
        }
        h = selectItem.addHidden("contextPath");
        h.setValue(contextPath);

        // NOTE: the "spinner" indicator image gets added in the XSLT.

        // the text input(s)
        Item ti = fl.addItem("textFields", "choices-lookup");
        Composite textItem = ti.addComposite("textFieldsComp", "choices-lookup");
        Text t1 = textItem.addText("text1", "choices-lookup");
        if (isName)
        {
            Text t2 = textItem.addText("text2", "choices-lookup");
            DCPersonName dp = new DCPersonName(value);
            t1.setValue(dp.getLastName());
            t2.setValue(dp.getFirstNames());
            if (isFieldMessage(field, "help.last"))
            {
                Message m = getFieldMessage(field, "help.last");
                t1.setLabel(m);
                t1.setHelp(m);
            }
            else
            {
                String m = getFieldLabel(field, "help.last");
                t1.setLabel(m);
                t1.setHelp(m);
            }
            if (isFieldMessage(field, "help.first"))
            {
                Message m = getFieldMessage(field, "help.first");
                t2.setLabel(m);
                t2.setHelp(m);
            }
            else
            {
                String m = getFieldLabel(field, "help.first");
                t2.setLabel(m);
                t2.setHelp(m);
            }
        }
        else
        {
            t1.setValue(value);
            if (isFieldMessage(field, "help"))
            {
                Message m = getFieldMessage(field, "help");
                t1.setLabel(m);
                t1.setHelp(m);
            }
            else
            {
                String m = getFieldLabel(field, "help");
                t1.setLabel(m);
                t1.setHelp(m);
            }
        }

        // confirmation buttons
        Item buttItem = fl.addItem("confirmation", "choices-lookup");
        Button accept = buttItem.addButton("accept", "choices-lookup");
        accept.setValue(isRepeating ? T_add : T_accept);
        Button more = buttItem.addButton("more", "choices-lookup");
        more.setDisabled();
        more.setValue(T_more);
        Button cancel = buttItem.addButton("cancel", "choices-lookup");
        cancel.setValue(T_cancel);
    }

    public void addPageMeta(PageMeta pageMeta) throws SAXException,
            WingException, UIException, SQLException, IOException,
            AuthorizeException
    {
        // Set the page title
        pageMeta.addMetadata("title").addContent(T_title);

        // This invokes magic popup transformation in XSL - "framing.popup"
        pageMeta.addMetadata("framing","popup").addContent("true");
    }

    /**
     *  Protocol to get custom and/or i18n strings:
     *   For label NAME,
     *    .. if config key xmlui.choices.FIELD.NAME is defined, and starts
     *       with "xmlui.", then it's a message key.
     *    .. if NO config key is defined, look for message
     *      xmlui.ChoiceLookupTransformer.field.FIELD.NAME
     *    .. otherwise take literal value from configuration
     */

    // return true if configured (or lack thereof) value points to Message
    private boolean isFieldMessage(String field, String name)
    {
        String cv = getFieldLabel(field, name);
        return (cv == null || cv.startsWith("xmlui."));
    }

    // get field-specific label value
    private String getFieldLabel(String field, String name)
    {
        return ConfigurationManager.getProperty(CONFIG_PREFIX+field+"."+name);
    }

    // get field-specific label value
    private Message getFieldMessage(String field, String name)
    {
        String cv = getFieldLabel(field, name);
        if (cv == null)
            return message(MESSAGE_PREFIX+"field."+field+"."+name);
        else
            return message(cv);
    }

    private int atoi(String s)
    {
        try
        {
            return Integer.parseInt(s);
        }
        catch (Exception e) {}
        return 0;
   }
}
