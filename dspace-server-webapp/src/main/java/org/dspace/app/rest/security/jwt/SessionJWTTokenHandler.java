/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.security.jwt;

import org.springframework.stereotype.Component;

/**
 * Class responsible for creating and parsing JSON Web Tokens (JWTs), supports both JWS and JWE
 * https://jwt.io/
 */
@Component
public class SessionJWTTokenHandler extends JWTTokenHandler {
    @Override
    protected String getTokenSecretConfigurationKey() {
        return "jwt.session.token.secret";
    }

    @Override
    protected String getEncryptionSecretConfigurationKey() {
        return "jwt.session.encryption.secret";
    }

    @Override
    protected String getTokenExpirationConfigurationKey() {
        return "jwt.session.token.expiration";
    }

    @Override
    protected String getTokenIncludeIPConfigurationKey() {
        return "jwt.session.token.include.ip";
    }

    @Override
    protected String getEncryptionEnabledConfigurationKey() {
        return "jwt.session.encryption.enabled";
    }

    @Override
    protected String getCompressionEnabledConfigurationKey() {
        return "jwt.session.compression.enabled";
    }
}
