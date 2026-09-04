/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.mfa;

/**
 * Enumeration of supported Multi-Factor Authentication mechanism types.
 *
 * <p>Currently only TOTP (Time-based One-Time Password, RFC 6238) is supported.
 * This enum is designed to be extensible for future MFA mechanisms such as
 * WebAuthn/FIDO2 or hardware tokens.</p>
 *
 * @author DSpace Contributors
 * @see Mfa#getMfaType()
 */
public enum MfaType {

    /**
     * Time-based One-Time Password as defined by RFC 6238.
     * Uses a shared secret and the current time to generate 6-digit codes
     * with a 30-second validity window.
     */
    TOTP
}
