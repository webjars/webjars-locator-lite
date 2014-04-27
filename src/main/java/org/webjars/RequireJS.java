package org.webjars;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public final class RequireJS {

    public static final String WEBJARS_MAVEN_PREFIX = "META-INF/maven/org.webjars";

    private static final Logger log = LoggerFactory.getLogger(RequireJS.class);

    protected static String requireConfigJavaScript = null;
    protected static String requireConfigJavaScriptCdn = null;

    protected static Map<String, ObjectNode> requireConfigJson = null;
    protected static Map<String, ObjectNode> requireConfigJsonCdn = null;

    /**
     * Returns the JavaScript that is used to setup the RequireJS config.
     * This value is cached in memory so that all of the processing to get the String only has to happen once.
     *
     * @param urlPrefix The URL prefix where the WebJars can be downloaded from with a trailing slash, e.g. /webJars/
     * @return The JavaScript block that can be embedded or loaded in a <script> tag
     */
    public synchronized static String getSetupJavaScript(String urlPrefix) {
        if (requireConfigJavaScript == null) {
            List<String> prefixes = new ArrayList<String>();
            prefixes.add(urlPrefix);

            requireConfigJavaScript = generateSetupJavaScript(prefixes);
        }
        return requireConfigJavaScript;
    }

    /**
     * Returns the JavaScript that is used to setup the RequireJS config.
     * This value is cached in memory so that all of the processing to get the String only has to happen once.
     *
     * @param urlPrefix The URL prefix where the WebJars can be downloaded from with a trailing slash, e.g. /webJars/
     * @param cdnPrefix The optional CDN prefix where the WebJars can be downloaded from
     * @return The JavaScript block that can be embedded or loaded in a <script> tag
     */
    public synchronized static String getSetupJavaScript(String cdnPrefix, String urlPrefix) {
        if (requireConfigJavaScriptCdn == null) {
            List<String> prefixes = new ArrayList<String>();
            prefixes.add(cdnPrefix);
            prefixes.add(urlPrefix);

            requireConfigJavaScriptCdn = generateSetupJavaScript(prefixes);
        }
        return requireConfigJavaScriptCdn;
    }

    /**
     * Returns the JavaScript that is used to setup the RequireJS config.
     * This value is not cached.
     *
     * @param prefixes A list of the prefixes to use in the `paths` part of the RequireJS config.
     * @return The JavaScript block that can be embedded or loaded in a <script> tag.
     */
    protected synchronized static String generateSetupJavaScript(List<String> prefixes) {
        Map<String, String> webJars = new WebJarAssetLocator().getWebJars();

        return generateSetupJavaScript(prefixes, webJars);
    }

    /**
     * Generate the JavaScript that is used to setup the RequireJS config.
     * This value is not cached.
     * This uses nasty stuff that is really not maintainable or testable.  So this has been deprecated and the implementation will eventually be replaced with something better.
     *
     * @param prefixes A list of the prefixes to use in the `paths` part of the RequireJS config.
     * @param webJars The WebJars (artifactId -> version) to use
     * @return The JavaScript block that can be embedded or loaded in a <script> tag.
     */
    @Deprecated
    protected static String generateSetupJavaScript(List<String> prefixes, Map<String, String> webJars) {

        ObjectMapper mapper = new ObjectMapper();

        ObjectNode webJarsVersions = mapper.createObjectNode();

        StringBuilder webJarConfigsString = new StringBuilder();

        if (webJars.isEmpty()) {
            log.warn("Can't find any WebJars in the classpath, RequireJS configuration will be empty.");
        } else {
            for (Map.Entry<String, String> webJar : webJars.entrySet()) {

                // assemble the WebJar versions string
                webJarsVersions.put(webJar.getKey(), webJar.getValue());

                // assemble the WebJar config string

                // default to the new pom.xml meta-data way
                ObjectNode webJarObjectNode = getWebJarRequireJsConfig(webJar, prefixes);
                if (webJarObjectNode.size() != 0) {
                    webJarConfigsString.append("\n").append("requirejs.config(").append(webJarObjectNode.toString()).append(")");
                }
                else {
                    webJarConfigsString.append("\n").append(getWebJarConfig(webJar));
                }
            }
        }

        String webJarBasePath = "webJarId + '/' + webjars.versions[webJarId] + '/' + path";

        StringBuilder webJarPath = new StringBuilder("[");

        for (String prefix : prefixes) {
            webJarPath.append("'").append(prefix).append("' + ").append(webJarBasePath).append(",\n");
        }

        //webJarBasePath.
        webJarPath.delete(webJarPath.lastIndexOf(",\n"), webJarPath.lastIndexOf(",\n") + 2);

        webJarPath.append("]");

        return "var webjars = {\n" +
                "    versions: " + webJarsVersions.toString() + ",\n" +
                "    path: function(webJarId, path) {\n" +
                "        console.error('The webjars.path() method of getting a WebJar path has been deprecated.  The RequireJS config in the ' + webJarId + ' WebJar may need to be updated.  Please file an issue: http://github.com/webjars/' + webJarId + '/issues/new');\n" +
                "        return " + webJarPath.toString() + ";\n" +
                "    }\n" +
                "};\n" +
                "\n" +
                "var require = {\n" +
                "    callback: function() {\n" +
                "        // Deprecated WebJars RequireJS plugin loader\n" +
                "        define('webjars', function() {\n" +
                "            return {\n" +
                "                load: function(name, req, onload, config) {\n" +
                "                    if (name.indexOf('.js') >= 0) {\n" +
                "                        console.warn('Detected a legacy file name (' + name + ') as the thing to load.  Loading via file name is no longer supported so the .js will be dropped in an effort to resolve the module name instead.');\n" +
                "                        name = name.replace('.js', '');\n" +
                "                    }\n" +
                "                    console.error('The webjars plugin loader (e.g. webjars!' + name + ') has been deprecated.  The RequireJS config in the ' + name + ' WebJar may need to be updated.  Please file an issue: http://github.com/webjars/webjars/issues/new');\n" +
                "                    req([name], function() {;\n" +
                "                        onload();\n" +
                "                    });\n" +
                "                }\n" +
                "            }\n" +
                "        });\n" +
                "\n" +
                "        // All of the WebJar configs\n\n" +
                webJarConfigsString +
                "    }\n" +
                "}";
    }

    /**
     * Returns the JSON that is used to setup the RequireJS config.
     * This value is cached in memory so that all of the processing to get the JSON only has to happen once.
     *
     * @param urlPrefix The URL prefix where the WebJars can be downloaded from with a trailing slash, e.g. /webJars/
     * @return The JSON structured config
     */
    public synchronized static Map<String, ObjectNode> getSetupJson(String urlPrefix) {
        if (requireConfigJson == null) {

            List<String> prefixes = new ArrayList<String>();
            prefixes.add(urlPrefix);

            requireConfigJson = generateSetupJson(prefixes);
        }
        return requireConfigJson;
    }

    /**
     * Returns the JSON that is used to setup the RequireJS config.
     * This value is cached in memory so that all of the processing to get the JSON only has to happen once.
     *
     * @param cdnPrefix The CDN prefix where the WebJars can be downloaded from
     * @param urlPrefix The URL prefix where the WebJars can be downloaded from with a trailing slash, e.g. /webJars/
     * @return The JSON structured config
     */
    public synchronized static Map<String, ObjectNode> getSetupJson(String cdnPrefix, String urlPrefix) {
        if (requireConfigJsonCdn == null) {

            List<String> prefixes = new ArrayList<String>();
            prefixes.add(cdnPrefix);
            prefixes.add(urlPrefix);

            requireConfigJsonCdn = generateSetupJson(prefixes);
        }
        return requireConfigJsonCdn;
    }

    /**
     * Returns the JSON used to setup the RequireJS config for each WebJar in the CLASSPATH.
     * This value is not cached.
     *
     * @param prefixes A list of the prefixes to use in the `paths` part of the RequireJS config.
     * @return The JSON structured config for each WebJar.
     */
    public synchronized static Map<String, ObjectNode> generateSetupJson(List<String> prefixes) {
        Map<String, String> webJars = new WebJarAssetLocator().getWebJars();

        Map<String, ObjectNode> jsonConfigs = new HashMap<String, ObjectNode>();

        for (Map.Entry<String, String> webJar : webJars.entrySet()) {
            jsonConfigs.put(webJar.getKey(), getWebJarRequireJsConfig(webJar, prefixes));
        }

        return jsonConfigs;
    }

    /**
     * Returns the JSON RequireJS config for a given WebJar
     *
     * @param webJar A tuple (artifactId -> version) representing the WebJar.
     * @param prefixes A list of the prefixes to use in the `paths` part of the RequireJS config.
     * @return The JSON RequireJS config for the WebJar based on the meta-data in the WebJar's pom.xml file.
     */
    protected static ObjectNode getWebJarRequireJsConfig(Map.Entry<String, String> webJar, List<String> prefixes) {
        String rawRequireJsConfig = getRawWebJarRequireJsConfig(webJar);

        ObjectMapper mapper = new ObjectMapper();

        // default to just an empty object
        ObjectNode webJarRequireJsNode = mapper.createObjectNode();

        try {
            JsonNode maybeRequireJsConfig = mapper.readTree(rawRequireJsConfig);
            if (maybeRequireJsConfig.isObject()) {
                // The provided config was parseable, now lets fix the paths

                webJarRequireJsNode = (ObjectNode) maybeRequireJsConfig;

                if (webJarRequireJsNode.isObject()) {

                    ObjectNode pathsNode = (ObjectNode) webJarRequireJsNode.get("paths");

                    ObjectNode newPaths = mapper.createObjectNode();

                    Iterator<Map.Entry<String, JsonNode>> paths = pathsNode.fields();
                    while (paths.hasNext()) {
                        Map.Entry<String, JsonNode> pathNode = paths.next();

                        String originalPath = null;

                        if (pathNode.getValue().isArray()) {
                            ArrayNode nodePaths = (ArrayNode) pathNode.getValue();
                            // lets just assume there is only 1 for now
                            originalPath = nodePaths.get(0).asText();
                        }
                        else if (pathNode.getValue().isTextual()) {
                            TextNode nodePath = (TextNode) pathNode.getValue();
                            originalPath = nodePath.textValue();
                        }

                        if (originalPath != null) {
                            ArrayNode newPathsNode = newPaths.putArray(pathNode.getKey());
                            for (String prefix : prefixes) {
                                String newPath = prefix + webJar.getKey() + "/" + webJar.getValue()  + "/" + originalPath;
                                newPathsNode.add(newPath);
                            }
                            newPathsNode.add(originalPath);
                        }
                        else {
                            log.error("Strange... The path could not be parsed.  Here is what was provided: " + pathNode.getValue().toString());
                        }
                    }

                    webJarRequireJsNode.replace("paths", newPaths);
                }

            }
            else {
                log.error(requireJsConfigErrorMessage(webJar));
            }
        } catch (IOException e) {
            log.warn(requireJsConfigErrorMessage(webJar));
        }

        return webJarRequireJsNode;
    }

    /**
     * A generic error message for when the RequireJS config could not be parsed out of the WebJar's pom.xml meta-data.
     *
     * @param webJar A tuple (artifactId -> version) representing the WebJar.
     * @return The error message.
     */
    private static String requireJsConfigErrorMessage(Map.Entry<String, String> webJar) {
        return "Could not read WebJar RequireJS config for: " + webJar.getKey() + " " + webJar.getValue() + "\n" +
                "Please file a bug at: http://github.com/webjars/" + webJar.getKey() + "/issues/new";
    }

    /**
     * @param webJar A tuple (artifactId -> version) representing the WebJar.
     * @return The raw RequireJS config string from the WebJar's pom.xml meta-data.
     */
    protected static String getRawWebJarRequireJsConfig(Map.Entry<String, String> webJar) {
        String filename = WEBJARS_MAVEN_PREFIX + "/" + webJar.getKey() + "/pom.xml";
        InputStream inputStream = RequireJS.class.getClassLoader().getResourceAsStream(filename);

        if (inputStream != null) {
            // try to parse: <root><properties><requirejs>{ /* some json */ }</requirejs></properties></root>
            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(inputStream);
                doc.getDocumentElement().normalize();

                NodeList propertiesNodes = doc.getElementsByTagName("properties");
                for (int i = 0; i < propertiesNodes.getLength(); i++) {
                    NodeList propertyNodes = propertiesNodes.item(i).getChildNodes();
                    for (int j = 0; j < propertyNodes.getLength(); j++) {
                        Node node = propertyNodes.item(j);
                        if (node.getNodeName().equals("requirejs")) {
                            return node.getTextContent();
                        }
                    }
                }

            } catch (ParserConfigurationException e) {
                log.warn(requireJsConfigErrorMessage(webJar));
            } catch (IOException e) {
                log.warn(requireJsConfigErrorMessage(webJar));
            } catch (SAXException e) {
                log.warn(requireJsConfigErrorMessage(webJar));
            }

            try {
                inputStream.close();
            } catch (IOException e) {
                // what-evs
            }

        }
        else {
            log.warn(requireJsConfigErrorMessage(webJar));
        }

        return "";
    }

    /**
     * The legacy webJars-requirejs.js based RequireJS config for a WebJar.
     *
     * @param webJar A tuple (artifactId -> version) representing the WebJar.
     * @return The contents of the webJars-requirejs.js file.
     */
    @Deprecated
    protected static String getWebJarConfig(Map.Entry<String, String> webJar) {
        String webJarConfig = "";

        // read the webJarConfigs
        String filename = WebJarAssetLocator.WEBJARS_PATH_PREFIX + "/" + webJar.getKey() + "/" + webJar.getValue() + "/" + "webjars-requirejs.js";
        InputStream inputStream = RequireJS.class.getClassLoader().getResourceAsStream(filename);
        if (inputStream != null) {
            log.warn("The " + webJar.getKey() + " " + webJar.getValue() + " WebJar is using the legacy RequireJS config.\n" +
                    "Please try a new version of the WebJar or file or file an issue at:\n" +
                    "http://github.com/webjars/" + webJar.getKey() + "/issues/new");

            StringBuilder webJarConfigBuilder = new StringBuilder("// WebJar config for " + webJar.getKey() + "\n");
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            try {
                String line;

                while((line=br.readLine())!=null){
                    webJarConfigBuilder.append(line).append("\n");
                }

                webJarConfig = webJarConfigBuilder.toString();
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

        return webJarConfig;
    }
}
