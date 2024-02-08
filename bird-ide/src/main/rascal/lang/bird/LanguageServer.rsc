module lang::bird::LanguageServer

import ParseTree;

import util::LanguageServer;
import util::Monitor;
import util::Reflective;
import IO;

import lang::bird::Checker;
import lang::bird::Syntax;
import lang::bird::Generator2Nest;

set[LanguageService] birdLanguageContributor() {
    return {
        parser(getBirdParser()),
        outliner(birdOutliner),
        summarizer(birdSummarizer)
    };
}

list[DocumentSymbol] birdOutliner(start[Program] input) {
    jobStart("Bird Outliner");
    l = input.src.top;
    jobStep("Bird Outliner", l.file);
    list[DocumentSymbol] children = [];
    for (declaration <- input.top.declarations) {
        children += buildOutline(declaration);
    }
    jobEnd("Bird Outliner");
    return [symbol("<input.src.file>", DocumentSymbolKind::file(), input.src, children=children)];
}

list[DocumentSymbol] buildOutline(current:(TopLevelDecl)`struct <Id id> <TypeFormals? _> <Formals? _> <Annos? _> { <DeclInStruct* declarations> }`)
    = [symbol("struct <id>", struct(), current.src, children=[*buildOutline(decl) | decl <- declarations])];

list[DocumentSymbol] buildOutline(current:(TopLevelDecl)`choice <Id id> <Formals? _> <Annos? _> { <DeclInChoice* declarations> }`)
    = [symbol("choice <id>", array(), current.src, children=[*buildOutline(decl) | decl <- declarations])];

list[DocumentSymbol] buildOutline(current:(TopLevelDecl)`@(<JavaId _>) <Type _> <Id id> <Formals? _>`)
    = [symbol("function <id>", method(), current.src)];

list[DocumentSymbol] buildOutline(current:(DeclInStruct)`<Type _> <DId id> <Arguments? _> <Size? _> <SideCondition? _>`)
    = [symbol("token <id>", field(), current.src)];

list[DocumentSymbol] buildOutline(current:(DeclInStruct)`<Type _> <Id id> = <Expr _>`)
    = [symbol("token <id>", field(), current.src)];

list[DocumentSymbol] buildOutline(current:(DeclInChoice)`abstract <Type _> <Id id>`)
    = [symbol("abstract <id>", field(), current.src)];

list[DocumentSymbol] buildOutline(current:(DeclInChoice)`<Type tp> <Arguments? _> <Size? _>`)
    = [symbol("token <tp>", field(), current.src)];

Summary birdSummarizer(loc l, start[Program] input) {
    jobStart("Bird Summarizer");
    // pc = makeConfig(l);
    jobStep("Bird Summarizer", l.file);
    pcfg = calculatePathConfig(input);
    tm = birdTModelFromTree(input, pathConf = pcfg);
    jobEnd("Bird Summarizer");
    if (tm.messages == []) {
        jobStart("Bird compiler");
        try {
            compileBirdModule(input, tm, "engineering.swat.bird.generated", pcfg);
        }
        catch e: {
            println("Bird compilation failed: <e>");
        }
        jobEnd("Bird compiler");
    }

    return summary(l,
        messages = {<message.at, message> | message <- tm.messages, message.at.top == l.top},
        definitions = tm.useDef,
        references = tm.useDef<1,0>);
}

PathConfig calculatePathConfig(start[Program] input) {
    loc srcPath = input.src.top;
    for (Id _ <- input.top.moduleName.moduleName) {
        srcPath = srcPath.parent;
    }
    loc outputPath = srcPath.parent + "java";
    return pathConfig(srcs=[srcPath], target=outputPath);
}



void main() {
    unregisterLanguage("Bird", "bird");
    registerLanguage(
        language(
            pathConfig(srcs=[|project://bird-core/src/main/rascal|, |project://bird-ide/src/main/rascal|, |jar+project://bird-core/target/lib/typepal.jar!/src|]),
            "Bird",
            "bird",
            "lang::bird::LanguageServer",
            "birdLanguageContributor"));
}