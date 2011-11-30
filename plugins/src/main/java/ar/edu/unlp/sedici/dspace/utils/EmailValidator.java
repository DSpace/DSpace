/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ar.edu.unlp.sedici.dspace.utils;

import org.apache.oro.text.perl.Perl5Util;

/**
 * <p>Perform email validations.</p>
 * <p>
 * This class is a Singleton; you can retrieve the instance via the getInstance() method.
 * </p>
 * <p>
 * Based on a script by <a href="mailto:stamhankar@hotmail.com">Sandeep V. Tamhankar</a>
 * http://javascript.internet.com
 * </p>
 * <p>
 * This implementation is not guaranteed to catch all possible errors in an email address.
 * For example, an address like nobody@noplace.somedog will pass validator, even though there
 * is no TLD "somedog"
 * </p>.
 *
 * @version $Revision: 478560 $ $Date: 2006-11-23 13:09:27 +0000 (Thu, 23 Nov 2006) $
 * @since Validator 1.1
 * @deprecated no se usa, queda solo como referencia por si hace falta. Se saco esta clase de /org/apache/commons/validator 1.3.1
 */
public class EmailValidator {

    private static final String SPECIAL_CHARS = "\\000-\\037\\(\\)<>@,;:'\\\\\\\"\\.\\[\\]\\177";
    private static final String VALID_CHARS = "[^\\s" + SPECIAL_CHARS + "]";
    private static final String QUOTED_USER = "(\"[^\"]*\")";
    private static final String ATOM = VALID_CHARS + '+';
    private static final String WORD = "((" + VALID_CHARS + "|')+|" + QUOTED_USER + ")";

    // Each pattern must be surrounded by /
    private static final String LEGAL_ASCII_PATTERN = "/^[\\000-\\177]+$/";
    private static final String EMAIL_PATTERN = "/^(.+)@(.+)$/";
    private static final String IP_DOMAIN_PATTERN =
            "/^\\[(\\d{1,3})[.](\\d{1,3})[.](\\d{1,3})[.](\\d{1,3})\\]$/";
    private static final String TLD_PATTERN = "/^([a-zA-Z]+)$/";
            
    private static final String USER_PATTERN = "/^\\s*" + WORD + "(\\." + WORD + ")*$/";
    private static final String DOMAIN_PATTERN = "/^" + ATOM + "(\\." + ATOM + ")*\\s*$/";
    private static final String ATOM_PATTERN = "/(" + ATOM + ")/";

    /**
     * Singleton instance of this class.
     */
    private static final EmailValidator EMAIL_VALIDATOR = new EmailValidator();

    /**
     * Returns the Singleton instance of this validator.
     * @return singleton instance of this validator.
     */
    public static EmailValidator getInstance() {
        return EMAIL_VALIDATOR;
    }

    /**
     * Protected constructor for subclasses to use.
     */
    protected EmailValidator() {
        super();
    }

    /**
     * <p>Checks if a field has a valid e-mail address.</p>
     *
     * @param email The value validation is being performed on.  A <code>null</code>
     * value is considered invalid.
     * @return true if the email address is valid.
     */
    public boolean isValid(String email) {
        if (email == null) {
            return false;
        }

        Perl5Util matchAsciiPat = new Perl5Util();
        if (!matchAsciiPat.match(LEGAL_ASCII_PATTERN, email)) {
            return false;
        }

        email = stripComments(email);

        // Check the whole email address structure
        Perl5Util emailMatcher = new Perl5Util();
        if (!emailMatcher.match(EMAIL_PATTERN, email)) {
            return false;
        }

        if (email.endsWith(".")) {
            return false;
        }

        if (!isValidUser(emailMatcher.group(1))) {
            return false;
        }

        if (!isValidDomain(emailMatcher.group(2))) {
            return false;
        }

        return true;
    }

    /**
     * Returns true if the domain component of an email address is valid.
     * @param domain being validatied.
     * @return true if the email address's domain is valid.
     */
    protected boolean isValidDomain(String domain) {
        boolean symbolic = false;
        Perl5Util ipAddressMatcher = new Perl5Util();

        if (ipAddressMatcher.match(IP_DOMAIN_PATTERN, domain)) {
            if (!isValidIpAddress(ipAddressMatcher)) {
                return false;
            } else {
                return true;
            }
        } else {
            // Domain is symbolic name
            Perl5Util domainMatcher = new Perl5Util();
            symbolic = domainMatcher.match(DOMAIN_PATTERN, domain);
        }

        if (symbolic) {
            if (!isValidSymbolicDomain(domain)) {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }

    /**
     * Returns true if the user component of an email address is valid.
     * @param user being validated
     * @return true if the user name is valid.
     */
    protected boolean isValidUser(String user) {
        Perl5Util userMatcher = new Perl5Util();
        return userMatcher.match(USER_PATTERN, user);
    }

    /**
     * Validates an IP address. Returns true if valid.
     * @param ipAddressMatcher Pattren matcher
     * @return true if the ip address is valid.
     */
    protected boolean isValidIpAddress(Perl5Util ipAddressMatcher) {
        for (int i = 1; i <= 4; i++) {
            String ipSegment = ipAddressMatcher.group(i);
            if (ipSegment == null || ipSegment.length() <= 0) {
                return false;
            }

            int iIpSegment = 0;

            try {
                iIpSegment = Integer.parseInt(ipSegment);
            } catch(NumberFormatException e) {
                return false;
            }

            if (iIpSegment > 255) {
                return false;
            }

        }
        return true;
    }

    /**
     * Validates a symbolic domain name.  Returns true if it's valid.
     * @param domain symbolic domain name
     * @return true if the symbolic domain name is valid.
     */
    protected boolean isValidSymbolicDomain(String domain) {
        String[] domainSegment = new String[10];
        boolean match = true;
        int i = 0;
        Perl5Util atomMatcher = new Perl5Util();
        while (match) {
            match = atomMatcher.match(ATOM_PATTERN, domain);
            if (match) {
                domainSegment[i] = atomMatcher.group(1);
                int l = domainSegment[i].length() + 1;
                domain =
                        (l >= domain.length())
                        ? ""
                        : domain.substring(l);

                i++;
            } 
        }

        int len = i;
        
        // Make sure there's a host name preceding the domain.
        if (len < 2) {
            return false;
        }
        
        // TODO: the tld should be checked against some sort of configurable 
        // list
        String tld = domainSegment[len - 1];
        if (tld.length() > 1) {
            Perl5Util matchTldPat = new Perl5Util();
            if (!matchTldPat.match(TLD_PATTERN, tld)) {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }
    /**
     *   Recursively remove comments, and replace with a single space.  The simpler
     *   regexps in the Email Addressing FAQ are imperfect - they will miss escaped
     *   chars in atoms, for example.
     *   Derived From    Mail::RFC822::Address
     * @param emailStr The email address
     * @return address with comments removed.
    */
    protected String stripComments(String emailStr)  {
     String input = emailStr;
     String result = emailStr;
     String commentPat = "s/^((?:[^\"\\\\]|\\\\.)*(?:\"(?:[^\"\\\\]|\\\\.)*\"(?:[^\"\\\\]|\111111\\\\.)*)*)\\((?:[^()\\\\]|\\\\.)*\\)/$1 /osx";
     Perl5Util commentMatcher = new Perl5Util();
     result = commentMatcher.substitute(commentPat,input);
     // This really needs to be =~ or Perl5Matcher comparison
     while (!result.equals(input)) {
        input = result;
        result = commentMatcher.substitute(commentPat,input);
     }
     return result;

    }
}
