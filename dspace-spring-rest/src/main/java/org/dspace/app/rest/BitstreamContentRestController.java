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
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.dspace.app.rest.model.BitstreamRest;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.MultipartFileSender;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.dspace.services.EventService;
import org.dspace.usage.UsageEvent;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This is a specialized controller to provide access to the bitstream binary content
 */
@RestController
@RequestMapping("/api/"+BitstreamRest.CATEGORY +"/"+ BitstreamRest.PLURAL_NAME + "/{uuid:[0-9a-fxA-FX]{8}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{4}-[0-9a-fxA-FX]{12}}/content")
public class BitstreamContentRestController implements InitializingBean {

	private static final Logger log = Logger.getLogger(BitstreamContentRestController.class);

	@Autowired
	private BitstreamService bitstreamService;

	@Autowired
	private EventService eventService;

	@Autowired
	private ConfigurationService configurationService;

	private int bufferSize;

    @Override
    public void afterPropertiesSet() throws Exception {
        bufferSize = configurationService.getIntProperty("bitstream-download.buffer.size", -1);
    }

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

		// Pipe the bits
		try(InputStream is = getInputStream(context, bit)) {

			MultipartFileSender sender = MultipartFileSender
					.fromInputStream(is)
					.withBufferSize(bufferSize)
					.withFileName(bit.getName())
					.withLength(bit.getSize())
					.withChecksum(bit.getChecksum())
					.withMimetype(mimetype)
					.withLastModified(lastModified)
					.with(request)
					.with(response);

			if (sender.isValid()) {
				sender.serveResource();
			}

			if (sender.isNoRangeRequest() && isNotAnErrorResponse(response)) {
				//We only log a download request when serving a request without Range header. This is because
				//a browser always sends a regular request first to check for Range support.
				eventService.fireEvent(
						new UsageEvent(
								UsageEvent.Action.VIEW,
								request,
								context,
								bit));
			}

		} catch(IOException ex) {
			log.debug("Client aborted the request before the download was completed. Client is probably switching to a Range request.", ex);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	private InputStream getInputStream(Context context, Bitstream bit) {
		try {
			return bitstreamService.retrieve(context, bit);
		} catch (Exception e) {
			log.warn("Unable to retrieve bitstream for bitstream with ID " + bit.getID());
			return null;
		}
	}

	private boolean isNotAnErrorResponse(HttpServletResponse response) {
		Response.Status.Family responseCode = Response.Status.Family.familyOf(response.getStatus());
		return responseCode.equals(Response.Status.Family.SUCCESSFUL)
				|| responseCode.equals(Response.Status.Family.REDIRECTION);
	}
}