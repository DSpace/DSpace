/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;
import static org.dspace.validation.service.ValidationService.OPERATION_PATH_SECTIONS;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.collections4.iterators.IteratorChain;
import org.dspace.app.customurl.CustomUrlService;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.validation.model.ValidationError;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link SubmissionStepValidator} to validate custom url
 * section data.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CustomUrlValidator implements SubmissionStepValidator {

    private static final String ERROR_VALIDATION_EMPTY = "error.validation.custom-url.empty";

    private static final String ERROR_VALIDATION_INVALID_CHARS = "error.validation.custom-url.invalid-characters";

    private static final String ERROR_VALIDATION_CONFLICT = "error.validation.custom-url.conflict";

    private static final Pattern URL_PATH_PATTERN = Pattern.compile("^[.a-zA-Z0-9-_]+$");

    @Autowired
    private ItemService itemService;

    @Autowired
    private CustomUrlService customUrlService;

    private String name;

    @Override
    public List<ValidationError> validate(Context context, InProgressSubmission<?> obj, SubmissionStepConfig config) {
        Item item = obj.getItem();
        return customUrlService.getCustomUrl(item)
                               .map(customUrl -> validateUrl(context, item, customUrl, config))
                               .orElse(List.of());
    }

    private List<ValidationError> validateUrl(Context context, Item item, String customUrl,
                                              SubmissionStepConfig config) {

        if (customUrl.isBlank()) {
            return urlValidationError(ERROR_VALIDATION_EMPTY, config);
        }

        if (hasInvalidCharacters(customUrl)) {
            return urlValidationError(ERROR_VALIDATION_INVALID_CHARS, config);
        }

        if (existsAnotherItemWithSameCustomUrl(context, item, customUrl)) {
            return urlValidationError(ERROR_VALIDATION_CONFLICT, config);
        }

        return List.of();
    }

    private boolean hasInvalidCharacters(String customUrl) {
        return !URL_PATH_PATTERN.matcher(customUrl).matches();
    }

    private boolean existsAnotherItemWithSameCustomUrl(Context context, Item item, String customUrl) {
        return convertToStream(findItemsByCustomUrl(context, customUrl))
            .anyMatch(foundItem -> !foundItem.getID().equals(item.getID()));
    }

    private Iterator<Item> findItemsByCustomUrl(Context context, String customUrl) {
        return new IteratorChain<Item>(
            findArchivedByMetadataFieldExcludingOldVersions(context, "dspace", "customurl", null, customUrl),
            findArchivedByMetadataFieldExcludingOldVersions(context, "dspace", "customurl", "old", customUrl));
    }

    private Iterator<Item> findArchivedByMetadataFieldExcludingOldVersions(Context context, String schema,
                                                                           String element, String qualifier,
                                                                           String value) {
        try {
            return itemService.findArchivedByMetadataFieldExcludingOldVersions(context, schema, element, qualifier,
                                                                               value);
        } catch (SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<Item> convertToStream(Iterator<Item> iterator) {
        return stream(spliteratorUnknownSize(iterator, ORDERED), false);
    }

    private List<ValidationError> urlValidationError(String message, SubmissionStepConfig config) {
        ValidationError error = new ValidationError();
        error.setMessage(message);
        if (config != null) {
            error.getPaths().add("/" + OPERATION_PATH_SECTIONS + "/" + config.getId() + "/url");
        }
        return List.of(error);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
