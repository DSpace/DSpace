package org.dspace.authenticate;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Context;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility for authentication.
 */
public class AuthenticationUtility {

    public static final String AUTHENTICATION_METHOD = "authenticationMethod";

    private AuthenticationUtility() {
        // private empty constructor
    }

    public enum Mapping {
        PASSWORD(new PasswordAuthentication().getName(), "/api/authn/login"),
        SHIBBOLETH(new ShibAuthentication().getName(), "/api/authn/shibboleth"),
        ORCID(new OrcidAuthentication().getName(), "/api/authn/orcid"),
        OIDC(new OidcAuthentication().getName(), "/api/authn/oidc"),
        SAML(new SamlAuthentication().getName(), "/api/authn/saml");

        private static final Map<String, String> urlToName = new HashMap<>();

        static {
            for (Mapping mapping : Mapping.values()) {
                urlToName.put(mapping.getMethodUrl(), mapping.getMethodName());
            }
        }

        private final String methodName;

        private final String methodUrl;

        Mapping(String methodName, String methodUrl) {
            this.methodName = methodName;
            this.methodUrl = methodUrl;
        }

        public String getMethodName() {
            return methodName;
        }

        public String getMethodUrl() {
            return methodUrl;
        }

        public static String getMethodName(String url) {
            return urlToName.get(url);
        }
    }

    /**
     * Update context authentication method from request servlet path.
     *
     * Sets context authentication method if blank and is a login request.
     *
     * @param context Context current DSpace context
     * @param request HttpServletRequest current HTTP request
     */
    public static void updateAuthenticationMethod(Context context, HttpServletRequest request) {
        String authMethod = context.getAuthenticationMethod();

        if (request != null && StringUtils.isBlank(authMethod)) {
            authMethod = Mapping.getMethodName(request.getServletPath());
            context.setAuthenticationMethod(authMethod);
        }
    }
}
