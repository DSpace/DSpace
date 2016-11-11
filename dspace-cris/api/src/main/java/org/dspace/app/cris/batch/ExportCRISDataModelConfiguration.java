/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.batch;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;
import org.dspace.app.cris.service.ApplicationService;
import org.dspace.app.cris.util.ImportExportUtils;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.springframework.orm.hibernate4.HibernateTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;

import jxl.read.biff.BiffException;
import jxl.write.WriteException;

public class ExportCRISDataModelConfiguration
{
    private static Logger log = Logger
            .getLogger(ExportCRISDataModelConfiguration.class);

    private static boolean append = false;

    public static void main(String[] args)
            throws ParseException, SQLException, BiffException, IOException,
            InstantiationException, IllegalAccessException, WriteException, IllegalArgumentException, InvocationTargetException
    {
		String fileExcel = null;
		CommandLineParser parser = new PosixParser();
		Options options = new Options();
		options.addOption("f", "excel", true,
				"Output Excel file");

		CommandLine line = parser.parse(options, args);
        if (line.hasOption('h') || !line.hasOption('f'))
        {
			HelpFormatter myhelp = new HelpFormatter();
            myhelp.printHelp(
                    "ExportCRISDataModelConfiguration (BETA version use with caution!!!)\n",
                    options);
			System.exit(0);
		}

        if (line.hasOption('f'))
        {
			fileExcel = line.getOptionValue('f');
		}

		Context dspaceContext = new Context();
		dspaceContext.setIgnoreAuthorization(true);
		DSpace dspace = new DSpace();
        ApplicationService applicationService = dspace.getServiceManager()
                .getServiceByName("applicationService",
				ApplicationService.class);

        OutputStream out = new FileOutputStream(fileExcel);
        ImportExportUtils.exportConfiguration(applicationService, out);
        
        PlatformTransactionManager transactionManager = (PlatformTransactionManager) dspace
                .getServiceManager().getServiceByName("transactionManager",
                        HibernateTransactionManager.class);
		DefaultTransactionAttribute transactionAttribute = new DefaultTransactionAttribute(
				TransactionDefinition.PROPAGATION_REQUIRED);

		TransactionStatus status = transactionManager
                .getTransaction(transactionAttribute);

        transactionManager.rollback(status);
    }
}
