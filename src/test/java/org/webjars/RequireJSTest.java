package org.webjars;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class RequireJSTest {

    private static String WEBJAR_URL_PREFIX = "/webjars/";
    private static String WEBJAR_CDN_PREFIX = "http://cdn.jsdelivr.net/webjars/";

    @Test
    public void should_generate_correct_javascript() {
        String javaScript = RequireJS.getSetupJavaScript(WEBJAR_URL_PREFIX);

        assertTrue(javaScript.indexOf("\"bootstrap\":\"3.1.1\"") > 0);
    }

    @Test
    public void should_generate_correct_json() {
        Map<String, ObjectNode> jsonNoCdn = RequireJS.getSetupJson(WEBJAR_URL_PREFIX);
        Map<String, ObjectNode> jsonWithCdn = RequireJS.getSetupJson(WEBJAR_CDN_PREFIX, WEBJAR_URL_PREFIX);

        assertEquals(WEBJAR_URL_PREFIX + "jquery/2.1.0/jquery", jsonNoCdn.get("jquery").get("paths").withArray("jquery").get(0).asText());
        assertEquals(WEBJAR_CDN_PREFIX + "jquery/2.1.0/jquery", jsonWithCdn.get("jquery").get("paths").withArray("jquery").get(0).asText());
        assertEquals(WEBJAR_URL_PREFIX + "jquery/2.1.0/jquery", jsonWithCdn.get("jquery").get("paths").withArray("jquery").get(1).asText());

        assertEquals("$", jsonNoCdn.get("jquery").get("shim").get("jquery").get("exports").asText());
    }

}