package org.dspace.app.util;

import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

/**
 * Created by frederic on 25/04/17.
 */
@RunWith(MockitoJUnitRunner.class)
public class GoogleMetadataTest {
    @InjectMocks
    private GoogleMetadata googleMetadata;

    @Mock
    private GoogleBitstreamComparator googleBitstreamComparator;

    @Mock
    private Item item;

    @Mock
    private Bundle bundle;

    @Mock
    private Bitstream bitstream1;

    @Mock
    private Bitstream bitstream2;

    @Mock
    private Bitstream bitstream3;

    @Mock
    private BitstreamFormat bitstreamFormat1;

    @Mock
    private BitstreamFormat bitstreamFormat2;

    @Mock
    private BitstreamFormat bitstreamFormat3;


    @Before
    public void setUp() throws Exception {
        HashMap<String, String> settings = new HashMap<>();
        settings.put("citation.prioritized_types", "PDF, WORD, RTF, PS");
        settings.put("citation.mimetypes.PDF" , "application/pdf");
        settings.put("citation.mimetypes.WORD", "application/msword, application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        settings.put("citation.mimetypes.RTF", "text/richtext");
        settings.put("citation.mimetypes.PS", "application/x-photoshop");
        googleBitstreamComparator = new GoogleBitstreamComparator(settings);
        when(item.getBundles("ORIGINAL")).thenReturn(new Bundle[] {bundle});
    }

    @Test
    public void testFindLinkableFulltext() throws Exception {

    }


}