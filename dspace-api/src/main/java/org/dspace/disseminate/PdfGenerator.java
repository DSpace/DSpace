/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

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
     * @param variables dynamic content for the template
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

    private static Map<String, Object> fixTypes(Map<String, String> variables) {
        return variables.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), (Object) entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * render a HTML coverpage to a file
     *
     * @param html the coverpage HTML
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
        try (var out = new ByteArrayOutputStream()) {
            generate(html, out);
            return PDDocument.load(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void generate(String html, OutputStream out) {
        var renderer = new ITextRenderer();

        renderer.setDocumentFromString(html);
        renderer.layout();
        renderer.createPDF(out);
    }
}
