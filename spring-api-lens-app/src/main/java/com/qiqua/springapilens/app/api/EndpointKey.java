package com.qiqua.springapilens.app.api;

import com.qiqua.springapilens.core.model.ApiEndpoint;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

final class EndpointKey {
    private EndpointKey() {
    }

    static String from(ApiEndpoint endpoint) {
        String raw = endpoint.className() + "#" + endpoint.methodName() + "|"
            + endpoint.httpMethod() + "|" + endpoint.path();
        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    static boolean matches(ApiEndpoint endpoint, String key) {
        return from(endpoint).equals(key);
    }
}
