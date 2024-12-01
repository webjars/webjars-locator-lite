webjars-locator-lite
====================

This project provides a means to locate assets within WebJars.

[![Latest Release](https://img.shields.io/maven-central/v/org.webjars/webjars-locator-lite.svg)](https://mvnrepository.com/artifact/org.webjars/webjars-locator-lite) [![Javadoc](https://img.shields.io/badge/Javadoc-latest-blue)](https://javadocs.dev/org.webjars/webjars-locator-lite/latest) [![.github/workflows/test.yml](https://github.com/webjars/webjars-locator-lite/actions/workflows/test.yml/badge.svg)](https://github.com/webjars/webjars-locator-lite/actions/workflows/test.yml)

Usage
--------------------------------

> Get the version of a WebJar on the classpath
```
new WebJarVersionLocator().version("bootstrap");
```

> Get the full path to a file in a WebJar
```
new WebJarVersionLocator().fullPath("bootstrap", "js/bootstrap.js");
// returns "META-INF/resources/webjars/bootstrap/<version>/js/bootstrap.js"
```

> Get the path in the standard WebJar classpath location
```
new WebJarVersionLocator().path("bootstrap", "js/bootstrap.js");
// returns "bootstrap/<version>/js/bootstrap.js"
```

`WebJarVersionLocator` has a built-in threadsafe cache that is created on construction.  It is highly recommended that you use it as a Singleton to utilize the cache, i.e.
```
WebJarVersionLocator webJarVersionLocator = new WebJarVersionLocator();
webJarVersionLocator.version("bootstrap"); // cache miss
webJarVersionLocator.version("bootstrap"); // cache hit, avoiding looking up metadata in the classpath
```

Custom WebJars
--------------------------------

By default, `webjars-locator-lite` only supports WebJars with the Maven group IDs of `org.webjars` and `org.webjars.npm`.
To support custom WebJars, you can provide a `webjars-locator.properties` file to register those WebJars with the locator.

The `webjars-locator.properties` file must be located in `META-INF/resources/`.

For a WebJar that has its resource files located in `META-INF/resources/mywebjar/3.2.1/`, the file would look like this:

```
mywebjar.version=3.2.1
```

For custom WebJars, it is recommended to package the file within the WebJar.  
However, for older releases that do not include this file, it is also possible to add it to your project directly.  

Multiple WebJar versions can be defined in a single file:

```
mywebjar.version=3.2.1
anotherwebjar.version=1.4.3
```

> This allows the use of [legacy](https://github.com/webjars/webjars/issues/2039) `org.webjars.bower` WebJars. However, `org.webjars.bowergithub.xyz` WebJars are not supported because their
> resource paths are missing the version part needed for `webjars-locator-lite`.

You can find an [example file](https://github.com/webjars/webjars-locator-lite/blob/main/src/test/resources/META-INF/resources/webjars-locator.properties) in our tests.
