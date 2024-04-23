webjars-locator-lite
====================

This project provides a means to locate assets within WebJars.

[Check out the JavaDoc](https://javadocs.dev/org.webjars/webjars-locator-lite/latest)

[![Latest Release](https://img.shields.io/maven-central/v/org.webjars/webjars-locator-lite.svg)](https://mvnrepository.com/artifact/org.webjars/webjars-locator-lite) [![.github/workflows/test.yml](https://github.com/webjars/webjars-locator-lite/actions/workflows/test.yml/badge.svg)](https://github.com/webjars/webjars-locator-lite/actions/workflows/test.yml)

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

The default cache uses a `ConcurrentHashMap` but you can provide a custom cache implementation:
```
class WebJarCacheMine implements WebJarCache {
    ...
}

WebJarVersionLocator webJarVersionLocator = new WebJarVersionLocator(new WebJarCacheMine());
```
