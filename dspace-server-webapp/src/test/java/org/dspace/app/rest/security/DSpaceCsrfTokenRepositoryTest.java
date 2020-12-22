/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import javax.servlet.http.Cookie;
import javax.ws.rs.core.HttpHeaders;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;

/**
 * This is almost an exact copy of Spring Security's CookieCsrfTokenRepositoryTests
 * https://github.com/spring-projects/spring-security/blob/5.2.x/web/src/test/java/org/springframework/security/web/csrf/CookieCsrfTokenRepositoryTests.java
 *
 * The only modifications are:
 *   - Updating these tests to use our custom DSpaceCsrfTokenRepository
 *   - Updating the saveTokenSecure() test, where we check for our custom SameSite attribute.
 */
@RunWith(MockitoJUnitRunner.class)
public class DSpaceCsrfTokenRepositoryTest {
    DSpaceCsrfTokenRepository repository;
    MockHttpServletResponse response;
    MockHttpServletRequest request;

    @Before
    public void setup() {
        this.repository = new DSpaceCsrfTokenRepository();
        this.request = new MockHttpServletRequest();
        this.response = new MockHttpServletResponse();
        this.request.setContextPath("/context");
    }

    @Test
    public void generateToken() {
        CsrfToken generateToken = this.repository.generateToken(this.request);

        assertThat(generateToken).isNotNull();
        assertThat(generateToken.getHeaderName())
            .isEqualTo(DSpaceCsrfTokenRepository.DEFAULT_CSRF_HEADER_NAME);
        assertThat(generateToken.getParameterName())
            .isEqualTo(DSpaceCsrfTokenRepository.DEFAULT_CSRF_PARAMETER_NAME);
        assertThat(generateToken.getToken()).isNotEmpty();
    }

    @Test
    public void generateTokenCustom() {
        String headerName = "headerName";
        String parameterName = "paramName";
        this.repository.setHeaderName(headerName);
        this.repository.setParameterName(parameterName);

        CsrfToken generateToken = this.repository.generateToken(this.request);

        assertThat(generateToken).isNotNull();
        assertThat(generateToken.getHeaderName()).isEqualTo(headerName);
        assertThat(generateToken.getParameterName()).isEqualTo(parameterName);
        assertThat(generateToken.getToken()).isNotEmpty();
    }

    @Test
    public void saveToken() {
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);

        Cookie tokenCookie = this.response
            .getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);

        assertThat(tokenCookie.getMaxAge()).isEqualTo(-1);
        assertThat(tokenCookie.getName())
            .isEqualTo(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getPath()).isEqualTo(this.request.getContextPath());
        assertThat(tokenCookie.getSecure()).isEqualTo(this.request.isSecure());
        assertThat(tokenCookie.getValue()).isEqualTo(token.getToken());
        assertThat(tokenCookie.isHttpOnly()).isEqualTo(true);
    }

    @Test
    public void saveTokenSecure() {
        this.request.setSecure(true);
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);

        Cookie tokenCookie = this.response
            .getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);

        assertThat(tokenCookie.getSecure()).isTrue();
        // DSpace Custom assert to verify SameSite attribute is set
        // The Cookie class doesn't yet support SameSite, so we have to re-read
        // the cookie from our headers, and check it.
        List<String> headers = this.response.getHeaders(HttpHeaders.SET_COOKIE);
        assertThat(headers.size()).isEqualTo(1);
        assertThat(headers.get(0)).containsIgnoringCase("SameSite=None");
    }

    @Test
    public void saveTokenNull() {
        this.request.setSecure(true);
        this.repository.saveToken(null, this.request, this.response);

        Cookie tokenCookie = this.response
            .getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);

        assertThat(tokenCookie.getMaxAge()).isZero();
        assertThat(tokenCookie.getName())
            .isEqualTo(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getPath()).isEqualTo(this.request.getContextPath());
        assertThat(tokenCookie.getSecure()).isEqualTo(this.request.isSecure());
        assertThat(tokenCookie.getValue()).isEmpty();
    }

    @Test
    public void saveTokenHttpOnlyTrue() {
        this.repository.setCookieHttpOnly(true);
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);

        Cookie tokenCookie = this.response
            .getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);

        assertThat(tokenCookie.isHttpOnly()).isTrue();
    }

    @Test
    public void saveTokenHttpOnlyFalse() {
        this.repository.setCookieHttpOnly(false);
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);

        Cookie tokenCookie = this.response
            .getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);

        assertThat(tokenCookie.isHttpOnly()).isFalse();
    }

    @Test
    public void saveTokenWithHttpOnlyFalse() {
        this.repository = DSpaceCsrfTokenRepository.withHttpOnlyFalse();
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);

        Cookie tokenCookie = this.response
            .getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);

        assertThat(tokenCookie.isHttpOnly()).isFalse();
    }

    @Test
    public void saveTokenCustomPath() {
        String customPath = "/custompath";
        this.repository.setCookiePath(customPath);
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);

        Cookie tokenCookie = this.response
            .getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);

        assertThat(tokenCookie.getPath()).isEqualTo(this.repository.getCookiePath());
    }

    @Test
    public void saveTokenEmptyCustomPath() {
        String customPath = "";
        this.repository.setCookiePath(customPath);
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);

        Cookie tokenCookie = this.response
            .getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);

        assertThat(tokenCookie.getPath()).isEqualTo(this.request.getContextPath());
    }

    @Test
    public void saveTokenNullCustomPath() {
        String customPath = null;
        this.repository.setCookiePath(customPath);
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);

        Cookie tokenCookie = this.response
            .getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);

        assertThat(tokenCookie.getPath()).isEqualTo(this.request.getContextPath());
    }

    @Test
    public void saveTokenWithCookieDomain() {
        String domainName = "example.com";
        this.repository.setCookieDomain(domainName);

        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);

        Cookie tokenCookie = this.response
            .getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);

        assertThat(tokenCookie.getDomain()).isEqualTo(domainName);
    }

    @Test
    public void loadTokenNoCookiesNull() {
        assertThat(this.repository.loadToken(this.request)).isNull();
    }

    @Test
    public void loadTokenCookieIncorrectNameNull() {
        this.request.setCookies(new Cookie("other", "name"));

        assertThat(this.repository.loadToken(this.request)).isNull();
    }

    @Test
    public void loadTokenCookieValueEmptyString() {
        this.request.setCookies(
            new Cookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME, ""));

        assertThat(this.repository.loadToken(this.request)).isNull();
    }

    @Test
    public void loadToken() {
        CsrfToken generateToken = this.repository.generateToken(this.request);

        this.request
            .setCookies(new Cookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME,
                                   generateToken.getToken()));

        CsrfToken loadToken = this.repository.loadToken(this.request);

        assertThat(loadToken).isNotNull();
        assertThat(loadToken.getHeaderName()).isEqualTo(generateToken.getHeaderName());
        assertThat(loadToken.getParameterName())
            .isEqualTo(generateToken.getParameterName());
        assertThat(loadToken.getToken()).isNotEmpty();
    }

    @Test
    public void loadTokenCustom() {
        String cookieName = "cookieName";
        String value = "value";
        String headerName = "headerName";
        String parameterName = "paramName";
        this.repository.setHeaderName(headerName);
        this.repository.setParameterName(parameterName);
        this.repository.setCookieName(cookieName);

        this.request.setCookies(new Cookie(cookieName, value));

        CsrfToken loadToken = this.repository.loadToken(this.request);

        assertThat(loadToken).isNotNull();
        assertThat(loadToken.getHeaderName()).isEqualTo(headerName);
        assertThat(loadToken.getParameterName()).isEqualTo(parameterName);
        assertThat(loadToken.getToken()).isEqualTo(value);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setCookieNameNullIllegalArgumentException() {
        this.repository.setCookieName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setParameterNameNullIllegalArgumentException() {
        this.repository.setParameterName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setHeaderNameNullIllegalArgumentException() {
        this.repository.setHeaderName(null);
    }



}
