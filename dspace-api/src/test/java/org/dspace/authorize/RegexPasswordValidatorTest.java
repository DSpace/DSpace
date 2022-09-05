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

import org.dspace.AbstractIntegrationTest;
import org.dspace.authorize.service.PasswordValidatorService;
import org.dspace.passwordvalidation.factory.PasswordValidationFactory;
import org.junit.Test;

/**
 * Unit tests for {@link RegexPasswordValidator}.
 * 
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 */
public class RegexPasswordValidatorTest extends AbstractIntegrationTest {

    private PasswordValidatorService regexPasswordValidator = PasswordValidationFactory.getInstance()
                                                                                       .getPasswordValidationService();

    @Test
    public void testValidPassword() {
        assertThat(regexPasswordValidator.isPasswordValid("TestPassword01!"), is(true));
    }

    @Test
    public void testInvalidPasswordForMissingSpecialCharacter() {
        assertThat(regexPasswordValidator.isPasswordValid("TestPassword01"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid("TestPassword01?"), is(true));
    }

    @Test
    public void testInvalidPasswordForMissingNumber() {
        assertThat(regexPasswordValidator.isPasswordValid("TestPassword!"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid("TestPassword1!"), is(true));
    }

    @Test
    public void testInvalidPasswordForMissingUppercaseCharacter() {
        assertThat(regexPasswordValidator.isPasswordValid("testpassword01!"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid("testPassword01!"), is(true));
    }

    @Test
    public void testInvalidPasswordForMissingLowercaseCharacter() {
        assertThat(regexPasswordValidator.isPasswordValid("TESTPASSWORD01!"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid("TESTPASSWORd01!"), is(true));
    }

    @Test
    public void testInvalidPasswordForTooShortValue() {
        assertThat(regexPasswordValidator.isPasswordValid("Test01!"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid("Test012!"), is(true));
    }

    @Test
    public void testInvalidPasswordForTooLongValue() {
        assertThat(regexPasswordValidator.isPasswordValid("ThisIsAVeryLongPassword01!"), is(false));
        assertThat(regexPasswordValidator.isPasswordValid("IsAPassword012!"), is(true));
    }

}