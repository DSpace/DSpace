/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.extraction;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.xml.bind.JAXBContext;

import gr.ekt.bte.core.DataLoadingSpec;
import gr.ekt.bte.core.MutableRecord;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.core.Value;
import gr.ekt.bte.dataloader.FileDataLoader;
import gr.ekt.bte.exceptions.MalformedSourceException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.dspace.submit.extraction.grobid.Abstract;
import org.dspace.submit.extraction.grobid.Analytic;
import org.dspace.submit.extraction.grobid.Author;
import org.dspace.submit.extraction.grobid.BiblStruct;
import org.dspace.submit.extraction.grobid.Date;
import org.dspace.submit.extraction.grobid.FileDesc;
import org.dspace.submit.extraction.grobid.Forename;
import org.dspace.submit.extraction.grobid.Idno;
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
import org.dspace.submit.util.SubmissionLookupPublication;

/**
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class GrobidFileDataLoader extends FileDataLoader {

    private static final Logger log = Logger.getLogger(GrobidFileDataLoader.class);

    private String url;

    private Map<String, String> fieldMap; // mapping between service fields and
    // local
    // intermediate fields

    @Override
    public RecordSet getRecords() throws MalformedSourceException {

        RecordSet recordSet = new RecordSet();
        try {
            HttpPost method = null;
            try {
                CloseableHttpClient client = HttpClients.createDefault();
                method = new HttpPost(url + "/api/processHeaderDocument");
                System.out.println(filename);
                InputStream inputStream = new FileInputStream(new File(filename));
                MultipartEntityBuilder builder = MultipartEntityBuilder.create();
                builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
                builder.addBinaryBody("input", inputStream);
                builder.addTextBody("consolidateHeader", "0");
                HttpEntity entity = builder.build();

                method.setEntity(entity);
                // Execute the method.
                HttpResponse response = client.execute(method);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();

                if (statusCode != HttpStatus.SC_OK) {
                    throw new RuntimeException("Http call failed: " + statusLine);
                }

                Record record;
                try {

                    JAXBContext jc = JAXBContext.newInstance(TEI.class);
                    TEI tei = (TEI) jc.createUnmarshaller().unmarshal(response.getEntity().getContent());

                    record = convertTEIToRecord(tei.getTeiHeader());
                    recordSet.addRecord(convertFields(record));
                } catch (Exception e) {
                    log.warn(e.getMessage());
                }
            } catch (Exception e1) {
                log.warn(e1.getMessage());
            } finally {
                if (method != null) {
                    method.releaseConnection();
                }
            }
        } catch (RuntimeException rt) {
            log.error(rt.getMessage(), rt);
        }
        return recordSet;
    }

    private Record convertTEIToRecord(TeiHeader teiHeader) {

        MutableRecord record = new SubmissionLookupPublication("");

        FileDesc fileDesc = teiHeader.getFileDesc();

        PublicationStmt publicationStmt = fileDesc.getPublicationStmt();
        List<Object> publishersAndAddressesAndDates = publicationStmt.getPublishersAndAddressesAndDates();
        extractInfo(record, publishersAndAddressesAndDates, "publicationStmt");


        List<SourceDesc> sourceDescs = fileDesc.getSourceDescs();
        for (SourceDesc sourceDesc : sourceDescs) {
            List<Object> biblsAndBiblStructsAndListBibls = sourceDesc.getBiblsAndBiblStructsAndListBibls();
            for (Object bibl : biblsAndBiblStructsAndListBibls) {
                if (bibl instanceof BiblStruct) {
                    BiblStruct struct = (BiblStruct) bibl;
                    List<Analytic> analytics = struct.getAnalytics();
                    for (Analytic analytic : analytics) {
                        // Analytic
                        List<Object> authorsAndEditorsAndTitles = analytic.getAuthorsAndEditorsAndTitles();
                        extractInfo(record, authorsAndEditorsAndTitles, "analytic");
                    }
                    List<Object> monogrsAndSeries = struct.getMonogrsAndSeries();
                    for (Object monogrsAndSerie : monogrsAndSeries) {
                        if (monogrsAndSerie instanceof Monogr) {
                            // Monogr
                            Monogr monogr = (Monogr) monogrsAndSerie;
                            List<Object> imprintsAndAuthorsAndEditors = monogr.getImprintsAndAuthorsAndEditors();
                            extractInfo(record, imprintsAndAuthorsAndEditors, "monogr");
                        } else {
                            // Series
                            Series series = (Series) monogrsAndSerie;
                            List<Object> imprintsAndAuthorsAndEditors = series.getContent();
                            extractInfo(record, imprintsAndAuthorsAndEditors, "series");
                        }
                    }

                    List<Object> notesAndIdnosAndPtrs = struct.getNotesAndIdnosAndPtrs();
                    extractInfo(record, notesAndIdnosAndPtrs, "biblstruct");
                }
            }
        }

        List<Object> getProfileDescsAndXenoDatas = teiHeader.getProfileDescsAndXenoDatas();
        extractInfo(record, getProfileDescsAndXenoDatas, "profileDesc");

        return record;
    }

    private void extractInfo(MutableRecord record, List<Object> objects, String prefix) {
        for (Object object : objects) {
            if (object instanceof Author) {
                extractAuthors(record, object, prefix);
            } else if (object instanceof Title) {
                extractTitle(record, object, prefix);
            } else if (object instanceof Idno) {
                extractIdno(record, (Idno) object, prefix);
            } else if (object instanceof ProfileDesc) {
                ProfileDesc profileDesc = (ProfileDesc) object;
                extractInfo(record, profileDesc.getAbstractsAndTextClassesAndCorrespDescs(), prefix);
            } else if (object instanceof Abstract) {
                extractInfo(record, ((Abstract) object).getContent(), prefix + "abstract");
            } else if (object instanceof TextClass) {
                extractInfo(record, ((TextClass) object).getClassCodesAndKeywords(), prefix);
            } else if (object instanceof P) {
                extractInfo(record, ((P) object).getContent(), prefix + "paragraph");
            } else if (object instanceof Keywords) {
                extractInfo(record, ((Keywords) object).getContent(), prefix + "keywords");
            } else if (object instanceof Term) {
                extractInfo(record, ((Term) object).getContent(), prefix + "term");
            } else if (object instanceof Date) {
                extractDate(record, ((Date) object), prefix + "date");
            } else if (object instanceof String) {
                String str = (String) object;
                if (StringUtils.isNotBlank(str)) {
                    record.addValue(prefix.toLowerCase(), new StringValue(str));
                }
            }
        }
    }

    private void extractDate(MutableRecord record, Date content, String prefix) {
        if ("published".equals(content.getType())) {
            record.addValue((prefix + "published").toLowerCase(), new StringValue(content.getWhen()));
        }
    }

    private void extractIdno(MutableRecord record, Idno object, String prefix) {
        extractInfo(record, object.getContent(), object.getType());
    }

    private void extractTitle(MutableRecord record, Object authorsAndEditorsAndTitle, String prefix) {
        Title title = (Title) authorsAndEditorsAndTitle;
        if ("main".equals(title.getType())) {
            String outputtitle = "";
            for (Object string : title.getContent()) {
                if (string instanceof String) {
                    outputtitle += (String) string;
                }
            }
            record.addValue((prefix + "title").toLowerCase(), new StringValue(outputtitle));
        }
    }

    private void extractAuthors(MutableRecord record, Object authorsAndEditorsAndTitle, String prefix) {
        Author author = (Author) authorsAndEditorsAndTitle;
        List<Object> contents = author.getContent();
        for (Object content : contents) {
            if (content instanceof PersName) {
                PersName persName = (PersName) content;
                List<Object> names = persName.getContent();
                String firstName = "";
                String lastName = "";
                for (Object name : names) {
                    if (name instanceof Surname) {
                        Surname surname = (Surname) name;
                        for (Object string : surname.getContent()) {
                            if (string instanceof String) {
                                lastName += (String) string;
                            }
                        }
                    }
                    if (name instanceof Forename) {
                        Forename forename = (Forename) name;
                        if ("first".equals(forename.getType())) {
                            for (Object string : forename.getContent()) {
                                if (string instanceof String) {
                                    firstName += (String) string;
                                }
                            }
                        }
                    }
                }
                String outputname = lastName;
                if (StringUtils.isNotBlank(firstName)) {
                    outputname += ", " + firstName;
                }
                record.addValue((prefix + "name").toLowerCase(), new StringValue(outputname));
            }
        }
    }

    @Override
    public RecordSet getRecords(DataLoadingSpec spec) throws MalformedSourceException {
        if (spec.getOffset() > 0) {
            return new RecordSet();
        }
        return getRecords();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Record convertFields(Record publication) {
        for (String fieldName : fieldMap.keySet()) {
            String md = null;
            if (fieldMap != null) {
                md = this.fieldMap.get(fieldName);
            }

            if (StringUtils.isBlank(md)) {
                continue;
            } else {
                md = md.trim();
            }

            if (publication.isMutable()) {
                List<Value> values = publication.getValues(fieldName);
                publication.makeMutable().removeField(fieldName);
                publication.makeMutable().addField(md, values);
            }
        }

        return publication;
    }

    public Map<String, String> getFieldMap() {
        return fieldMap;
    }

    public void setFieldMap(Map<String, String> fieldMap) {
        this.fieldMap = fieldMap;
    }
}
