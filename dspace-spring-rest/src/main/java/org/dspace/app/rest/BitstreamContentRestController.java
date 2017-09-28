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

import org.apache.commons.io.IOUtils;
import org.apache.tika.Tika;
import org.apache.tika.mime.MimeTypes;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.repository.BitstreamRestRepository;
import org.dspace.app.rest.utils.MultipartFileSender;
import org.springframework.beans.factory.annotation.Autowired;
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

	private int buffer = 20480;


	@RequestMapping(method = RequestMethod.GET)
	public void retrieve(@PathVariable UUID uuid, HttpServletResponse response,
											 HttpServletRequest request) throws IOException {
		BitstreamRest bit = bitstreamRestRepository.findOne(uuid);
		if (bit == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

        // Pipe the bits
        InputStream is = bitstreamRestRepository.retrieve(uuid);

        String mimetype = bit.getFormat().getMimetype();
        //This should be improved somewhere else so we don't have to look for the correct mimetype here
        if (mimetype.equals(MimeTypes.OCTET_STREAM)) {
        	Tika tika = new Tika();
        	mimetype = tika.detect(is);
        	is.close();
			is = bitstreamRestRepository.retrieve(uuid);
		}

        //MultipartFileSender
		try {
			MultipartFileSender.fromBitstream(bit).with(request).with(response).with(is).with(mimetype).serveResource();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			IOUtils.closeQuietly(is);
		}

	}

}