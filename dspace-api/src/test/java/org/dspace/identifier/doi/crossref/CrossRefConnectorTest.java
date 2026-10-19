/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.doi.crossref;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.dspace.AbstractDSpaceTest;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.handle.service.HandleService;
import org.dspace.identifier.service.DOIService;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.Test;
import org.mockito.Mockito;

public class CrossRefConnectorTest extends AbstractDSpaceTest {

    ItemService itemService = Mockito.mock(ItemService.class);

    DOIResolverClient doiResolverClient = Mockito.mock(DOIResolverClient.class);
    CrossRefClient crossRefClient = Mockito.mock(CrossRefClient.class);
    DOIService doiService = Mockito.mock(DOIService.class);
    HandleService handleService = Mockito.mock(HandleService.class);

    protected ConfigurationService configurationService = new DSpace().getConfigurationService();

    CrossRefConnector sut = new CrossRefConnector(itemService,
            doiService,
            doiResolverClient,
            crossRefClient,
            Mockito.mock(CrossRefPayloadService.class),
            handleService,
            configurationService,
            true,
            "dc.identifier.issn",
            "registrant",
            "depositor",
            "depositorEmail"
            );

    @Test
    public void canCheckDOIReservation() throws Exception {
        assertThat(sut.isDOIReserved(aContext(), aDoi())).isTrue();
    }

    @Test
    public void canReserveDOI() throws Exception {
        sut.reserveDOI(aContext(), null, aDoi());
    }

    @Test
    public void canCheckDOIRegistration404() throws Exception {

        Mockito.doReturn(new HttpResponse(404, null, null))
                .when(doiResolverClient).sendDOIGetRequest(Mockito.anyString());

        assertThat(sut.isDOIRegistered(aContext(), aDoi())).isFalse();
    }

    @Test
    public void canCheckDOIRegistration302() throws Exception {

        Mockito.doReturn(new HttpResponse(302, null, "https://www.example.com/"))
                .when(doiResolverClient).sendDOIGetRequest(Mockito.anyString());

        assertThat(sut.isDOIRegistered(aContext(), aDoi())).isTrue();
    }

    @Test
    public void canRegisterDOI() throws Exception {

        givenDoiNotRegistered();

        var dso = Mockito.mock(Item.class);

        Mockito.doReturn(List.of(aMetadataValue("article")))
                .when(itemService)
                .getMetadata(Mockito.any(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.any());

        sut.registerDOI(aContext(), dso, aDoi());
    }

    private void givenDoiNotRegistered() throws Exception {
        Mockito.doReturn(new HttpResponse(404, null, null))
                .when(doiResolverClient).sendDOIGetRequest(Mockito.anyString());
    }

    private Context aContext() {
        return Mockito.mock(Context.class);
    }

    private String aDoi() {
        return "doi";
    }

    private MetadataValue aMetadataValue(String withValue) {
        var md = Mockito.mock(MetadataValue.class);
        Mockito.doReturn(withValue).when(md).getValue();
        return md;
    }
}
