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
 * This is a custom Spring Security CsrfTokenRepository which supports *cross-domain* CSRF protection (allowing the
 * client and backend to be on different domains). It's inspired by https://stackoverflow.com/a/33175322
 * <P>
 * This also borrows heavily from Spring Security's CookieCsrfTokenRepository:
 *  https://github.com/spring-projects/spring-security/blob/5.2.x/web/src/main/java/org/springframework/security/web/csrf/CookieCsrfTokenRepository.java
 *
 * How it works:
 *
 *  1. Backend generates XSRF token & stores in a *server-side* cookie named DSPACE-XSRF-COOKIE. By default, this cookie
 *     is not readable to JS clients (HttpOnly=true). But, it is returned (by user's browser) on every subsequent
 *     request to backend. See "saveToken()" method below.
 *  2. At the same time, backend also sends the generated XSRF token in a header named DSPACE-XSRF-TOKEN to client.
 *     See "saveToken()" method below.
 *  3. Client MUST look for DSPACE-XSRF-TOKEN header in a response from backend. If found, the client MUST store/save
 *     this token for later request(s).  For Angular UI, this task is performed by the XsrfInterceptor.
 *  4. Whenever the client is making a mutating request (e.g. POST, PUT, DELETE, etc), the XSRF token is REQUIRED to be
 *     sent back in the X-XSRF-TOKEN header.
 *        * NOTE: non-mutating requests (e.g. GET, HEAD) do not check for an XSRF token. This is default behavior in
 *          Spring Security
 *  5. On backend, the X-XSRF-TOKEN header is received & compared to the current value of the *server-side* cookie
 *     named DSPACE-XSRF-COOKIE. If tokens match, the request is accepted. If tokens don't match a 403 is returned.
 *     This is done automatically by Spring Security.
 *
 *  In summary, the XSRF token is ALWAYS sent to/from the client & backend via *headers*. This is what allows the client
 *  and backend to be on different domains. The server-side cookie named DSPACE-XSRF-COOKIE is (usually) not accessible
 *  to the client. It only exists to allow the server-side to remember the currently active XSRF token, so that it can
 *  validate the token sent (by the client) in the X-XSRF-TOKEN header.
 */
public class DSpaceCsrfTokenRepository implements CsrfTokenRepository {
    // This cookie name is changed from the default "XSRF-TOKEN" to ensure it is uniquely named and doesn't conflict
    // with any other XSRF-TOKEN cookies (e.g. in Angular UI, the XSRF-TOKEN cookie is a *client-side* only cookie)
    static final String DEFAULT_CSRF_COOKIE_NAME = "DSPACE-XSRF-COOKIE";

    static final String DEFAULT_CSRF_PARAMETER_NAME = "_csrf";

    static final String DEFAULT_CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    private String parameterName = DEFAULT_CSRF_PARAMETER_NAME;

    private String headerName = DEFAULT_CSRF_HEADER_NAME;

    private String cookieName = DEFAULT_CSRF_COOKIE_NAME;

    private boolean cookieHttpOnly = true;

    private String cookiePath;

    private String cookieDomain;

    public DSpaceCsrfTokenRepository() {
    }

    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return new DefaultCsrfToken(this.headerName, this.parameterName,
                                    createNewToken());
    }

    /**
     * This method has been modified for DSpace.
     * <P>
     * It now uses ResponseCookie to build the cookie, so that the "SameSite" attribute can be applied.
     * <P>
     * It also sends the token (if not empty) in both the cookie and the custom "DSPACE-XSRF-TOKEN" header
     * @param token current token
     * @param request current request
     * @param response current response
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
        // If client is on a different domain than the backend, then Cookie MUST use "SameSite=None" and "Secure".
        // Most modern browsers will block it otherwise.
        // TODO: Make SameSite configurable? "Lax" cookies are more secure, but require client & backend on same domain.
        String sameSite = "None";
        if (!cookie.getSecure()) {
            sameSite = "Lax";
        }
        ResponseCookie responseCookie = ResponseCookie.from(cookie.getName(), cookie.getValue())
                                              .path(cookie.getPath()).maxAge(cookie.getMaxAge())
                                              .domain(cookie.getDomain()).httpOnly(cookie.isHttpOnly())
                                              .secure(cookie.getSecure()).sameSite(sameSite).build();

        // Write the ResponseCookie to the Set-Cookie header
        // This cookie is only used by the backend & not needed by client
        response.addHeader(HttpHeaders.SET_COOKIE, responseCookie.toString());

        // Send custom header to client with token (only if token not empty)
        // We send our token via a custom header because client can be on a different domain.
        // Cookies cannot be reliably sent cross-domain.
        if (StringUtils.hasLength(tokenValue)) {
            response.setHeader("DSPACE-XSRF-TOKEN", tokenValue);
        }
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
    public static DSpaceCsrfTokenRepository withHttpOnlyFalse() {
        DSpaceCsrfTokenRepository result = new DSpaceCsrfTokenRepository();
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
