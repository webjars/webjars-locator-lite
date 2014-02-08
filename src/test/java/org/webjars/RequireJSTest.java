package org.webjars;

import org.junit.Test;

import static org.junit.Assert.*;

public class RequireJSTest {

    @Test
    public void should_generate_correct_javascript() {
        String javaScript = RequireJS.getSetupJavaScript("/webjars/");
        
        assertTrue(javaScript.indexOf("'bootstrap': '2.2.2'") > 0);
        assertTrue(javaScript.indexOf("'angular-route': [ 'webjars!angular.js' ],") > 0);
        
        // todo: it would be nice to actually run the JS and make sure it at least evaluates correctly
    }

}