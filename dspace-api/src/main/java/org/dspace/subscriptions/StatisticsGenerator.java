/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.subscriptions;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;


/**
 * Standalone generator which handles the logic of sending the emails
 * in case of 'statistics' subscriptionType. It builds an Excel report of the
 * {@link CrisMetrics} associated with a subscriber's subscribed objects and
 * emails it as an attachment.
 *
 * @author Alba Aliu
 */
public class StatisticsGenerator {
    private static final Logger log = LogManager.getLogger(StatisticsGenerator.class);

    public void notifyForSubscriptions(Context c, EPerson ePerson, List<CrisMetrics> crisMetricsList) {
        try {
            // send the notification to the user
            if (Objects.isNull(ePerson) || crisMetricsList.isEmpty()) {
                return;
            }
            Locale supportedLocale = I18nUtil.getEPersonLocale(ePerson);
            Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "subscriptions_statistics"));
            email.addRecipient(ePerson.getEmail());
            email.addAttachment(generateExcel(crisMetricsList, c), "subscriptions.xlsx");
            email.send();
        } catch (Exception ex) {
            // log this email error
            log.warn("cannot email user" + " eperson_id" + ePerson.getID()
                    + " eperson_email" + ePerson.getEmail());
        }
    }

    private File generateExcel(List<CrisMetrics> crisMetricsList, Context c) {
        try {
            File file = File.createTempFile("Report", "xlsx");
            Workbook workbook = new HSSFWorkbook();
            Sheet sheet = workbook.createSheet("SubscriptionTest");
            CellStyle style = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            style.setFont(bold);
            Row sheetRow = sheet.createRow(1);
            Cell cellTitleName = sheetRow.createCell(1);
            cellTitleName.setCellStyle(style);
            cellTitleName.setCellValue("Title");
            Cell cellTitleType = sheetRow.createCell(2);
            cellTitleType.setCellValue("Type");
            cellTitleType.setCellStyle(style);
            Cell cellTitleCount = sheetRow.createCell(3);
            cellTitleCount.setCellValue("Value");
            Cell cellTitleDeltaP1 = sheetRow.createCell(4);
            cellTitleCount.setCellStyle(style);
            cellTitleDeltaP1.setCellValue("Last Week");
            cellTitleDeltaP1.setCellStyle(style);
            Cell cellTitleDeltaP2 = sheetRow.createCell(5);
            cellTitleDeltaP2.setCellValue("Last Month");
            cellTitleDeltaP2.setCellStyle(style);
            int rowCount = 2;
            for (CrisMetrics crisMetrics : crisMetricsList) {
                // for each cris metrics add a row
                Row sheetRowdata = sheet.createRow(rowCount);
                Cell cell = sheetRowdata.createCell(1);
                cell.setCellValue(crisMetrics.getResource().getName());

                Cell cell2 = sheetRowdata.createCell(2);
                cell2.setCellValue(crisMetrics.getMetricType());

                Cell cell3 = sheetRowdata.createCell(3);
                cell3.setCellValue(crisMetrics.getMetricCount());

                Cell cell4 = sheetRowdata.createCell(4);
                if (crisMetrics.getDeltaPeriod1() != null) {
                    cell4.setCellValue(crisMetrics.getDeltaPeriod1());
                }
                Cell cell5 = sheetRowdata.createCell(5);
                if (crisMetrics.getDeltaPeriod2() != null) {
                    cell5.setCellValue(crisMetrics.getDeltaPeriod2());
                }
                rowCount++;
            }
            autoSizeColumns(sheet);
            FileOutputStream fileOut = new FileOutputStream(file);
            workbook.write(fileOut);
            fileOut.close();
            workbook.close();
            return file;
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    private void autoSizeColumns(Sheet sheet) {
        if (sheet.getPhysicalNumberOfRows() > 0) {
            Row row = sheet.getRow(sheet.getFirstRowNum());
            for (Cell cell : row) {
                int columnIndex = cell.getColumnIndex();
                sheet.autoSizeColumn(columnIndex);
            }
        }
    }
}
