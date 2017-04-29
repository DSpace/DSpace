/**
 * Copyright 2012 Lyncode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Development @ Lyncode <development@lyncode.com>
 * @version 3.1.0
 */

package com.lyncode.xoai.dataprovider;

import com.lyncode.xoai.dataprovider.core.OAIParameters;
import com.lyncode.xoai.dataprovider.core.XOAIContext;
import com.lyncode.xoai.dataprovider.core.XOAIManager;
import com.lyncode.xoai.dataprovider.data.internal.ItemRepositoryHelper;
import com.lyncode.xoai.dataprovider.data.internal.SetRepositoryHelper;
import com.lyncode.xoai.dataprovider.exceptions.*;
import com.lyncode.xoai.dataprovider.handlers.*;
import com.lyncode.xoai.dataprovider.services.api.*;
import com.lyncode.xoai.dataprovider.services.impl.BaseDateProvider;
import com.lyncode.xoai.dataprovider.services.impl.DefaultResumptionTokenFormatter;
import com.lyncode.xoai.dataprovider.xml.XmlOutputContext;
import com.lyncode.xoai.dataprovider.xml.oaipmh.OAIPMH;
import com.lyncode.xoai.dataprovider.xml.oaipmh.OAIPMHtype;
import com.lyncode.xoai.dataprovider.xml.oaipmh.RequestType;
import com.lyncode.xoai.dataprovider.xml.oaipmh.VerbType;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.xml.stream.XMLStreamException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Development @ Lyncode <development@lyncode.com>
 * @version 3.1.0
 */
public class OAIDataProvider {
    private static Logger log = LogManager.getLogger(OAIDataProvider.class);
    private static DateProvider formatter = new BaseDateProvider();

    public static void setDateFormatter(DateProvider newFormat) {
        formatter = newFormat;
    }

    private XOAIManager manager;

    private RepositoryConfiguration repositoryConfiguration;
    private SetRepositoryHelper setRepository;
    private ItemRepositoryHelper itemRepository;
    private List<String> compressions;
    private XOAIContext xoaiContext;
    private ResumptionTokenFormatter resumptionTokenFormatter;

    private GetRecordHandler getRecordHandler;
    private IdentifyHandler identifyHandler;
    private ListIdentifiersHandler listIdentifiersHandler;
    private ListMetadataFormatsHandler listMetadataFormatsHandler;
    private ListRecordsHandler listRecordsHandler;
    private ListSetsHandler listSetsHandler;
    private ErrorHandler errorHandler;


    public OAIDataProvider(XOAIManager manager, String contextUrl, RepositoryConfiguration identify,
                           SetRepository setRepository,
                           ItemRepository itemRepository)
            throws InvalidContextException {
        log.debug("ContextConfiguration chosen: " + contextUrl);

        this.manager = manager;

        xoaiContext = this.manager.getContextManager().getOAIContext(contextUrl);

        if (xoaiContext == null)
            throw new InvalidContextException("ContextConfiguration \"" + contextUrl
                    + "\" does not exist");

        repositoryConfiguration = identify;
        this.setRepository = new SetRepositoryHelper(setRepository);
        this.itemRepository = new ItemRepositoryHelper(itemRepository);
        compressions = new ArrayList<String>();
        resumptionTokenFormatter = new DefaultResumptionTokenFormatter();

        getRecordHandler = new GetRecordHandler(formatter, xoaiContext, this.itemRepository, repositoryConfiguration);
        identifyHandler = new IdentifyHandler(formatter, repositoryConfiguration, compressions);
        listMetadataFormatsHandler = new ListMetadataFormatsHandler(formatter, this.itemRepository, xoaiContext);
        listRecordsHandler = new ListRecordsHandler(formatter, this.manager.getMaxListRecordsSize(), this.setRepository, this.itemRepository, repositoryConfiguration, xoaiContext, resumptionTokenFormatter);
        listIdentifiersHandler = new ListIdentifiersHandler(formatter, this.manager.getMaxListIdentifiersSize(), this.setRepository, this.itemRepository, repositoryConfiguration, xoaiContext, resumptionTokenFormatter);
        listSetsHandler = new ListSetsHandler(formatter, this.manager.getMaxListSetsSize(), this.setRepository, xoaiContext, resumptionTokenFormatter);
        errorHandler = new ErrorHandler();
    }

    public OAIDataProvider(XOAIManager manager, String contexturl, RepositoryConfiguration identify,
                           SetRepository setRepository,
                           ItemRepository itemRepository,
                           ResumptionTokenFormatter format)
            throws InvalidContextException {
        log.debug("ContextConfiguration chosen: " + contexturl);

        this.manager = manager;

        xoaiContext = this.manager.getContextManager().getOAIContext(contexturl);

        if (xoaiContext == null)
            throw new InvalidContextException("ContextConfiguration \"" + contexturl
                    + "\" does not exist");
        repositoryConfiguration = identify;
        this.setRepository = new SetRepositoryHelper(setRepository);
        this.itemRepository = new ItemRepositoryHelper(itemRepository);
        compressions = new ArrayList<String>();
        resumptionTokenFormatter = format;

        getRecordHandler = new GetRecordHandler(formatter, xoaiContext, this.itemRepository, repositoryConfiguration);
        identifyHandler = new IdentifyHandler(formatter, repositoryConfiguration, compressions);
        listMetadataFormatsHandler = new ListMetadataFormatsHandler(formatter, this.itemRepository, xoaiContext);
        listRecordsHandler = new ListRecordsHandler(formatter, this.manager.getMaxListRecordsSize(), this.setRepository, this.itemRepository, repositoryConfiguration, xoaiContext, resumptionTokenFormatter);
        listIdentifiersHandler = new ListIdentifiersHandler(formatter, this.manager.getMaxListIdentifiersSize(), this.setRepository, this.itemRepository, repositoryConfiguration, xoaiContext, resumptionTokenFormatter);
        listSetsHandler = new ListSetsHandler(formatter, this.manager.getMaxListSetsSize(), this.setRepository, xoaiContext, resumptionTokenFormatter);
        errorHandler = new ErrorHandler();
    }

    public OAIDataProvider(XOAIManager manager, String contexturl, RepositoryConfiguration identify,
                           SetRepository setRepository,
                           ItemRepository itemRepository, List<String> compressions)
            throws InvalidContextException {
        this.manager = manager;

        xoaiContext = this.manager.getContextManager().getOAIContext(contexturl);

        if (xoaiContext == null)
            throw new InvalidContextException();
        repositoryConfiguration = identify;
        this.setRepository = new SetRepositoryHelper(setRepository);
        this.itemRepository = new ItemRepositoryHelper(itemRepository);
        this.compressions = compressions;
        resumptionTokenFormatter = new DefaultResumptionTokenFormatter();

        getRecordHandler = new GetRecordHandler(formatter, xoaiContext, this.itemRepository, repositoryConfiguration);
        identifyHandler = new IdentifyHandler(formatter, repositoryConfiguration, this.compressions);
        listMetadataFormatsHandler = new ListMetadataFormatsHandler(formatter, this.itemRepository, xoaiContext);
        listRecordsHandler = new ListRecordsHandler(formatter, this.manager.getMaxListRecordsSize(), this.setRepository, this.itemRepository, repositoryConfiguration, xoaiContext, resumptionTokenFormatter);
        listIdentifiersHandler = new ListIdentifiersHandler(formatter, this.manager.getMaxListIdentifiersSize(), this.setRepository, this.itemRepository, repositoryConfiguration, xoaiContext, resumptionTokenFormatter);
        listSetsHandler = new ListSetsHandler(formatter, this.manager.getMaxListSetsSize(), this.setRepository, xoaiContext, resumptionTokenFormatter);
        errorHandler = new ErrorHandler();
    }

    public OAIPMH handle(OAIRequestParameters params) throws OAIException {
        log.debug("Starting handling OAI request");

        OAIPMH response = new OAIPMH(manager);
        OAIPMHtype info = new OAIPMHtype();
        response.setInfo(info);

        RequestType request = new RequestType();
        info.setRequest(request);
        info.setResponseDate(formatter.now());

        request.setValue(this.repositoryConfiguration.getBaseUrl());
        try {
            OAIParameters parameters = new OAIParameters(params, resumptionTokenFormatter);
            VerbType verb = parameters.getVerb();
            request.setVerb(verb);

            if (params.getResumptionToken() != null)
                request.setResumptionToken(params.getResumptionToken());
            if (params.getIdentifier() != null)
                request.setIdentifier(parameters.getIdentifier());
            if (params.getFrom() != null)
                try {
                    request.setFrom(formatter.parse(params.getFrom()));
                } catch (java.text.ParseException e) {
                    throw new BadArgumentException("Invalid date given in from parameter");
                }
            if (params.getMetadataPrefix() != null)
                request.setMetadataPrefix(params.getMetadataPrefix());
            if (params.getSet() != null)
                request.setSet(params.getSet());
            if (params.getUntil() != null)
                try {
                    request.setUntil(formatter.parse(params.getUntil()));
                } catch (java.text.ParseException e) {
                    throw new BadArgumentException("Invalid date given in until parameter");
                }

            switch (verb) {
                case IDENTIFY:
                    info.setIdentify(identifyHandler.handle(parameters));
                    break;
                case LIST_SETS:
                    info.setListSets(listSetsHandler.handle(parameters));
                    break;
                case LIST_METADATA_FORMATS:
                    info.setListMetadataFormats(listMetadataFormatsHandler.handle(parameters));
                    break;
                case GET_RECORD:
                    info.setGetRecord(getRecordHandler.handle(parameters));
                    break;
                case LIST_IDENTIFIERS:
                    info.setListIdentifiers(listIdentifiersHandler.handle(parameters));
                    break;
                case LIST_RECORDS:
                    info.setListRecords(listRecordsHandler.handle(parameters));
                    break;
            }
        } catch (HandlerException e) {
            log.debug(e.getMessage(), e);
            info.getError().add(errorHandler.handle(e));
        }

        return response;
    }

    public void handle(OAIRequestParameters params, OutputStream out)
            throws OAIException, XMLStreamException, WritingXmlException {

        XmlOutputContext context = XmlOutputContext.emptyContext(out);
        context.getWriter().writeStartDocument();
        this.handle(params).write(context);
        context.getWriter().writeEndDocument();
        context.getWriter().flush();
        context.getWriter().close();
    }
}
