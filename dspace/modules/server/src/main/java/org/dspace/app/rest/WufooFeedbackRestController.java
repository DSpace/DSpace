package org.dspace.app.rest;

import static org.dspace.app.rest.utils.ContextUtil.obtainContext;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Link;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint for retrieving the Wufoo Feedback Form information.
 */
@RestController
public class WufooFeedbackRestController implements InitializingBean {
    private static final Logger log = LogManager.getLogger(WufooFeedbackRestController.class);

    /**
     * The REST endpoint to use to retrieve Wufoo Feedback form information
     */
    public static final String WUFOO_ENDPOINT ="/api/wufoo-feedback";

    // Configuration property names for retrieving Wufoo API settings
    // Each of these properties should be defined in the "local.cfg" file.
    private static final String FORM_URL = "wufoo.feedback.formUrl";
    private static final String FORM_HASH = "wufoo.feedback.formHash";
    private static final String EMAIL_FIELD = "wufoo.feedback.field.email";
    private static final String PAGE_FIELD = "wufoo.feedback.field.page";
    private static final String EPERSON_FIELD = "wufoo.feedback.field.eperson";
    private static final String AGENT_FIELD = "wufoo.feedback.field.agent";
    private static final String DATE_FIELD = "wufoo.feedback.field.date";
    private static final String SESSION_FIELD = "wufoo.feedback.field.session";
    private static final String HOST_FIELD = "wufoo.feedback.field.host";

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private DiscoverableEndpointsService discoverableEndpointsService;

    /**
     * Constructs and returns a WufooFeedbackDTO object, containing the
     * information needed to redirect to the Wufoo feedback form, using
     * "URL modifications" to pre-populate form fields
     * (see https://help.wufoo.com/articles/en_US/kb/URL-Modifications).
     *
     * @param request the HttpServletRequest. Should include an (optional)
     * "page" query parameter, which indicates the referring page for the
     * feedback.
     */
    @PreAuthorize("permitAll()")
    @RequestMapping(WUFOO_ENDPOINT)
    public WufooFeedbackDTO wufooFeedback(HttpServletRequest request) {
        Context context = obtainContext(request);

        Map<String, String> defaultFormValues = getDefaultFormValues(request, context);
        String wufooFeedbackUrl = constructWufooFeedbackFormUrl(defaultFormValues);

        WufooFeedbackDTO dto = new WufooFeedbackDTO();
        dto.setWufooFormUrl(wufooFeedbackUrl);

        return dto;
    }

    /**
     * Adds WUFOO_ENDPOINT to the list of discoverable endpoints, so that it is
     * available to REST.
     */
    @Override
    public void afterPropertiesSet() {
        List<Link> links = List.of(Link.of(WUFOO_ENDPOINT, "wufoo-feedback"));
        discoverableEndpointsService.register(this, links);
    }

    /**
     * Constructs a String representing the Wufoo feedback form URL, with
     * query parameters representing the default form values.
     *
     * @param defaultValues a Map of strings, indexed by Wufoo form fields
     * identifiers.
     */
    protected String constructWufooFeedbackFormUrl(Map<String, String> defaultValues) {
        List<String> defaultValuesParams = new ArrayList<>();

        // URL encode all default values (required by Wufoo), and add to
        // defaultValuesParams list
        for (Entry<String, String> entry: defaultValues.entrySet()) {
            if (entry.getValue() != null) {
                String urlEncodedValue = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
                defaultValuesParams.add(entry.getKey() + "=" + urlEncodedValue);
            } else {
                defaultValuesParams.add(entry.getKey() + "=null");
            }
        }

        // Construct the Wufoo feedback form URL
        String baseFormUrl = configurationService.getProperty(FORM_URL);
        if (!baseFormUrl.endsWith("/")) {
            baseFormUrl = baseFormUrl + "/";
        }

        String formHash = configurationService.getProperty(FORM_HASH);

        String params = String.join("&", defaultValuesParams);

        String wufooFeedbackFormUrl = baseFormUrl + formHash + "/?" + params;

        return wufooFeedbackFormUrl;
    }

    /**
     * Returns a Map for form default values, indexed by the Wufoo form API IDs.
     *
     * @param the HttpServletRequest to use in constructing the default values
     * @param context the DSpace context
     * @return a Map for form default values, indexed by the Wufoo form API IDs.
     */
    protected Map<String, String> getDefaultFormValues(HttpServletRequest request, Context context) {
        Map<String, String> defaultValues = new HashMap<>();

        String hostname = configurationService.getProperty("dspace.ui.url");

        if (StringUtils.isNotEmpty(configurationService.getProperty(PAGE_FIELD))) {
            String page = hostname;
            String pageParam = request.getParameter("page");
            if (pageParam != null) {
                page = pageParam;
            }
            defaultValues.put(configurationService.getProperty(PAGE_FIELD), page);
        }

        EPerson loggedin = context.getCurrentUser();
        String eperson = null;
        if (loggedin != null) {
            eperson = loggedin.getEmail();
        }

        if (StringUtils.isNotEmpty(configurationService.getProperty(AGENT_FIELD))) {
            defaultValues.put(configurationService.getProperty(AGENT_FIELD), request.getHeader("User-Agent"));
        }
        if (StringUtils.isNotEmpty(configurationService.getProperty(EMAIL_FIELD)) && eperson != null) {
            defaultValues.put(configurationService.getProperty(EMAIL_FIELD), eperson);
        }
        if (StringUtils.isNotEmpty(configurationService.getProperty(EPERSON_FIELD))) {
            defaultValues.put(configurationService.getProperty(EPERSON_FIELD), eperson);
        }
        if (StringUtils.isNotEmpty(configurationService.getProperty(SESSION_FIELD))) {
            defaultValues.put(configurationService.getProperty(SESSION_FIELD), request.getSession().getId());
        }
        if (StringUtils.isNotEmpty(configurationService.getProperty(DATE_FIELD))) {
            defaultValues.put(configurationService.getProperty(DATE_FIELD), new Date().toString());
        }
        if (StringUtils.isNotEmpty(configurationService.getProperty(HOST_FIELD))) {
            defaultValues.put(configurationService.getProperty(HOST_FIELD), hostname);
        }

        return defaultValues;
    }
}
