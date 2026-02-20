/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.extraction;

import static org.dspace.submit.extraction.grobid.client.ConsolidateHeaderEnum.CONSOLIDATE_AND_INJECT_METADATA;

import java.io.InputStream;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.importer.external.service.components.AbstractPlainMetadataSource;
import org.dspace.importer.external.service.components.dto.PlainMetadataSourceDto;
import org.dspace.submit.extraction.grobid.Abstract;
import org.dspace.submit.extraction.grobid.Analytic;
import org.dspace.submit.extraction.grobid.Author;
import org.dspace.submit.extraction.grobid.BiblStruct;
import org.dspace.submit.extraction.grobid.Date;
import org.dspace.submit.extraction.grobid.FileDesc;
import org.dspace.submit.extraction.grobid.Forename;
import org.dspace.submit.extraction.grobid.Idno;
import org.dspace.submit.extraction.grobid.Imprint;
import org.dspace.submit.extraction.grobid.Keywords;
import org.dspace.submit.extraction.grobid.Monogr;
import org.dspace.submit.extraction.grobid.P;
import org.dspace.submit.extraction.grobid.PersName;
import org.dspace.submit.extraction.grobid.ProfileDesc;
import org.dspace.submit.extraction.grobid.PublicationStmt;
import org.dspace.submit.extraction.grobid.Series;
import org.dspace.submit.extraction.grobid.SourceDesc;
import org.dspace.submit.extraction.grobid.Surname;
import org.dspace.submit.extraction.grobid.TEI;
import org.dspace.submit.extraction.grobid.TeiHeader;
import org.dspace.submit.extraction.grobid.Term;
import org.dspace.submit.extraction.grobid.TextClass;
import org.dspace.submit.extraction.grobid.Title;
import org.dspace.submit.extraction.grobid.client.GrobidClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements a metadata importer that use GROBID to read pdfs.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class GrobidImportMetadataSourceServiceImpl extends AbstractPlainMetadataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidImportMetadataSourceServiceImpl.class);

    private GrobidClient grobidClient;

    public GrobidImportMetadataSourceServiceImpl(GrobidClient grobidClient) {
        this.grobidClient = grobidClient;
    }

    public GrobidClient getGrobidClient() {
        return grobidClient;
    }

    public void setGrobidClient(GrobidClient grobidClient) {
        this.grobidClient = grobidClient;
    }

    @Override
    public String getImportSource() {
        return "GrobidMetadataSource";
    }

    @Override
    protected List<PlainMetadataSourceDto> readData(InputStream inputStream) {
        try {
            TEI tei = grobidClient.processHeaderDocument(inputStream, CONSOLIDATE_AND_INJECT_METADATA);
            return List.of(convertToPlainMetadataSourceDto(tei.getTeiHeader()));
        } catch (RuntimeException ex) {
            LOGGER.error("An error occurs processing header document", ex);
            return null;
        }
    }

    private PlainMetadataSourceDto convertToPlainMetadataSourceDto(TeiHeader teiHeader) {

        PlainMetadataSourceDto metadata = new PlainMetadataSourceDto();

        FileDesc fileDesc = teiHeader.getFileDesc();

        PublicationStmt publicationStmt = fileDesc.getPublicationStmt();
        List<Object> publishersAndAddressesAndDates = publicationStmt.getPublishersAndAddressesAndDates();
        extractInfo(metadata, publishersAndAddressesAndDates, "publicationStmt");

        List<SourceDesc> sourceDescs = fileDesc.getSourceDescs();
        for (SourceDesc sourceDesc : sourceDescs) {
            List<Object> biblsAndBiblStructsAndListBibls = sourceDesc.getBiblsAndBiblStructsAndListBibls();
            for (Object bibl : biblsAndBiblStructsAndListBibls) {
                if (bibl instanceof BiblStruct struct) {
                    List<Analytic> analytics = struct.getAnalytics();
                    for (Analytic analytic : analytics) {
                        // Analytic
                        List<Object> authorsAndEditorsAndTitles = analytic.getAuthorsAndEditorsAndTitles();
                        extractInfo(metadata, authorsAndEditorsAndTitles, "analytic");
                    }
                    List<Object> monogrsAndSeries = struct.getMonogrsAndSeries();
                    for (Object monogrsAndSerie : monogrsAndSeries) {
                        if (monogrsAndSerie instanceof Monogr monogr) {
                            // Monogr
                            List<Object> imprintsAndAuthorsAndEditors = monogr.getImprintsAndAuthorsAndEditors();
                            extractInfo(metadata, imprintsAndAuthorsAndEditors, "monogr");
                        } else {
                            // Series
                            Series series = (Series) monogrsAndSerie;
                            List<Object> imprintsAndAuthorsAndEditors = series.getContent();
                            extractInfo(metadata, imprintsAndAuthorsAndEditors, "series");
                        }
                    }

                    List<Object> notesAndIdnosAndPtrs = struct.getNotesAndIdnosAndPtrs();
                    extractInfo(metadata, notesAndIdnosAndPtrs, "biblstruct");
                }
            }
        }

        List<Object> getProfileDescsAndXenoDatas = teiHeader.getProfileDescsAndXenoDatas();
        extractInfo(metadata, getProfileDescsAndXenoDatas, "profileDesc");

        return metadata;
    }

    private void extractInfo(PlainMetadataSourceDto meatadata, List<Object> objects, String prefix) {
        for (Object object : objects) {
            if (object instanceof Author author) {
                extractAuthors(meatadata, author, prefix);
            } else if (object instanceof Title title) {
                extractTitle(meatadata, title, prefix);
            } else if (object instanceof Idno idno) {
                extractIdno(meatadata, idno, prefix);
            } else if (object instanceof ProfileDesc profile) {
                extractInfo(meatadata, profile.getAbstractsAndTextClassesAndCorrespDescs(), prefix);
            } else if (object instanceof Abstract abs) {
                extractInfo(meatadata, abs.getContent(), prefix + "abstract");
            } else if (object instanceof TextClass text) {
                extractInfo(meatadata, text.getClassCodesAndKeywords(), prefix);
            } else if (object instanceof P p) {
                extractInfo(meatadata, p.getContent(), prefix + "paragraph");
            } else if (object instanceof Keywords keywords) {
                extractInfo(meatadata, keywords.getContent(), prefix + "keywords");
            } else if (object instanceof Term term) {
                extractInfo(meatadata, term.getContent(), prefix + "term");
            } else if (object instanceof Date date) {
                extractDate(meatadata, date, prefix + "date");
            } else if (object instanceof Imprint imprint) {
                extractInfo(meatadata, imprint.getBiblScopesAndDatesAndPubPlaces(), prefix + "imprint");
            } else if (object instanceof String str) {
                if (StringUtils.isNotBlank(str)) {
                    meatadata.addMetadata(prefix.toLowerCase(), str);
                }
            }
        }
    }

    private void extractDate(PlainMetadataSourceDto meatadataSource, Date content, String prefix) {
        if ("published".equals(content.getType())) {
            meatadataSource.addMetadata((prefix + "published").toLowerCase(), content.getWhen());
        }
    }

    private void extractIdno(PlainMetadataSourceDto meatadataSource, Idno object, String prefix) {
        extractInfo(meatadataSource, object.getContent(), object.getType().toLowerCase());
    }

    private void extractTitle(PlainMetadataSourceDto meatadataSource, Object authorsAndEditorsAndTitle, String prefix) {
        Title title = (Title) authorsAndEditorsAndTitle;
        if ("main".equals(title.getType())) {
            StringBuilder outputtitle = new StringBuilder();
            for (Object string : title.getContent()) {
                if (string instanceof String) {
                    outputtitle.append((String) string);
                }
            }
            meatadataSource.addMetadata((prefix + "title").toLowerCase(), outputtitle.toString());
        }
    }

    private void extractAuthors(
        PlainMetadataSourceDto meatadataSource, Object authorsAndEditorsAndTitle, String prefix
    ) {
        Author author = (Author) authorsAndEditorsAndTitle;
        List<Object> contents = author.getContent();
        for (Object content : contents) {
            if (content instanceof PersName persName) {
                List<Object> names = persName.getContent();
                StringBuilder firstName = new StringBuilder();
                StringBuilder lastName = new StringBuilder();
                for (Object name : names) {
                    if (name instanceof Surname surname) {
                        for (Object string : surname.getContent()) {
                            if (string instanceof String) {
                                lastName.append((String) string);
                            }
                        }
                    }
                    if (name instanceof Forename forename) {
                        if ("first".equals(forename.getType())) {
                            for (Object string : forename.getContent()) {
                                if (string instanceof String) {
                                    firstName.append((String) string);
                                }
                            }
                        }
                    }
                }
                String outputname = lastName.toString();
                if (StringUtils.isNotBlank(firstName.toString())) {
                    outputname += ", " + firstName;
                }
                meatadataSource.addMetadata((prefix + "name").toLowerCase(), outputname);
            }
        }
    }
}
