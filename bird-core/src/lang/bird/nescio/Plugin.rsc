module lang::bird::nescio::Plugin



import lang::nescio::Plugin;

import lang::nescio::API;

import lang::bird::Syntax;
import lang::bird::Checker;
import lang::bird::nescio::NescioPlugin;

import ParseTree;
import IO;
import util::IDE;

private str BIRD_LANG_NAME = "bird";

private loc BIRD_BASE_DIR = |project://bird-core/bird-src/|;

Contribution commonSyntaxProperties 
    = syntaxProperties(
        fences = {<"{","}">,<"(",")">}, 
        lineComment = "//", 
        blockComment = <"/*","*","*/">
    );
    
Tree checkBird(Tree input){
    model = birdTModelFromTree(input); // your function that collects & solves
    types = getFacts(model);
  
  return input[@messages={*getMessages(model)}]
              [@hyperlinks=getUseDef(model)]
              [@docs=(l:"<prettyPrintAType(types[l])>" | l <- types)]
         ; 
}

void main() {
	println("Registering plugin...");
	LanguageConf birdConf = languageConf(birdGraphCalculator, buildBirdModulesComputer(BIRD_BASE_DIR), buildBirdModuleMapper(BIRD_BASE_DIR));
	
	registerLanguage(NESCIO_LANG_NAME, "nescio", Tree(str src, loc org) {
		return parseNescio(src, org);
 	});
 	
	registerContributions(NESCIO_LANG_NAME, {
        commonSyntaxProperties,
        treeProperties(hasQuickFixes = false), // performance
        annotator(checkNescio((BIRD_LANG_NAME:birdConf)))
    });

	registerLanguage(BIRD_LANG_NAME, "bird", Tree(str src, loc org) {
		return parse(#start[Program], src, org);
 	});
 	
 	registerContributions(BIRD_LANG_NAME, {
        commonSyntaxProperties,
        //compiler,
        treeProperties(hasQuickFixes = false), // performance
        annotator(checkBird)
    });
}