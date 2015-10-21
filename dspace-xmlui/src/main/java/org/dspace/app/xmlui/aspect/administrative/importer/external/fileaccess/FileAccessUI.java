/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.importer.external.fileaccess;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.*;
import org.dspace.content.DCDate;
import org.dspace.importer.external.scidir.entitlement.ArticleAccess;

import java.util.Locale;

import static org.dspace.app.xmlui.wing.AbstractWingTransformer.message;

/**
 * Created by: Antoine Snyers (antoine at atmire dot com)
 * Date: 02 Oct 2015
 */
public class FileAccessUI {

    public static final Message file_access_label = message("xmlui.file-access.FileAccessUI.label");

    protected static final Message T_embargo_date = message("xmlui.file-access.FileAccessUI.embargo_date");
    protected static final Message T_embargo_date_help = message("xmlui.file-access.FileAccessUI.embargo_date_help");

    protected static final Message T_year=
            message("xmlui.Submission.submit.DescribeStep.year");
    protected static final Message T_month=
            message("xmlui.Submission.submit.DescribeStep.month");
    protected static final Message T_day=
            message("xmlui.Submission.submit.DescribeStep.day");

    public static Radio addAccessSelection(List upload, String paramName, boolean error) throws WingException {
        return addAccessSelection(upload, paramName, null, error);
    }

    public static Radio addAccessSelection(List upload, String paramName, String selected, boolean error) throws WingException {
        String[] options = {"public","restricted","embargo"};

        if (StringUtils.isBlank(selected)) {
            selected = "open_access";
        }

        Radio embargoRadio = upload.addItem("file-access", "file-access").addRadio(paramName);
        embargoRadio.setLabel(file_access_label);
        embargoRadio.setRequired(true);
        if (error) {
            embargoRadio.addError("No file access has been selected.");
        }

        for (String option : options) {
            Message optionMessage = message("xmlui.file-access.FileAccessUI.option." + option);  // xmlui.file-access.FileAccessUI.option.public, xmlui.file-access.FileAccessUI.option.restricted
            embargoRadio.addOption(selected.equals(option), option, optionMessage);
        }
        return embargoRadio;
    }

    public static Composite addEmbargoDateField(List upload, ArticleAccess openAccess) throws WingException {
        return addEmbargoDateField(upload, openAccess.getStartDate());
    }

    public static Composite addEmbargoDateField(List upload,String date) throws WingException {
        Composite fullDate = upload.addItem().addComposite("file-access-date", "file-access-date");
        Text year = fullDate.addText("file-access-date_year");
        Select month = fullDate.addSelect("file-access-date_month");
        Text day = fullDate.addText("file-access-date_day");

        fullDate.setLabel(T_embargo_date);
        fullDate.setHelp(T_embargo_date_help);

        year.setLabel(T_year);
        year.setSize(4,4);

        month.setLabel(T_month);
        month.addOption(0,"");
        for (int i = 1; i < 13; i++)
        {
            month.addOption(i,org.dspace.content.DCDate.getMonthName(i, Locale.getDefault()));
        }

        day.setLabel(T_day);
        day.setSize(2,2);

        DCDate dcDate = new DCDate(date);

        if(dcDate.toDate()!=null) {
                year.setValue(String.valueOf(dcDate.getYear()));
                month.setOptionSelected(dcDate.getMonth());

            // Check if the day field is not specified, if so then just
            // put a blank value in instead of the weird looking -1.
            if (dcDate.getDay() == -1) {
                day.setValue("");
            } else {
                day.setValue(String.valueOf(dcDate.getDay()));
            }
        }

        return fullDate;
    }
}
