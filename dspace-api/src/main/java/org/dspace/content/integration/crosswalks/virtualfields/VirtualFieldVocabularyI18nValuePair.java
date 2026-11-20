/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.ChoiceAuthority;
import org.dspace.content.authority.DCInputAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link VirtualField} that translates {@code value-pair}
 * and {@code vocabulary-fields} into displayable labels.
 * Internally uses the {@link ChoiceAuthorityService} to translate them.
 * <br/>
 * <br/>
 * (Example: {@code @virtual.vocabulary_18n.metadataField@})
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class VirtualFieldVocabularyI18nValuePair implements VirtualField {

    private final static Logger LOGGER = LoggerFactory.getLogger(VirtualFieldVocabularyI18nValuePair.class);

    @Autowired
    private ItemService itemService;
    @Autowired
    private ChoiceAuthorityService choiceAuthorityService;

    private PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

    @Override
    public String[] getMetadata(Context context, Item item, String fieldName) {
        String[] virtualFieldName = fieldName.split("\\.", 4);

        if (virtualFieldName.length < 3 || virtualFieldName.length > 4) {
            LOGGER.warn("Invalid value-pairs virtual field: " + fieldName);
            return new String[] {};
        }
        String vocabularyName = getVocabularyName(virtualFieldName);
        String metadataField = virtualFieldName[2].replaceAll("-", ".");
        Locale locale = getLocale(context);

        return itemService.getMetadataByMetadataString(item, metadataField)
            .stream()
            .map(metadataValue ->
                getLabelForVocabulary(vocabularyName, metadataValue, locale)
                    .orElse(getDisplayableLabel(item, metadataValue, locale.getLanguage()))
            )
            .toArray(String[]::new);
    }

    protected Optional<String> getLabelForVocabulary(
        String vocabularyName, MetadataValue metadataValue, Locale locale
    ) {
        return Optional.ofNullable(vocabularyName)
            .map(vocabulary -> (ChoiceAuthority) pluginService.getNamedPlugin(ChoiceAuthority.class, vocabulary))
            .filter(Objects::nonNull)
            .flatMap(choiceAuthority -> Optional.ofNullable(metadataValue.getAuthority())
                .flatMap(
                    authority -> getLabelWithFallback(choiceAuthority, authority, locale, I18nUtil.getDefaultLocale())
                )
                .or(
                    () -> getLabelWithFallback(
                        choiceAuthority, metadataValue.getValue(),
                        locale, I18nUtil.getDefaultLocale()
                    )
                )
            );
    }

    private Optional<String> getLabelWithFallback(
        ChoiceAuthority choiceAuthority, String authKey, Locale locale, Locale fallbackLocale
    ) {
        return getValidLabel(
            Optional.ofNullable(choiceAuthority.getLabel(authKey, locale.getLanguage()))
        )
            .or(
                () -> getValidLabel(
                    Optional.ofNullable(
                        choiceAuthority.getLabel(
                            authKey,
                            fallbackLocale.getLanguage()
                        )
                    )
                )
            );
    }

    protected String getDisplayableLabel(Item item, MetadataValue metadataValue, String language) {
        return getLabelForCurrentLanguage(item, metadataValue, language)
            .or(() -> getLabelForDefaultLanguage(item, metadataValue))
            .orElse(metadataValue.getValue());
    }

    protected Optional<String> getLabelForDefaultLanguage(Item item, MetadataValue metadataValue) {
        return getLabelForVocabulary(item, metadataValue, I18nUtil.getDefaultLocale().getLanguage())
            .or(() -> getLabelForValuePair(item, metadataValue, I18nUtil.getDefaultLocale().getLanguage()));
    }

    protected Optional<String> getLabelForCurrentLanguage(Item item, MetadataValue metadataValue, String language) {
        return getLabelForVocabulary(item, metadataValue, language)
            .or(() -> getLabelForValuePair(item, metadataValue, language));
    }

    private Optional<String> getLabelForVocabulary(Item item, MetadataValue metadataValue, String language) {
        return getValidLabel(
            Optional.ofNullable(metadataValue)
            .filter(mv -> StringUtils.isNotBlank(mv.getAuthority()))
            .map(mv -> getVocabulary(item, mv, language))
        );
    }

    private Optional<String> getLabelForValuePair(Item item, MetadataValue metadataValue, String language) {
        return getValidLabel(
            Optional.ofNullable(metadataValue)
                .filter(mv -> StringUtils.isNotBlank(mv.getValue()))
                .map(mv -> getValuePair(item, mv, language))
        );
    }

    private String getVocabulary(Item item, MetadataValue metadataValue, String language) {
        try {
            return this.choiceAuthorityService
                .getLabel(
                    metadataValue, item.getType(),
                    item.getOwningCollection(), language
                );
        } catch (Exception e) {
            LOGGER.warn("Error while retrieving the vocabulary for: " +
                metadataValue.getMetadataField().toString(), e
            );
        }
        return null;
    }


    private String getValuePair(Item item, MetadataValue metadataValue, String language) {
        try {
            return this.choiceAuthorityService
                .getLabel(
                    metadataValue.getMetadataField().toString(), item.getType(),
                    item.getOwningCollection(), metadataValue.getValue(), language
                );
        } catch (Exception e) {
            LOGGER.warn(
                "Error while retrievingthe value-pair for: " +
                    metadataValue.getMetadataField().toString(),
                e
            );
        }
        return null;
    }

    private String getVocabularyName(String[] virtualFieldName) {
        return Optional.of(virtualFieldName.length)
            .filter(l -> l == 4)
            .map(l -> virtualFieldName[l - 1])
            .orElse(null);
    }

    private Optional<String> getValidLabel(Optional<String> label) {
        return label.filter(this::isValidLabel);
    }

    private boolean isValidLabel(String s) {
        return s != null && !s.contains(DCInputAuthority.UNKNOWN_KEY);
    }

    private Locale getLocale(Context context) {
        return Optional.ofNullable(context.getCurrentLocale())
            .orElse(I18nUtil.getDefaultLocale());
    }

}
