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
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.requestitem.RequestItemEmailNotifier;
import org.dspace.app.requestitem.service.RequestItemService;
import org.dspace.app.rest.Parameter;
import org.dspace.app.rest.SearchRestMethod;
import org.dspace.app.rest.converter.RequestItemConverter;
import org.dspace.app.rest.exception.IncompleteItemRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.RequestItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.InvalidReCaptchaException;
import org.dspace.eperson.factory.CaptchaServiceFactory;
import org.dspace.eperson.service.CaptchaService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;
/**
 * Component to expose item requests and handle operations like create (request), put (grant/deny), and
 * email sending. Support for requested item access by a secure token / link is supported as well as the legacy
 * "attach files to email" method. See dspace.cfg for configuration.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 * @author Kim Shepherd
 */
@Component(RequestItemRest.CATEGORY + '.' + RequestItemRest.PLURAL_NAME)
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

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired(required = true)
    protected RequestItemEmailNotifier requestItemEmailNotifier;
    @Autowired
    protected AuthorizeService authorizeService;

    // TODO: Work towards full coverage of captcha, so we can use getCaptchaService() instead
    private CaptchaService captchaService = CaptchaServiceFactory.getInstance().getAltchaCaptchaService();

    private static final Logger log = LogManager.getLogger();

    @Autowired
    private ObjectMapper mapper;

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

        // If captcha is configured for this action, perform validation
        if (configurationService.getBooleanProperty("request.item.create.captcha", false)) {
            // Get captcha payload header, if any
            String captchaPayloadHeader = req.getHeader("x-captcha-payload");
            if (StringUtils.isBlank(captchaPayloadHeader)) {
                throw new AuthorizeException("Valid captcha payload is required");
            }
            // Validate and verify captcha payload token or proof of work
            // Rethrow exception as authZ exception if validation fails
            try {
                captchaService.processResponse(captchaPayloadHeader, "request_item");
            } catch (InvalidReCaptchaException e) {
                throw new AuthorizeException(e.getMessage());
            }
        }

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
            username = HtmlUtils.htmlEscape(rir.getRequestName(),"UTF-8");
        }

        // Requester's message text, escaped to evade nasty XSS attempts
        String message = HtmlUtils.htmlEscape(rir.getRequestMessage(),"UTF-8");

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
            responseLink = requestItemService.getLinkTokenEmail(ri.getToken());
        } catch (URISyntaxException | MalformedURLException e) {
            LOG.warn("Impossible URL error while composing email:  {}",
                    e::getMessage);
            throw new RuntimeException("Request not sent:  " + e.getMessage());
        }

        // Send the request email
        try {
            requestItemEmailNotifier.sendRequest(ctx, ri, responseLink);
        } catch (IOException | SQLException ex) {
            throw new RuntimeException("Request not sent.", ex);
        }
        // #8636 - Security issue: Should not return RequestItemRest to avoid token exposure
        return null;
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

        // Previously there was a check here to prevent updates after *any* decision was given.
        // This is now updated to allow specific updates to *granted* requests, so that it is possible
        // to revoke access tokens or alter access period
        // Throw error only if decision date was set but was denied
        if (null != ri.getDecision_date() && !ri.isAccept_request()) {
            throw new UnprocessableEntityException("Item request was already denied, no further updates are possible");
        }

        // Make the changes

        // Extract and set the 'accept' indicator
        JsonNode acceptRequestNode = requestBody.findValue("acceptRequest");
        if (null == acceptRequestNode) {
            throw new UnprocessableEntityException("acceptRequest is required");
        } else {
            ri.setAccept_request(acceptRequestNode.asBoolean());
        }

        // Extract and set the response message to include in the email
        JsonNode responseMessageNode = requestBody.findValue("responseMessage");
        String message = null;
        if (responseMessageNode != null && !responseMessageNode.isNull()) {
            message = responseMessageNode.asText();
        }

        // Set the decision date (now)`
        ri.setDecision_date(Instant.now());

        // If the (optional) access expiry period was included, extract it here and set accordingly
        // We expect it to be sent either as a timestamp or as a delta math like +7DAYS
        JsonNode accessPeriod = requestBody.findValue("accessPeriod");
        if (accessPeriod != null && !accessPeriod.isNull()) {
            // The request item service is responsible for parsing and setting the expiry date based
            // on a delta like "+7DAYS" or special string like "FOREVER", or a formatted date
            requestItemService.setAccessExpiry(ri, accessPeriod.asText());
        }

        JsonNode responseSubjectNode = requestBody.findValue("subject");
        String subject = null;
        if (responseSubjectNode != null && !responseSubjectNode.isNull()) {
            subject = responseSubjectNode.asText();
        }
        requestItemService.update(context, ri);

        // Send the response email
        try {
            requestItemEmailNotifier.sendResponse(context, ri, subject, message);
        } catch (IOException ex) {
            LOG.warn("Response not sent:  {}", ex::getMessage);
            throw new RuntimeException("Response not sent", ex);
        }

        // Perhaps send Open Access request to admin.s.
        if (requestBody.findValue("suggestOpenAccess").asBoolean(false)) {
            try {
                requestItemEmailNotifier.requestOpenAccess(context, ri);
            } catch (IOException ex) {
                LOG.warn("Open access request not sent:  {}", ex::getMessage);
                throw new RuntimeException("Open access request not sent", ex);
            }
        }

        // Return updated request.
        RequestItemRest rir = requestItemConverter.convert(ri, Projection.DEFAULT);
        return rir;
    }

    /**
     *
     * @param accessToken
     * @return
     */
    @PreAuthorize("permitAll()")
    @SearchRestMethod(name = "byAccessToken")
    public RequestItemRest findByAccessToken(@Parameter(value = "accessToken", required = true) String accessToken) {

        // Send 404 NOT FOUND if access token is null
        if (StringUtils.isBlank(accessToken)) {
            throw new ResourceNotFoundException("No such accessToken=" + accessToken);
        }

        // Get the current context and request item
        Context context = obtainContext();
        RequestItem requestItem = requestItemService.findByAccessToken(context, accessToken);

        // Previously, a 404 was thrown if the request item was not found, and a 401 or 403 was thrown depending
        // on authorization and validity checks. These checks are still strictly enforced in the BitstreamContoller
        // and BitstreamResourceAccessByToken classes for actual downloads, but here we continue to pass a 200 OK
        // response so that we can display more meaningful alerts to users in the item page rather than serve hard
        // redirects or lose information like expiry dates and access status

        // Sanitize the request item (stripping personal data) for privacy
        requestItemService.sanitizeRequestItem(context, requestItem);
        // Convert and return the final request item
        return requestItemConverter.convert(requestItem, utils.obtainProjection());
    }

    @Override
    public Class<RequestItemRest> getDomainClass() {
        return RequestItemRest.class;
    }

}
