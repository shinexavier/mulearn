package org.mulearn.protocol.service.resolver;

import org.mulearn.protocol.domain.did.DidDocument;
import org.mulearn.protocol.exception.DidResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AcapyDidResolver {

    private static final Logger logger = LoggerFactory.getLogger(AcapyDidResolver.class);

    private final RestTemplate restTemplate;
    private final String acapyAdminUrl;
    private final String acapyApiKey;

    public AcapyDidResolver(
            RestTemplate restTemplate,
            @Value("${acapy.admin.url}") String acapyAdminUrl,
            @Value("${acapy.admin.api-key}") String acapyApiKey
    ) {
        this.restTemplate = restTemplate;
        this.acapyAdminUrl = acapyAdminUrl;
        this.acapyApiKey = acapyApiKey;
    }

    public DidDocument resolve(String did) {
        String url = UriComponentsBuilder.fromHttpUrl(acapyAdminUrl)
                .path("/resolver/resolve/{did}")
                .buildAndExpand(did)
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", acapyApiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<DidDocument> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, DidDocument.class
            );
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            }
            throw new DidResolutionException("Failed to resolve DID: " + did + ", status code: " + response.getStatusCode());
        } catch (Exception e) {
            logger.error("Error resolving DID: {}", did, e);
            throw new DidResolutionException("Error resolving DID: " + did, e);
        }
    }
}