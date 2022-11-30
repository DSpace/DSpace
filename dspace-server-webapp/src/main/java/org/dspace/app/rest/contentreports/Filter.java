/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.contentreports;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.contentreports.ItemFilterUtil.BundleName;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Available filters for the Filtered Collections and Filtered Items reports.
 * In this enum, each item corresponds to a separate property, not values of
 * a single property, hence the @JsonProperty applied to each of them.
 * For each item, the annotation value is read through reflection and copied into
 * the id property, which eliminates repetitions, hence reducing the risk or errors.
 *
 * @author Jean-François Morin (Université Laval)
 */
public enum Filter {

    @JsonProperty("is_item")
    IS_ITEM(FilterCategory.PROPERTY, (context, item) -> true),
    @JsonProperty("is_withdrawn")
    IS_WITHDRAWN(FilterCategory.PROPERTY, (context, item) -> item.isWithdrawn()),
    @JsonProperty("is_not_withdrawn")
    IS_NOT_WITHDRAWN(FilterCategory.PROPERTY, (context, item) -> !item.isWithdrawn()),
    @JsonProperty("is_discoverable")
    IS_DISCOVERABLE(FilterCategory.PROPERTY, (context, item) -> item.isDiscoverable()),
    @JsonProperty("is_not_discoverable")
    IS_NOT_DISCOVERABLE(FilterCategory.PROPERTY, (context, item) -> !item.isDiscoverable()),

    @JsonProperty("has_multiple_originals")
    HAS_MULTIPLE_ORIGINALS(FilterCategory.BITSTREAM, (context, item) ->
        ItemFilterUtil.countOriginalBitstream(item) > 1),
    @JsonProperty("has_no_originals")
    HAS_NO_ORIGINALS(FilterCategory.BITSTREAM, (context, item) -> ItemFilterUtil.countOriginalBitstream(item) == 0),
    @JsonProperty("has_one_original")
    HAS_ONE_ORIGINAL(FilterCategory.BITSTREAM, (context, item) -> ItemFilterUtil.countOriginalBitstream(item) == 1),

    @JsonProperty("has_doc_original")
    HAS_DOC_ORIGINAL(FilterCategory.BITSTREAM_MIME, (context, item) ->
        ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.getDocumentMimeTypes()) > 0),
    @JsonProperty("has_image_original")
    HAS_IMAGE_ORIGINAL(FilterCategory.BITSTREAM_MIME, (context, item) ->
        ItemFilterUtil.countOriginalBitstreamMimeStartsWith(context, item, "image") > 0),
    @JsonProperty("has_unsupp_type")
    HAS_UNSUPPORTED_TYPE(FilterCategory.BITSTREAM_MIME, (context, item) -> {
        int bitCount = ItemFilterUtil.countOriginalBitstream(item);
        if (bitCount == 0) {
            return false;
        }
        int docCount = ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.getDocumentMimeTypes());
        int imgCount = ItemFilterUtil.countOriginalBitstreamMimeStartsWith(context, item, "image");
        return (bitCount - docCount - imgCount) > 0;
    }),
    @JsonProperty("has_mixed_original")
    HAS_MIXED_ORIGINAL(FilterCategory.BITSTREAM_MIME, (context, item) -> {
        int countBit = ItemFilterUtil.countOriginalBitstream(item);
        if (countBit <= 1) {
            return false;
        }
        int countDoc = ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.getDocumentMimeTypes());
        if (countDoc > 0) {
            return countDoc != countBit;
        }
        int countImg = ItemFilterUtil.countOriginalBitstreamMimeStartsWith(context, item, "image");
        if (countImg > 0) {
            return countImg != countBit;
        }
        return false;
    }),
    @JsonProperty("has_pdf_original")
    HAS_PDF_ORIGINAL(FilterCategory.BITSTREAM_MIME, (context, item) ->
        ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.MIMES_PDF) > 0),
    @JsonProperty("has_jpg_original")
    HAS_JPEG_ORIGINAL(FilterCategory.BITSTREAM_MIME, (context, item) ->
        ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.MIMES_JPG) > 0),
    @JsonProperty("has_small_pdf")
    HAS_SMALL_PDF(FilterCategory.BITSTREAM_MIME, (context, item) ->
        ItemFilterUtil.countBitstreamSmallerThanMinSize(
                context, BundleName.ORIGINAL, item, ItemFilterUtil.MIMES_PDF, "rest.report-pdf-min-size") > 0),
    @JsonProperty("has_large_pdf")
    HAS_LARGE_PDF(FilterCategory.BITSTREAM_MIME, (context, item) ->
        ItemFilterUtil.countBitstreamLargerThanMaxSize(
                context, BundleName.ORIGINAL,  item, ItemFilterUtil.MIMES_PDF, "rest.report-pdf-max-size") > 0),
    @JsonProperty("has_doc_without_text")
    HAS_DOC_WITHOUT_TEXT(FilterCategory.BITSTREAM_MIME, (context, item) -> {
        int countDoc = ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.getDocumentMimeTypes());
        if (countDoc == 0) {
            return false;
        }
        int countText = ItemFilterUtil.countBitstream(BundleName.TEXT, item);
        return countDoc > countText;
    }),

    @JsonProperty("has_only_supp_image_type")
    HAS_ONLY_SUPPORTED_IMAGE_TYPE(FilterCategory.MIME, (context, item) -> {
        int imageCount = ItemFilterUtil.countOriginalBitstreamMimeStartsWith(context, item, "image/");
        if (imageCount == 0) {
            return false;
        }
        int suppImageCount = ItemFilterUtil.countOriginalBitstreamMime(
                context, item, ItemFilterUtil.getSupportedImageMimeTypes());
        return (imageCount == suppImageCount);
    }),
    @JsonProperty("has_unsupp_image_type")
    HAS_UNSUPPORTED_IMAGE_TYPE(FilterCategory.MIME, (context, item) -> {
        int imageCount = ItemFilterUtil.countOriginalBitstreamMimeStartsWith(context, item, "image/");
        if (imageCount == 0) {
            return false;
        }
        int suppImageCount = ItemFilterUtil.countOriginalBitstreamMime(
                context, item, ItemFilterUtil.getSupportedImageMimeTypes());
        return (imageCount - suppImageCount) > 0;
    }),
    @JsonProperty("has_only_supp_doc_type")
    HAS_ONLY_SUPPORTED_DOC_TYPE(FilterCategory.MIME, (context, item) -> {
        int docCount = ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.getDocumentMimeTypes());
        if (docCount == 0) {
            return false;
        }
        int suppDocCount = ItemFilterUtil.countOriginalBitstreamMime(
                context, item, ItemFilterUtil.getSupportedDocumentMimeTypes());
        return docCount == suppDocCount;
    }),
    @JsonProperty("has_unsupp_doc_type")
    HAS_UNSUPPORTED_DOC_TYPE(FilterCategory.MIME, (context, item) -> {
        int docCount = ItemFilterUtil.countOriginalBitstreamMime(context, item, ItemFilterUtil.getDocumentMimeTypes());
        if (docCount == 0) {
            return false;
        }
        int suppDocCount = ItemFilterUtil.countOriginalBitstreamMime(
                context, item, ItemFilterUtil.getSupportedDocumentMimeTypes());
        return (docCount - suppDocCount) > 0;
    }),

    @JsonProperty("has_unsupported_bundle")
    HAS_UNSUPPORTED_BUNDLE(FilterCategory.BUNDLE, (context, item) -> {
        String[] bundleList = DSpaceServicesFactory.getInstance().getConfigurationService()
                .getArrayProperty("rest.report-supp-bundles");
        return ItemFilterUtil.hasUnsupportedBundle(item, bundleList);
    }),
    @JsonProperty("has_small_thumbnail")
    HAS_SMALL_THUMBNAIL(FilterCategory.BUNDLE, (context, item) ->
        ItemFilterUtil.countBitstreamSmallerThanMinSize(
                context, BundleName.THUMBNAIL, item, ItemFilterUtil.MIMES_JPG, "rest.report-thumbnail-min-size") > 0),
    @JsonProperty("has_original_without_thumbnail")
    HAS_ORIGINAL_WITHOUT_THUMBNAIL(FilterCategory.BUNDLE, (context, item) -> {
        int countBit = ItemFilterUtil.countOriginalBitstream(item);
        if (countBit == 0) {
            return false;
        }
        int countThumb = ItemFilterUtil.countBitstream(BundleName.THUMBNAIL, item);
        return countBit > countThumb;
    }),
    @JsonProperty("has_invalid_thumbnail_name")
    HAS_INVALID_THUMBNAIL_NAME(FilterCategory.BUNDLE, (context, item) -> {
        List<String> originalNames = ItemFilterUtil.getBitstreamNames(BundleName.ORIGINAL, item);
        List<String> thumbNames = ItemFilterUtil.getBitstreamNames(BundleName.THUMBNAIL, item);
        if (thumbNames.size() != originalNames.size()) {
            return false;
        }
        for (String name: originalNames) {
            if (!thumbNames.contains(name + ".jpg")) {
                return true;
            }
        }
        return false;
    }),
    @JsonProperty("has_non_generated_thumb")
    HAS_NON_GENERATED_THUMBNAIL(FilterCategory.BUNDLE, (context, item) -> {
        String[] generatedThumbDesc = DSpaceServicesFactory.getInstance().getConfigurationService()
                .getArrayProperty("rest.report-gen-thumbnail-desc");
        int countThumb = ItemFilterUtil.countBitstream(BundleName.THUMBNAIL, item);
        if (countThumb == 0) {
            return false;
        }
        int countGen = ItemFilterUtil.countBitstreamByDesc(BundleName.THUMBNAIL, item, generatedThumbDesc);
        return (countThumb > countGen);
    }),
    @JsonProperty("no_license")
    NO_LICENSE(FilterCategory.BUNDLE, (context, item) ->
        ItemFilterUtil.countBitstream(BundleName.LICENSE, item) == 0),
    @JsonProperty("has_license_documentation")
    HAS_LICENSE_DOCUMENTATION(FilterCategory.BUNDLE, (context, item) -> {
        List<String> names = ItemFilterUtil.getBitstreamNames(BundleName.LICENSE, item);
        for (String name: names) {
            if (!name.equals("license.txt")) {
                return true;
            }
        }
        return false;
    }),

    @JsonProperty("has_restricted_original")
    HAS_RESTRICTED_ORIGINAL(FilterCategory.PERMISSION, (context, item) -> {
        try {
            for (Bundle bundle: item.getBundles()) {
                if (!bundle.getName().equals(BundleName.ORIGINAL.name())) {
                    continue;
                }
                for (Bitstream bit: bundle.getBitstreams()) {
                    if (!getAuthorizeService()
                            .authorizeActionBoolean(getAnonymousContext(), bit, org.dspace.core.Constants.READ)) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            getLog().warn("SQL Exception testing original bitstream access " + e.getMessage(), e);
        }
        return false;
    }),
    @JsonProperty("has_restricted_thumbnail")
    HAS_RESTRICTED_THUMBNAIL(FilterCategory.PERMISSION, (context, item) -> {
        try {
            for (Bundle bundle: item.getBundles()) {
                if (!bundle.getName().equals(BundleName.THUMBNAIL.name())) {
                    continue;
                }
                for (Bitstream bit: bundle.getBitstreams()) {
                    if (!getAuthorizeService()
                            .authorizeActionBoolean(getAnonymousContext(), bit, org.dspace.core.Constants.READ)) {
                        return true;
                    }
                }
            }
        } catch (SQLException e) {
            getLog().warn("SQL Exception testing thumbnail bitstream access " + e.getMessage(), e);
        }
        return false;
    }),
    @JsonProperty("has_restricted_metadata")
    HAS_RESTRICTED_METADATA(FilterCategory.PERMISSION, (context, item) -> {
        try {
            return !getAuthorizeService()
                    .authorizeActionBoolean(getAnonymousContext(), item, org.dspace.core.Constants.READ);
        } catch (SQLException e) {
            getLog().warn("SQL Exception testing item metadata access " + e.getMessage(), e);
            return false;
        }
    });

    private static final Logger log = LogManager.getLogger();
    private static AuthorizeService authorizeService;
    private static Context anonymousContext;

    private String id;
    private FilterCategory category;
    private BiPredicate<Context, Item> itemTester;

    Filter(FilterCategory category, BiPredicate<Context, Item> itemTester) {
        try {
            JsonProperty jp = getClass().getField(name()).getAnnotation(JsonProperty.class);
            id = Optional.ofNullable(jp).map(JsonProperty::value).orElse(name());
        } catch (Exception e) {
            id = name();
        }
        this.category = category;
        this.itemTester = itemTester;
    }

    public String getId() {
        return id;
    }

    public FilterCategory getCategory() {
        return category;
    }

    public boolean testItem(Context context, Item item) {
        return itemTester.test(context, item);
    }

    private static Logger getLog() {
        return log;
    }

    private static AuthorizeService getAuthorizeService() {
        if (authorizeService == null) {
            authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        }
        return authorizeService;
    }

    private static Context getAnonymousContext() {
        if (anonymousContext == null) {
            anonymousContext = new Context();
        }
        return anonymousContext;
    }

    @JsonCreator
    public static Filter get(String id) {
        return Arrays.stream(values())
                .filter(item -> Objects.equals(item.id, id))
                .findFirst()
                .orElse(null);
    }

    public static Set<Filter> getFilters(String filters) {
        String[] ids = Optional.ofNullable(filters).orElse("").split("[^a-z_]+");
        Set<Filter> set = Arrays.stream(ids)
                .map(Filter::get)
                .filter(f -> f != null)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Filter.class)));
        if (set == null) {
            set = EnumSet.noneOf(Filter.class);
        }
        return set;
    }

}
