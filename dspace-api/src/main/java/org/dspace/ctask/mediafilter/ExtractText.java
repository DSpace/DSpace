/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.mediafilter;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import org.apache.commons.io.input.ReaderInputStream;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.CompositeParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.ParsingReader;
import org.apache.tika.parser.ParseContext;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.curate.Curator;
import org.dspace.curate.Mutative;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ExtractText task produces text derivatives and stores them in designated
 * bundles. Primary purpose is to expose this derivative artifact for indexing,
 * although it could also be regarded as a crude preservation transformation.
 * Roughly equivalent to the family of MediaFilters for text extraction
 * (PDF, HTML, Word, Powerpoint, etc) from org.dspace.app.mediafilter,
 * but invokes the Apache Tika framework to provide extraction services. 
 * Succeeds if one or more derivatives are created, otherwise fails.
 *
 * @author richardrodgers
 */
@Mutative
public class ExtractText extends MediaFilter
{
    private static final Logger log = LoggerFactory.getLogger(ExtractText.class);
    // list of actionable mime-types
    private final List<String> mimeList = new ArrayList<>();
    // parse context
    private final ParseContext pctx = new ParseContext();
    // text extraction parser
    private final CompositeParser teParser = new CompositeParser();

    @Override 
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);

        // check for any declared parsers - if present, they preempt Tika's defaults
        // if multiple parsers are declared having same media type capabilities,
        // last declared is the one used for that media type
        String parsers = taskProperty("filter.parsers");
        if (parsers != null)  {
            Map<MediaType, Parser> specParsers = new HashMap<>();
            for (String parser : parsers.split(",")) {
                try {
                    Parser specParser = (Parser)Class.forName(parser).newInstance();
                    for (MediaType mt : specParser.getSupportedTypes(pctx)) {
                        mimeList.add(mt.toString());
                        specParsers.put(mt, specParser);
                    }
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                    log.error("Declared parser could not be instantiated: {}", parser);
                }
            }
            teParser.setParsers(specParsers);
        }
        Parser adParser = new AutoDetectParser();
        for (MediaType mt : adParser.getSupportedTypes(pctx)) {
            mimeList.add(mt.toString());
        }
        teParser.setFallback(adParser);
    }

    @Override
    protected boolean canFilter(Item item, Bitstream bitstream) {
        try
        {
            Context context = Curator.curationContext();
            return mimeList.contains(bitstream.getFormat(context).getMIMEType());
        } catch (SQLException ex)
        {
            log.error("Unable to determine whether bitstream is filterable", ex);
            return false;
        }
    }

    @Override
    protected boolean filterBitstream(Item item, Bitstream bitstream)
        		throws AuthorizeException, IOException, SQLException {
        BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    	return createDerivative(item, bitstream,
                                new ReaderInputStream(
                                new ParsingReader(teParser,
                                        bitstreamService.retrieve(Curator.curationContext(), bitstream),
                                        new Metadata(), pctx)));
    }
}
