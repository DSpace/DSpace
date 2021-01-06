/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.util.UUID;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

/**
 * This is a Spring Security CookieCsrfTokenRepository which supports cross-site cookies (i.e. SameSite=None).
 * PLEASE NOTE: It will NOT support cross-domain CSRF, as Cookies cannot be sent across domains. Therefore, this
 * CsrfTokenRepository is similar to Spring Security's in that it requires the REST API and UI to be on the same domain.
 * <P>
 * This code was mostly borrowed from Spring Security's CookieCsrfTokenRepository
 * https://github.com/spring-projects/spring-security/blob/5.2.x/web/src/main/java/org/springframework/security/web/csrf/CookieCsrfTokenRepository.java
 * <P>
 * Corresponding tests were also copied to CrossSiteCookieCsrfTokenRepositoryTest.
 * <P>
 * The only modification were to the saveToken() method below. See that method's JavaDocs.
 * <P>
 * NOTE: This class is TEMPORARY and should be REMOVED as soon as the "SameSite" attribute is supported by
 * Spring Security's CookieCsrfTokenRepository. As soon as the below ticket is resolved & we upgrade Spring Security,
 * then this custom class can be removed:
 * https://github.com/spring-projects/spring-security/issues/7537
 */
public class CrossSiteCookieCsrfTokenRepository implements CsrfTokenRepository {
    static final String DEFAULT_CSRF_COOKIE_NAME = "XSRF-TOKEN";

    static final String DEFAULT_CSRF_PARAMETER_NAME = "_csrf";

    static final String DEFAULT_CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    private String parameterName = DEFAULT_CSRF_PARAMETER_NAME;

    private String headerName = DEFAULT_CSRF_HEADER_NAME;

    private String cookieName = DEFAULT_CSRF_COOKIE_NAME;

    private boolean cookieHttpOnly = true;

    private String cookiePath;

    private String cookieDomain;

    public CrossSiteCookieCsrfTokenRepository() {
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return new DefaultCsrfToken(this.headerName, this.parameterName,
                                    createNewToken());
    }

    /**
     * This is the only method modified for DSpace.  We changed this method to use ResponseCookie to build the
     * cookie, so that we could hardcode the "SameSite" attribute to a value of "None". This allows for cross site
     * XSRF-TOKEN cookies.
     * @param token
     * @param request
     * @param response
     */
    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request,
                          HttpServletResponse response) {
        String tokenValue = token == null ? "" : token.getToken();
        Cookie cookie = new Cookie(this.cookieName, tokenValue);
        cookie.setSecure(request.isSecure());
        if (this.cookiePath != null && !this.cookiePath.isEmpty()) {
            cookie.setPath(this.cookiePath);
        } else {
            cookie.setPath(this.getRequestContext(request));
        }
        if (token == null) {
            cookie.setMaxAge(0);
        } else {
            cookie.setMaxAge(-1);
        }
        cookie.setHttpOnly(cookieHttpOnly);
        if (this.cookieDomain != null && !this.cookieDomain.isEmpty()) {
            cookie.setDomain(this.cookieDomain);
        }

        // Custom: Turn the above Cookie into a ResponseCookie so that we can set "SameSite" attribute
        // NOTE: ONLY set "SameSite=None" if cookie is also secure. Most modern browsers will block it otherwise.
        // This means that DSpace MUST USE HTTPS if the UI is on a different domain then backend.
        String sameSite = "";
        if (cookie.getSecure()) {
            sameSite = "None";
        }
        ResponseCookie responseCookie = ResponseCookie.from(cookie.getName(), cookie.getValue())
                                              .path(cookie.getPath()).maxAge(cookie.getMaxAge())
                                              .domain(cookie.getDomain()).httpOnly(cookie.isHttpOnly())
                                              .secure(cookie.getSecure()).sameSite(sameSite).build();
        // Write the ResponseCookie to the Set-Cookie header
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());
    }

    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, this.cookieName);
        if (cookie == null) {
            return null;
        }
        String token = cookie.getValue();
        if (!StringUtils.hasLength(token)) {
            return null;
        }
        return new DefaultCsrfToken(this.headerName, this.parameterName, token);
    }


    /**
     * Sets the name of the HTTP request parameter that should be used to provide a token.
     *
     * @param parameterName the name of the HTTP request parameter that should be used to
     * provide a token
     */
    public void setParameterName(String parameterName) {
        Assert.notNull(parameterName, "parameterName is not null");
        this.parameterName = parameterName;
    }

    /**
     * Sets the name of the HTTP header that should be used to provide the token.
     *
     * @param headerName the name of the HTTP header that should be used to provide the
     * token
     */
    public void setHeaderName(String headerName) {
        Assert.notNull(headerName, "headerName is not null");
        this.headerName = headerName;
    }

    /**
     * Sets the name of the cookie that the expected CSRF token is saved to and read from.
     *
     * @param cookieName the name of the cookie that the expected CSRF token is saved to
     * and read from
     */
    public void setCookieName(String cookieName) {
        Assert.notNull(cookieName, "cookieName is not null");
        this.cookieName = cookieName;
    }

    /**
     * Sets the HttpOnly attribute on the cookie containing the CSRF token.
     * Defaults to <code>true</code>.
     *
     * @param cookieHttpOnly <code>true</code> sets the HttpOnly attribute, <code>false</code> does not set it
     */
    public void setCookieHttpOnly(boolean cookieHttpOnly) {
        this.cookieHttpOnly = cookieHttpOnly;
    }

    private String getRequestContext(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        return contextPath.length() > 0 ? contextPath : "/";
    }

    /**
     * Factory method to conveniently create an instance that has
     * {@link #setCookieHttpOnly(boolean)} set to false.
     *
     * @return an instance of CookieCsrfTokenRepository with
     * {@link #setCookieHttpOnly(boolean)} set to false
     */
    public static CrossSiteCookieCsrfTokenRepository withHttpOnlyFalse() {
        CrossSiteCookieCsrfTokenRepository result = new CrossSiteCookieCsrfTokenRepository();
        result.setCookieHttpOnly(false);
        return result;
    }

    private String createNewToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Set the path that the Cookie will be created with. This will override the default functionality which uses the
     * request context as the path.
     *
     * @param path the path to use
     */
    public void setCookiePath(String path) {
        this.cookiePath = path;
    }

    /**
     * Get the path that the CSRF cookie will be set to.
     *
     * @return the path to be used.
     */
    public String getCookiePath() {
        return this.cookiePath;
    }

    /**
     * Sets the domain of the cookie that the expected CSRF token is saved to and read from.
     *
     * @since 5.2
     * @param cookieDomain the domain of the cookie that the expected CSRF token is saved to
     * and read from
     */
    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }

}
