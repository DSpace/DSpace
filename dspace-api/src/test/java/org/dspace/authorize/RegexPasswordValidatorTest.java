/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link RegexPasswordValidator}.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
@RunWith(MockitoJUnitRunner.class)
public class RegexPasswordValidatorTest {

    private static final String regerx = "^(?=.*?[a-z])(?=.*?[A-Z])(?=\\S*?[0-9])(?=\\S*?[!?$@#$%^&+=]).{8,15}$";

    @Mock
    private ConfigurationService configurationService;

    @Mock
    private Context context;

    @InjectMocks
    private RegexPasswordValidator regexPasswordValidator;

    @Before
    public void setUp() throws Exception {
        when(configurationService.getProperty("authentication-password.regex-validation.pattern")).thenReturn(regerx);
    }

    @Test
    public void testValidPassword() {
        assertThat(regexPasswordValidator.isPasswordValid(context, "TestPassword01!"), is(true));
    }

    @Test
    public void testInvalidPasswordForMissingSpecialCharacter() {
        assertThat(regexPasswordValidator.isPasswordValid(context, "TestPassword01"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid(context, "TestPassword01?"), is(true));
    }

    @Test
    public void testInvalidPasswordForMissingNumber() {
        assertThat(regexPasswordValidator.isPasswordValid(context, "TestPassword!"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid(context, "TestPassword1!"), is(true));
    }

    @Test
    public void testInvalidPasswordForMissingUppercaseCharacter() {
        assertThat(regexPasswordValidator.isPasswordValid(context, "testpassword01!"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid(context, "testPassword01!"), is(true));
    }

    @Test
    public void testInvalidPasswordForMissingLowercaseCharacter() {
        assertThat(regexPasswordValidator.isPasswordValid(context, "TESTPASSWORD01!"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid(context, "TESTPASSWORd01!"), is(true));
    }

    @Test
    public void testInvalidPasswordForTooShortValue() {
        assertThat(regexPasswordValidator.isPasswordValid(context, "Test01!"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid(context, "Test012!"), is(true));
    }

    @Test
    public void testInvalidPasswordForTooLongValue() {
        assertThat(regexPasswordValidator.isPasswordValid(context, "ThisIsAVeryLongPassword01!"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid(context, "IsAPassword012!"), is(true));
    }

}