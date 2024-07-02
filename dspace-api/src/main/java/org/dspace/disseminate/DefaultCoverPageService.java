/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.disseminate;

import java.awt.Color;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.disseminate.service.CoverPageService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

public class DefaultCoverPageService implements CoverPageService {

    private static final Logger LOG = LogManager.getLogger(DefaultCoverPageService.class);

    @Autowired
    protected ConfigurationService configurationService;

    @Autowired
    protected ItemService itemService;

    private String[] header1;
    private String[] header2;
    private String[] fields;
    private String footer;
    private PDRectangle citationPageFormat = PDRectangle.LETTER;

    @PostConstruct
    void afterPropertiesSet() {
        // Configurable text/fields, we'll set sane defaults
        header1 = configurationService.getArrayProperty("citation-page.header1");
        if (header1 == null || header1.length == 0) {
            header1 = new String[] {"DSpace Institution", ""};
        }

        header2 = configurationService.getArrayProperty("citation-page.header2");
        if (header2 == null || header2.length == 0) {
            header2 = new String[] {"DSpace Repository", "http://dspace.org"};
        }

        fields = configurationService.getArrayProperty("citation-page.fields");
        if (fields == null || fields.length == 0) {
            fields = new String[] {"dc.date.issued", "dc.title", "dc.creator", "dc.contributor.author",
                "dc.publisher", "_line_", "dc.identifier.citation", "dc.identifier.uri"};
        }

        String footerConfig = configurationService.getProperty("citation-page.footer");
        if (StringUtils.isNotBlank(footerConfig)) {
            footer = footerConfig;
        } else {
            footer = "Downloaded from DSpace Repository, DSpace Institution's institutional repository";
        }

        String pageformatCfg = configurationService.getProperty("citation-page.page_format");

        if (pageformatCfg != null) {
            if (pageformatCfg.equalsIgnoreCase("A4")) {
                citationPageFormat = PDRectangle.A4;
            } else if (!pageformatCfg.equalsIgnoreCase("LETTER")) {
                LOG.info("Citation-page: Unknown page format ' " + pageformatCfg + "', using LETTER.");
            }
        }
    }

    @Override
    public PDDocument renderCoverDocument(Context context, Item item) {
        try {
            return doRenderCoverDocument(context, item);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    PDDocument doRenderCoverDocument(Context context, Item item) throws IOException {

        PDDocument document = new PDDocument();
        PDPage coverPage = new PDPage(citationPageFormat);
        document.addPage(coverPage);

        PDPageContentStream contentStream = new PDPageContentStream(document, coverPage);
        try {
            int ypos = 760;
            int xpos = 30;
            int xwidth = 550;
            int ygap = 20;

            PDFont fontHelvetica = PDType1Font.HELVETICA;
            PDFont fontHelveticaBold = PDType1Font.HELVETICA_BOLD;
            PDFont fontHelveticaOblique = PDType1Font.HELVETICA_OBLIQUE;
            contentStream.setNonStrokingColor(Color.BLACK);

            String[][] content = {header1};
            drawTable(coverPage, contentStream, ypos, xpos, content, fontHelveticaBold, 11, false);
            ypos -= (ygap);

            String[][] content2 = {header2};
            drawTable(coverPage, contentStream, ypos, xpos, content2, fontHelveticaBold, 11, false);
            ypos -= ygap;

            contentStream.addRect(xpos, ypos, xwidth, 1);
            contentStream.fill();
            contentStream.closeAndStroke();

            String[][] content3 = {{getOwningCommunity(context, item), getOwningCollection(item)}};
            drawTable(coverPage, contentStream, ypos, xpos, content3, fontHelvetica, 9, false);
            ypos -= ygap;

            contentStream.addRect(xpos, ypos, xwidth, 1);
            contentStream.fill();
            contentStream.closeAndStroke();
            ypos -= (ygap * 2);

            for (String field : fields) {
                field = field.trim();
                PDFont font = fontHelvetica;
                int fontSize = 11;
                if (field.contains("title")) {
                    fontSize = 26;
                    ypos -= ygap;
                } else if (field.contains("creator") || field.contains("contributor")) {
                    fontSize = 16;
                }

                if (field.equals("_line_")) {
                    contentStream.addRect(xpos, ypos, xwidth, 1);
                    contentStream.fill();
                    contentStream.closeAndStroke();
                    ypos -= (ygap);

                } else if (StringUtils.isNotEmpty(itemService.getMetadata(item, field))) {
                    ypos = drawStringWordWrap(coverPage, contentStream, itemService.getMetadata(item, field), xpos,
                            ypos, font, fontSize);
                }

                if (field.contains("title")) {
                    ypos -= ygap;
                }
            }

            contentStream.beginText();
            contentStream.setFont(fontHelveticaOblique, 11);
            contentStream.newLineAtOffset(xpos, ypos);
            contentStream.showText(footer);
            contentStream.endText();
        } finally {
            contentStream.close();
        }

        return document;
    }

    /**
     * Get name of owning collection
     *
     * @param item DSpace Item
     * @return owning collection name
     */
    private String getOwningCollection(Item item) {
        return item.getOwningCollection().getName();
    }

    /**
     * Get name of owning community
     *
     * @param context DSpace context
     * @param item    DSpace Item
     * @return name
     */
    private String getOwningCommunity(Context context, Item item) {
        try {
            List<Community> comms = itemService.getCommunities(context, item);
            if (comms.size() > 0) {
                return comms.get(0).getName();
            } else {
                return " ";
            }

        } catch (SQLException e) {
            LOG.error(e.getMessage());
            return e.getMessage();
        }
    }

    /**
     * @param page          page
     * @param contentStream content stream
     * @param text          text to draw
     * @param startX        x-coordinate of word
     * @param startY        y-coordinate of word
     * @param pdfFont       font
     * @param fontSize      size of font
     * @return integer
     * @throws IOException if IO error
     */
    private int drawStringWordWrap(PDPage page, PDPageContentStream contentStream, String text,
                                  int startX, int startY, PDFont pdfFont, float fontSize) throws IOException {
        float leading = 1.5f * fontSize;

        PDRectangle mediabox = page.getMediaBox();
        float margin = 72;
        float width = mediabox.getWidth() - 2 * margin;

        List<String> lines = new ArrayList<>();
        int lastSpace = -1;
        while (text.length() > 0) {
            int spaceIndex = text.indexOf(' ', lastSpace + 1);
            if (spaceIndex < 0) {
                lines.add(text);
                text = "";
            } else {
                String subString = text.substring(0, spaceIndex);
                float size = fontSize * pdfFont.getStringWidth(subString) / 1000;
                if (size > width) {
                    // So we have a word longer than the line... draw it anyways
                    if (lastSpace < 0) {
                        lastSpace = spaceIndex;
                    }
                    subString = text.substring(0, lastSpace);
                    lines.add(subString);
                    text = text.substring(lastSpace).trim();
                    lastSpace = -1;
                } else {
                    lastSpace = spaceIndex;
                }
            }
        }

        contentStream.beginText();
        contentStream.setFont(pdfFont, fontSize);
        contentStream.newLineAtOffset(startX, startY);
        int currentY = startY;
        for (String line : lines) {
            contentStream.showText(line);
            currentY -= leading;
            contentStream.newLineAtOffset(0, -leading);
        }
        contentStream.endText();
        return currentY;
    }

    private void drawTable(PDPage page, PDPageContentStream contentStream,
                          float y, float margin,
                          String[][] content, PDFont font, int fontSize, boolean cellBorders) throws IOException {
        final int rows = content.length;
        final int cols = content[0].length;
        final float rowHeight = 20f;
        final float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
        final float tableHeight = rowHeight * rows;
        final float colWidth = tableWidth / (float) cols;
        final float cellMargin = 5f;

        if (cellBorders) {
            //draw the rows
            float nexty = y;
            for (int i = 0; i <= rows; i++) {
                contentStream.moveTo(margin, nexty);
                contentStream.lineTo(margin + tableWidth, nexty);
                contentStream.stroke();
                nexty -= rowHeight;
            }

            //draw the columns
            float nextx = margin;
            for (int i = 0; i <= cols; i++) {
                contentStream.moveTo(nextx, y);
                contentStream.lineTo(nextx, y - tableHeight);
                contentStream.stroke();
                nextx += colWidth;
            }
        }

        //now add the text
        contentStream.setFont(font, fontSize);

        float textx = margin + cellMargin;
        float texty = y - 15;
        for (int i = 0; i < content.length; i++) {
            for (int j = 0; j < content[i].length; j++) {
                String text = content[i][j];
                contentStream.beginText();
                contentStream.newLineAtOffset(textx, texty);
                contentStream.showText(text);
                contentStream.endText();
                textx += colWidth;
            }
            texty -= rowHeight;
            textx = margin + cellMargin;
        }
    }
}
