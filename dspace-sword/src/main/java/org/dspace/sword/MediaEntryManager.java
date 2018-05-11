/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword;

import org.purl.sword.base.AtomDocumentResponse;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.SWORDEntry;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Bitstream;

/**
 * @author Richard Jones
 *
 * Class to provide tools to manage media links and media entries for sword
 *
 */
public class MediaEntryManager
{
    /** sword service implementation */
    private SWORDService swordService;

    public MediaEntryManager(SWORDService swordService)
    {
        this.swordService = swordService;
    }

    /**
     * Get the media entry for the given URL request.  If the URL is
     * unavailable this method will throw the appropriate SWORD errors,
     * with DSpace custom URLs.
     *
     * @param url
     * @throws DSpaceSWORDException
     * @throws SWORDErrorException
     */
    public AtomDocumentResponse getMediaEntry(String url)
            throws DSpaceSWORDException, SWORDErrorException
    {
        SWORDUrlManager urlManager = swordService.getUrlManager();

        AtomDocumentResponse response = new AtomDocumentResponse(200);

        if (url == null || urlManager.isBaseMediaLinkUrl(url))
        {
            // we are dealing with a default media-link, indicating that something
            // is wrong

            // FIXME: what do we actually do about this situation?
            // throwing an error for the time being
            throw new SWORDErrorException(
                    DSpaceSWORDErrorCodes.MEDIA_UNAVAILABLE,
                    "The media link you requested is not available");
        }

        // extract the thing that we are trying to get a media entry on
        DSpaceObject dso = urlManager.extractDSpaceObject(url);

        // now, the media entry should always be to an actual file, so we only care that this is a bitstream
        if (!(dso instanceof Bitstream))
        {
            throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL,
                    "The url you provided does not resolve to an appropriate object");
        }

        // now construct the atom entry for the bitstream
        DSpaceATOMEntry dsatom = new BitstreamEntryGenerator(swordService);
        SWORDEntry entry = dsatom.getSWORDEntry(dso);
        response.setEntry(entry);
        return response;
    }
}
