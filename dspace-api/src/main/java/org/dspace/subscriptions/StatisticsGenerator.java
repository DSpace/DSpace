/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.subscriptions;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.dspace.app.metrics.CrisMetrics;
import org.dspace.app.metrics.service.CrisMetricsService;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;
import org.dspace.subscriptions.service.SubscriptionGenerator;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;


/**
 * Implementation class of SubscriptionGenerator
 * which will handle the logic of sending the emails
 * in case of statistics subscriptions
 *
 * @author Alba Aliu
 */
public class StatisticsGenerator implements SubscriptionGenerator<CrisMetrics> {
    private static final Logger log = LogManager.getLogger(StatisticsGenerator.class);

    @Autowired
    private CrisMetricsService crisMetricsService;

    @Override
    public void notifyForSubscriptions(Context c, EPerson ePerson, List<CrisMetrics> crisMetricsList) {
        // find statistics for all the subscribed objects
        try {
            // send the notification to the user
            if (ePerson != null) {
                // Get rejector's name
                String rejector = "";
                Locale supportedLocale = I18nUtil.getEPersonLocale(ePerson);
                Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "submit_reject"));
                email.addRecipient("babanaliu017@gmail.com");
                File attachement = generateExcel(crisMetricsList);
                if (attachement != null) {
                    email.addAttachment(generateExcel(crisMetricsList), "File");
                } else {
                    log.error("Error on generating excel attachement");
                }
                email.setContent("test", "test");
                email.send();
            }
        } catch (Exception ex) {
            // log this email error
            log.warn(org.dspace.core.LogManager.getHeader(c, "notify_of_reject",
                    "cannot email user" + " eperson_id" + ePerson.getID()
                            + " eperson_email" + ePerson.getEmail()));
        }
    }

    private File generateExcel(List<CrisMetrics> crisMetricsList) {
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("SubscriptionTest");
        int rowCount = 0;
        for (CrisMetrics crisMetrics : crisMetricsList) {
            // for each cris metrics add a row
            Row sheetRow = sheet.createRow(rowCount++);
            Cell cell = sheetRow.createCell(1);
            cell.setCellValue("Item name " + crisMetrics.getResource().getName());

            Cell cell2 = sheetRow.createCell(1);
            cell2.setCellValue("Type: " + crisMetrics.getMetricType());

            Cell cell3 = sheetRow.createCell(1);
            cell3.setCellValue("Metric Count: " + crisMetrics.getMetricCount());

            Cell cell4 = sheetRow.createCell(1);
            cell4.setCellValue("Delta Period 1: " + crisMetrics.getDeltaPeriod1());

            Cell cell5 = sheetRow.createCell(1);
            cell5.setCellValue("Delta Period 2: " + crisMetrics.getDeltaPeriod2());
        }
        try {
            File file = File.createTempFile("Report", "xlsx");
            FileOutputStream fileOut = new FileOutputStream(file);
            workbook.write(fileOut);
            return file;
        } catch (IOException ioException) {
            log.error(ioException.getMessage());
        }
        return null;
    }
}
