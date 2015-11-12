package ua.edu.sumdu.essuir.utils;

import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.core.ConfigurationManager;

import java.io.File;

/**
 * Created by Igor on 12.11.2015.
 */
public class DCInputReader extends DCInputsReader {
    private static String prevSessionLocale = "";
    private static DCInputReader inputsReader = null;

    public DCInputReader() throws DCInputsReaderException {
    }

    public DCInputReader(String fileName) throws DCInputsReaderException {
        super(fileName);
    }

    public static DCInputReader getInputsReader(String sessionLocale) {
        try {
            if (inputsReader == null)
                inputsReader = new DCInputReader();

            if (!sessionLocale.equals(prevSessionLocale)) {
                StringBuilder fileName = new StringBuilder(ConfigurationManager.getProperty("dspace.dir")
                        + File.separator + "config" + File.separator + getFormDefFile());

                if (!sessionLocale.equals("en"))
                    fileName.insert(fileName.length() - 4, "_" + sessionLocale);

                inputsReader.buildInputs(fileName.toString());

                prevSessionLocale = sessionLocale;
            }
        } catch (DCInputsReaderException e) {
            e.printStackTrace();
        }

        return inputsReader;
    }
}
