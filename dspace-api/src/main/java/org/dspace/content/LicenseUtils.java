/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Formatter;
import java.util.Locale;
import java.util.Map;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.license.FormattableArgument;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Utility class to manage generation and storing of the license text that the
 * submitter has to grant/granted for archiving the item
 * 
 * @author bollini
 * 
 */
public class LicenseUtils
{
    private static final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private static final BitstreamFormatService bitstreamFormat = ContentServiceFactory.getInstance().getBitstreamFormatService();
    private static final CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
    private static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    /**
     * Return the text of the license that the user has granted/must grant
     * before for submit the item. The license text is build using the template
     * defined for the collection if any or the wide site configuration. In the
     * license text the following substitutions can be used.<br>
     * {0} the eperson firstname<br>
     * {1} the eperson lastname<br>
     * {2} the eperson email<br>
     * {3} the current date<br>
     * {4} the collection object that will be formatted using the appropriate
     * LicenseArgumentFormatter plugin (if defined)<br>
     * {5} the item object that will be formatted using the appropriate
     * LicenseArgumentFormatter plugin (if defined)<br>
     * {6} the eperson object that will be formatted using the appropriate
     * LicenseArgumentFormatter plugin (if defined)<br>
     * {x} any addition argument supplied wrapped in the
     * LicenseArgumentFormatter based on his type (map key)
     * 
     * @param locale
     * @param collection
     * @param item
     * @param eperson
     * @param additionalInfo
     * @return the license text obtained substituting the provided argument in
     *         the license template
     */
    public static String getLicenseText(Locale locale, Collection collection,
            Item item, EPerson eperson, Map<String, Object> additionalInfo)
    {
        Formatter formatter = new Formatter(locale);

        // EPerson firstname, lastname, email and the current date
        // will be available as separate arguments to make more simple produce
        // "tradition" text license
        // collection, item and eperson object will be also available
        int numArgs = 7 + (additionalInfo != null ? additionalInfo.size() : 0);
        Object[] args = new Object[numArgs];
        args[0] = eperson.getFirstName();
        args[1] = eperson.getLastName();
        args[2] = eperson.getEmail();
        args[3] = new java.util.Date();
        args[4] = new FormattableArgument("collection", collection);
        args[5] = new FormattableArgument("item", item);
        args[6] = new FormattableArgument("eperson", eperson);

        if (additionalInfo != null)
        {
            int i = 7; // Start is next index after previous args
            for (Map.Entry<String, Object> info : additionalInfo.entrySet())
            {
                args[i] = new FormattableArgument(info.getKey(), info.getValue());
                i++;
            }
        }

        String licenseTemplate = collectionService.getLicense(collection);

        return formatter.format(licenseTemplate, args).toString();
    }

    /**
     * Utility method if no additional arguments are to be supplied to the
     * license template. (equivalent to calling the full getLicenseText
     * supplying {@code null} for the additionalInfo argument)
     *
     * @param locale
     * @param collection
     * @param item
     * @param eperson
     * @return the license text, with no custom substitutions.
     */
    public static String getLicenseText(Locale locale, Collection collection,
            Item item, EPerson eperson)
    {
        return getLicenseText(locale, collection, item, eperson, null);
    }

    /**
     * Store a copy of the license a user granted in the item.
     * 
     * @param context
     *            the dspace context
     * @param item
     *            the item object of the license
     * @param licenseText
     *            the license the user granted
     * @throws SQLException if database error
     * @throws IOException if IO error
     * @throws AuthorizeException if authorization error
     */
    public static void grantLicense(Context context, Item item,
            String licenseText) throws SQLException, IOException,
            AuthorizeException
    {
        // Put together text to store
        // String licenseText = "License granted by " + eperson.getFullName()
        // + " (" + eperson.getEmail() + ") on "
        // + DCDate.getCurrent().toString() + " (GMT):\n\n" + license;

        // Store text as a bitstream
        byte[] licenseBytes = licenseText.getBytes("UTF-8");
        ByteArrayInputStream bais = new ByteArrayInputStream(licenseBytes);
        Bitstream b = itemService.createSingleBitstream(context, bais, item, "LICENSE");

        // Now set the format and name of the bitstream
        b.setName(context, "license.txt");
        b.setSource(context, "Written by org.dspace.content.LicenseUtils");

        // Find the License format
        BitstreamFormat bf = bitstreamFormat.findByShortDescription(context,
                "License");
        b.setFormat(bf);

        bitstreamService.update(context, b);
    }
}
