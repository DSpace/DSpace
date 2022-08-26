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
public class LoginJWTTokenHandler extends JWTTokenHandler {
    @Override
    protected String getTokenSecretConfigurationKey() {
        return "jwt.login.token.secret";
    }

    @Override
    protected String getEncryptionSecretConfigurationKey() {
        return "jwt.login.encryption.secret";
    }

    @Override
    protected String getTokenExpirationConfigurationKey() {
        return "jwt.login.token.expiration";
    }

    @Override
    protected String getEncryptionEnabledConfigurationKey() {
        return "jwt.login.encryption.enabled";
    }

    @Override
    protected String getCompressionEnabledConfigurationKey() {
        return "jwt.login.compression.enabled";
    }
}
