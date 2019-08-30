package org.ssu;

import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.core.ConfigurationManager;

import java.io.File;

public class LocalizedInputsReader extends org.dspace.app.util.DCInputsReader {
    private static String prevSessionLocale = "";
    private static LocalizedInputsReader inputsReader = null;

    public LocalizedInputsReader() throws DCInputsReaderException {
    }

    public DCInputsReader getInputsReader(String sessionLocale) throws DCInputsReaderException {

        if (inputsReader == null)
            inputsReader = new LocalizedInputsReader();

        if (!sessionLocale.equals(prevSessionLocale)) {
            StringBuilder fileName = new StringBuilder(ConfigurationManager.getProperty("dspace.dir")
                    + File.separator + "config" + File.separator + getFormDefFile());

            if (!"en".equals(sessionLocale))
                fileName.insert(fileName.length() - 4, "_" + sessionLocale);

            return new DCInputsReader(fileName.toString());
        }

        return new DCInputsReader();
    }
}
