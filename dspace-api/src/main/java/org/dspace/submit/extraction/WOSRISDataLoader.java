/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.extraction;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import gr.ekt.bte.core.DataLoadingSpec;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.dataloader.FileDataLoader;
import gr.ekt.bte.exceptions.EmptySourceException;
import gr.ekt.bte.exceptions.MalformedSourceException;
import gr.ekt.bte.record.MapRecord;
import gr.ekt.bteio.loaders.EndnoteDataLoader;

import org.apache.log4j.Logger;

/**
 * Based on {@link gr.ekt.bteio.loaders.EndnoteDataLoader} implementation
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class WOSRISDataLoader extends FileDataLoader {

    private static Logger logger_ = Logger.getLogger(EndnoteDataLoader.class);
    private BufferedReader reader_;
    private Map<String, String> field_map_;

    public WOSRISDataLoader() {
        super();
        reader_ = null;
        field_map_ = null;
    }

    public WOSRISDataLoader(String filename, Map<String, String> field_map) throws EmptySourceException {
        super(filename);
        field_map_ = field_map;
        openReader();
    }

    @Override
    public RecordSet getRecords() throws MalformedSourceException {
        if (reader_ == null) {
            throw new EmptySourceException("File " + filename + " could not be opened");
        }
        RecordSet ret = new RecordSet();

        try {
            String line;

            // Read the first two lines. They should contain the tags
            // FN and VR in that order.
            line = reader_.readLine();

            // We have reached the end of file
            if (line == null) {
                return ret;
            }
            if (!line.startsWith("FN")) {
                throw new MalformedSourceException(
                        "File " + filename + " is not a valid Endnote file: First line does not contain \"FN\" tag.");
            }
            line = reader_.readLine();
            if (!line.startsWith("VR")) {
                throw new MalformedSourceException(
                        "File " + filename + " is not a valid Endnote file: Second line does not contain \"VR\" tag.");
            }

            MapRecord current_record = new MapRecord();
            Pattern endnote_pattern = Pattern.compile("(^[A-Z]{2}) ?(.*)$");
            String current_value = null;
            String current_tag = null;
            String current_field = null;
            int line_no = 2;

            while ((line = reader_.readLine()) != null) {
                line_no++;
                line = line.trim();
                // Ignore empty lines
                if (line.isEmpty() || line.equals("")) {
                    continue;
                }
                Matcher endnote_matcher = endnote_pattern.matcher(line);
                if (endnote_matcher.matches()) {
                    current_tag = endnote_matcher.group(1);
                    // We found the end record tag. Add the record to
                    // the record set, create a new record and continue
                    // with the next iteration.
                    if (current_tag.equals("ER")) {
                        ret.addRecord(current_record);
                        current_record = new MapRecord();
                        current_value = null;
                        current_tag = null;
                        current_field = null;
                        continue;
                    }

                    // End of file reached. Break out of the loop
                    if (current_tag.equals("EF")) {
                        break;
                    }
                    current_field = field_map_.get(current_tag);
                    current_value = endnote_matcher.group(2);
                } else {
                    current_value = line;
                }

                if (current_field == null && current_tag == null) {
                    logger_.debug("Parse error on line " + line_no + ": Tag expected\n" + line);
                    throw new MalformedSourceException("Parse error on line " + line_no + ": Tag expected\n" + line);
                }

                if (current_value == null) {
                    logger_.debug("Parse error on line " + line_no + ": Value expected.");
                    throw new MalformedSourceException("Parse error on line " + line_no + ": Value expected.");
                }
                if (current_field != null) {
                    current_record.addValue(current_field, new StringValue(current_value));
                }
            }
        } catch (IOException e) {
            logger_.info("Error while reading from file " + filename);
            throw new MalformedSourceException("Error while reading from file " + filename);
        }
        return ret;
    }

    @Override
    public RecordSet getRecords(DataLoadingSpec spec) throws MalformedSourceException {
        return getRecords();
    }

    @Override
    public void setFilename(String filename) {
        this.filename = filename;
        try {
            openReader();
        } catch (EmptySourceException e) {
            logger_.info("Could not open file " + filename);
            reader_ = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        reader_.close();
    }

    private void openReader() throws EmptySourceException {
        try {
            reader_ = new BufferedReader(new FileReader(filename));
        } catch (FileNotFoundException e) {
            throw new EmptySourceException("File " + filename + " not found");
        }
    }

    /**
     * @return the field_map_
     */
    public Map<String, String> getFieldMap() {
        return field_map_;
    }

    /**
     * @param field_map_
     *            the field_map_ to set
     */
    public void setFieldMap(Map<String, String> field_map_) {
        this.field_map_ = field_map_;
    }
}
