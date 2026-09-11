/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.io.ScratchFile;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.dspace.app.util.XMLUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xml.sax.SAXException;

/**
 * Generates a PDF coverpage.
 *
 * The generation is a two step process:
 * Step 1 is to generate a HTML layout using a thymeleaf template.
 * Step 2 is to render the HTML to PDF.
 */
public class PdfGenerator {

    /**
     * Render a HTML coverpage.
     *
     * @param templateName the name of the thymeleaf template
     * @param variables    dynamic content for the template
     * @return a rendered HTML coverpage
     */
    public String parseTemplate(String templateName, Map<String, String> variables) {
        var templateResolver = new ClassLoaderTemplateResolver(getClass().getClassLoader());
        templateResolver.setSuffix(".html");
        templateResolver.setOrder(2);

        templateResolver.setTemplateMode(TemplateMode.HTML);

        var templateEngine = new TemplateEngine();

        templateEngine.addTemplateResolver(templateResolver);

        var context = new Context();
        context.setVariables(fixTypes(variables));

        return templateEngine.process(templateName, context);
    }

    /**
     * Render a HTML coverpage directly to file.
     *
     * @param templateName the name of the thymeleaf template
     * @param variables    dynamic content for the template
     * @param outputFile   target file path
     * @return the same outputFile path
     * @throws IOException if writing template fails
     */
    public Path parseTemplate(String templateName, Map<String, String> variables, Path outputFile) throws IOException {
        Path parent = outputFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        var templateResolver = new ClassLoaderTemplateResolver(getClass().getClassLoader());
        templateResolver.setSuffix(".html");
        templateResolver.setOrder(2);

        templateResolver.setTemplateMode(TemplateMode.HTML);

        var templateEngine = new TemplateEngine();
        templateEngine.addTemplateResolver(templateResolver);

        var context = new Context();
        context.setVariables(fixTypes(variables));

        try (var writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            templateEngine.process(templateName, context, writer);
        }

        return outputFile;
    }

    private static Map<String, Object> fixTypes(Map<String, String> variables) {
        return variables.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), (Object) entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * render a HTML coverpage to a file
     *
     * @param html   the coverpage HTML
     * @param toFile file to write to
     */
    public void generateToFile(String html, File toFile) {
        try (var out = new FileOutputStream(toFile)) {
            generate(html, out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * render a HTML coverpage to a PDDocument (pdfbox)
     *
     * @param html the coverpage HTML
     * @return resulting pdfbox PDDocument
     */
    public PDDocument generate(String html) {
        try {
            Path htmlFile = Files.createTempFile("citation-cover-", ".html");
            Files.writeString(htmlFile, html, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                return generate(htmlFile);
            } finally {
                Files.deleteIfExists(htmlFile);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * render a HTML coverpage file to a PDDocument (pdfbox)
     *
     * @param htmlFile path to rendered HTML template
     * @return resulting pdfbox PDDocument
     */
    public PDDocument generate(Path htmlFile) {
        Path tempPdfFile = null;
        DeleteOnCloseRandomAccessRead pdfSource = null;
        try {
            tempPdfFile = Files.createTempFile("citation-cover-", ".pdf");
            try (var out = Files.newOutputStream(tempPdfFile, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
                generate(htmlFile, out);
            }
            pdfSource = new DeleteOnCloseRandomAccessRead(tempPdfFile);
            PDDocument document = Loader.loadPDF(pdfSource,
                    () -> new ScratchFile(MemoryUsageSetting.setupTempFileOnly()));
            pdfSource = null;
            return document;
        } catch (IOException e) {
            if (pdfSource != null) {
                try {
                    pdfSource.close();
                } catch (IOException closeException) {
                    e.addSuppressed(closeException);
                }
            } else if (tempPdfFile != null) {
                try {
                    Files.deleteIfExists(tempPdfFile);
                } catch (IOException deleteException) {
                    e.addSuppressed(deleteException);
                }
            }
            throw new RuntimeException(e);
        }
    }

    private void generate(String html, OutputStream out) {
        try {
            var renderer = new ITextRenderer();
            DocumentBuilder builder = XMLUtils.getDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)));
            renderer.setDocument(doc, null);
            renderer.layout();
            renderer.createPDF(out);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void generate(Path htmlFile, OutputStream out) {
        try {
            var renderer = new ITextRenderer();
            DocumentBuilder builder = XMLUtils.getDocumentBuilder();
            try (var in = Files.newInputStream(htmlFile)) {
                Document doc = builder.parse(in);
                renderer.setDocument(doc, null);
                renderer.layout();
                renderer.createPDF(out);
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
