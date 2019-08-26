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
import gr.ekt.bte.core.Value;
import gr.ekt.bte.dataloader.FileDataLoader;
import gr.ekt.bte.exceptions.EmptySourceException;
import gr.ekt.bte.exceptions.MalformedSourceException;
import gr.ekt.bte.record.MapRecord;

import org.apache.log4j.Logger;

/**
 * Based on {@link gr.ekt.bteio.loaders.RISDataLoader} implementation
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class EndnoteDataLoader extends FileDataLoader {

    private static Logger logger_ = Logger.getLogger(EndnoteDataLoader.class);
    private BufferedReader reader_;
    private Map<String, String> field_map_;

    public EndnoteDataLoader() {
        super();
        reader_ = null;
        field_map_ = null;
    }

    public EndnoteDataLoader(String filename, Map<String, String> field_map) throws EmptySourceException {
        super(filename);
        field_map_ = field_map;
        openReader();
    }

    @Override
    public RecordSet getRecords() throws MalformedSourceException {
        if (reader_ == null) {
            throw new EmptySourceException("Input file is not open");
        }
       RecordSet records = new RecordSet();

        try {
            String line;
            boolean in_record = false;
            int line_cnt = 0;
            int inRecordCount = 0;
            MapRecord rec = null;

            String ris_tag = null;
            while ((line = reader_.readLine()) != null) {
                line_cnt++;

                // Ignore empty lines
                if (line.isEmpty() || line.equals("") || line.matches("^\\s*$")) {
                    continue;
                }
                Pattern endnote_pattern = Pattern.compile("(^%[A-Z|0-9]) ?(.*)$");
                Matcher ris_matcher = endnote_pattern.matcher(line);
                Value val;
                if (ris_matcher.matches()) {
                    ris_tag = ris_matcher.group(1);

                    if (ris_tag.equals("%0")) {
                        if (inRecordCount > 0) {
                            in_record = false;
                            records.addRecord(rec);
                            rec = null;
                        }
                    }
                    if (!in_record) {
                        // The first tag of the record should be "TY". If we
                        // encounter it we should create a new record.
                        if (ris_tag.equals("%0")) {
                            in_record = true;
                            inRecordCount++;
                            rec = new MapRecord();
                        } else {
                            logger_.debug("Line: " + line_cnt + " in file " + filename + " should contain tag \"%0\"");
                            throw new MalformedSourceException(
                                    "Line: " + line_cnt + " in file " + filename + " should contain tag \"%0\"");
                        }
                    }

                    // If there is no mapping for the current tag we do not
                    // know what to do with it, so we ignore it.
                    if (!field_map_.containsKey(ris_tag)) {
                        logger_.warn("Tag \"" + ris_tag + "\" is not in the field map. Ignoring");
                        continue;
                    }
                    val = new StringValue(ris_matcher.group(2));
                } else {
                    val = new StringValue(line);
                }
                String field = field_map_.get(ris_tag);
                if (field != null) {
                    rec.addValue(field, val);
                }
            }
            if(rec!= null ) {
            	records.addRecord(rec);
            }
        } catch (IOException e) {
            logger_.info("Error while reading from file " + filename);
            throw new MalformedSourceException("Error while reading from file " + filename);
        }

        return records;
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