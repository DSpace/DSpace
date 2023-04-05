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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Equator;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.submit.ListenerProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.UploadableStep;
import org.dspace.app.rest.utils.Utils;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.ImportService;
import org.dspace.submit.listener.MetadataListener;
import org.dspace.utils.DSpace;
import org.springframework.web.multipart.MultipartFile;

/**
 * This submission step allows to extract metadata from an uploaded file and/or
 * use provided identifiers/metadata to further enrich a submission.
 * 
 * The processing of the file is delegated to the Import Service (see
 * {@link ImportService} that can be extended with Data Provider specialized by
 * format (i.e. a Grobid extractor to get data from a PDF file, an extractor to
 * get data from bibliographic file such as BibTeX, etc)
 *
 * Some metadata are monitored by listener (see {@link MetadataListener} and when
 * changed the are used to generate an identifier that is used to query the
 * External Data Provider associated with the specific listener
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class ExtractMetadataStep implements ListenerProcessingStep, UploadableStep {

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private ImportService importService = new DSpace().getSingletonService(ImportService.class);
    private MetadataListener listener = new DSpace().getSingletonService(MetadataListener.class);

    // we need to use thread local as we need to store the status of the item before that changes are performed
    private ThreadLocal<Map<String, List<MetadataValue>>> metadataMap =
            new ThreadLocal<Map<String, List<MetadataValue>>>();

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(ExtractMetadataStep.class);

    @Override
    public void doPreProcessing(Context context, InProgressSubmission wsi) {
        Map<String, List<MetadataValue>> metadataMapValue = new HashMap<String, List<MetadataValue>>();
        for (String metadata : listener.getMetadataToListen()) {
            String[] tokenized = org.dspace.core.Utils.tokenize(metadata);
            List<MetadataValue> mm = itemService.getMetadata(wsi.getItem(), tokenized[0], tokenized[1],
                    tokenized[2], Item.ANY);
            if (mm != null && !mm.isEmpty()) {
                metadataMapValue.put(metadata, mm);
            } else {
                metadataMapValue.put(metadata, new ArrayList<MetadataValue>());
            }
        }
        metadataMap.set(metadataMapValue);
    }

    @Override
    public void doPostProcessing(Context context, InProgressSubmission wsi) {
        Map<String, List<MetadataValue>> metadataMapValue = metadataMap.get();
        Set<String> changedMetadata = getChangedMetadata(wsi.getItem(), listener.getMetadataToListen(),
                metadataMapValue);
        // are listened metadata changed?
        try {
            if (!changedMetadata.isEmpty()) {
                ExternalDataObject obj = listener.getExternalDataObject(context, wsi.getItem(), changedMetadata);
                if (obj != null) {
                    // add metadata to the item if no values are already here
                    Set<String> alreadyFilledMetadata = new HashSet();
                    for (MetadataValue mv : wsi.getItem().getMetadata()) {
                        alreadyFilledMetadata.add(mv.getMetadataField().toString('.'));
                    }
                    for (MetadataValueDTO metadataValue : obj.getMetadata()) {
                        StringJoiner joiner = new StringJoiner(".");
                        joiner.add(metadataValue.getSchema());
                        joiner.add(metadataValue.getElement());
                        if (StringUtils.isNoneBlank(metadataValue.getQualifier())) {
                            joiner.add(metadataValue.getQualifier());
                        }
                        if (!alreadyFilledMetadata.contains(joiner.toString())) {
                            itemService.addMetadata(context, wsi.getItem(), metadataValue.getSchema(),
                                metadataValue.getElement(), metadataValue.getQualifier(), null,
                                metadataValue.getValue());
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Set<String> getChangedMetadata(Item item, Set<String> listenedMedata,
            Map<String, List<MetadataValue>> previousValues) {
        Set<String> changedMetadata = new HashSet<String>();
        for (String metadata : listenedMedata) {
            List<MetadataValue> prevMetadata = previousValues.get(metadata);
            List<MetadataValue> currMetadata = itemService.getMetadataByMetadataString(item, metadata);
            if (prevMetadata != null) {
                if (currMetadata != null) {
                    if (!CollectionUtils.isEqualCollection(prevMetadata, currMetadata, new Equator<MetadataValue>() {
                        @Override
                        public boolean equate(MetadataValue o1, MetadataValue o2) {
                            return StringUtils.equals(o1.getValue(), o2.getValue())
                                    && StringUtils.equals(o1.getAuthority(), o2.getAuthority());
                        }
                        @Override
                        public int hash(MetadataValue o) {
                            return o.getValue().hashCode()
                                    + (o.getAuthority() != null ? o.getAuthority().hashCode() : 0);
                        }
                    })) {
                        // one or more values has been changed from the listened metadata
                        changedMetadata.add(metadata);
                    }
                } else if (prevMetadata.size() != 0) {
                    // a value has been removed from the listened metadata
                    changedMetadata.add(metadata);
                }
            } else if (currMetadata != null && currMetadata.size() != 0) {
                // a value has been added to the listened metadata
                changedMetadata.add(metadata);
            }
        }
        return changedMetadata;
    }

    @Override
    public ErrorRest upload(Context context, SubmissionService submissionService, SubmissionStepConfig stepConfig,
            InProgressSubmission wsi, MultipartFile multipartFile)
        throws IOException {

        Item item = wsi.getItem();
        File file = Utils.getFile(multipartFile, "extract-metadata-step", stepConfig.getId());
        try {
            ImportRecord record = importService.getRecord(file, multipartFile.getOriginalFilename());
            if (record != null) {
                // add metadata to the item if no values are already here
                Set<String> alreadyFilledMetadata = new HashSet();
                for (MetadataValue mv : item.getMetadata()) {
                    alreadyFilledMetadata.add(mv.getMetadataField().toString('.'));
                }
                for (MetadatumDTO metadataValue : record.getValueList()) {
                    StringJoiner joiner = new StringJoiner(".");
                    joiner.add(metadataValue.getSchema());
                    joiner.add(metadataValue.getElement());
                    if (StringUtils.isNoneBlank(metadataValue.getQualifier())) {
                        joiner.add(metadataValue.getQualifier());
                    }
                    if (!alreadyFilledMetadata.contains(joiner.toString())) {
                        itemService.addMetadata(context, item, metadataValue.getSchema(),
                            metadataValue.getElement(), metadataValue.getQualifier(), null,
                            metadataValue.getValue());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error processing data", e);
            throw new RuntimeException(e);
        } finally {
            file.delete();
        }
        return null;
    }

}
