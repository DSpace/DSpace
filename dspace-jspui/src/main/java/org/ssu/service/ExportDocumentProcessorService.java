package org.ssu.service;

import org.apache.poi.xwpf.usermodel.*;
import org.dspace.content.Item;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.stereotype.Service;
import org.ssu.service.localization.AuthorsCache;

import javax.annotation.Resource;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
public class ExportDocumentProcessorService {
    Locale ukrainianLocale = Locale.forLanguageTag("uk");

    @Resource
    private AuthorsService authorsService;

    @Resource
    private ItemService itemService;

    public XWPFDocument createDocument(String author, List<Item> publications) {
        XWPFDocument document = new XWPFDocument();
        CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
        CTPageMar pageMar = sectPr.addNewPgMar();
        pageMar.setLeft(BigInteger.valueOf(1020L));
        pageMar.setTop(BigInteger.valueOf(455L));
        pageMar.setRight(BigInteger.valueOf(455L));
        pageMar.setBottom(BigInteger.valueOf(455L));

        createTitle(authorsService.getAuthorLocalization(author).getFormattedAuthorData("%s %s", ukrainianLocale), document);
        createPublicationsTable(document, publications);
        document.createParagraph().setSpacingAfter(100);
        createBottomTable(document);
        return document;
    }

    private void createHeaderRun(XWPFDocument document, ParagraphAlignment alignment, boolean bold, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(alignment);
        XWPFRun run = paragraph.createRun();
        paragraph.setSpacingAfter(0);
        run.setBold(bold);
        run.setFontFamily("Times New Roman");
        run.getCTR().getRPr().getRFonts().setHAnsi("Times New Roman");
        run.setFontSize(14);
        run.setText(text);
    }

    private void createTitle(String author, XWPFDocument document) {
        createHeaderRun(document, ParagraphAlignment.CENTER, true, "СПИСОК");
        createHeaderRun(document, ParagraphAlignment.CENTER, true, "навчально-методичних та наукових праць");
        createHeaderRun(document, ParagraphAlignment.CENTER, false, author.replaceAll(",", ""));
        document.createParagraph();
    }

    private XWPFParagraph tableCellParagraph(XWPFParagraph paragraph, String text, ParagraphAlignment alignment, int fontSize) {
        paragraph.setAlignment(alignment);

        XWPFRun run = paragraph.createRun();
        paragraph.setSpacingAfter(0);
        paragraph.setIndentationFirstLine(0);
        paragraph.setSpacingAfterLines(0);
        paragraph.setSpacingBefore(0);
        paragraph.setSpacingBeforeLines(0);
        run.setFontSize(fontSize);
        run.setFontFamily("Times New Roman");
        run.getCTR().getRPr().getRFonts().setHAnsi("Times New Roman");
        run.setText(text);
        return paragraph;
    }

    private void processRow(XWPFTable table, int rowIndex, String[] text, ParagraphAlignment alignment, int fontSize) {
        XWPFTableRow row = table.getRow(rowIndex);
        for (int cellIndex = 0; cellIndex < text.length; cellIndex++) {
            XWPFTableCell cell = row.getCell(cellIndex);
            tableCellParagraph(cell.addParagraph(), text[cellIndex], alignment, fontSize);
        }
    }

    private void setTableProperties(XWPFTable table, Integer[] width) {
        table.setWidth(Arrays.stream(width).reduce(0, (a, b) -> a + b));
        table.getCTTbl().addNewTblGrid().addNewGridCol().setW(BigInteger.valueOf(width[0]));
        for (int col = 1; col < width.length; col++) {
            table.getCTTbl().getTblGrid().addNewGridCol().setW(BigInteger.valueOf(width[col]));
        }

        for (int i = 0; i < width.length; i++) {
            CTTblWidth tblWidth = table.getRow(0).getCell(i).getCTTc().addNewTcPr().addNewTcW();
            tblWidth.setW(BigInteger.valueOf(width[i]));
            tblWidth.setType(STTblWidth.DXA);
        }
    }

    private void createPublicationsTable(XWPFDocument document, List<Item> publications) {
        XWPFTable table = document.createTable(publications.size() + 1, 6);
        Integer[] width = {540, 2200, 1250, 3000, 1400, 2300};
        setTableProperties(table, width);

        processRow(table, 0, new String[]{"№ з/п", "Назва", "Характер роботи", "Вихідні дані", "Обсяг (у сторінках)/авторський доробок", "Співавтори"}, ParagraphAlignment.CENTER, 14);
        for (int index = 0; index < publications.size(); index++) {
            String localizedAuthors = itemService.extractAuthorListForItem(publications.get(index))
                    .stream()
                    .map(author -> author.getFormattedAuthorData("%s %s", ukrainianLocale))
                    .collect(Collectors.joining(";\r\n"));

            String[] rowData = {Integer.toString(index + 1),
                    publications.get(index).getName(),
                    itemService.getItemTypeLocalized(publications.get(index), ukrainianLocale),
                    itemService.getCitationForItem(publications.get(index)),
                    "",
                    localizedAuthors};
            processRow(table, index + 1, rowData, ParagraphAlignment.CENTER, 14);
        }
    }

    private void createBottomTable(XWPFDocument document) {
        XWPFTable table = document.createTable(5, 3);
        Integer[] width = {3000, 2500, 3500};
        setTableProperties(table, width);

        CTTblPr tblpro = table.getCTTbl().getTblPr();

        CTTblBorders borders = tblpro.addNewTblBorders();
        borders.addNewBottom().setVal(STBorder.NONE);
        borders.addNewLeft().setVal(STBorder.NONE);
        borders.addNewRight().setVal(STBorder.NONE);
        borders.addNewTop().setVal(STBorder.NONE);

        borders.addNewInsideH().setVal(STBorder.NONE);
        borders.addNewInsideV().setVal(STBorder.NONE);

        table.getRow(0).setHeight(30);
        processRow(table, 0, new String[]{"Автор або здобувач вченого звання (наукового ступеня)", "\r\n\r\n     ________________\r\n\t\t\t(підпис)", "\r\n\r\n     __________________________\r\n                           (прізвище, ініціали)"}, ParagraphAlignment.LEFT, 12);

        table.getRow(1).setHeight(30);
        processRow(table, 1, new String[]{"________________________\r\n\t\t\t(число, місяць, рік)", "", ""}, ParagraphAlignment.LEFT, 12);

        table.getRow(2).setHeight(30);
        processRow(table, 2, new String[]{"Засвідчено:", "", ""}, ParagraphAlignment.LEFT, 12);

        table.getRow(3).setHeight(30);
        processRow(table, 3, new String[]{"Завідуючий (начальник) кафедрою", "\r\n\r\n     ________________\r\n\t\t\t(підпис)", "\r\n\r\n     __________________________\r\n                           (прізвище, ініціали)"}, ParagraphAlignment.LEFT, 12);

        table.getRow(4).setHeight(30);
        processRow(table, 4, new String[]{"Вчений секретар", "\r\n\r\n     ________________\r\n\t\t\t(підпис)", "\r\n\r\n     __________________________\r\n                           (прізвище, ініціали)"}, ParagraphAlignment.LEFT, 12);
    }
}