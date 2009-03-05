/* MediaEntryManager.java
 *
 * Copyright (c) 2007, Aberystwyth University
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  - Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the
 *    following disclaimer.
 *
 *  - Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 *  - Neither the name of the Centre for Advanced Software and
 *    Intelligent Systems (CASIS) nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
package org.dspace.sword;

import org.purl.sword.base.AtomDocumentResponse;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.SWORDEntry;
import org.apache.log4j.Logger;
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
	/** logger */
	private static Logger log = Logger.getLogger(MediaEntryManager.class);

	/** sword service implementation */
	private SWORDService swordService;

	public MediaEntryManager(SWORDService swordService)
	{
		this.swordService = swordService;
	}

	/**
	 * Get the media entry for the given URL request.  If the url is unavailable
	 * this method will throw the appropriate sword errors, with DSpace custom
	 * URLs
	 * 
	 * @param url
	 * @return
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
			throw new SWORDErrorException(DSpaceSWORDErrorCodes.MEDIA_UNAVAILABLE, "The media link you requested is not available");
		}

		// extract the thing that we are trying to get a media entry on
		DSpaceObject dso = urlManager.extractDSpaceObject(url);

		// now, the media entry should always be to an actual file, so we only care that this is a bitstream
		if (!(dso instanceof Bitstream))
		{
			throw new SWORDErrorException(DSpaceSWORDErrorCodes.BAD_URL, "The url you provided does not resolve to an appropriate object");
		}

		// now construct the atom entry for the bitstream
		DSpaceATOMEntry dsatom = new BitstreamEntryGenerator(swordService);
		SWORDEntry entry = dsatom.getSWORDEntry(dso);
		response.setEntry(entry);
		return response;
	}
}
