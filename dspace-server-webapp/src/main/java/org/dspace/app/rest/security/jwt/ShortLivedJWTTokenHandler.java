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
 * Class responsible for creating and parsing JSON Web Tokens (JWTs) used for bitstream
 * dowloads, supports both JWS and JWE https://jwt.io/ .
 */
@Component
public class ShortLivedJWTTokenHandler extends JWTTokenHandler {
    @Override
    protected String getTokenSecretConfigurationKey() {
        return "jwt.shortLived.token.secret";
    }

    @Override
    protected String getEncryptionSecretConfigurationKey() {
        return "jwt.shortLived.encryption.secret";
    }

    @Override
    protected String getTokenExpirationConfigurationKey() {
        return "jwt.shortLived.token.expiration";
    }

    @Override
    protected String getTokenIncludeIPConfigurationKey() {
        return "jwt.shortLived.token.include.ip";
    }

    @Override
    protected String getEncryptionEnabledConfigurationKey() {
        return "jwt.shortLived.encryption.enabled";
    }

    @Override
    protected String getCompressionEnabledConfigurationKey() {
        return "jwt.shortLived.compression.enabled";
    }
}
