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
import java.sql.SQLException;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.MultipartFileSender;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is a specialized controller to provide access to the bitstream binary content
 *
 */
@RestController
@RequestMapping("/api/"+BitstreamRest.CATEGORY +"/"+ BitstreamRest.PLURAL_NAME + "/{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}/content")
public class BitstreamContentRestController {

	private static final Logger log = Logger.getLogger(BitstreamContentRestController.class);

	@Autowired
	private BitstreamService bitstreamService;

	@RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD})
	public void retrieve(@PathVariable UUID uuid, HttpServletResponse response,
											 HttpServletRequest request) throws IOException, SQLException {

		Context context = ContextUtil.obtainContext(request);

		Bitstream bit = bitstreamService.find(context, uuid);
		if (bit == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		Long lastModified = bitstreamService.getLastModified(bit);
        String mimetype = bit.getFormat(context).getMIMEType();

        //TODO LOG DOWNLOAD if no range or if last chunk

		// Pipe the bits
		try(InputStream is = bitstreamService.retrieve(context, bit)) {

			MultipartFileSender
					.fromInputStream(is)
					.withFileName(bit.getName())
					.withLength(bit.getSize())
					.withChecksum(bit.getChecksum())
					.withMimetype(mimetype)
					.withLastModified(lastModified)
					.with(request)
					.with(response)

					.serveResource();

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}

	}


}