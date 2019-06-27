module lang::bird::nescio::Plugin

import lang::nescio::Plugin;

import lang::nescio::Syntax;
import lang::nescio::API;

import lang::bird::nescio::NescioPlugin;

import ParseTree;
import util::IDE;

private str BIRD_LANG_NAME = "bird";

private loc BIRD_BASE_DIR = |project://bird-core/bird-src/|;

void main() {

	LanguageConf birdConf = languageConf(birdGraphCalculator, buildBirdModulesComputer(BIRD_BASE_DIR), buildBirdModuleMapper(BIRD_BASE_DIR));
	
	registerLanguage(NESCIO_LANG_NAME, "nescio", start[Specification](str src, loc org) {
		return parse(#start[Specification], src, org);
 	});
 	
 	registerContributions(NESCIO_LANG_NAME, {
        commonSyntaxProperties,
        treeProperties(hasQuickFixes = false), // performance
        annotator(checkNescio((BIRD_LANG_NAME:birdConf)))
    });

	registerLanguage(BIRD_LANG_NAME, "bird", start[Program](str src, loc org) {
		return parseBird(#start[Program], src, org);
 	});
}