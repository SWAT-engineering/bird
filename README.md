# Bird: A DSL to describe binary file formats 

Bird is a DSL to describe binary file formats such as BMP, PNG, JPEG or data protocols such as HTTP, UDP. Such a description of a format is declarative: it describes how the format looks like but does not describe how it can be parsed.

In Bird each token definition has its own type. Users can define new structured types that correspond to parsers, making the process of encoding new binary specifications less error-prone. To execute these specifications, DAN generates calls to the Nest API, a Java library for parsing binary data.

For a detailed introduction to Bird, check the [manual](https://github.com/SWAT-engineering/bird/blob/main/bird-doc/bird-manual.md).

## Integration with Nescio

The [Nescio project](https://github.com/SWAT-engineering/nescio) allows users to defina anonymization rules for arbitrary languages.

Bird features a Nescio plugin that makes it possible to anonymize data defined by Bird descriptions.

We provide an IDE that allows users to edit and compile Bird and Nescio descriptions.

## Sub-projects

The Bird project consist of several interrelated components, which are subprojects in this repository:

- `nest`: Java library for parsing binary data (the Bird compiler targets Java code that uses this library).
- `bird-core`: Bird toolset developed in Rascal. It includes the grammar definition, type checker, generator, and components for the integration with Nescio.
- `bird-ide`: Bird language server developed in Rascal. It integrates the Bird toolset with support for syntax highlighting, jump to definition, visualization, code lenses, inlay hints, and outlines.
- `bird-extension`: The VS Code extension for Bird. It leverages the Bird language server to offer a full IDE experience.
- `bird-doc`: Bird documentation.

To build `bird-core` and `bird-ide`, clone this project and execute `mvn build` on the root subdirectory. To build a VSIX that can be installed in VS Code, run `npm i && npx vsce package` in the `bird-extension` subdirectory.
