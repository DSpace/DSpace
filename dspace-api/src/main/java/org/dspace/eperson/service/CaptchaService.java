/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.dspace.eperson.InvalidReCaptchaException;

/**
 * This service for validate the reCaptcha token
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science dot it)
 */
public interface CaptchaService {

    public String REGISTER_ACTION = "register_email";

    /**
     * validate the entered reCaptcha token
     *
     * @param response reCaptcha token to be validated
     * @param action action of reCaptcha
     * @throws InvalidReCaptchaException if reCaptcha was not successfully validated
     */
    public void processResponse(String response, String action) throws InvalidReCaptchaException;

    /**
     * Encode bytes to hex string
     * @param bytes bytes to encode
     * @return hex string
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : bytes) {
            stringBuilder.append(String.format("%02x", b));
        }
        return stringBuilder.toString();
    }

    /**
     * Calculate a hex string from a digest, given an input string
     * @param input input string
     * @param algorithm algorithm key, eg. SHA-256
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String calculateHash(String input, String algorithm) throws NoSuchAlgorithmException {
        MessageDigest sha256 = MessageDigest.getInstance(algorithm);
        byte[] hashBytes = sha256.digest(input.getBytes());
        return bytesToHex(hashBytes);
    }

}