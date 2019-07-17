/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.submit.step;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.core.Value;
import gr.ekt.bte.dataloader.FileDataLoader;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.repository.WorkspaceItemRestRepository;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.UploadableStep;
import org.dspace.app.rest.utils.Utils;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.submit.extraction.MetadataExtractor;
import org.dspace.submit.step.ExtractionStep;
import org.springframework.web.multipart.MultipartFile;

/**
 * This submission step allows to extract metadata from an uploaded file to enrich or initialize a submission. The
 * processing is delegated to a list of extractor specialized by format (i.e. a Grobid extractor to get data from a PDF
 * file, an extractor to get data from bibliographic file such as BibTeX, etc)
 *
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class ExtractMetadataStep extends ExtractionStep implements UploadableStep {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ExtractMetadataStep.class);

    @Override
    public ErrorRest upload(Context context, SubmissionService submissionService, SubmissionStepConfig stepConfig,
            InProgressSubmission wsi, MultipartFile multipartFile)
        throws IOException {

        Item item = wsi.getItem();
        try {
            List<MetadataExtractor> extractors =
                DSpaceServicesFactory.getInstance().getServiceManager().getServicesByType(MetadataExtractor.class);
            File file = null;
            for (MetadataExtractor extractor : extractors) {
                FileDataLoader dataLoader = extractor.getDataLoader();
                RecordSet recordSet = null;
                if (extractor.getExtensions()
                    .contains(FilenameUtils.getExtension(multipartFile.getOriginalFilename()))) {

                    if (file == null) {
                        file = Utils.getFile(multipartFile, "submissionlookup-loader", stepConfig.getId());
                    }

                    FileDataLoader fdl = (FileDataLoader) dataLoader;
                    fdl.setFilename(file.getAbsolutePath());

                    recordSet = convertFields(dataLoader.getRecords(), bteBatchImportService.getOutputMap());

                    enrichItem(context, recordSet.getRecords(), item);

                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            ErrorRest result = new ErrorRest();
            result.setMessage(e.getMessage());
            result.getPaths().add("/" + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS + "/" + stepConfig.getId());
            return result;
        }
        return null;
    }

    private RecordSet convertFields(RecordSet recordSet, Map<String, String> fieldMap) {
        RecordSet result = new RecordSet();
        for (Record publication : recordSet.getRecords()) {
            for (String fieldName : fieldMap.keySet()) {
                String md = null;
                if (fieldMap != null) {
                    md = fieldMap.get(fieldName);
                }

                if (StringUtils.isBlank(md)) {
                    continue;
                } else {
                    md = md.trim();
                }

                if (publication.isMutable()) {
                    List<Value> values = publication.getValues(md);
                    publication.makeMutable().removeField(md);
                    publication.makeMutable().addField(fieldName, values);
                }
            }

            result.addRecord(publication);
        }
        return result;
    }
}
