webjars-locator
===============

This project provides a means to locate assets within WebJars.

[![Build Status](https://travis-ci.org/webjars/webjars-locator-core.svg?branch=master)](https://travis-ci.org/webjars/webjars-locator-core)

Obtain the full path of an asset
--------------------------------

	WebJarAssetLocator locator = new WebJarAssetLocator();
	String fullPathToRequirejs = locator.getFullPath("require.js");
	
Obtain all of the assets within a base folder
---------------------------------------------
	
	WebJarAssetLocator locator = new WebJarAssetLocator();
	Set<String> fullPathsOfAssets = locator.listAssets("/multiple/1.0.0");

Advanced usage
--------------

The locator can also be configured with the class loaders that it should use for looking up resources and filter the types of resources that should be included for searching. Please visit the source code for more information.
