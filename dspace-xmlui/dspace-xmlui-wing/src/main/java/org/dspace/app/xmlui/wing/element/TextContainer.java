/*
 * TextContainer.java
 *
 * Version: $Revision: 1.6 $
 *
 * Date: $Date: 2006/07/05 21:40:00 $
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
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


import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;

/**
 * A class representing all the containers that contain unformatted text, such
 * as head, label, help, value, etc...
 * 
 * This class may not be instantiated on it's own instead you must use one of
 * the extending classes listed above. This abstract class implements the
 * methods common to each of those elements.
 * 
 * @author Scott Phillips
 */

public abstract class TextContainer extends Container
{

    /**
     * Construct a new text container.
     * 
     * This method doesn't do anything but because the inheriting abstract class
     * mandates a constructor for this class to compile it must ensure that the
     * parent constructor is called. Just as implementors of this class must
     * ensure that this constructor is called, thus is the chain of life. :)
     * 
     * @param context
     *            (Required) The context this element is contained in.
     */
    protected TextContainer(WingContext context) throws WingException
    {
        super(context);
    }

    /**
     * Add character content to container.
     * 
     * @param characters
     *            (Required) Direct content or a dictionary tag to be inserted
     *            into the element.
     */
    public void addContent(String characters) throws WingException
    {
        Data data = new Data(context, characters);
        contents.add(data);
    }
    
    /**
     * Add integer content to container.
     * 
     * @param integer
     *            (Required) Add the integer into the element's container.
     */
    public void addContent(int integer) throws WingException
    {
        Data data = new Data(context, String.valueOf(integer));
        contents.add(data);
    }
    
    /**
     * Add translated content to container.
     * 
     * @param message
     *            (Required) A key into the i18n catalogue for translation into
     *            the user's preferred language.
     */
    public void addContent(Message message) throws WingException
    {
        Data data = new Data(context, message);
        contents.add(data);
    }
}
