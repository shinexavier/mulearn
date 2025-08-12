package org.mulearn.protocol.domain.did;

public enum DidMethod {
    KEY("key"),
    WEB("web"),
    INDY("indy");

    private final String method;

    DidMethod(String method) {
        this.method = method;
    }

    public String getMethod() {
        return method;
    }

    public static DidMethod fromString(String method) {
        for (DidMethod didMethod : DidMethod.values()) {
            if (didMethod.method.equalsIgnoreCase(method)) {
                return didMethod;
            }
        }
        return null;
    }
}