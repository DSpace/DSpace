package edu.umd.lib.dspace.content;

/**
* The cli version of the {@link EmbargoListExport} script
*/
public class EmbargoListExportCli extends EmbargoListExport {

   @Override
   protected String getFileNameOrExportFile() {
       return commandLine.getOptionValue('f');
   }
}
