package org.dspace.app.statistics;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.dspace.content.Community;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;
import org.dspace.core.Context;
import org.dspace.servicemanager.config.DSpaceConfigurationService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class ListNumberOfDownloadedBitstreamsPerInstitution {


    public static void main(String[] args) {
        Map<String, String> instCountMap = new HashMap<>();
        String year = "";
        String statistics_folder = "statistics";

        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption("h", "help", false, "help");
        options.addOption("y", "year", true, "year to list number of downloaded bistreams for");
        options.addOption("sd", "shard-destination", false, "if you want to read the statistics from the sharded folder of the given year");

        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption('h')) {
                HelpFormatter myhelp = new HelpFormatter();
                myhelp.printHelp("\nlist-number-of-downloaded-bitstreams\n", options);
                System.out.println("\nList number of downloads for the given year: list-number-of-downloaded-bitstreams -y 2021\n" +
                        "by adding the param 'shard-destination' the statistics will be run against the sharded-index (e.g. /statistics-2021/):" +
                        "list-number-of-downloaded-bitstreams -y 2021 -sd\n");
                System.exit(0);
            }

            if (line.hasOption("y")) {
                year = line.getOptionValue('y');
            }

            if (year.isEmpty()) {
                System.out.println("\n\n" + "Error - year must be provided");
                System.out.println(" (run with -h flag for details)" + "\n");
                System.exit(1);
            } else if (line.hasOption("sd")) {
                statistics_folder = statistics_folder + "-" + year;
            }

            Context context = new Context();

            DSpaceConfigurationService dSpaceConfigurationService = new DSpaceConfigurationService();
            String solrServer = dSpaceConfigurationService.getProperty("solr.server");
            System.out.println("Using solr-host: " + solrServer);
            CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
            List<Community> topCommunities = communityService.findAllTop(context);
            for (Community topCommunity : topCommunities) {
                String communityID = topCommunity.getID().toString();
                String numFound = new ListNumberOfDownloadedBitstreamsPerInstitution()
                        .findDownloadedItems(solrServer + "/" + statistics_folder + "/select?", communityID, year);
                instCountMap.put(topCommunity.getName(), numFound);
            }

            System.out.println("\n\n");
            printResult(instCountMap, year);

            // Close database connection - no updates
            context.abort();

        } catch (SQLException | ParseException e) {
            e.printStackTrace();
        }
    }

    private static void printResult(Map<String, String> instCountMap, String year) {
        System.out.println("----------------------------------------------");
        System.out.println("Statistikk for nedlastinger " + year + ":\n\n");
        for (String inst : new TreeSet<>(instCountMap.keySet())) {
            String count = instCountMap.get(inst);
            String s = inst + ": " + count;
            System.out.println(s);
        }
        System.out.println("----------------------------------------------\n");
    }

    public String findDownloadedItems(String solrHost, String topCommunityId, String year) {
        String solrQuery = generateQueryBitStream(topCommunityId, year);
        return requestSolr(solrHost, solrQuery);
    }

    private String generateQueryBitStream(String topCommunityId, String year) {
        StringBuilder solrQuery = new StringBuilder();
        solrQuery.append("q=type%3A0"); // org.dspace.core.Constants.BITSTREAM = 0;
        solrQuery.append("&");
        solrQuery.append("fq=-%28bundleName%3A%5B*+TO+*%5D-bundleName%3AORIGINAL%29");
        solrQuery.append("&");
        solrQuery.append("fq=owningItem%3A%5B*+TO+*%5D");
        solrQuery.append("&");
        solrQuery.append("fq=owningComm%3A");
        solrQuery.append(topCommunityId);
        solrQuery.append("&");
        solrQuery.append("fq=owningColl%3A%5B*+TO+*%5D");
        solrQuery.append("&");
        solrQuery.append("fq=time%3A");
        solrQuery.append(year);
        solrQuery.append("*");
        solrQuery.append("&");
        solrQuery.append("fq=-isBot%3Atrue");
        solrQuery.append("&");
        solrQuery.append("rows=10");
        return solrQuery.toString();
    }

    private String requestSolr(String solrHost, String solrQuery) {
        String uri = solrHost + solrQuery;
        //System.out.println("Using uri: " + uri);
        String numFound = "";
        HttpClient client = new HttpClient();
        GetMethod method = new GetMethod(uri);
        try {
            String responseBody = "";
            int statusCode = client.executeMethod(method);
            if (statusCode == HttpStatus.SC_OK) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(
                        method.getResponseBodyAsStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = rd.readLine()) != null) {
                    sb.append(line);
                }
                rd.close();
                responseBody = sb.toString();
                if (!responseBody.isEmpty()) {
                    numFound = parseNumFoundFromResponse(responseBody);
                }
            } else {
                numFound = "-" + statusCode;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            method.releaseConnection();
        }
        return numFound;
    }

    private String parseNumFoundFromResponse(String response) {
        String num = "-1";
        if (response.contains("numFound=\"")) {
            String temp = response.substring(response.indexOf("numFound=\"") + 10);
            num = temp.substring(0, temp.indexOf("\" start"));
        } else {
            System.out.println(response);
        }
        return num;
    }

}

