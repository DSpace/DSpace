package org.dspace.disseminate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.stream.Collectors;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.export.OutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import net.sf.jasperreports.export.SimplePdfReportConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.disseminate.service.CoverPageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;

/**
 * This is an alternative implementation of the CoverPageService. It uses a configurable
 * <a href="https://github.com/TIBCOSoftware/jasperreports">JasperReports</a> template
 * to render the coverpage.
 */
class JRCoverPageService implements CoverPageService {

    private static final Logger LOG = LogManager.getLogger(CoverPageService.class);

    private final JasperReport jasperReport;

    JRCoverPageService(
            @Value("${citation-page.cover-template}")
            String coverTemplateLocation,

            ResourceLoader resourceLoader
    ) {
        try {
            try (var coverTemplate = resourceLoader.getResource(coverTemplateLocation).getInputStream()) {
                jasperReport
                        = JasperCompileManager.compileReport(coverTemplate);
            } catch (JRException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PDDocument renderCoverDocument(Context context, Item item) {

        var parameters = prepareParams(item);

        try {
            var jasperPrint
                    = JasperFillManager.fillReport(jasperReport, parameters);

            var exporter = new JRPdfExporter();

            exporter.setExporterInput(new SimpleExporterInput(jasperPrint));

            var exporterOutput = new PDDocumentOutputStream();

            exporter.setExporterOutput(exporterOutput);

            var reportConfig
                    = new SimplePdfReportConfiguration();
            reportConfig.setSizePageToContent(true);
            reportConfig.setForceLineBreakPolicy(false);

            var exportConfig
                    = new SimplePdfExporterConfiguration();
            exportConfig.setMetadataAuthor("dspace");
            exportConfig.setEncrypted(false);
            exportConfig.setAllowedPermissionsHint("PRINTING");

            exporter.setConfiguration(reportConfig);
            exporter.setConfiguration(exportConfig);

            exporter.exportReport();

            return exporterOutput.getPDDocument();
        } catch (JRException e) {
            throw new RuntimeException(e);
        }
    }

    protected Map<String, Object> prepareParams(Item item) {
        return item.getMetadata().stream()
                .filter(meta -> meta.getPlace() == 0)
                .map(meta -> Map.entry(meta.getMetadataField().toString(), meta.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private static class PDDocumentOutputStream implements OutputStreamExporterOutput {

        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        @Override
        public OutputStream getOutputStream() {
            return out;
        }

        @Override
        public void close() {
        }

        PDDocument getPDDocument() {
            try {
                return PDDocument.load(new ByteArrayInputStream(out.toByteArray()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
