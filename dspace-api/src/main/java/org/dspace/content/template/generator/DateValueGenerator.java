/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template.generator;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;

/**
 * Generates a value based on current date.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class DateValueGenerator implements TemplateValueGenerator {

    /**
     * Generates a value based on current date, custom date template has to be provided as extraParams. If no
     * template is provided, date is set in {@code java.util.Date} standard string representation
     *
     * @param context       application Context
     * @param targetItem          target item
     * @param templateItem  template item
     * @param extraParams   pattern (i.e. yyyy-MM-dd) to be used to represent generated date
     * @return
     */
    @Override
    public List<MetadataValueVO> generator(Context context, Item targetItem, Item templateItem, String extraParams) {
        return Arrays.asList(new MetadataValueVO(buildValue(extraParams)));
    }

    private String buildValue(String extraParams) {

        //FIXME : This logic has to be uncommented and adapted
//        String[] params = StringUtils.split(extraParams, "\\.");
//        String formatter = "";


//        Date date = new Date();
//        DateMathParser dmp = new DateMathParser();
//        String value = "";
//        if (params != null && params.length > 1) {
//            operazione = params[0];
//            formatter = params[1];
//            try {
//                date = dmp.parseMath(operazione);
//            } catch (ParseException e) {
//                log.error(e.getMessage(), e);
//            } finally {
//                DateFormat df = new SimpleDateFormat(formatter);
//                value = df.format(date);
//            }
//        } else if (params.length == 1) {
//            formatter = params[0];
//            DateFormat df = new SimpleDateFormat(formatter);
//            value = df.format(date);
//        } else {
//            value = date.toString();
//        }
//        return value;

        String[] params = StringUtils.split(extraParams, "\\.");
        final Date date = new Date();
        if (params.length == 1) {
            DateFormat df = new SimpleDateFormat(params[0]);
            return df.format(date);
        }
        return date.toString();
    }
}
