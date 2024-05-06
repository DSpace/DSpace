/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.Cookie;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DeferredCsrfToken;

/**
 * This is almost an exact copy of Spring Security's DSpaceCsrfTokenRepositoryTests
 * https://github.com/spring-projects/spring-security/blob/6.2.x/web/src/test/java/org/springframework/security/web/csrf/CookieCsrfTokenRepositoryTests.java
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
        assertThat(generateToken.getHeaderName()).isEqualTo(DSpaceCsrfTokenRepository.DEFAULT_CSRF_HEADER_NAME);
        assertThat(generateToken.getParameterName()).isEqualTo(DSpaceCsrfTokenRepository.DEFAULT_CSRF_PARAMETER_NAME);
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
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getMaxAge()).isEqualTo(-1);
        assertThat(tokenCookie.getName()).isEqualTo(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getPath()).isEqualTo(this.request.getContextPath());
        assertThat(tokenCookie.getSecure()).isEqualTo(this.request.isSecure());
        assertThat(tokenCookie.getValue()).isEqualTo(token.getToken());
        assertThat(tokenCookie.isHttpOnly()).isTrue();
    }

    @Test
    public void saveTokenShouldUseResponseAddCookie() {
        CsrfToken token = this.repository.generateToken(this.request);
        MockHttpServletResponse spyResponse = spy(this.response);
        this.repository.saveToken(token, this.request, spyResponse);
        verify(spyResponse).addCookie(any(Cookie.class));
    }

    @Test
    public void saveTokenSecure() {
        this.request.setSecure(true);
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getSecure()).isTrue();

        // DSpace Custom assert to verify SameSite attribute is "None" when cookie is secure
        assertThat(tokenCookie.getAttribute("SameSite")).containsIgnoringCase("None");
    }

    // Custom test for DSpace to verify behavior for non-secure requests
    @Test
    public void saveTokenNotSecure() {
        this.request.setSecure(false);
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getSecure()).isFalse();

        // DSpace Custom assert to verify SameSite attribute is "Lax" when cookie is NOT secure
        assertThat(tokenCookie.getAttribute("SameSite")).containsIgnoringCase("Lax");
    }

    @Test
    public void saveTokenSecureFlagTrue() {
        this.request.setSecure(false);
        this.repository.setSecure(Boolean.TRUE);
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getSecure()).isTrue();
    }

    @Test
    public void saveTokenSecureFlagTrueUsingCustomizer() {
        this.request.setSecure(false);
        this.repository.setCookieCustomizer((customizer) -> customizer.secure(Boolean.TRUE));
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getSecure()).isTrue();
    }

    @Test
    public void saveTokenSecureFlagFalse() {
        this.request.setSecure(true);
        this.repository.setSecure(Boolean.FALSE);
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getSecure()).isFalse();
    }

    @Test
    public void saveTokenSecureFlagFalseUsingCustomizer() {
        this.request.setSecure(true);
        this.repository.setCookieCustomizer((customizer) -> customizer.secure(Boolean.FALSE));
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getSecure()).isFalse();
    }

    @Test
    public void saveTokenNull() {
        this.request.setSecure(true);
        this.repository.saveToken(null, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getMaxAge()).isZero();
        assertThat(tokenCookie.getName()).isEqualTo(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getPath()).isEqualTo(this.request.getContextPath());
        assertThat(tokenCookie.getSecure()).isEqualTo(this.request.isSecure());
        assertThat(tokenCookie.getValue()).isEmpty();
    }

    @Test
    public void saveTokenHttpOnlyTrue() {
        this.repository.setCookieHttpOnly(true);
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.isHttpOnly()).isTrue();
    }

    @Test
    public void saveTokenHttpOnlyTrueUsingCustomizer() {
        this.repository.setCookieCustomizer((customizer) -> customizer.httpOnly(true));
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.isHttpOnly()).isTrue();
    }

    @Test
    public void saveTokenHttpOnlyFalse() {
        this.repository.setCookieHttpOnly(false);
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.isHttpOnly()).isFalse();
    }

    @Test
    public void saveTokenHttpOnlyFalseUsingCustomizer() {
        this.repository.setCookieCustomizer((customizer) -> customizer.httpOnly(false));
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.isHttpOnly()).isFalse();
    }

    @Test
    public void saveTokenWithHttpOnlyFalse() {
        this.repository = DSpaceCsrfTokenRepository.withHttpOnlyFalse();
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.isHttpOnly()).isFalse();
    }

    @Test
    public void saveTokenCustomPath() {
        String customPath = "/custompath";
        this.repository.setCookiePath(customPath);
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getPath()).isEqualTo(this.repository.getCookiePath());
    }

    @Test
    public void saveTokenEmptyCustomPath() {
        String customPath = "";
        this.repository.setCookiePath(customPath);
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getPath()).isEqualTo(this.request.getContextPath());
    }

    @Test
    public void saveTokenNullCustomPath() {
        String customPath = null;
        this.repository.setCookiePath(customPath);
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getPath()).isEqualTo(this.request.getContextPath());
    }

    @Test
    public void saveTokenWithCookieDomain() {
        String domainName = "example.com";
        this.repository.setCookieDomain(domainName);
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getDomain()).isEqualTo(domainName);
    }

    @Test
    public void saveTokenWithCookieDomainUsingCustomizer() {
        String domainName = "example.com";
        this.repository.setCookieCustomizer((customizer) -> customizer.domain(domainName));
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getDomain()).isEqualTo(domainName);
    }

    @Test
    public void saveTokenWithCookieMaxAge() {
        int maxAge = 1200;
        this.repository.setCookieMaxAge(maxAge);
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getMaxAge()).isEqualTo(maxAge);
    }

    @Test
    public void saveTokenWithCookieMaxAgeUsingCustomizer() {
        int maxAge = 1200;
        this.repository.setCookieCustomizer((customizer) -> customizer.maxAge(maxAge));
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getMaxAge()).isEqualTo(maxAge);
    }

    @Test
    public void saveTokenWithSameSiteNull() {
        String sameSitePolicy = null;
        this.repository.setCookieCustomizer((customizer) -> customizer.sameSite(sameSitePolicy));
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getAttribute("SameSite")).isNull();
    }

    @Test
    public void saveTokenWithSameSiteStrict() {
        String sameSitePolicy = "Strict";
        this.repository.setCookieCustomizer((customizer) -> customizer.sameSite(sameSitePolicy));
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getAttribute("SameSite")).isEqualTo(sameSitePolicy);
    }

    @Test
    public void saveTokenWithSameSiteLax() {
        String sameSitePolicy = "Lax";
        this.repository.setCookieCustomizer((customizer) -> customizer.sameSite(sameSitePolicy));
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getAttribute("SameSite")).isEqualTo(sameSitePolicy);
    }

    @Test
    public void saveTokenWithExistingSetCookieThenDoesNotOverwrite() {
        this.response.setHeader(HttpHeaders.SET_COOKIE, "MyCookie=test");
        this.repository = new DSpaceCsrfTokenRepository();
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        assertThat(this.response.getCookie("MyCookie")).isNotNull();
        assertThat(this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME)).isNotNull();
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
        this.request.setCookies(new Cookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME, ""));
        assertThat(this.repository.loadToken(this.request)).isNull();
    }

    @Test
    public void loadToken() {
        CsrfToken generateToken = this.repository.generateToken(this.request);
        this.request
            .setCookies(new Cookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME, generateToken.getToken()));
        CsrfToken loadToken = this.repository.loadToken(this.request);
        assertThat(loadToken).isNotNull();
        assertThat(loadToken.getHeaderName()).isEqualTo(generateToken.getHeaderName());
        assertThat(loadToken.getParameterName()).isEqualTo(generateToken.getParameterName());
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

    @Test
    public void loadDeferredTokenWhenDoesNotExistThenGeneratedAndSaved() {
        DeferredCsrfToken deferredCsrfToken = this.repository.loadDeferredToken(this.request, this.response);
        CsrfToken csrfToken = deferredCsrfToken.get();
        assertThat(csrfToken).isNotNull();
        assertThat(deferredCsrfToken.isGenerated()).isTrue();
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie).isNotNull();
        assertThat(tokenCookie.getMaxAge()).isEqualTo(-1);
        assertThat(tokenCookie.getName()).isEqualTo(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie.getPath()).isEqualTo(this.request.getContextPath());
        assertThat(tokenCookie.getSecure()).isEqualTo(this.request.isSecure());
        assertThat(tokenCookie.getValue()).isEqualTo(csrfToken.getToken());
        assertThat(tokenCookie.isHttpOnly()).isEqualTo(true);
    }

    @Test
    public void loadDeferredTokenWhenExistsAndNullSavedThenGeneratedAndSaved() {
        CsrfToken generatedToken = this.repository.generateToken(this.request);
        this.request
            .setCookies(new Cookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME, generatedToken.getToken()));
        this.repository.saveToken(null, this.request, this.response);
        DeferredCsrfToken deferredCsrfToken = this.repository.loadDeferredToken(this.request, this.response);
        CsrfToken csrfToken = deferredCsrfToken.get();
        assertThat(csrfToken).isNotNull();
        assertThat(generatedToken).isNotEqualTo(csrfToken);
        assertThat(deferredCsrfToken.isGenerated()).isTrue();
    }

    @Test
    public void cookieCustomizer() {
        String domainName = "example.com";
        String customPath = "/custompath";
        String sameSitePolicy = "Strict";
        this.repository.setCookieCustomizer((customizer) -> {
            customizer.domain(domainName);
            customizer.secure(false);
            customizer.path(customPath);
            customizer.sameSite(sameSitePolicy);
        });
        CsrfToken token = this.repository.generateToken(this.request);
        this.repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie).isNotNull();
        assertThat(tokenCookie.getMaxAge()).isEqualTo(-1);
        assertThat(tokenCookie.getDomain()).isEqualTo(domainName);
        assertThat(tokenCookie.getPath()).isEqualTo(customPath);
        assertThat(tokenCookie.isHttpOnly()).isEqualTo(Boolean.TRUE);
        assertThat(tokenCookie.getAttribute("SameSite")).isEqualTo(sameSitePolicy);
    }

    @Test
    public void withHttpOnlyFalseWhenCookieCustomizerThenStillDefaultsToFalse() {
        DSpaceCsrfTokenRepository repository = DSpaceCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer((customizer) -> customizer.maxAge(1000));
        CsrfToken token = repository.generateToken(this.request);
        repository.saveToken(token, this.request, this.response);
        Cookie tokenCookie = this.response.getCookie(DSpaceCsrfTokenRepository.DEFAULT_CSRF_COOKIE_NAME);
        assertThat(tokenCookie).isNotNull();
        assertThat(tokenCookie.getMaxAge()).isEqualTo(1000);
        assertThat(tokenCookie.isHttpOnly()).isEqualTo(Boolean.FALSE);
    }

    @Test
    public void setCookieNameNullIllegalArgumentException() {
        assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setCookieName(null));
    }

    @Test
    public void setParameterNameNullIllegalArgumentException() {
        assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setParameterName(null));
    }

    @Test
    public void setHeaderNameNullIllegalArgumentException() {
        assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setHeaderName(null));
    }

    @Test
    public void setCookieMaxAgeZeroIllegalArgumentException() {
        assertThatIllegalArgumentException().isThrownBy(() -> this.repository.setCookieMaxAge(0));
    }

}
