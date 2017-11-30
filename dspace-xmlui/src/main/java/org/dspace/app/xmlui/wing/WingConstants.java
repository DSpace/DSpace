/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
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
