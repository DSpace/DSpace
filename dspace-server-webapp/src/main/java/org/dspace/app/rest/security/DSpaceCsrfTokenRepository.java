/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import java.util.UUID;
import java.util.function.Consumer;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
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
 * This is essentially a customization of Spring Security's CookieCsrfTokenRepository:
 * https://github.com/spring-projects/spring-security/blob/6.2.x/web/src/main/java/org/springframework/security/web/csrf/CookieCsrfTokenRepository.java
 * However, as that class is "final" we aannot override it directly.
 * <P>
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
    public static final String DEFAULT_CSRF_COOKIE_NAME = "DSPACE-XSRF-COOKIE";

    // The HTTP header that is sent back to the client whenever a new CSRF token is created
    // (NOTE: This is purposefully different from DEFAULT_CSRF_HEADER_NAME below!)
    public static final String DSPACE_CSRF_HEADER_NAME = "DSPACE-XSRF-TOKEN";

    public static final String DEFAULT_CSRF_PARAMETER_NAME = "_csrf";

    // The HTTP header that Spring Security expects to receive from the client in order to validate a CSRF token
    public static final String DEFAULT_CSRF_HEADER_NAME = "X-XSRF-TOKEN";

    private static final String CSRF_TOKEN_REMOVED_ATTRIBUTE_NAME = CookieCsrfTokenRepository.class.getName()
                                                                                                   .concat(".REMOVED");

    private String parameterName = DEFAULT_CSRF_PARAMETER_NAME;

    private String headerName = DEFAULT_CSRF_HEADER_NAME;

    private String cookieName = DEFAULT_CSRF_COOKIE_NAME;

    private boolean cookieHttpOnly = true;

    private String cookiePath;

    private String cookieDomain;

    private Boolean secure;

    private int cookieMaxAge = -1;

    private Consumer<ResponseCookie.ResponseCookieBuilder> cookieCustomizer = (builder) -> {
    };

    public DSpaceCsrfTokenRepository() {
    }

    /**
     * Method is copied from {@link CookieCsrfTokenRepository#setCookieCustomizer(Consumer)}
     */
    public void setCookieCustomizer(Consumer<ResponseCookie.ResponseCookieBuilder> cookieCustomizer) {
        Assert.notNull(cookieCustomizer, "cookieCustomizer must not be null");
        this.cookieCustomizer = cookieCustomizer;
    }

    /**
     * Method is copied from {@link CookieCsrfTokenRepository#generateToken(HttpServletRequest)}
     */
    @Override
    public CsrfToken generateToken(HttpServletRequest request) {
        return new DefaultCsrfToken(this.headerName, this.parameterName, createNewToken());
    }

    /**
     * This method has been modified for DSpace. It borrows MOST of the logic from
     * {@link CookieCsrfTokenRepository#saveToken(CsrfToken, HttpServletRequest, HttpServletResponse)}
     * <P>
     * It applies a "SameSite" attribute to every cookie by default.
     * <P>
     * It also sends the token (if not empty) back in BOTH the cookie and the custom "DSPACE-XSRF-TOKEN" header.
     * By default, Spring Security will only send the token back in the cookie.
     * @param token current token
     * @param request current request
     * @param response current response
     */
    @Override
    public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
        String tokenValue = (token != null) ? token.getToken() : "";

        ResponseCookie.ResponseCookieBuilder cookieBuilder =
            ResponseCookie.from(this.cookieName, tokenValue)
                          .secure((this.secure != null) ? this.secure : request.isSecure())
                          .path(StringUtils.hasLength(this.cookiePath) ?
                                    this.cookiePath : this.getRequestContext(request))
                          .maxAge((token != null) ? this.cookieMaxAge : 0)
                          .httpOnly(this.cookieHttpOnly)
                          .domain(this.cookieDomain)
                          // Custom for DSpace: If client is on a different domain than the backend, then Cookie MUST
                          // use "SameSite=None" and "Secure". Most modern browsers will block it otherwise.
                          // TODO: Make SameSite configurable? "Lax" cookies are more secure, but require client &
                          // backend on same domain.
                          .sameSite(request.isSecure() ? "None" : "Lax");;

        this.cookieCustomizer.accept(cookieBuilder);

        // Custom for DSpace: also send custom header to client with token.
        // We send our token via a custom header because client may be on a different domain.
        // Cookies cannot be reliably sent cross-domain.
        if (StringUtils.hasLength(tokenValue)) {
            response.setHeader(DSPACE_CSRF_HEADER_NAME, tokenValue);
        }

        Cookie cookie = mapToCookie(cookieBuilder.build());
        response.addCookie(cookie);

        // Set request attribute to signal that response has blank cookie value,
        // which allows loadToken to return null when token has been removed
        if (!StringUtils.hasLength(tokenValue)) {
            request.setAttribute(CSRF_TOKEN_REMOVED_ATTRIBUTE_NAME, Boolean.TRUE);
        } else {
            request.removeAttribute(CSRF_TOKEN_REMOVED_ATTRIBUTE_NAME);
        }
    }

    /**
     * Method is copied from {@link CookieCsrfTokenRepository#loadToken(HttpServletRequest)}
     */
    @Override
    public CsrfToken loadToken(HttpServletRequest request) {
        // Return null when token has been removed during the current request
        // which allows loadDeferredToken to re-generate the token
        if (Boolean.TRUE.equals(request.getAttribute(CSRF_TOKEN_REMOVED_ATTRIBUTE_NAME))) {
            return null;
        }
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
     * Method is copied from {@link CookieCsrfTokenRepository#setParameterName(String)}
     */
    public void setParameterName(String parameterName) {
        Assert.notNull(parameterName, "parameterName cannot be null");
        this.parameterName = parameterName;
    }

    /**
     * Method is copied from {@link CookieCsrfTokenRepository#setHeaderName(String)}
     */
    public void setHeaderName(String headerName) {
        Assert.notNull(headerName, "headerName cannot be null");
        this.headerName = headerName;
    }

    /**
     * Method is copied from {@link CookieCsrfTokenRepository#setCookieName(String)}
     */
    public void setCookieName(String cookieName) {
        Assert.notNull(cookieName, "cookieName cannot be null");
        this.cookieName = cookieName;
    }

    /**
     * Method is copied from {@link CookieCsrfTokenRepository#setCookieHttpOnly(boolean)}
     * @deprecated Use {@link #setCookieCustomizer(Consumer)} instead.
     */
    @Deprecated
    public void setCookieHttpOnly(boolean cookieHttpOnly) {
        this.cookieHttpOnly = cookieHttpOnly;
    }

    /**
     * Method is copied from {@link CookieCsrfTokenRepository}
     */
    private String getRequestContext(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        return (contextPath.length() > 0) ? contextPath : "/";
    }

    /**
     * Method is copied from {@link CookieCsrfTokenRepository}
     * (and only modified to return the DSpaceCsrfTokenRepository instead)
     */
    public static DSpaceCsrfTokenRepository withHttpOnlyFalse() {
        DSpaceCsrfTokenRepository result = new DSpaceCsrfTokenRepository();
        result.cookieHttpOnly = false;
        return result;
    }

    /**
     * Method is copied from {@link CookieCsrfTokenRepository}
     */
    private String createNewToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Method is copied from {@link CookieCsrfTokenRepository}
     */
    private Cookie mapToCookie(ResponseCookie responseCookie) {
        Cookie cookie = new Cookie(responseCookie.getName(), responseCookie.getValue());
        cookie.setSecure(responseCookie.isSecure());
        cookie.setPath(responseCookie.getPath());
        cookie.setMaxAge((int) responseCookie.getMaxAge().getSeconds());
        cookie.setHttpOnly(responseCookie.isHttpOnly());
        if (StringUtils.hasLength(responseCookie.getDomain())) {
            cookie.setDomain(responseCookie.getDomain());
        }
        if (StringUtils.hasText(responseCookie.getSameSite())) {
            cookie.setAttribute("SameSite", responseCookie.getSameSite());
        }
        return cookie;
    }

    /**
     * Method is copied from {@link CookieCsrfTokenRepository#setCookiePath(String)}
     */
    public void setCookiePath(String path) {
        this.cookiePath = path;
    }

    /**
     * Method is copied from {@link CookieCsrfTokenRepository#getCookiePath()}
     */
    public String getCookiePath() {
        return this.cookiePath;
    }

    /**
     * Method is copied from {@link CookieCsrfTokenRepository#setCookieDomain(String)}
     * @deprecated Use {@link #setCookieCustomizer(Consumer)} instead.
     */
    @Deprecated
    public void setCookieDomain(String cookieDomain) {
        this.cookieDomain = cookieDomain;
    }

    /**
     * Method is copied from {@link CookieCsrfTokenRepository#setSecure(Boolean)}
     * @deprecated Use {@link #setCookieCustomizer(Consumer)} instead.
     */
    @Deprecated
    public void setSecure(Boolean secure) {
        this.secure = secure;
    }

    /**
     * Method is copied from {@link CookieCsrfTokenRepository#setCookieMaxAge(int)}
     * @deprecated Use {@link #setCookieCustomizer(Consumer)} instead.
     */
    @Deprecated
    public void setCookieMaxAge(int cookieMaxAge) {
        Assert.isTrue(cookieMaxAge != 0, "cookieMaxAge cannot be zero");
        this.cookieMaxAge = cookieMaxAge;
    }
}
