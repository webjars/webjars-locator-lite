package org.webjars;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class RequireJS {

    /**
     * Returns the JavaScript that is used to setup the RequireJS config
     * 
     * @param webjarUrlPrefix The URL prefix where the WebJars can be downloaded from with a trailing slash, e.g. /webjars/
     * @return The JavaScript block that can be embedded or loaded in a <script> tag
     */
    public static String getSetupJavaScript(String webjarUrlPrefix) {
        
        Map<String, String> webjars = new WebJarAssetLocator().getWebJars();
        
        // this is just silly.
        String webjarsVersionsString = "";
        for (String webjarId : webjars.keySet()) {
            webjarsVersionsString += "'" + webjarId + "': '" + webjars.get(webjarId) + "', ";
        }
        webjarsVersionsString = webjarsVersionsString.substring(0, webjarsVersionsString.length() - 2);
        
        
        Map<String, String> webjarConfigs = new HashMap<String, String>();

        // read the webjarConfigs
        
        for (String webjarId : webjars.keySet()) {
            String filename = WebJarAssetLocator.WEBJARS_PATH_PREFIX + "/" + webjarId + "/" + webjars.get(webjarId) + "/" + "webjars-requirejs.js";
            InputStream inputStream = RequireJS.class.getClassLoader().getResourceAsStream(filename);
            if (inputStream != null) {
                String webjarConfig = "";
                BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
                try {
                    String line;
                    while((line=br.readLine())!=null){
                        webjarConfig += line;
                    }
                    
                    webjarConfigs.put(webjarId, webjarConfig);
                }
                catch (IOException e) {
                    // ignored
                }
                finally {
                    try {
                        br.close();
                    }
                    catch (IOException e) {
                        // really?
                    }
                }
            }
        }
        
        
        String webjarConfigsString = "";
        
        for (String webjarId : webjarConfigs.keySet()) {
            webjarConfigsString += "// webjar config for " + webjarId + "\n";
            webjarConfigsString += webjarConfigs.get(webjarId) + "\n";
        }
        
        
        // assemble the JavaScript
        // todo: could use a templating language but that would add a dependency
        
        String javaScript = "var webjars = {\n" +
            "    versions: { " + webjarsVersionsString + " },\n" +
            "    path: function(webjarid, path) {\n" + 
            "        return '" + webjarUrlPrefix + "' + webjarid + '/' + webjars.versions[webjarid] + '/' + path;\n" +
            "    }\n" +
            "};\n" +
            "\n" +
            "var require = {\n" +
            "    callback: function() {\n" +
            "        // no-op webjars requirejs plugin loader for backwards compatibility\n" +
            "        define('webjars', function () {\n" +
            "            return { load: function (name, req, onload, config) { onload(); } }\n" +
            "        });\n" +
            "\n" +
            "        // all of the webjar configs from their webjars-requirejs.js files\n" +
            webjarConfigsString +
            "    }\n" +
            "}";
        
        
        return javaScript;
    }
    
}