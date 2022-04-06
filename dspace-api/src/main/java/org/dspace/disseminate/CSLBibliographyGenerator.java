/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.inject.Inject;
import javax.inject.Named;

import de.undercouch.citeproc.CSL;
import de.undercouch.citeproc.csl.CSLDate;
import de.undercouch.citeproc.csl.CSLDateBuilder;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLName;
import de.undercouch.citeproc.csl.CSLNameBuilder;
import de.undercouch.citeproc.csl.CSLType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.dspace.util.MultiFormatDateParser;

/**
 * Generate bibliographic descriptions for DSpace objects in various formats.
 * <p>
 * NOTE:  THIS IS A BEAN.  It must be instantiated by the DI framework.
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 * @see http://citationstyles.org/downloads/specification.html
 */
@Named
public class CSLBibliographyGenerator {
    private MetadataSchemaService metadataSchemaService;

    private MetadataFieldService metadataFieldService;

    private ItemService itemService;

    /** Map DSpace item types to CSL types. */
    private final Map<String, String> documentTypeMap;

    /** Map DSpace metadata fields to CSL fields. */
    private final Map<String, String> metadataFieldMap;

    /** CSL type of any work with an unmapped DSpace type. */
    private final String defaultType;

    /** Logging context. */
    private static final Logger LOG = LogManager.getLogger();

    /** Field types which hold human names (needing special formatting). */
    private static final List<String> cslNameTypes
            = Arrays.asList("author", "editor", "translator", "recipient",
                    "interviewer", "publisher", "composer", "original-publisher",
                    "original-author", "container-author", "collection-editor");

    /** Field types which hold dates (needing special treatment). */
    private static final List<String> cslDateTypes
            = Arrays.asList("accessed", "container", "event-date", "issued",
                    "original-date", "submitted");

    /** List of works in this bibliography. */
    private final List<CSLItemData> items = new ArrayList<>();

    /**
     * Configure an instance with document type and field mappings.
     *
     * @param documentTypes map DSpace object types to CSL types.
     * @param metadataFields map DSpace metadata fields to CSL fields.
     * @param defaultType
     */
    public CSLBibliographyGenerator(Map<String, String> documentTypes,
            Map<String, String> metadataFields,
            String defaultType) {
        this.documentTypeMap = documentTypes;
        this.metadataFieldMap = metadataFields;
        this.defaultType = defaultType.toUpperCase();
    }

    /**
     * @param metadataSchemaService the metadataSchemaService to set
     */
    @Inject
    public void setMetadataSchemaService(
            MetadataSchemaService metadataSchemaService) {
        this.metadataSchemaService = metadataSchemaService;
    }

    /**
     * @param metadataFieldService the metadataFieldService to set
     */
    @Inject
    public void setMetadataFieldService(
            MetadataFieldService metadataFieldService) {
        this.metadataFieldService = metadataFieldService;
    }

    /**
     * @param itemService the itemService to set
     */
    @Inject
    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    /** Do not use. */
    private CSLBibliographyGenerator() {
        documentTypeMap = metadataFieldMap = null;
        defaultType = null;
    }

    /**
     * Add a work to the bibliography.
     *
     * @param context current DSpace context.
     * @param item the work to be added.
     */
    public void addWork(Context context, Item item) {
        items.add(buildItem(context, item));
    }

    /**
     * Generate the bibliography in a given style and format.
     *
     * @param style citation style (e.g. "bibtex").
     * @param outputFormat document type (e.g. HTML, text).
     * @return styled and formatted bibliography.
     * @throws IOException passed through.
     */
    public String render(String style, OutputFormat outputFormat)
            throws IOException {
        return CSL.makeAdhocBibliography(style,
                outputFormat.value,
                items.toArray(CSLItemData[]::new))
                .makeString();
    }

    /**
     * Discover which bibliographic styles are supported.
     *
     * @return paths to defined styles, ordered alphabetically by name.
     * @throws java.io.IOException passed through.
     */
    static public Set<String> getStyles()
            throws IOException {
        return new TreeSet<>(CSL.getSupportedStyles());
    }

    /**
     * String constants for citation output formats such as HTML.
     * Not to be confused with citation formats such as BibTeX.  For example, a
     * citation is first formatted as BibTeX and then rendered as HTML which
     * will, in the browser, display a citation in BibTeX format.
     */
    public static enum OutputFormat {
        /** Render as HTML. */
        HTML ("html"),
        /** Render as plain text. */
        TEXT ("text"),
        /** Render as ASCIIDOC. */
        ASCIIDOC ("asciidoc"),
        /** Render as XSL-FO. */
        FO ("fo"),
        /** Render as Rich Text Format. */
        RTF ("rtf");

        private final String value;

        OutputFormat(String format) {
            this.value = format;
        }

        /** @return the enclosed value. */
        public String getValue() {
            return value;
        }
    };

    /**
     * Construct a CSLItemData from DSpace Item metadata.
     *
     * @param context current DSpace context.
     * @param item source of metadata.
     * @return holder of data for a new entry in the bibliography.
     */
    private CSLItemData buildItem(Context context, Item item) {
        CSLItemDataBuilder builder = new CSLItemDataBuilder();

        // Accumulators for multivalued fields.
        List<String> categories = new ArrayList<>();
        List<CSLName> authors = new ArrayList<>();
        List<CSLName> collectionEditors = new ArrayList<>();
        List<CSLName> composers = new ArrayList<>();
        List<CSLName> containerAuthors = new ArrayList<>();
        List<CSLName> directors = new ArrayList<>();
        List<CSLName> editors = new ArrayList<>();
        List<CSLName> editorialDirectors = new ArrayList<>();
        List<CSLName> interviewers = new ArrayList<>();
        List<CSLName> illustrators = new ArrayList<>();
        List<CSLName> originalAuthors = new ArrayList<>();
        List<CSLName> recipients = new ArrayList<>();
        List<CSLName> reviewedAuthors = new ArrayList<>();
        List<CSLName> translators = new ArrayList<>();

        try {
            // Fill the holder with field values.
            for (MetadataSchema schema : metadataSchemaService.findAll(context)) {
                String schemaName = schema.getName();
                for (MetadataField field :
                        metadataFieldService.findAllInSchema(context, schema)) {
                    // What field is this?
                    String fieldElement = field.getElement();
                    String fieldQualifier = field.getQualifier();
                    String dsFieldName = mdFieldName(schemaName, fieldElement, fieldQualifier);

                    // Map to CSL field
                    String cslFieldName = metadataFieldMap.get(dsFieldName);
                    LOG.debug("Map DSpace {} to CSL {}", dsFieldName, cslFieldName);

                    // Skip this field if not mapped.
                    if (null == cslFieldName) {
                        continue;
                    }

                    CSLName nameValue = null;
                    CSLDate dateValue = null;

                    // Add field values to CSL item.
                    for (MetadataValue datum : itemService.getMetadata(item,
                            schemaName, fieldElement, fieldQualifier, Item.ANY)) {
                        String value = datum.getValue();

                        // Figure out the CSL work type.
                        if ("type".equals(cslFieldName)) {
                            String key = value.replaceAll(" ", "_");
                            if (documentTypeMap.containsKey(key)) {
                                value = documentTypeMap.get(key)
                                        .replaceAll("[ -]", "_")
                                        .toUpperCase();
                            } else {
                                LOG.warn("No CSL work type for {} -- using default {}.",
                                        key, defaultType);
                                value = defaultType;
                            }
                        } else if (cslNameTypes.contains(cslFieldName)) {
                            // If this is a name then format
                            nameValue = CSLNameFactory.getInstance(value);
                        } else if (cslDateTypes.contains(cslFieldName)) {
                            // If this is a date then format
                            dateValue = CSLDateFactory.getInstance(value);
                        } else {
                        }

                        // Store in builder; multivalued fields are accumulated instead.
                        switch (cslFieldName) {
                        // id?
                            case "type":
                                builder.type(CSLType.valueOf(value));
                                break;
                            case "categories":
                                categories.add(value);
                                break;
                            case "author":
                                authors.add(nameValue);
                                break;
                            case "collectionEditor":
                                collectionEditors.add(nameValue);
                                break;
                            case "composer":
                                composers.add(nameValue);
                                break;
                            case "containerAuthor":
                                containerAuthors.add(nameValue);
                                break;
                            case "director":
                                directors.add(nameValue);
                                break;
                            case "editor":
                                editors.add(nameValue);
                                break;
                            case "interviewer":
                                interviewers.add(nameValue);
                                break;
                            case "editorialDirector":
                                editorialDirectors.add(nameValue);
                                break;
                            case "illustrator":
                                illustrators.add(nameValue);
                                break;
                            case "originalAuthor":
                                originalAuthors.add(nameValue);
                                break;
                            case "recipient":
                                recipients.add(nameValue);
                                break;
                            case "reviewedAuthor":
                                reviewedAuthors.add(nameValue);
                                break;
                            case "translator":
                                translators.add(nameValue);
                                break;
                            case "accessed":
                                builder.accessed(dateValue);
                                break;
                            case "container":
                                builder.container(dateValue);
                                break;
                            case "eventDate":
                                builder.eventDate(dateValue);
                                break;
                            case "issued":
                                builder.issued(dateValue);
                                break;
                            case "originalDate":
                                builder.originalDate(dateValue);
                                break;
                            case "submitted":
                                builder.submitted(dateValue);
                                break;
                            case "annote":
                                builder.annote(value);
                                break;
                            case "abstract":
                                builder.abstrct(value);
                                break;
                            case "archive":
                                builder.archive(value);
                                break;
                            case "archiveLocation":
                                builder.archiveLocation(value);
                                break;
                            case "archivePlace":
                                builder.archivePlace(value);
                                break;
                            case "authority":
                                builder.authority(value);
                                break;
                            case "callNumber":
                                builder.callNumber(value);
                                break;
                            case "chapterNumber":
                                builder.chapterNumber(value);
                                break;
                            case "citationNumber":
                                builder.citationNumber(value);
                                break;
                            case "citationLabel":
                                builder.citationLabel(value);
                                break;
                            case "collectionNumber":
                                builder.collectionNumber(value);
                                break;
                            case "collectionTitle":
                                builder.collectionTitle(value);
                                break;
                            case "containerTitle":
                                builder.containerTitle(value);
                                break;
                            case "containerTitleShort":
                                builder.containerTitleShort(value);
                                break;
                            case "dimensions":
                                builder.dimensions(value);
                                break;
                            case "DOI":
                                builder.DOI(value);
                                break;
                            case "edition":
                                builder.edition(value);
                                break;
                            case "event":
                                builder.event(value);
                                break;
                            case "eventPlace":
                                builder.eventPlace(value);
                                break;
                            case "firstReferenceNoteNumber":
                                builder.firstReferenceNoteNumber(value);
                                break;
                            case "genre":
                                builder.genre(value);
                                break;
                            case "ISBN":
                                builder.ISBN(value);
                                break;
                            case "ISSN":
                                builder.ISSN(value);
                                break;
                            case "issue":
                                builder.issue(value);
                                break;
                            case "jurisdiction":
                                builder.jurisdiction(value);
                                break;
                            case "keyword":
                                builder.keyword(value);
                                break;
                            case "locator":
                                builder.locator(value);
                                break;
                            case "medium":
                                builder.medium(value);
                                break;
                            case "note":
                                builder.note(value);
                                break;
                            case "number":
                                builder.number(value);
                                break;
                            case "numberOfPages":
                                builder.numberOfPages(value);
                                break;
                            case "numberOfVolumes":
                                builder.numberOfVolumes(value);
                                break;
                            case "originalPublisher":
                                builder.originalPublisher(value);
                                break;
                            case "originalPublisherPlace":
                                builder.originalPublisherPlace(value);
                                break;
                            case "originalTitle":
                                builder.originalTitle(value);
                                break;
                            case "page":
                                builder.page(value);
                                break;
                            case "pageFirst":
                                builder.pageFirst(value);
                                break;
                            case "PMCID":
                                builder.PMCID(value);
                                break;
                            case "PMID":
                                builder.PMID(value);
                                break;
                            case "publisher":
                                builder.publisher(value);
                                break;
                            case "publisherPlace":
                                builder.publisherPlace(value);
                                break;
                            case "references":
                                builder.references(value);
                                break;
                            case "reviewedTitle":
                                builder.reviewedTitle(value);
                                break;
                            case "scale":
                                builder.scale(value);
                                break;
                            case "section":
                                builder.section(value);
                                break;
                            case "source":
                                builder.source(value);
                                break;
                            case "status":
                                builder.status(value);
                                break;
                            case "title":
                                builder.title(value);
                                break;
                            case "titleShort":
                                builder.titleShort(value);
                                break;
                            case "URL":
                                builder.URL(value);
                                break;
                            case "version":
                                builder.version(value);
                                break;
                            case "volume":
                                builder.volume(value);
                                break;
                            case "yearSuffix":
                                builder.yearSuffix(value);
                                break;
                            default:
                                builder.note(value);
                                break;
                        }
                    }

                    // Store accumulated multiple-valued fields.
                    if (!categories.isEmpty()) {
                        builder.categories(categories.toArray(String[]::new));
                    }
                    if (!authors.isEmpty()) {
                        builder.author(authors.toArray(CSLName[]::new));
                    }
                    if (!collectionEditors.isEmpty()) {
                        builder.collectionEditor(collectionEditors.toArray(CSLName[]::new));
                    }
                    if (!composers.isEmpty()) {
                        builder.composer(composers.toArray(CSLName[]::new));
                    }
                    if (!containerAuthors.isEmpty()) {
                        builder.containerAuthor(containerAuthors.toArray(CSLName[]::new));
                    }
                    if (!directors.isEmpty()) {
                        builder.director(directors.toArray(CSLName[]::new));
                    }
                    if (!editors.isEmpty()) {
                        builder.editor(editors.toArray(CSLName[]::new));
                    }
                    if (!editorialDirectors.isEmpty()) {
                        builder.editorialDirector(editorialDirectors.toArray(CSLName[]::new));
                    }
                    if (!interviewers.isEmpty()) {
                        builder.interviewer(interviewers.toArray(CSLName[]::new));
                    }
                    if (!illustrators.isEmpty()) {
                        builder.illustrator(illustrators.toArray(CSLName[]::new));
                    }
                    if (!originalAuthors.isEmpty()) {
                        builder.originalAuthor(originalAuthors.toArray(CSLName[]::new));
                    }
                    if (!recipients.isEmpty()) {
                        builder.recipient(recipients.toArray(CSLName[]::new));
                    }
                    if (!reviewedAuthors.isEmpty()) {
                        builder.reviewedAuthor(reviewedAuthors.toArray(CSLName[]::new));
                    }
                    if (!translators.isEmpty()) {
                        builder.translator(translators.toArray(CSLName[]::new));
                    }
                }
            }
        } catch (SQLException ex) {
            LOG.error("Could not enumerate item metadata:  ", ex);
        }

        return builder.build();
    }

    /**
     * Compose a metadata field "name" as 'schema.element' or
     * 'schema.element.qualifier'.
     *
     * @param schema
     * @param element
     * @param qualifier
     * @return formatted field name.
     */
    private String mdFieldName(String schema, String element, String qualifier) {
        StringBuilder sb = new StringBuilder(schema);
        sb.append('.').append(element);
        if (null != qualifier) {
            sb.append('.')
                    .append(qualifier);
        }
        return sb.toString();
    }

    /**
     * Generate CSLName instances from name Strings.
     */
    private static class CSLNameFactory {
        /**
         * Split the input name at a comma.  Assume that the first part is the
         * family name and the latter is the given name.
         * NOTE:  this really only works for some western European languages.
         * FIXME make it smarter, but how?
         * @param name some person's full name.
         * @return full name divided into family and personal names.
         */
        static CSLName getInstance(String name) {
            String[] nameParts;
            nameParts = name.split(name, 2);
            CSLNameBuilder builder = new CSLNameBuilder();
            if (nameParts.length > 0) {
                builder.family(nameParts[0]);
            }
            if (nameParts.length > 1) {
                builder.given(nameParts[1]);
            }
            return builder.build();
        }
    }

    /**
     * Generate CSLDate instances from date Strings.
     * DSpace date metadata are tricky but we will try our best.
     */
    private static class CSLDateFactory {
        /**
         * Feed a date into the CSL date parser.
         * @param date a date to be parsed.
         * @return same date in CSL form.
         */
        static CSLDate getInstance(String date) {
            Calendar parsedDate = new GregorianCalendar();
            parsedDate.setTime(MultiFormatDateParser.parse(date));
            CSLDate result = new CSLDateBuilder()
                    .dateParts(parsedDate.get(Calendar.YEAR),
                            parsedDate.get(Calendar.MONTH),
                            parsedDate.get(Calendar.DAY_OF_MONTH))
                    .build();
            return result;
        }
    }
}
