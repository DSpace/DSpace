/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.saml2;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Handler for a successful SAML authentication. Note that this is not a successful DSpace login,
 * only a successful login with a SAML identity provider. This handler initiates a DSpace login
 * attempt, using the identity information received from the SAML IdP.
 *
 * @author Ray Lee
 */
public class DSpaceSamlAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private static final Logger logger = LoggerFactory.getLogger(DSpaceSamlAuthenticationSuccessHandler.class);

    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * When a SAML authentication succeeds:
     * <ul>
     *   <li>Extract attributes from the assertion, and map them into request attributes using the mapping
     *       configured for the relying party that initiated the login.</li>
     *   <li>Forward the request to the DSpace SAML authentication endpoint.</li>
     * </ul>
     * @see
     * AuthenticationSuccessHandler#onAuthenticationSuccess(HttpServletRequest, HttpServletResponse, Authentication)
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();
        String relyingPartyId = principal.getRelyingPartyRegistrationId();
        Map<String, List<Object>> samlAttributes = principal.getAttributes();

        samlAttributes.forEach((attributeName, values) -> {
            values.forEach(value -> {
                logger.info("Incoming SAML attribute: {} = {}", attributeName, value);
            });
        });

        setRequestAttributesFromSamlAttributes(request, relyingPartyId, samlAttributes);

        request.setAttribute(getRelyingPartyIdAttributeName(), relyingPartyId);
        request.setAttribute(getNameIdAttributeName(), principal.getName());

        // Store all the attributes from the SAML assertion for debugging.
        request.setAttribute("org.dspace.saml.ATTRIBUTES", samlAttributes);

        request.getRequestDispatcher("/api/authn/saml")
            .forward(new DSpaceSamlAuthRequest(request), response);
    }

    /**
     * Extract attributes from a SAML identity assertion, and place the values into request
     * attributes. The mapping of SAML attribute names to request attribute names is read from
     * DSpace configuration. The mapping may be different for each SAML relying party.
     *
     * @param request The request in which the SAML assertion was received. Attributes from the
     *                assertion will be placed into attributes in this request.
     * @param relyingPartyId The ID of the relying party that initiated the SAML login.
     * @param samlAttributes The attributes from the SAML assertion.
     */
    private void setRequestAttributesFromSamlAttributes(
            HttpServletRequest request, String relyingPartyId, Map<String, List<Object>> samlAttributes) {

        String[] attributeMappings = configurationService.getArrayProperty(
            "saml-relying-party." + relyingPartyId + ".attributes");

        if (attributeMappings == null || attributeMappings.length == 0) {
            logger.warn("No SAML attribute mappings found for relying party {}", relyingPartyId);

            return;
        }

        Arrays.stream(attributeMappings)
            .forEach(attributeMapping -> {
                String[] parts = attributeMapping.split("=>");

                if (parts.length != 2) {
                    logger.error("Unable to parse SAML attribute mapping for relying party {}: {}",
                        relyingPartyId, attributeMapping);

                    return;
                }

                String samlAttributeName = parts[0].trim();
                String requestAttributeName = parts[1].trim();

                List<Object> values = samlAttributes.get(samlAttributeName);

                if (values != null) {
                    request.setAttribute(requestAttributeName, values);
                } else {
                    logger.warn("No value found for SAML attribute {} in assertion", samlAttributeName);
                }
            });
    }

    private String getRelyingPartyIdAttributeName() {
        return configurationService.getProperty("authentication-saml.attribute.relying-party-id",
            "org.dspace.saml.RELYING_PARTY_ID");
    }

    private String getNameIdAttributeName() {
        return configurationService.getProperty("authentication-saml.attribute.name-id", "org.dspace.saml.NAME_ID");
    }
}
