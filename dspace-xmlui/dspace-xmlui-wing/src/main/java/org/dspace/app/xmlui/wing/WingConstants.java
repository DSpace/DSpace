/*
 * WingConstants.java
 *
 * Version: $Revision: 1.5 $
 *
 * Date: $Date: 2006/07/05 21:39:48 $
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

package org.dspace.app.xmlui.wing;

import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Cell;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.Field;
import org.dspace.app.xmlui.wing.element.Figure;
import org.dspace.app.xmlui.wing.element.Head;
import org.dspace.app.xmlui.wing.element.Help;
import org.dspace.app.xmlui.wing.element.Highlight;
import org.dspace.app.xmlui.wing.element.Item;
import org.dspace.app.xmlui.wing.element.Label;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Meta;
import org.dspace.app.xmlui.wing.element.Metadata;
import org.dspace.app.xmlui.wing.element.Options;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.app.xmlui.wing.element.Para;
import org.dspace.app.xmlui.wing.element.Params;
import org.dspace.app.xmlui.wing.element.Row;
import org.dspace.app.xmlui.wing.element.Table;
import org.dspace.app.xmlui.wing.element.Trail;
import org.dspace.app.xmlui.wing.element.UserMeta;
import org.dspace.app.xmlui.wing.element.Value;
import org.dspace.app.xmlui.wing.element.Instance;
import org.dspace.app.xmlui.wing.element.WingDocument;
import org.dspace.app.xmlui.wing.element.Xref;

/**
 * 
 * Static constants relating to Wing and the DRI schema.
 * 
 * @author Scott Phillips
 */
public class WingConstants
{
    /** The DRI schema's namespace */
    public static final Namespace DRI = new Namespace(
            "http://di.tamu.edu/DRI/1.0/");

    /** Cocoon's i18n namespace */
    public static final Namespace I18N = new Namespace(
            "http://apache.org/cocoon/i18n/2.1");

    /** All the DRI mergeable elements */
    public static final String[] MERGEABLE_ELEMENTS = {
            WingDocument.E_DOCUMENT, Meta.E_META, UserMeta.E_USER_META,
            PageMeta.E_PAGE_META, Metadata.E_METADATA, Body.E_BODY,
            Options.E_OPTIONS, List.E_LIST };

    /** All the DRI metadata elements */
    public static final String[] METADATA_ELEMENTS = { Meta.E_META,
            UserMeta.E_USER_META, PageMeta.E_PAGE_META, Trail.E_TRAIL,
            Metadata.E_METADATA };

    /** All the DRI structural elements */
    public static final String[] STRUCTURAL_ELEMENTS = { Division.E_DIVISION,
            Head.E_HEAD, Table.E_TABLE, Row.E_ROW, Cell.E_CELL, Para.E_PARA,
            List.E_LIST, Label.E_LABEL, Item.E_ITEM, Highlight.E_HIGHLIGHT,
            Xref.E_XREF, Figure.E_FIGURE, Field.E_FIELD, Params.E_PARAMS,
            Help.E_HELP, Value.E_VALUE, Instance.E_INSTANCE };

    /** All the DRI text container elements */
    public static final String[] TEXT_CONTAINERS = { Metadata.E_METADATA,
            Trail.E_TRAIL, Head.E_HEAD, Xref.E_XREF, Figure.E_FIGURE,
            Help.E_HELP, Value.E_VALUE, Label.E_LABEL, Cell.E_CELL,
            Para.E_PARA, Highlight.E_HIGHLIGHT, Item.E_ITEM };

    /** All the DRI rich text container elements */
    public static final String[] RICH_TEXT_CONTAINERS = { Cell.E_CELL,
            Para.E_PARA, Highlight.E_HIGHLIGHT, Item.E_ITEM, Value.E_VALUE};
}
