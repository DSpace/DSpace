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

    static public String buildVar() {

        int mySuffixVarLength=5;
        SecureRandom myRandom = new SecureRandom();

        try{
            mySuffixVarLength = ConfigurationManager.getIntProperty("doi.suffix.length");
        }catch (NumberFormatException nfe){
            mySuffixVarLength=5;
        }

        String bigInt = new BigInteger(mySuffixVarLength * 5, myRandom).toString(32);
        StringBuilder buffer = new StringBuilder(bigInt);
        int charCount = 0;

        while (buffer.length() < mySuffixVarLength) {
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
