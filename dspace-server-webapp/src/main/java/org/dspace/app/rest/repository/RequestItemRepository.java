/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.repository;

import static org.apache.commons.lang3.StringUtils.isBlank;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.requestitem.RequestItemAuthorExtractor;
import org.dspace.app.requestitem.RequestItemEmailNotifier;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.app.rest.converter.RequestItemConverter;
import org.dspace.app.rest.exception.IncompleteItemRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.RequestItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Component to expose item requests.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
@Component(RequestItemRest.CATEGORY + '.' + RequestItemRest.NAME)
public class RequestItemRepository
        extends DSpaceRestRepository<RequestItemRest, String> {
    private static final Logger LOG = LogManager.getLogger();

    @Autowired(required = true)
    protected RequestItemService requestItemService;

    @Autowired(required = true)
    protected BitstreamService bitstreamService;

    @Autowired(required = true)
    protected ItemService itemService;

    @Autowired(required = true)
    protected RequestItemConverter requestItemConverter;

    @Resource(name = "requestItemAuthorExtractor")
    protected RequestItemAuthorExtractor requestItemAuthorExtractor;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    /*
     * DSpaceRestRepository
     */

    @PreAuthorize("permitAll()")
    @Override
    public RequestItemRest findOne(Context context, String token) {
        RequestItem requestItem = requestItemService.findByToken(context, token);
        if (null == requestItem) {
            return null;
        } else {
            return requestItemConverter.convert(requestItem, Projection.DEFAULT);
        }
    }

    @Override
    public Page<RequestItemRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException(RequestItemRest.NAME, "findAll");
    }

    @Override
    @PreAuthorize("permitAll()")
    public RequestItemRest createAndReturn(Context ctx)
            throws AuthorizeException, SQLException {
        // Fill a RequestItemRest from the client's HTTP request.
        HttpServletRequest req = getRequestService()
                .getCurrentRequest()
                .getHttpServletRequest();
        ObjectMapper mapper = new ObjectMapper();
        RequestItemRest rir;
        try {
            rir = mapper.readValue(req.getInputStream(), RequestItemRest.class);
        } catch (IOException ex) {
            throw new UnprocessableEntityException("error parsing the body", ex);
        }

        // Check request.item.type:
        // "all" = anyone can request.
        // "logged" = only authenticated user can request.
        EPerson user = ctx.getCurrentUser();
        String allowed = configurationService.getProperty("request.item.type", "logged");
        if ("logged".equalsIgnoreCase(allowed) && null == user) {
            throw new AuthorizeException("Anonymous requests are not permitted.");
        }

        /* Create the item request model object from the REST object. */

        // Requesting all bitstreams of an item, or a single bitstream?
        boolean allFiles = rir.isAllfiles();

        // Requested bitstream.  Ignored if all files requested, otherwise required.
        Bitstream bitstream;
        if (!allFiles) {
            String bitstreamId = rir.getBitstreamId();
            if (isBlank(bitstreamId)) {
                throw new IncompleteItemRequestException("A bitstream ID is required");
            }
            bitstream = bitstreamService.find(ctx, UUID.fromString(bitstreamId));
            if (null == bitstream) {
                throw new IncompleteItemRequestException("That bitstream does not exist");
            }
        } else {
            bitstream = null;
        }

        // Requested item.
        String itemId = rir.getItemId();
        if (isBlank(itemId)) {
            throw new IncompleteItemRequestException("An item ID is required");
        }
        Item item = itemService.find(ctx, UUID.fromString(itemId));
        if (null == item) {
            throw new IncompleteItemRequestException("That item does not exist");
        }

        // Requester's email address.
        String email;
        if (null != user) { // Prefer authenticated user's email.
            email = user.getEmail();
        } else { // Require an anonymous session to provide an email address.
            email = rir.getRequestEmail();
            if (isBlank(email)) {
                throw new IncompleteItemRequestException("A submitter's email address is required");
            }
            EmailValidator emailValidator = EmailValidator.getInstance(false, false);
            if (!emailValidator.isValid(email)) {
                throw new UnprocessableEntityException("Invalid email address");
            }
        }

        // Requester's human-readable name.
        String username;
        if (null != user) { // Prefer authenticated user's name.
            username = user.getFullName();
        } else { // An anonymous session may provide a name.
            // Escape username to evade nasty XSS attempts
            username = StringEscapeUtils.escapeHtml4(rir.getRequestName());
        }

        // Requester's message text, escaped to evade nasty XSS attempts
        String message = StringEscapeUtils.escapeHtml4(rir.getRequestMessage());

        // Create the request.
        String token;
        token = requestItemService.createRequest(ctx, bitstream, item,
                allFiles, email, username, message);

        // Some fields are given values during creation, so return created request.
        RequestItem ri = requestItemService.findByToken(ctx, token);
        ri.setAccept_request(false); // Not accepted yet.  Must set:  DS-4032
        requestItemService.update(ctx, ri);

        // Create a link back to DSpace for the approver's response.
        String responseLink;
        try {
            responseLink = getLinkTokenEmail(ri.getToken());
        } catch (URISyntaxException | MalformedURLException e) {
            LOG.warn("Impossible URL error while composing email:  {}",
                    e::getMessage);
            throw new RuntimeException("Request not sent:  " + e.getMessage());
        }

        // Send the request email
        try {
            RequestItemEmailNotifier.sendRequest(ctx, ri, responseLink);
        } catch (IOException | SQLException ex) {
            throw new RuntimeException("Request not sent.", ex);
        }

        return requestItemConverter.convert(ri, Projection.DEFAULT);
    }

    // NOTICE:  there is no service method for this -- requests are never deleted?
    @Override
    public void delete(Context context, String token)
            throws AuthorizeException, RepositoryMethodNotImplementedException {
        throw new RepositoryMethodNotImplementedException(RequestItemRest.NAME, "delete");
    }

    @Override
    @PreAuthorize("permitAll()")
    public RequestItemRest put(Context context, HttpServletRequest request,
            String apiCategory, String model, String token, JsonNode requestBody)
            throws AuthorizeException {
        RequestItem ri = requestItemService.findByToken(context, token);
        if (null == ri) {
            throw new UnprocessableEntityException("Item request not found");
        }

        // Do not permit updates after a decision has been given.
        Date decisionDate = ri.getDecision_date();
        if (null != decisionDate) {
            throw new UnprocessableEntityException("Request was "
                    + (ri.isAccept_request() ? "granted" : "denied")
                    + " on " + decisionDate + " and may not be updated.");
        }

        // Make the changes
        JsonNode acceptRequestNode = requestBody.findValue("acceptRequest");
        if (null == acceptRequestNode) {
            throw new UnprocessableEntityException("acceptRequest is required");
        } else {
            ri.setAccept_request(acceptRequestNode.asBoolean());
        }

        JsonNode responseMessageNode = requestBody.findValue("responseMessage");
        String message = responseMessageNode.asText();

        ri.setDecision_date(new Date());
        requestItemService.update(context, ri);

        // Send the response email
        String subject = requestBody.findValue("subject").asText();
        try {
            RequestItemEmailNotifier.sendResponse(context, ri, subject, message);
        } catch (IOException ex) {
            LOG.warn("Response not sent:  {}", ex::getMessage);
            throw new RuntimeException("Response not sent", ex);
        }

        // Perhaps send Open Access request to admin.s.
        if (requestBody.findValue("suggestOpenAccess").asBoolean(false)) {
            try {
                RequestItemEmailNotifier.requestOpenAccess(context, ri);
            } catch (IOException ex) {
                LOG.warn("Open access request not sent:  {}", ex::getMessage);
                throw new RuntimeException("Open access request not sent", ex);
            }
        }

        // Return updated request.
        RequestItemRest rir = requestItemConverter.convert(ri, Projection.DEFAULT);
        return rir;
    }

    @Override
    public Class<RequestItemRest> getDomainClass() {
        return RequestItemRest.class;
    }

    /**
     * Generate a link back to DSpace, to act on a request.
     *
     * @param token identifies the request.
     * @return URL to the item request API, with the token as request parameter
     *          "token".
     * @throws URISyntaxException passed through.
     * @throws MalformedURLException passed through.
     */
    private String getLinkTokenEmail(String token)
            throws URISyntaxException, MalformedURLException {
        final String base = configurationService.getProperty("dspace.ui.url");

        URI link = new URIBuilder(base)
                .setPathSegments("request-a-copy", token)
                .build();

        return link.toURL().toExternalForm();
    }
}
