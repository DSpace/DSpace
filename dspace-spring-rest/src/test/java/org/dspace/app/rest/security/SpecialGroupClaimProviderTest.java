package org.dspace.app.rest.security;

import com.nimbusds.jwt.JWTClaimsSet;
import org.dspace.core.Context;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;

@RunWith(MockitoJUnitRunner.class)
public class SpecialGroupClaimProviderTest {


    @InjectMocks
    private SpecialGroupClaimProvider specialGroupClaimProvider;

    private List<UUID> specialGroups = new ArrayList<>();


    private Context context;

    private String id1 = "02af436f-a531-4934-9b36-a21cd8fdcc57";
    private String id2 = "f39d3947-c75d-4d09-86ef-f732cfae7d88";
    private String id3 = "2262d8ad-8bb6-4330-9cee-06da30f3feae";

    @Mock
    private HttpServletRequest httpServletRequest;

    private JWTClaimsSet jwtClaimsSet;


    @Before
    public void setUp() throws Exception {
        context = Mockito.mock(Context.class);
        //Stub the specialgroups list that is normally kept in the context class
        Mockito.doAnswer(invocation -> {
            UUID uuid = invocation.getArgumentAt(0, UUID.class);
            specialGroups.add(uuid);
            return "done";
        }).when(context).setSpecialGroup(any(UUID.class));

        List<String> groupIds = new ArrayList<>();
        groupIds.add(id1);
        groupIds.add(id2);
        groupIds.add(id3);

        jwtClaimsSet = new JWTClaimsSet.Builder()
                .claim(SpecialGroupClaimProvider.SPECIAL_GROUPS, groupIds)
                .build();
    }

    @After
    public void tearDown() throws Exception {
        specialGroups.clear();
    }

    @Test
    public void parseClaim() throws Exception {
        specialGroupClaimProvider.parseClaim(context, httpServletRequest, jwtClaimsSet);
        assertEquals(specialGroups.get(0), UUID.fromString(id1));
        assertEquals(specialGroups.get(1), UUID.fromString(id2));
        assertEquals(specialGroups.get(2), UUID.fromString(id3));
    }

}