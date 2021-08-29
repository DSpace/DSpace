/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.subscriptions;


import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.discovery.IndexableObject;
import org.dspace.eperson.EPerson;
import org.dspace.subscriptions.service.SubscriptionGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;


/**
 * Implementation class of SubscriptionGenerator
 * which will handle the logic of sending the emails
 * in case of content subscriptions
 *
 * @author Alba Aliu
 */

public class ContentGenerator implements SubscriptionGenerator<IndexableObject> {
    private final Logger log = org.apache.logging.log4j.LogManager.getLogger(ContentGenerator.class);

    @Override
    public void notifyForSubscriptions(Context c, EPerson ePerson, List<IndexableObject> indexableObjects) {
        try {
            // send the notification to the user
            if (ePerson != null) {
                // Get rejector's name
                String rejector = getEPersonName(ePerson);
                Locale supportedLocale = I18nUtil.getEPersonLocale(ePerson);
                Email email = Email.getEmail(I18nUtil.getEmailFilename(supportedLocale, "submit_reject"));
                email.addRecipient(ePerson.getEmail());
                email.addAttachment(generateExcel(indexableObjects), "Attachment");
                email.setContent("Subscriptions", "");
                email.send();
            } else {
                // DO nothing
            }
        } catch (Exception ex) {
            // log this email error
            log.warn(LogManager.getHeader(c, "notify_of_reject",
                    "cannot email user" + " eperson_id" + ePerson.getID()
                            + " eperson_email" + ePerson.getEmail()));
        }
    }

    public String getEPersonName(EPerson ePerson) {
        String submitter = ePerson.getFullName();

        submitter = submitter + "(" + ePerson.getEmail() + ")";

        return submitter;
    }

    private File generateExcel(List<IndexableObject> indexableObjects) {
        Workbook workbook = new HSSFWorkbook();
        Sheet sheet = workbook.createSheet("Subscription Test");
        int rowCount = 0;
        // datas are ordered communities -> collections -> items
        boolean comm = false;
        boolean coll = false;
        boolean items = false;
        for (IndexableObject indexableObject : indexableObjects) {
            // for each object add a row
            if (indexableObject.getType().equals(Community.class.getSimpleName()) || !comm) {
                Row sheetRow = sheet.createRow(rowCount++);
                Cell cell = sheetRow.createCell(1);
                cell.setCellValue("List of changed communities");
                comm = true;
                rowCount++;
            }
            if (indexableObject.getType().equals(Collection.class.getSimpleName()) || !coll) {
                Row sheetRow = sheet.createRow(rowCount++);
                Cell cell = sheetRow.createCell(1);
                cell.setCellValue("List of changed collections");
                coll = true;
                rowCount++;
            }
            if (indexableObject.getType().equals(Item.class.getSimpleName()) || !items) {
                Row sheetRow = sheet.createRow(rowCount++);
                Cell cell = sheetRow.createCell(1);
                cell.setCellValue("List of changed items");
                items = true;
                rowCount++;
            }
            Row sheetRow = sheet.createRow(rowCount);
            Cell cell = sheetRow.createCell(1);
            cell.setCellValue("Name");
            rowCount++;
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
