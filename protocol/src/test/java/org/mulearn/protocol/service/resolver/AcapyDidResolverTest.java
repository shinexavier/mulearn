package org.mulearn.protocol.service.resolver;

import org.junit.jupiter.api.Test;
import org.mulearn.protocol.domain.did.DidDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class AcapyDidResolverTest {

    @Autowired
    private AcapyDidResolver acapyDidResolver;

    @Test
    public void testResolveDid() {
        String did = "did:indy:bcovrin:test:2juZ9a3Nc9F7WVK26hzAFr";
        DidDocument didDocument = acapyDidResolver.resolve(did);
        assertNotNull(didDocument);
    }
}