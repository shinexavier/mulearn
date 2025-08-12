package org.mulearn.protocol.service;

import org.mulearn.protocol.domain.did.DidDocument;
import org.mulearn.protocol.service.resolver.AcapyDidResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DIDResolverService {

    private final AcapyDidResolver acapyDidResolver;

    @Autowired
    public DIDResolverService(AcapyDidResolver acapyDidResolver) {
        this.acapyDidResolver = acapyDidResolver;
    }

    public DidDocument resolve(String did) {
        return acapyDidResolver.resolve(did);
    }
}