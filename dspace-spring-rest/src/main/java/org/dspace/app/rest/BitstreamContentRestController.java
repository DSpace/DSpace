/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.repository.BitstreamRestRepository;
import org.dspace.core.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is a specialized controller to provide access to the bitstream binary content
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@RestController
@RequestMapping("/api/"+BitstreamRest.CATEGORY +"/"+ BitstreamRest.PLURAL_NAME + "/{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}/content")
public class BitstreamContentRestController {
	@Autowired
	private BitstreamRestRepository bitstreamRestRepository; 
	
	@RequestMapping(method = RequestMethod.GET)
	public void retrieve(@PathVariable UUID uuid, HttpServletResponse response,
            HttpServletRequest request) throws IOException {
		BitstreamRest bit = bitstreamRestRepository.findOne(uuid);
		if (bit == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		response.setHeader("ETag", bit.getCheckSum().getValue());
		response.setContentLengthLong(bit.getSizeBytes());
        // Check for if-modified-since header
        long modSince = request.getDateHeader("If-Modified-Since");
// we should keep last modification date on the bitstream
//            if (modSince != -1 && item.getLastModified().getTime() < modSince)
//            {
//                // Item has not been modified since requested date,
//                // hence bitstream has not; return 304
//                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
//                return;
//            }
        
        // Pipe the bits
        InputStream is = bitstreamRestRepository.retrieve(uuid);
     
		// Set the response MIME type
        response.setContentType(bit.getFormat().getMimetype());

        Utils.bufferedCopy(is, response.getOutputStream());
        is.close();
        response.getOutputStream().flush();
	}
}