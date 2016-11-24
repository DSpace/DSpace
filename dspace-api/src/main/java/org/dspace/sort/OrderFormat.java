/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sort;

import org.dspace.core.factory.CoreServiceFactory;

/**
 * Class implementing static helpers for anywhere that interacts with the sort columns
 * (ie. ItemsByAuthor.sort_author, ItemsByTitle.sort_title)
 *
 * This class maps index 'types' to delegates that implement the sort string creation
 * 
 * Types can be defined or configured using the plugin manager:
 * 
 * plugin.named.org.dspace.sort.OrderFormatDelegate=
 *         org.dspace.sort.OrderFormatTitleMarc21=title
 *         org.dspace.sort.OrderFormatAuthor=author
 * 
 * The following standard types have been defined by default, but can be reconfigured
 * via the plugin manager:
 * 
 * author    = org.dspace.sort.OrderFormatAuthor
 * title    = org.dspace.sort.OrderFormatTitle
 * text     = org.dspace.sort.OrderFormatText
 * 
 * IMPORTANT - If you change any of the orderings, you need to rebuild the browse sort columns
 * (ie. run 'index-all', or 'dsrun org.dspace.browse.InitializeBrowse')
 * 
 * @author Graham Triggs
 * @version $Revision$
 */
public class OrderFormat
{
    public static final String AUTHOR = "author";
    public static final String TITLE  = "title";
    public static final String TEXT   = "text";
    public static final String DATE   = "date";
    public static final String AUTHORITY = "authority";
    
    // Array of all available order delegates - avoids excessive calls to plugin manager
    private static final String[] delegates = CoreServiceFactory.getInstance().getPluginService().getAllPluginNames(OrderFormatDelegate.class);

    private static final OrderFormatDelegate authorDelegate = new OrderFormatAuthor();
    private static final OrderFormatDelegate titleDelegate  = new OrderFormatTitle();
    private static final OrderFormatDelegate textDelegate   = new OrderFormatText();
    private static final OrderFormatDelegate dateDelegate   = new OrderFormatDate();
    private static final OrderFormatDelegate authorityDelegate = new OrderFormatText();
    
    /**
     * Generate a sort string for the given DC metadata
     * @param value
     *     metadata value
     * @param language
     *     metadata language code
     * @param type
     *     metadata type
     * @return sort string
     *     
     * @see OrderFormat#AUTHOR
     * @see OrderFormat#TITLE
     * @see OrderFormat#TEXT
     * @see OrderFormat#DATE
     * @see #AUTHORITY
     */
    public static String makeSortString(String value, String language, String type)
    {
        OrderFormatDelegate delegate = null;
        
        // If there is no value, return null
        if (value == null)
        {
            return null;
        }

        // If a named index has been supplied
        if (type != null && type.length() > 0)
        {
            // Use a delegate if one is configured
            delegate = OrderFormat.getDelegate(type);
            if (delegate != null)
            {
                return delegate.makeSortString(value, language);
            }

            // No delegates found, so apply defaults
            if (type.equalsIgnoreCase(OrderFormat.AUTHOR) && authorDelegate != null)
            {
              return authorDelegate.makeSortString(value, language);
            }

            if (type.equalsIgnoreCase(OrderFormat.TITLE) && titleDelegate != null)
            {
              return titleDelegate.makeSortString(value, language);
            }

            if (type.equalsIgnoreCase(OrderFormat.TEXT) && textDelegate != null)
            {
              return textDelegate.makeSortString(value, language);
            }
            
            if (type.equalsIgnoreCase(OrderFormat.DATE) && dateDelegate != null)
            {
              return dateDelegate.makeSortString(value, language);
            }

            if (type.equalsIgnoreCase(OrderFormat.AUTHORITY) && authorityDelegate != null)
            {
              return authorityDelegate.makeSortString(value, language);
            }
        }

        return value;
    }

    /**
     * Retrieve the named delegate
     */
    private static OrderFormatDelegate getDelegate(String name)
    {
           if (name != null && name.length() > 0)
           {
               // Check the cached array of names to see if the delegate has been configured
               for (int idx = 0; idx < delegates.length; idx++)
               {
                   if (delegates[idx].equals(name))
                   {
                       return (OrderFormatDelegate)CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(OrderFormatDelegate.class, name);
                   }
               }
           }
           
        return null;
    }
}
