webjars-locator-lite
====================

This project provides a means to locate assets within WebJars.

[Check out the JavaDoc](https://javadocs.dev/org.webjars/webjars-locator-lite/latest)

[![Latest Release](https://img.shields.io/maven-central/v/org.webjars/webjars-locator-lite.svg)](https://mvnrepository.com/artifact/org.webjars/webjars-locator-lite) [![CodeQL](https://github.com/webjars/webjars-locator-lite/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/webjars/webjars-locator-lite/actions/workflows/codeql-analysis.yml) [![.github/workflows/test.yml](https://github.com/webjars/webjars-locator-lite/actions/workflows/test.yml/badge.svg)](https://github.com/webjars/webjars-locator-lite/actions/workflows/test.yml) 

Usage
--------------------------------

> Get the version of a WebJar on the classpath
```
WebJarVersionLocator.webJarVersion("bootstrap")
```

> Get the full path to a file in a WebJar
```
WebJarVersionLocator.fullPath("bootstrap", "js/bootstrap.js")
```
