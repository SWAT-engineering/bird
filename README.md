# Bird: A DSL to describe binary file formats 

Bird is a DSL to describe binary file formats such as BMP, PNG, JPEG or data protocols such as HTTP, UDP. Such a description of a format is declarative: it describes how the format looks like but does not describe how it can be parsed.

In Bird each token definition has its own type. Users can define new structured types that correspond to parsers, making the process of encoding new binary specifications less error-prone. To execute these specifications, DAN generates calls to the Nest API, a Java library for parsing binary data.

For a detailed introduction to Bird, check the [manual](https://github.com/SWAT-engineering/bird/blob/master/bird-doc/bird-manual.md).

## Integration with Nescio

The [Nescio project](https://github.com/SWAT-engineering/nescio) allows users to defina anonymization rules for arbitrary languages.

Bird features a Nescio plugin that makes it possible to anonymize data defined by Bird descriptions.

We provide an IDE that allows users to edit and compile Bird and Nescio descriptions.

## Sub-projects

The Bird project consist of several interrelated components, which are subprojects in this repository:

- nest: Java library for parsing binary data (the Bird compiler targets Java code that uses this library).
- bird-core: Bird toolset developed in Rascal. It includes the grammar definition, type checker, generator, and components for the integration with Nescio.
- bird-nescio-ide: Bird and Nescio IDE plugin.
- bird-nescio-feature: Bird and Nescio IDE eclipse feature.
- bird-nescio-update-site: Bird and Nescio IDE eclipse update site.
- bird-doc: Bird documentation.

In order to build them all, clone this project and execute `mvn install` on the root subdirectory. Notice that it is necessary first to build the [Nescio project](https://github.com/SWAT-engineering/nescio), since the Bird components related to the integration with Nescio depend on it.
