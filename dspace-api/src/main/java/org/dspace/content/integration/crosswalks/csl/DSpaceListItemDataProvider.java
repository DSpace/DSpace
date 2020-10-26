/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.csl;

import static org.dspace.app.itemupdate.MetadataUtilities.parseCompoundForm;
import static org.dspace.content.Item.ANY;

import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.undercouch.citeproc.ListItemDataProvider;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLName;
import de.undercouch.citeproc.csl.CSLType;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.DCDate;
import org.dspace.content.DCPersonName;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;

/**
 * Implementation of {@link ListItemDataProvider} to provide {@link CSLItemData}
 * starting from a DSpace Item.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class DSpaceListItemDataProvider extends ListItemDataProvider {

    private final ItemService itemService;

    private String id;
    private String type;
    private String categories;
    private String language;
    private String journalAbbreviation;
    private String shortTitle;
    private String author;
    private String collectionEditor;
    private String composer;
    private String containerAuthor;
    private String director;
    private String editor;
    private String editorialDirector;
    private String interviewer;
    private String illustrator;
    private String originalAuthor;
    private String recipient;
    private String reviewedAuthor;
    private String translator;
    private String accessed;
    private String container;
    private String eventDate;
    private String issued;
    private String originalDate;
    private String submitted;
    private String abstrct;
    private String annote;
    private String archive;
    private String archiveLocation;
    private String archivePlace;
    private String authority;
    private String callNumber;
    private String chapterNumber;
    private String citationNumber;
    private String citationLabel;
    private String collectionNumber;
    private String collectionTitle;
    private String containerTitle;
    private String containerTitleShort;
    private String dimensions;
    private String DOI;
    private String edition;
    private String event;
    private String eventPlace;
    private String firstReferenceNoteNumber;
    private String genre;
    private String ISBN;
    private String ISSN;
    private String issue;
    private String jurisdiction;
    private String keyword;
    private String locator;
    private String medium;
    private String note;
    private String number;
    private String numberOfPages;
    private String numberOfVolumes;
    private String originalPublisher;
    private String originalPublisherPlace;
    private String originalTitle;
    private String page;
    private String pageFirst;
    private String PMCID;
    private String PMID;
    private String publisher;
    private String publisherPlace;
    private String references;
    private String reviewedTitle;
    private String scale;
    private String section;
    private String source;
    private String status;
    private String title;
    private String titleShort;
    private String URL;
    private String version;
    private String volume;
    private String yearSuffix;

    private Map<String, CSLType> CSLTypeMap;

    private CSLItemDataBuilder itemBuilder;

    public DSpaceListItemDataProvider(ItemService itemService) {
        this.itemService = itemService;
        this.itemBuilder = new CSLItemDataBuilder();
    }

    public void processItem(Item item) {
        itemBuilder.id(String.valueOf(item.getID()));

        try {
            handleStringFields(item);
            handleCslNameFields(item);
            handleCslDateFields(item);
        } catch (ParseException e) {
            throw new RuntimeException("An error occurs parsing the configured metadata", e);
        }

        CSLItemData cslItemData = itemBuilder.build();
        this.items.put(cslItemData.getId(), cslItemData);
    }

    public void processItems(Iterator<Item> items) {
        while (items.hasNext()) {
            processItem(items.next());
        }
    }

    protected CSLItemDataBuilder handleStringFields(Item item)
        throws ParseException {
        if (StringUtils.isNotBlank(type)) {
            CSLType cslType = CSLTypeMap.get(getMetadataFirstValueFromItem(item, type));
            itemBuilder.type(cslType);
        }

        if (StringUtils.isNotBlank(categories)) {
            itemBuilder.categories(getMetadataValuesFromItem(item, categories));
        }

        if (StringUtils.isNotBlank(language)) {
            String metadataFirstValueFromItem = getMetadataFirstValueFromItem(item, language);
            itemBuilder.language(metadataFirstValueFromItem);
        }

        if (StringUtils.isNotBlank(journalAbbreviation)) {
            itemBuilder.journalAbbreviation(getMetadataFirstValueFromItem(item, journalAbbreviation));
        }

        if (StringUtils.isNotBlank(shortTitle)) {
            itemBuilder.shortTitle(getMetadataFirstValueFromItem(item, shortTitle));
        }

        if (StringUtils.isNotBlank(abstrct)) {
            itemBuilder.abstrct(getMetadataFirstValueFromItem(item, abstrct));
        }

        if (StringUtils.isNotBlank(annote)) {
            itemBuilder.annote(getMetadataFirstValueFromItem(item, annote));
        }

        if (StringUtils.isNotBlank(archive)) {
            itemBuilder.archive(getMetadataFirstValueFromItem(item, archive));
        }

        if (StringUtils.isNotBlank(archiveLocation)) {
            itemBuilder.archiveLocation(getMetadataFirstValueFromItem(item, archiveLocation));
        }

        if (StringUtils.isNotBlank(archivePlace)) {
            itemBuilder.archivePlace(getMetadataFirstValueFromItem(item, archivePlace));
        }

        if (StringUtils.isNotBlank(authority)) {
            itemBuilder.authority(getMetadataFirstValueFromItem(item, authority));
        }

        if (StringUtils.isNotBlank(callNumber)) {
            itemBuilder.callNumber(getMetadataFirstValueFromItem(item, callNumber));
        }

        if (StringUtils.isNotBlank(chapterNumber)) {
            itemBuilder.chapterNumber(getMetadataFirstValueFromItem(item, chapterNumber));
        }

        if (StringUtils.isNotBlank(citationNumber)) {
            itemBuilder.citationNumber(getMetadataFirstValueFromItem(item, citationNumber));
        }

        if (StringUtils.isNotBlank(citationLabel)) {
            itemBuilder.citationLabel(getMetadataFirstValueFromItem(item, citationLabel));
        }

        if (StringUtils.isNotBlank(collectionNumber)) {
            itemBuilder.collectionNumber(getMetadataFirstValueFromItem(item, collectionNumber));
        }

        if (StringUtils.isNotBlank(collectionTitle)) {
            itemBuilder.collectionTitle(getMetadataFirstValueFromItem(item, collectionTitle));
        }

        if (StringUtils.isNotBlank(containerTitle)) {
            itemBuilder.containerTitle(getMetadataFirstValueFromItem(item, containerTitle));
        }

        if (StringUtils.isNotBlank(containerTitleShort)) {
            itemBuilder.containerTitleShort(getMetadataFirstValueFromItem(item, containerTitleShort));
        }

        if (StringUtils.isNotBlank(dimensions)) {
            itemBuilder.dimensions(getMetadataFirstValueFromItem(item, dimensions));
        }

        if (StringUtils.isNotBlank(DOI)) {
            itemBuilder.DOI(getMetadataFirstValueFromItem(item, DOI));
        }

        if (StringUtils.isNotBlank(edition)) {
            itemBuilder.edition(getMetadataFirstValueFromItem(item, edition));
        }

        if (StringUtils.isNotBlank(event)) {
            itemBuilder.event(getMetadataFirstValueFromItem(item, event));
        }

        if (StringUtils.isNotBlank(eventPlace)) {
            itemBuilder.eventPlace(getMetadataFirstValueFromItem(item, eventPlace));
        }

        if (StringUtils.isNotBlank(firstReferenceNoteNumber)) {
            itemBuilder.firstReferenceNoteNumber(getMetadataFirstValueFromItem(item, firstReferenceNoteNumber));
        }

        if (StringUtils.isNotBlank(genre)) {
            itemBuilder.genre(getMetadataFirstValueFromItem(item, genre));
        }

        if (StringUtils.isNotBlank(ISBN)) {
            itemBuilder.ISBN(getMetadataFirstValueFromItem(item, ISBN));
        }

        if (StringUtils.isNotBlank(ISSN)) {
            itemBuilder.ISSN(getMetadataFirstValueFromItem(item, ISSN));
        }

        if (StringUtils.isNotBlank(issue)) {
            itemBuilder.issue(getMetadataFirstValueFromItem(item, issue));
        }

        if (StringUtils.isNotBlank(jurisdiction)) {
            itemBuilder.jurisdiction(getMetadataFirstValueFromItem(item, jurisdiction));
        }

        if (StringUtils.isNotBlank(keyword)) {
            itemBuilder.keyword(getMetadataFirstValueFromItem(item, keyword));
        }

        if (StringUtils.isNotBlank(locator)) {
            itemBuilder.locator(getMetadataFirstValueFromItem(item, locator));
        }

        if (StringUtils.isNotBlank(medium)) {
            itemBuilder.medium(getMetadataFirstValueFromItem(item, medium));
        }

        if (StringUtils.isNotBlank(note)) {
            itemBuilder.note(getMetadataFirstValueFromItem(item, note));
        }

        if (StringUtils.isNotBlank(number)) {
            itemBuilder.number(getMetadataFirstValueFromItem(item, number));
        }

        if (StringUtils.isNotBlank(numberOfPages)) {
            itemBuilder.numberOfPages(getMetadataFirstValueFromItem(item, numberOfPages));
        }

        if (StringUtils.isNotBlank(numberOfVolumes)) {
            itemBuilder.numberOfVolumes(getMetadataFirstValueFromItem(item, numberOfVolumes));
        }

        if (StringUtils.isNotBlank(originalPublisher)) {
            itemBuilder.originalPublisher(getMetadataFirstValueFromItem(item, originalPublisher));
        }

        if (StringUtils.isNotBlank(originalPublisherPlace)) {
            itemBuilder.originalPublisherPlace(getMetadataFirstValueFromItem(item, originalPublisherPlace));
        }

        if (StringUtils.isNotBlank(originalTitle)) {
            itemBuilder.originalTitle(getMetadataFirstValueFromItem(item, originalTitle));
        }

        if (StringUtils.isNotBlank(page)) {
            itemBuilder.page(getMetadataFirstValueFromItem(item, page));
        }

        if (StringUtils.isNotBlank(pageFirst)) {
            itemBuilder.pageFirst(getMetadataFirstValueFromItem(item, pageFirst));
        }

        if (StringUtils.isNotBlank(PMCID)) {
            itemBuilder.PMCID(getMetadataFirstValueFromItem(item, PMCID));
        }

        if (StringUtils.isNotBlank(PMID)) {
            itemBuilder.PMID(getMetadataFirstValueFromItem(item, PMID));
        }

        if (StringUtils.isNotBlank(publisher)) {
            itemBuilder.publisher(getMetadataFirstValueFromItem(item, publisher));
        }

        if (StringUtils.isNotBlank(publisherPlace)) {
            itemBuilder.publisherPlace(getMetadataFirstValueFromItem(item, publisherPlace));
        }

        if (StringUtils.isNotBlank(references)) {
            itemBuilder.references(getMetadataFirstValueFromItem(item, references));
        }

        if (StringUtils.isNotBlank(reviewedTitle)) {
            itemBuilder.reviewedTitle(getMetadataFirstValueFromItem(item, reviewedTitle));
        }

        if (StringUtils.isNotBlank(scale)) {
            itemBuilder.scale(getMetadataFirstValueFromItem(item, scale));
        }

        if (StringUtils.isNotBlank(section)) {
            itemBuilder.section(getMetadataFirstValueFromItem(item, section));
        }

        if (StringUtils.isNotBlank(source)) {
            itemBuilder.source(getMetadataFirstValueFromItem(item, source));
        }

        if (StringUtils.isNotBlank(status)) {
            itemBuilder.status(getMetadataFirstValueFromItem(item, status));
        }

        if (StringUtils.isNotBlank(title)) {
            itemBuilder.title(getMetadataFirstValueFromItem(item, title));
        }

        if (StringUtils.isNotBlank(titleShort)) {
            itemBuilder.titleShort(getMetadataFirstValueFromItem(item, titleShort));
        }

        if (StringUtils.isNotBlank(URL)) {
            itemBuilder.URL(getMetadataFirstValueFromItem(item, URL));
        }

        if (StringUtils.isNotBlank(version)) {
            itemBuilder.version(getMetadataFirstValueFromItem(item, version));
        }

        if (StringUtils.isNotBlank(volume)) {
            itemBuilder.volume(getMetadataFirstValueFromItem(item, volume));
        }

        if (StringUtils.isNotBlank(yearSuffix)) {
            itemBuilder.yearSuffix(getMetadataFirstValueFromItem(item, yearSuffix));
        }
        return itemBuilder;
    }

    protected CSLItemDataBuilder handleCslNameFields(Item item) throws ParseException {
        if (StringUtils.isNotBlank(author)) {
            itemBuilder.author(getCslNameFromMetadataValueFromItem(item, author));
        }
        if (StringUtils.isNotBlank(collectionEditor)) {
            itemBuilder.collectionEditor(getCslNameFromMetadataValueFromItem(item, collectionEditor));
        }
        if (StringUtils.isNotBlank(composer)) {
            itemBuilder.composer(getCslNameFromMetadataValueFromItem(item, composer));
        }
        if (StringUtils.isNotBlank(containerAuthor)) {
            itemBuilder.containerAuthor(getCslNameFromMetadataValueFromItem(item, containerAuthor));
        }
        if (StringUtils.isNotBlank(director)) {
            itemBuilder.director(getCslNameFromMetadataValueFromItem(item, director));
        }
        if (StringUtils.isNotBlank(editor)) {
            itemBuilder.editor(getCslNameFromMetadataValueFromItem(item, editor));
        }
        if (StringUtils.isNotBlank(editorialDirector)) {
            itemBuilder.editorialDirector(getCslNameFromMetadataValueFromItem(item, editorialDirector));
        }
        if (StringUtils.isNotBlank(interviewer)) {
            itemBuilder.interviewer(getCslNameFromMetadataValueFromItem(item, interviewer));
        }
        if (StringUtils.isNotBlank(illustrator)) {
            itemBuilder.illustrator(getCslNameFromMetadataValueFromItem(item, illustrator));
        }
        if (StringUtils.isNotBlank(originalAuthor)) {
            itemBuilder.originalAuthor(getCslNameFromMetadataValueFromItem(item, originalAuthor));
        }
        if (StringUtils.isNotBlank(recipient)) {
            itemBuilder.recipient(getCslNameFromMetadataValueFromItem(item, recipient));
        }
        if (StringUtils.isNotBlank(reviewedAuthor)) {
            itemBuilder.reviewedAuthor(getCslNameFromMetadataValueFromItem(item, reviewedAuthor));
        }
        if (StringUtils.isNotBlank(translator)) {
            itemBuilder.translator(getCslNameFromMetadataValueFromItem(item, translator));
        }
        return itemBuilder;
    }

    protected CSLItemDataBuilder handleCslDateFields(Item item) throws ParseException {
        if (StringUtils.isNotBlank(accessed)) {
            DCDate dcDate = getDCDateFromMetadataValueFromItem(item, accessed);
            if (dcDate.toDate() != null) {
                itemBuilder.accessed(dcDate.getYear(), dcDate.getMonth(), dcDate.getDay());
            }
        }

        if (StringUtils.isNotBlank(container)) {
            DCDate dcDate = getDCDateFromMetadataValueFromItem(item, container);
            if (dcDate.toDate() != null) {
                itemBuilder.container(dcDate.getYear(), dcDate.getMonth(), dcDate.getDay());
            }
        }

        if (StringUtils.isNotBlank(eventDate)) {
            DCDate dcDate = getDCDateFromMetadataValueFromItem(item, eventDate);
            if (dcDate.toDate() != null) {
                itemBuilder.eventDate(dcDate.getYear(), dcDate.getMonth(), dcDate.getDay());
            }
        }

        if (StringUtils.isNotBlank(issued)) {
            DCDate dcDate = getDCDateFromMetadataValueFromItem(item, issued);
            if (dcDate.toDate() != null) {
                itemBuilder.issued(dcDate.getYear(), dcDate.getMonth(), dcDate.getDay());
            }
        }

        if (StringUtils.isNotBlank(originalDate)) {
            DCDate dcDate = getDCDateFromMetadataValueFromItem(item, originalDate);
            if (dcDate.toDate() != null) {
                itemBuilder.originalDate(dcDate.getYear(), dcDate.getMonth(), dcDate.getDay());
            }
        }

        if (StringUtils.isNotBlank(submitted)) {
            DCDate dcDate = getDCDateFromMetadataValueFromItem(item, submitted);
            if (dcDate.toDate() != null) {
                itemBuilder.submitted(dcDate.getYear(), dcDate.getMonth(), dcDate.getDay());
            }
        }
        return itemBuilder;
    }

    protected DCDate getDCDateFromMetadataValueFromItem(Item item, String metadataField) throws ParseException {
        String[] mdf = parseCompoundForm(metadataField);
        return new DCDate(itemService.getMetadataFirstValue(item, mdf[0], mdf[1], mdf.length > 2 ? mdf[2] : null, ANY));
    }

    protected String getMetadataFirstValueFromItem(Item item, String metadataField) throws ParseException {
        String[] mdf = parseCompoundForm(metadataField);
        return itemService.getMetadataFirstValue(item, mdf[0], mdf[1], mdf.length > 2 ? mdf[2] : null, Item.ANY);
    }

    protected CSLName[] getCslNameFromMetadataValueFromItem(Item item, String metadataField) throws ParseException {

        String[] mdf = parseCompoundForm(metadataField);
        List<MetadataValue> list = itemService.getMetadata(item, mdf[0], mdf[1], mdf.length > 2 ? mdf[2] : null, ANY);
        List<CSLName> cslNames = new LinkedList<>();
        for (MetadataValue mdv : list) {
            String author = mdv.getValue();
            DCPersonName name = new DCPersonName(author);
            CSLName cslName = new CSLName(name.getLastName(), name.getFirstNames(), null, null, null, null, null, null,
                null, null, null, null);
            cslNames.add(cslName);
        }

        return cslNames.toArray(new CSLName[0]);
    }

    protected String[] getMetadataValuesFromItem(Item item, String metadataField) throws ParseException {

        String[] mdf = parseCompoundForm(metadataField);

        List<MetadataValue> list = itemService.getMetadata(item, mdf[0], mdf[1], mdf.length > 2 ? mdf[2] : null, ANY);
        List<String> metadataValueList = new LinkedList<>();
        for (MetadataValue metadataValue : list) {
            metadataValueList.add(metadataValue.getValue());
        }
        return metadataValueList.toArray(new String[metadataValueList.size()]);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getCategories() {
        return categories;
    }

    public String getLanguage() {
        return language;
    }

    public String getJournalAbbreviation() {
        return journalAbbreviation;
    }

    public String getShortTitle() {
        return shortTitle;
    }

    public String getAuthor() {
        return author;
    }

    public String getCollectionEditor() {
        return collectionEditor;
    }

    public String getComposer() {
        return composer;
    }

    public String getContainerAuthor() {
        return containerAuthor;
    }

    public String getDirector() {
        return director;
    }

    public String getEditor() {
        return editor;
    }

    public String getEditorialDirector() {
        return editorialDirector;
    }

    public String getInterviewer() {
        return interviewer;
    }

    public String getIllustrator() {
        return illustrator;
    }

    public String getOriginalAuthor() {
        return originalAuthor;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getReviewedAuthor() {
        return reviewedAuthor;
    }

    public String getTranslator() {
        return translator;
    }

    public String getAccessed() {
        return accessed;
    }

    public String getContainer() {
        return container;
    }

    public String getEventDate() {
        return eventDate;
    }

    public String getIssued() {
        return issued;
    }

    public String getOriginalDate() {
        return originalDate;
    }

    public String getSubmitted() {
        return submitted;
    }

    public String getAbstrct() {
        return abstrct;
    }

    public String getAnnote() {
        return annote;
    }

    public String getArchive() {
        return archive;
    }

    public String getArchiveLocation() {
        return archiveLocation;
    }

    public String getArchivePlace() {
        return archivePlace;
    }

    public String getAuthority() {
        return authority;
    }

    public String getCallNumber() {
        return callNumber;
    }

    public String getChapterNumber() {
        return chapterNumber;
    }

    public String getCitationNumber() {
        return citationNumber;
    }

    public String getCitationLabel() {
        return citationLabel;
    }

    public String getCollectionNumber() {
        return collectionNumber;
    }

    public String getCollectionTitle() {
        return collectionTitle;
    }

    public String getContainerTitle() {
        return containerTitle;
    }

    public String getContainerTitleShort() {
        return containerTitleShort;
    }

    public String getDimensions() {
        return dimensions;
    }

    public String getDOI() {
        return DOI;
    }

    public String getEdition() {
        return edition;
    }

    public String getEvent() {
        return event;
    }

    public String getEventPlace() {
        return eventPlace;
    }

    public String getFirstReferenceNoteNumber() {
        return firstReferenceNoteNumber;
    }

    public String getGenre() {
        return genre;
    }

    public String getISBN() {
        return ISBN;
    }

    public String getISSN() {
        return ISSN;
    }

    public String getIssue() {
        return issue;
    }

    public String getJurisdiction() {
        return jurisdiction;
    }

    public String getKeyword() {
        return keyword;
    }

    public String getLocator() {
        return locator;
    }

    public String getMedium() {
        return medium;
    }

    public String getNote() {
        return note;
    }

    public String getNumber() {
        return number;
    }

    public String getNumberOfPages() {
        return numberOfPages;
    }

    public String getNumberOfVolumes() {
        return numberOfVolumes;
    }

    public String getOriginalPublisher() {
        return originalPublisher;
    }

    public String getOriginalPublisherPlace() {
        return originalPublisherPlace;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public String getPage() {
        return page;
    }

    public String getPageFirst() {
        return pageFirst;
    }

    public String getPMCID() {
        return PMCID;
    }

    public String getPMID() {
        return PMID;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getPublisherPlace() {
        return publisherPlace;
    }

    public String getReferences() {
        return references;
    }

    public String getReviewedTitle() {
        return reviewedTitle;
    }

    public String getScale() {
        return scale;
    }

    public String getSection() {
        return section;
    }

    public String getSource() {
        return source;
    }

    public String getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getTitleShort() {
        return titleShort;
    }

    public String getURL() {
        return URL;
    }

    public String getVersion() {
        return version;
    }

    public String getVolume() {
        return volume;
    }

    public String getYearSuffix() {
        return yearSuffix;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setJournalAbbreviation(String journalAbbreviation) {
        this.journalAbbreviation = journalAbbreviation;
    }

    public void setShortTitle(String shortTitle) {
        this.shortTitle = shortTitle;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setCollectionEditor(String collectionEditor) {
        this.collectionEditor = collectionEditor;
    }

    public void setComposer(String composer) {
        this.composer = composer;
    }

    public void setContainerAuthor(String containerAuthor) {
        this.containerAuthor = containerAuthor;
    }

    public void setDirector(String director) {
        this.director = director;
    }

    public void setEditor(String editor) {
        this.editor = editor;
    }

    public void setEditorialDirector(String editorialDirector) {
        this.editorialDirector = editorialDirector;
    }

    public void setInterviewer(String interviewer) {
        this.interviewer = interviewer;
    }

    public void setIllustrator(String illustrator) {
        this.illustrator = illustrator;
    }

    public void setOriginalAuthor(String originalAuthor) {
        this.originalAuthor = originalAuthor;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public void setReviewedAuthor(String reviewedAuthor) {
        this.reviewedAuthor = reviewedAuthor;
    }

    public void setTranslator(String translator) {
        this.translator = translator;
    }

    public void setAccessed(String accessed) {
        this.accessed = accessed;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public void setEventDate(String eventDate) {
        this.eventDate = eventDate;
    }

    public void setIssued(String issued) {
        this.issued = issued;
    }

    public void setOriginalDate(String originalDate) {
        this.originalDate = originalDate;
    }

    public void setSubmitted(String submitted) {
        this.submitted = submitted;
    }

    public void setAbstrct(String abstrct) {
        this.abstrct = abstrct;
    }

    public void setAnnote(String annote) {
        this.annote = annote;
    }

    public void setArchive(String archive) {
        this.archive = archive;
    }

    public void setArchiveLocation(String archiveLocation) {
        this.archiveLocation = archiveLocation;
    }

    public void setArchivePlace(String archivePlace) {
        this.archivePlace = archivePlace;
    }

    public void setAuthority(String authority) {
        this.authority = authority;
    }

    public void setCallNumber(String callNumber) {
        this.callNumber = callNumber;
    }

    public void setChapterNumber(String chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public void setCitationNumber(String citationNumber) {
        this.citationNumber = citationNumber;
    }

    public void setCitationLabel(String citationLabel) {
        this.citationLabel = citationLabel;
    }

    public void setCollectionNumber(String collectionNumber) {
        this.collectionNumber = collectionNumber;
    }

    public void setCollectionTitle(String collectionTitle) {
        this.collectionTitle = collectionTitle;
    }

    public void setContainerTitle(String containerTitle) {
        this.containerTitle = containerTitle;
    }

    public void setContainerTitleShort(String containerTitleShort) {
        this.containerTitleShort = containerTitleShort;
    }

    public void setDimensions(String dimensions) {
        this.dimensions = dimensions;
    }

    public void setDOI(String DOI) {
        this.DOI = DOI;
    }

    public void setEdition(String edition) {
        this.edition = edition;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public void setEventPlace(String eventPlace) {
        this.eventPlace = eventPlace;
    }

    public void setFirstReferenceNoteNumber(String firstReferenceNoteNumber) {
        this.firstReferenceNoteNumber = firstReferenceNoteNumber;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setISBN(String ISBN) {
        this.ISBN = ISBN;
    }

    public void setISSN(String ISSN) {
        this.ISSN = ISSN;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public void setJurisdiction(String jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public void setLocator(String locator) {
        this.locator = locator;
    }

    public void setMedium(String medium) {
        this.medium = medium;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public void setNumberOfPages(String numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public void setNumberOfVolumes(String numberOfVolumes) {
        this.numberOfVolumes = numberOfVolumes;
    }

    public void setOriginalPublisher(String originalPublisher) {
        this.originalPublisher = originalPublisher;
    }

    public void setOriginalPublisherPlace(String originalPublisherPlace) {
        this.originalPublisherPlace = originalPublisherPlace;
    }

    public void setOriginalTitle(String originalTitle) {
        this.originalTitle = originalTitle;
    }

    public void setPage(String page) {
        this.page = page;
    }

    public void setPageFirst(String pageFirst) {
        this.pageFirst = pageFirst;
    }

    public void setPMCID(String PMCID) {
        this.PMCID = PMCID;
    }

    public void setPMID(String PMID) {
        this.PMID = PMID;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public void setPublisherPlace(String publisherPlace) {
        this.publisherPlace = publisherPlace;
    }

    public void setReferences(String references) {
        this.references = references;
    }

    public void setReviewedTitle(String reviewedTitle) {
        this.reviewedTitle = reviewedTitle;
    }

    public void setScale(String scale) {
        this.scale = scale;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTitleShort(String titleShort) {
        this.titleShort = titleShort;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public void setYearSuffix(String yearSuffix) {
        this.yearSuffix = yearSuffix;
    }

    public void setCSLTypeMap(Map<String, CSLType> cslTypeMap) {
        this.CSLTypeMap = cslTypeMap;
    }

    public Map<String, CSLType> getCSLTypeMap() {
        return CSLTypeMap;
    }
}
