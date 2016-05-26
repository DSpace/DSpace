/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.wing.element;

import java.util.ArrayList;

import org.dspace.app.xmlui.wing.AttributeMap;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingConstants;
import org.dspace.app.xmlui.wing.WingContext;
import org.dspace.app.xmlui.wing.WingException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * Class representing a Division, or the div element, in the XML UI schema.
 *
 * <p>The {@code div} element represents a major section of content and can contain a wide
 * variety of other elements to present that content to the user. It can contain
 * TEI style paragraphs, tables, and lists, as well as references to artifact
 * information stored in artifactMeta. The {@code div} element is also recursive,
 * allowing it to be further divided into other {@code div}s.
 *
 * @author Scott Phillips
 */
public class Division extends AbstractWingElement implements StructuralElement, WingMergeableElement
{
    /** The name of the division element */
    public static final String E_DIVISION = "div";

    /** The name of the interactive attribute */
    public static final String A_INTERACTIVE = "interactive";

    /** The name of the action attribute */
    public static final String A_ACTION = "action";

    /** The name of the method attribute */
    public static final String A_METHOD = "method";

    /** The name of the method attribute */
    public static final String A_BEHAVIOR = "behavior";
    
    /** The name of the continuation name attribute. */
    public static final String A_BEHVIOR_SENSITIVE_FIELDS = "behaviorSensitiveFields";
    
    /** The name of the pagination attribute */
    public static final String A_PAGINATION = "pagination";

    /** The name of the previous page attribute */
    public static final String A_PREVIOUS_PAGE = "previousPage";

    /** The name of the next page attribute */
    public static final String A_NEXT_PAGE = "nextPage";

    /** The name of the items total attribute */
    public static final String A_ITEMS_TOTAL = "itemsTotal";

    /** The name of the first item index attribute */
    public static final String A_FIRST_ITEM_INDEX = "firstItemIndex";

    /** The name of the last item index attribute */
    public static final String A_LAST_ITEM_INDEX = "lastItemIndex";

    /** The name of the current page attribute */
    public static final String A_CURRENT_PAGE = "currentPage";

    /** The name of the pages total attribute */
    public static final String A_PAGES_TOTAL = "pagesTotal";

    /** The name of the page url mask attribute */
    public static final String A_PAGE_URL_MASK = "pageURLMask";

    /** Determines whether this division is being merged */
    private boolean merged = false;
    
    /** The name assigned to this div */
    private final String name;

    /** Is this division interactive if so then action & method must be defined */
    private boolean interactive;

    /** Where should the result of this interactive division be posted to? */
    private String action;

    /** What method should this interactive division use for posting the result? */
    private String method;

    /** Does this interactive division support the AJAX behavior? */
    private boolean behaviorAJAXenabled = false;

    /** A list of fields which need to be handled specially when using behavior */
    private String behaviorSensitiveFields;
    
    /** Special rendering instructions */
    private final String rend;

    /** The head, or label of this division */
    private Head head;

    /** A paragraph that contains hidden fields */
    private Para hiddenFieldsPara;
    
    /**
     * The pagination type of this div, either simple or masked. If null then
     * this div is not paginated
     */
    private String paginationType;

    /** URL to the previousPage. (used by simple pagination) */
    private String previousPage;

    /** URL to the nextPage. (used by simple pagination) */
    private String nextPage;

    /**
     * How many items exist across all paginated divs. (used by both pagination
     * types)
     */
    private int itemsTotal;

    /**
     * The index of the first item included in this div. (used by both
     * pagination types)
     */
    private int firstItemIndex;

    /**
     * The index of the first item included in this div. (used by both
     * pagination types)
     */
    private int lastItemIndex;

    /**
     * The index the current page being displayed. (used by masked pagination
     * type)
     */
    private int currentPage;

    /**
     * The total number of pages in the pagination set. (used by masked
     * pagination type)
     */
    private int pagesTotal;

    /** The pagination URL mask. (used by masked pagination type) */
    private String pageURLMask;

    /** The possible interactive division methods: get, post, or multipart. */
    public static final String METHOD_GET = "get";

    public static final String METHOD_POST = "post";

    public static final String METHOD_MULTIPART = "multipart";

    /** The possible interactive division methods names collected into one array */
    public static final String[] METHODS = { METHOD_GET, METHOD_POST,
            METHOD_MULTIPART };

    /** The possible pagination types: simple and masked */
    public static final String PAGINATION_SIMPLE = "simple";

    public static final String PAGINATION_MASKED = "masked";

    /** The possible pagination division types collected into one array */
    public static final String[] PAGINATION_TYPES = { PAGINATION_SIMPLE,
            PAGINATION_MASKED };

    /** All content of this container, items & lists */
    private java.util.List<AbstractWingElement> contents = new ArrayList<>();

    /**
     * Construct a non-interactive division.
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected Division(WingContext context, String name, String rend)
            throws WingException
    {
        super(context);
        require(name, "The 'name' parameter is required for divisions.");

        this.name = name;
        this.rend = rend;
    }

    /**
     * Construct an interactive division. Interactive divisions must be
     * accompanied by both an action and method to determine how to process form
     * data.
     * 
     * The valid values for method may be found in the static variable METHODS.
     * 
     * @param context
     *            (Required) The context this element is contained in, such as
     *            where to route SAX events and what i18n catalogue to use.
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * @param action
     *            (Required) The form action attribute determines where the form
     *            information should be sent for processing.
     * @param method
     *            (Required) Accepted values are "get", "post", and "multipart".
     *            Determines the method used to pass gathered field values to
     *            the handler specified by the action attribute. The multipart
     *            method should be used if there are any file fields used within
     *            the division.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    protected Division(WingContext context, String name, String action,
            String method, String rend) throws WingException
    {
        super(context);
        require(name, "The 'name' parameter is required for divisions.");
        // Blank actions are okay:
        // require(action,
        //         "The 'action' parameter is required for interactive divisions.");
        require(method, "The 'method' parameter is required for divisions.");
        restrict(
                method,
                METHODS,
                "The 'method' parameter must be one of these values: 'get', 'post' or 'multipart'.");

        this.name = name;
        this.interactive = true;
        this.action = action;
        this.method = method;
        this.rend = rend;
    }

    /**
     * Enable AJAX behaviors on this interactive division.
     */
    public void enableAJAX()
    {
        this.behaviorAJAXenabled = true;
    }
    
    
    /**
     * Add to the list of behavior sensitive fields, these fields should be
     * updated each time a request partial page is submitted.
     * 
     * @param fieldName
     *            (Required) The name of a single field (with no spaces).
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addBehaviorSensitiveField(String fieldName) throws WingException
    {
        require(fieldName, "The fieldName parameter is required for the behaviorSensitiveFields attribute.");
        if (this.behaviorSensitiveFields == null)
        {
            this.behaviorSensitiveFields = fieldName;
        }
        else
        {
            this.behaviorSensitiveFields += " " + fieldName;
        }
    }
    
    /**
     * Make this div paginated ( a div that spans multiple pages ) using the
     * simple page paradigm.
     * 
     * @param itemsTotal
     *            (Required) How many items exist across all paginated divs.
     * @param firstItemIndex
     *            (Required) The index of the first item included in this div.
     * @param lastItemIndex
     *            (Required) The index of the last item included in this div.
     * @param previousPage
     *            (May be null) The URL of the previous page of the div, if it
     *            exists.
     * @param nextPage
     *            (May be null) The URL of the previous page of the div, if it
     *            exists.
     */
    public void setSimplePagination(int itemsTotal, int firstItemIndex,
            int lastItemIndex, String previousPage, String nextPage)
    {
        this.paginationType = PAGINATION_SIMPLE;
        this.previousPage = previousPage;
        this.nextPage = nextPage;
        this.itemsTotal = itemsTotal;
        this.firstItemIndex = firstItemIndex;
        this.lastItemIndex = lastItemIndex;
    }

    /**
     * Make this div paginated ( a div that spans multiple pages ) using the
     * masked page paradigm.
     * 
     * @param itemsTotal
     *            (Required) How many items exist across all paginated divs.
     * @param firstItemIndex
     *            (Required) The index of the first item included in this div.
     * @param lastItemIndex
     *            (Required) The index of the last item included in this div.
     * @param currentPage
     *            (Required) The index of the page currently displayed for this
     *            div.
     * @param pagesTotal
     *            (Required) How many pages the paginated div spans.
     * @param pageURLMask
     *            (Required) The mask of a URL to a particular within the
     *            paginated set. Number of the destination page should replace
     *            the {pageNum} string in the URL mask to generate a full URL to
     *            that page.
     */
    public void setMaskedPagination(int itemsTotal, int firstItemIndex,
            int lastItemIndex, int currentPage, int pagesTotal,
            String pageURLMask)
    {
        this.paginationType = PAGINATION_MASKED;
        this.itemsTotal = itemsTotal;
        this.firstItemIndex = firstItemIndex;
        this.lastItemIndex = lastItemIndex;
        this.currentPage = currentPage;
        this.pagesTotal = pagesTotal;
        this.pageURLMask = pageURLMask;
    }

    /**
     * Set the head element which is the label associated with this division.
     * @return the new Head.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Head setHead() throws WingException
    {
        this.head = new Head(context, null);
        return head;
    }

    /**
     * Set the head element which is the label associated with this division.
     * 
     * @param characters
     *            (May be null) Unprocessed characters to be included
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setHead(String characters) throws WingException
    {
        Head theHead = this.setHead();
        theHead.addContent(characters);
    }

    /**
     * Set the head element which is the label associated with this division.
     * 
     * @param message
     *            (Required) A key into the i18n catalogue for translation into
     *            the user's preferred language.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void setHead(Message message) throws WingException
    {
        Head theHead = this.setHead();
        theHead.addContent(message);
    }

    /**
     * Add a sub division for further logical grouping of content.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return A new sub Division
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Division addDivision(String name, String rend) throws WingException
    {

        Division division = new Division(context, name, rend);
        contents.add(division);
        return division;
    }

    /**
     * Add a sub division for further logical grouping of content.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * @return A new sub division
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Division addDivision(String name) throws WingException
    {
        return this.addDivision(name, null);
    }

    /**
     * Add an interactive sub division for further logical grouping of content.
     * 
     * The valid values for method may be found in the static variable METHODS.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * @param action
     *            (Required) The form action attribute determines where the form
     *            information should be sent for processing.
     * @param method
     *            (Required) Accepted values are "get", "post", and "multipart".
     *            Determines the method used to pass gathered field values to
     *            the handler specified by the action attribute. The multipart
     *            method should be used if there are any file fields used within
     *            the division.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return A new interactive sub division
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Division addInteractiveDivision(String name, String action,
            String method, String rend) throws WingException
    {
        Division division = new Division(context, name, action, method, rend);
        contents.add(division);
        return division;
    }

    /**
     * Add an interactive sub division for further logical grouping of content
     * without specifying special rendering instructions.
     * 
     * The valid values for method may be found in the static variable METHODS.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * @param action
     *            (Required) The form action attribute determines where the form
     *            information should be sent for processing.
     * @param method
     *            (Required) Accepted values are "get", "post", and "multipart".
     *            Determines the method used to pass gathered field values to
     *            the handler specified by the action attribute. The multipart
     *            method should be used if there are any file fields used within
     *            the division.
     * @return A new interactive sub division
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Division addInteractiveDivision(String name, String action,
            String method) throws WingException
    {
        return addInteractiveDivision(name, action, method, null);
    }

    /**
     * Append a paragraph to the division
     * 
     * @param name
     *            (May be null) a local identifier used to differentiate the
     *            element from its siblings.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return A new paragraph.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Para addPara(String name, String rend) throws WingException
    {
        Para para = new Para(context, name, rend);
        contents.add(para);
        return para;
    }

    /**
     * Append an unnamed paragraph to the division
     * 
     * @return A new unnamed paragraph.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Para addPara() throws WingException
    {
        return addPara(null, null);
    }

    /**
     * Append a paragraph to the division and set the content of the paragraph.
     * 
     * @param characters
     *            (May be null) Untranslated character data to be included as
     *            the contents of this para.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addPara(String characters) throws WingException
    {
        Para para = this.addPara();
        para.addContent(characters);
    }

    /**
     * Append a paragraph to the division and set the content of the paragraph.
     * 
     * @param message
     *            (Required) Key to the i18n catalogue to translate the content
     *            into the language preferred by the user.
     * @throws org.dspace.app.xmlui.wing.WingException passed through
     */
    public void addPara(Message message) throws WingException
    {
        Para para = this.addPara();
        para.addContent(message);
    }

    /**
     * append a list to the division.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * 
     * @param type
     *            (May be null) an optional attribute to explicitly specify the
     *            type of list. In the absence of this attribute, the type of a
     *            list will be inferred from the presence and content of labels
     *            on its items. Accepted values are found at
     *            {@link org.dspace.app.xmlui.wing.element.List#TYPES}
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * 
     * @return A new List
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public List addList(String name, String type, String rend)
            throws WingException
    {
        List list = new List(context, name, type, rend);
        contents.add(list);
        return list;
    }

    /**
     * Append a list to the division.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * 
     * @param type
     *            (May be null) an optional attribute to explicitly specify the
     *            type of list. In the absence of this attribute, the type of a
     *            list will be inferred from the presence and content of labels
     *            on its items. Accepted values are found at
     *            {@link org.dspace.app.xmlui.wing.element.List#TYPES}
     *
     * @return A new List
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public List addList(String name, String type) throws WingException
    {
        return this.addList(name, type, null);
    }

    /**
     * Append a list to the division. The list type will be inferred by the
     * presence and contents of labels and items.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * @return A new List
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public List addList(String name) throws WingException
    {
        return this.addList(name, null, null);
    }

    /**
     * Append a table to the division. When creating a table the number of rows
     * and columns contained in the table must be precomputed and provided
     * here.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * 
     * @param rows
     *            (Required) The number of rows in the table.
     * @param cols
     *            (Required) The number of columns in the table.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * 
     * @return A new table.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Table addTable(String name, int rows, int cols, String rend)
            throws WingException
    {
        Table table = new Table(context, name, rows, cols, rend);
        contents.add(table);
        return table;
    }

    /**
     * Append a table to the division. When creating a table the number of rows
     * and columns contained in the table must be pre computed and provided
     * here.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * 
     * @param rows
     *            (Required) The number of rows in the table.
     * @param cols
     *            (Required) The number of columns in the table.
     * 
     * @return A new table.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Table addTable(String name, int rows, int cols) throws WingException
    {
        return this.addTable(name, rows, cols, null);
    }

    /**
     * Add a reference set for metadata references.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * @param type
     *            (Required) The include type, see IncludeSet.TYPES
     * @param orderBy
     *            (May be null) An statement of ordering within the include set.
     * @param rend
     *            (May be null) a rendering hint used to override the default
     *            display of the element.
     * @return a new ReferenceSet.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public ReferenceSet addReferenceSet(String name, String type, String orderBy,
            String rend) throws WingException
    {
        ReferenceSet referenceSet = new ReferenceSet(context, false, name, type, orderBy, rend);
        contents.add(referenceSet);
        return referenceSet;
    }

    /**
     * Add a reference set for metadata references.
     * 
     * @param name
     *            (Required) a local identifier used to differentiate the
     *            element from its siblings.
     * @param type
     *            (Required) The include type, see IncludeSet.TYPES
     * @return a new ReferenceSet.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public ReferenceSet addReferenceSet(String name, String type)
            throws WingException
    {
        return addReferenceSet(name, type, null, null);
    }

    /**
     * Add a hidden field to the division, this is a common operation that is 
     * not directly supported by DRI. To create support for it a new paragraph 
     * will be created with the name "hidden-fields" and a render attribute of 
     * "hidden".
     * 
     * @param name 
     *              (Required) The hidden fields name.
     * @return A new hidden field.
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public Hidden addHidden(String name) throws WingException
    {
        if (hiddenFieldsPara == null) 
        {
            hiddenFieldsPara = addPara("hidden-fields","hidden");
        }
        
        return hiddenFieldsPara.addHidden(name);
    }

    /**
     * Add a section of translated HTML to the DRI document. This will only handle 
     * simple transformations such as {@literal <p>}, {@literal <b>},
     * {@literal <i>} and {@literal <a>} tags.
     *
     * <p>Depending on the given HTML this may result in multiple paragraphs being
     * opened and several bold tags being included.
     * 
     * @param blankLines
     * 				(Required) Treat blank lines as paragraphs delimiters.
     * @param HTML 
     * 				(Required) The HTML content
     * @throws org.dspace.app.xmlui.wing.WingException passed through.
     */
    public void addSimpleHTMLFragment(boolean blankLines, String HTML) throws WingException
    {
    	contents.add(new SimpleHTMLFragment(context,blankLines,HTML));
    }
    
    /**
     * Determine if the given SAX event is the same division element. This method 
     * will compare interactiveness, and rendering.
     * 
     * @param namespace
     *            The element's name space
     * @param localName
     *            The local, unqualified, name for this element
     * @param qName
     *            The qualified name for this element
     * @param attributes
     *            The element's attributes
     * @return True if this WingElement is equivalent to the given SAX Event.
     */
    @Override
    public boolean mergeEqual(String namespace, String localName, String qName,
            Attributes attributes) throws SAXException, WingException
    {

        if (!WingConstants.DRI.URI.equals(namespace))
        {
            return false;
        }

        if (!E_DIVISION.equals(localName))
        {
            return false;
        }

        context.getLogger().debug("Merging a division");
        
        String aName = attributes.getValue(A_NAME);
        String isInteractive = attributes.getValue(A_INTERACTIVE);
        String anAction = attributes.getValue(A_ACTION);
        String aMethod = attributes.getValue(A_METHOD);
        String render = attributes.getValue(A_RENDER);
        String pagination = attributes.getValue(A_PAGINATION);
        String behavior = attributes.getValue(A_BEHAVIOR);

        context.getLogger().debug("Merging got parameters name=" + aName
                + ", interactive=" + isInteractive
                + ", action=" + anAction
                + ", method=" + aMethod
                + ", render=" + render
                + ", pagination=" + pagination);

        // The name must be identical (but id's can differ)
        if (!this.name.equals(aName))
        {
            return false;
        }

        // Ensure the render attributes are identical.
        if (this.rend == null)
        {
            if (render != null)
            {
                return false;
            }
        }
        else if (!this.rend.equals(render))
        {
            return false;
        }

        if (this.interactive)
        {
            // Ensure all the interactive fields are identical.
            if (!"yes".equals(isInteractive))
            {
                return false;
            }
            if (!this.action.equals(anAction))
            {
                return false;
            }
            if (!this.method.equals(aMethod))
            {
                return false;
            }
            
            // For now let's just not merge divs that have behavior.
            if (!(behavior == null || behavior.equals("")))
            {
                return false;
            }
            
        } else {
            // Else, ensure that it is also not interactive.
            if (!(isInteractive == null || "no".equals(isInteractive)))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Merge the given sub-domain of metadata elements.
     * 
     * @param namespace
     *            The element's name space
     * @param localName
     *            The local, unqualified, name for this element *
     * @param qName
     *            The qualified name for this element
     * @param attributes
     *            The element's attributes
     * @return The child element
     */
    @Override
    public WingMergeableElement mergeChild(String namespace, String localName,
            String qName, Attributes attributes) throws SAXException,
            WingException
    {
        WingMergeableElement found = null;
        for (AbstractWingElement content : contents)
        {
            if (content instanceof WingMergeableElement)
            {
                WingMergeableElement candidate = (WingMergeableElement) content;
                if (candidate.mergeEqual(namespace, localName, qName,
                        attributes))
                {
                    found = candidate;
                }
            }
        }
        contents.remove(found);
        return found;
    }

    /**
     * Notify this element that it is being merged.
     * 
     * @return The attributes for this merged element
     */
    @Override
    public Attributes merge(Attributes attributes) throws SAXException,
            WingException
    {
        this.merged = true;
        return attributes;
    }

    /**
     * Translate this division to SAX
     * 
     * @param contentHandler
     *            (Required) The registered contentHandler where SAX events
     *            should be routed, too.
     * @param lexicalHandler
     *            (Required) The registered lexicalHandler where lexical 
     *            events (such as CDATA, DTD, etc) should be routed too.
     * @param namespaces
     *            (Required) SAX Helper class to keep track of namespaces able
     *            to determine the correct prefix for a given namespace URI.
     */
    @Override
    public void toSAX(ContentHandler contentHandler, LexicalHandler lexicalHandler, 
            NamespaceSupport namespaces) throws SAXException
    {
        if (!merged)
        {
            AttributeMap divAttributes = new AttributeMap();
            divAttributes.put(A_NAME, name);
            divAttributes.put(A_ID, context.generateID(E_DIVISION, name));
            if (interactive)
            {
                divAttributes.put(A_INTERACTIVE, "yes");
                divAttributes.put(A_ACTION, action);
                divAttributes.put(A_METHOD, method);
                
                if (behaviorAJAXenabled)
                {
                    divAttributes.put(A_BEHAVIOR,"ajax");
                }
                
                if (behaviorSensitiveFields != null)
                {
                    divAttributes.put(A_BEHVIOR_SENSITIVE_FIELDS,behaviorSensitiveFields);
                }
            }

            if (PAGINATION_SIMPLE.equals(paginationType))
            {

                divAttributes.put(A_PAGINATION, paginationType);
                if (previousPage != null)
                {
                    divAttributes.put(A_PREVIOUS_PAGE, previousPage);
                }
                if (nextPage != null)
                {
                    divAttributes.put(A_NEXT_PAGE, nextPage);
                }
                divAttributes.put(A_ITEMS_TOTAL, itemsTotal);
                divAttributes.put(A_FIRST_ITEM_INDEX, firstItemIndex);
                divAttributes.put(A_LAST_ITEM_INDEX, lastItemIndex);
            }
            else if (PAGINATION_MASKED.equals(paginationType))
            {

                divAttributes.put(A_PAGINATION, paginationType);
                divAttributes.put(A_ITEMS_TOTAL, itemsTotal);
                divAttributes.put(A_FIRST_ITEM_INDEX, firstItemIndex);
                divAttributes.put(A_LAST_ITEM_INDEX, lastItemIndex);
                divAttributes.put(A_CURRENT_PAGE, currentPage);
                divAttributes.put(A_PAGES_TOTAL, pagesTotal);
                divAttributes.put(A_PAGE_URL_MASK, pageURLMask);
            }

            if (rend != null)
            {
                divAttributes.put(A_RENDER, rend);
            }

            startElement(contentHandler, namespaces, E_DIVISION, divAttributes);

            if (head != null)
            {
                head.toSAX(contentHandler, lexicalHandler, namespaces);
            }
        }
        
        for (AbstractWingElement content : contents)
        {
            content.toSAX(contentHandler, lexicalHandler, namespaces);
        }

        if (!merged) {
            endElement(contentHandler, namespaces, E_DIVISION);
        }
    }

    /**
     * dispose
     */
    @Override
    public void dispose()
    {

        if (head != null)
        {
            head.dispose();
            head = null;
        }
        if (contents != null)
        {
            for (AbstractWingElement content : contents)
            {
                content.dispose();
            }
            contents.clear();
            contents = null;
        }
        super.dispose();
    }
}
