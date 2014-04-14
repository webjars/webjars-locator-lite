package org.webjars;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public final class RequireJS {
    
    private static final Logger log = LoggerFactory.getLogger(RequireJS.class);

    protected static String setupJavaScript = null;

    /**
     * Returns the JavaScript that is used to setup the RequireJS config.
     * This value is cached in memory so that all of the processing to get the String only has to happen once.
     *
     * @param webjarUrlPrefix The URL prefix where the WebJars can be downloaded from with a trailing slash, e.g. /webjars/
     * @return The JavaScript block that can be embedded or loaded in a <script> tag
     */
    public synchronized static String getSetupJavaScript(String webjarUrlPrefix) {
        return getSetupJavaScript(webjarUrlPrefix, null);
    }
    
    /**
     * Returns the JavaScript that is used to setup the RequireJS config.
     * This value is cached in memory so that all of the processing to get the String only has to happen once.
     *
     * @param webjarUrlPrefix The URL prefix where the WebJars can be downloaded from with a trailing slash, e.g. /webjars/
     * @param cdnPrefix The optional CDN prefix where the WebJars can be downloaded from
     * @return The JavaScript block that can be embedded or loaded in a <script> tag
     */
    public synchronized static String getSetupJavaScript(String webjarUrlPrefix, String cdnPrefix) {
        
        // cache this thing since it should never change at runtime
        if (setupJavaScript == null) {

            Map<String, String> webjars = new WebJarAssetLocator().getWebJars();

            StringBuilder webjarsVersionsString = new StringBuilder();

            StringBuilder webjarConfigsString = new StringBuilder();

            if (webjars.isEmpty()) {
                log.warn("Can't find any WebJars in the classpath, RequireJS configuration will be empty.");
            } else {
                for (Map.Entry<String, String> webjar : webjars.entrySet()) {

                    // assemble the webjar versions string
                    webjarsVersionsString.append("'").append(webjar.getKey()).append("': '").append(webjar.getValue()).append("', ");

                    // assemble the webjar config string
                    webjarConfigsString.append("\n").append(getWebJarConfig(webjar));
                }

                // remove the trailing ", "
                webjarsVersionsString.delete(webjarsVersionsString.length() - 2, webjarsVersionsString.length());
            }

            // assemble the JavaScript
            // todo: could use a templating language but that would add a dependency

            String webjarBasePath = "'" + webjarUrlPrefix + "' + webjarid + '/' + webjars.versions[webjarid] + '/' + path";
            String webjarPath = "";
            if (cdnPrefix != null) {
                webjarPath = "[\n" +
                             "'" + cdnPrefix + "' + " + webjarBasePath + ",\n" +
                             webjarBasePath + "\n" +
                             "];\n";
            }
            else {
                webjarPath = webjarBasePath + ";\n";
            }
            
            setupJavaScript = "var webjars = {\n" +
                    "    versions: { " + webjarsVersionsString + " },\n" +
                    "    path: function(webjarid, path) {\n" +
                    "        return " + webjarPath +
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
        }

        return setupJavaScript;
    }

    protected static String getWebJarConfig(Map.Entry<String, String> webjar) {
        String webjarConfig = "";
        
        // read the webjarConfigs
        String filename = WebJarAssetLocator.WEBJARS_PATH_PREFIX + "/" + webjar.getKey() + "/" + webjar.getValue() + "/" + "webjars-requirejs.js";
        InputStream inputStream = RequireJS.class.getClassLoader().getResourceAsStream(filename);
        if (inputStream != null) {
            StringBuilder webjarConfigBuilder = new StringBuilder("// webjar config for " + webjar.getKey() + "\n");
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            try {
                String line;

                while((line=br.readLine())!=null){
                    webjarConfigBuilder.append(line).append("\n");
                }

                webjarConfig = webjarConfigBuilder.toString();
            }
            catch (IOException e) {
                log.warn(filename + " could not be read.");
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

        return webjarConfig;
    }
}
