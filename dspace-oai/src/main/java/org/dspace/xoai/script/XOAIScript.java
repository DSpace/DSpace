package org.dspace.xoai.script;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.ParseException;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.utils.DSpace;
import org.dspace.xoai.app.XOAI;

/**
 * Script that just calls the XOAI CLI, so it can be used from the DSpace interface as a 'Process'.
 * @see XOAI
 */
public class XOAIScript extends DSpaceRunnable<XOAIScriptConfiguration> {
    private List<String> args = new ArrayList<>();

    /**
     * Helper class to also log the 'println' calls to a DSpaceRunnableHandler, so it shows in the process log.
     */
    private class DSpaceRunnableHandlerPrintStream extends PrintStream {
        private final DSpaceRunnableHandler handler;

        public DSpaceRunnableHandlerPrintStream(PrintStream stream, DSpaceRunnableHandler handler) {
            super(stream);
            this.handler = handler;
        }

        @Override
        public void println(String x) {
            super.println(x);
            handler.logInfo(x);
        }
    }

    @Override
    public XOAIScriptConfiguration getScriptConfiguration() {
        return new DSpace().getServiceManager().getServiceByName("xoai-script", XOAIScriptConfiguration.class);
    }

    @Override
    public void setup() throws ParseException {
        String action = XOAIScriptOptions.getAction(commandLine);
        boolean clearIndex = commandLine.hasOption("c");
        boolean verbose = commandLine.hasOption("v");
        boolean help = commandLine.hasOption("h");

        args.clear();
        if (action == null || help) {
            args.add("-h");
        } else {
            args.add(action);
            if (clearIndex) {
                args.add("-c");
            }
            if (verbose) {
                args.add("-v");
            }
        }
    }

    @Override
    public void internalRun() throws Exception {
        PrintStream stream = System.out;
        try {
            if (!args.contains("-h")) {
                // Changes the 'PrintStream' of 'System.out' so the output of the XOAI is shown in the process log.
                // No need to change it for the 'help' command.
                System.setOut(new DSpaceRunnableHandlerPrintStream(stream, handler));
            }
            // Just calls the original XOAI command with the arguments.
            XOAI.main(args.toArray(new String[0]));
        } finally {
            // Restores the original 'PrintStream' of 'System.out'.
            System.setOut(stream);
        }
    }
}
