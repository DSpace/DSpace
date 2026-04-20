/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Singleton that encapsulates the configuration of each different token {@link RegistrationTypeEnum} duration. <br/>
 * Contains also utility methods to compute the expiration date of the registered token.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class RegistrationDataExpirationConfiguration {

    private static final String EXPIRATION_PROP = "eperson.registration-data.token.{0}.expiration";
    private static final String DURATION_FORMAT = "PT{0}";

    public static final RegistrationDataExpirationConfiguration INSTANCE =
        new RegistrationDataExpirationConfiguration();

    public static RegistrationDataExpirationConfiguration getInstance() {
        return INSTANCE;
    }

    private final Map<RegistrationTypeEnum, Duration> expirationMap;

    private RegistrationDataExpirationConfiguration() {
        this.expirationMap =
            Stream.of(RegistrationTypeEnum.values())
                  .map(type -> Optional.ofNullable(getDurationOf(type))
                                       .map(duration -> Map.entry(type, duration))
                                       .orElse(null)
                  )
                  .filter(Objects::nonNull)
                  .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Duration getDurationOf(RegistrationTypeEnum type) {
        String format = MessageFormat.format(EXPIRATION_PROP, type.toString().toLowerCase());
        ConfigurationService config = DSpaceServicesFactory.getInstance().getConfigurationService();
        String typeValue = config.getProperty(format);

        if (StringUtils.isBlank(typeValue)) {
            return null;
        }

        return Duration.parse(MessageFormat.format(DURATION_FORMAT, typeValue));
    }

    /**
     * Retrieves the {@link Duration} configuration of a given {@link RegistrationTypeEnum}.
     *
     * @param type is the type of the given registration token
     * @return the {@link Duration} of that specific token.
     */
    public Duration getExpiration(RegistrationTypeEnum type) {
        return expirationMap.get(type);
    }

    /**
     * Retrieves the expiration date of the given {@link RegistrationTypeEnum}.
     *
     * @param type is the RegistrationTypeEnum of the token
     * @return a Date that represents the expiration date.
     */
    public Instant computeExpirationDate(RegistrationTypeEnum type) {

        if (type == null) {
            return null;
        }

        Duration duration = this.expirationMap.get(type);

        if (duration == null) {
            return null;
        }

        return Instant.now().plus(duration);
    }

}
