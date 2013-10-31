package org.dspace.app.util;

import org.dspace.core.ConfigurationManager;
import org.dspace.services.ConfigurationService;

import java.math.BigInteger;
import java.security.SecureRandom;

/**
 * User: lantian @ atmire . com
 * Date: 10/31/13
 * Time: 12:32 PM
 */
public class NoidGenerator {

    static public String buildVar(int length) {

        SecureRandom myRandom = new SecureRandom();

        String bigInt = new BigInteger(length * 5, myRandom).toString(32);
        StringBuilder buffer = new StringBuilder(bigInt);
        int charCount = 0;

        while (buffer.length() < length) {
            buffer.append('0');
        }

        for (int index = 0; index < buffer.length(); index++) {
            char character = buffer.charAt(index);
            int random;

            if (character == 'a' | character == 'l' | character == 'e'
                    | character == 'i' | character == 'o' | character == 'u') {
                random = myRandom.nextInt(9);
                buffer.setCharAt(index, String.valueOf(random).charAt(0));
                charCount = 0;
            } else if (Character.isLetter(character)) {
                charCount += 1;

                if (charCount > 2) {
                    random = myRandom.nextInt(9);
                    buffer.setCharAt(index, String.valueOf(random).charAt(0));
                    charCount = 0;
                }
            }
        }

        return buffer.toString();
    }
}
