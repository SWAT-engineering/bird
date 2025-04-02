import * as vscode from 'vscode';
import { ParameterizedLanguageServer, VSCodeUriResolverServer, LanguageParameter } from '@usethesource/rascal-vscode-dsl-lsp-server';
import { join } from 'path';

export function activate(context: vscode.ExtensionContext) {
	const birdCoreJar = `|jar+file://${context.extensionUri.path}/assets/jars/bird-core.jar!|`;
	const birdIdeJar = `|jar+file://${context.extensionUri.path}/assets/jars/bird-ide.jar!|`;
	const typepalJar = `|jar+file://${context.extensionUri.path}/assets/jars/typepal.jar!|`;
	const language = <LanguageParameter>{
		pathConfig: `pathConfig(srcs=[${birdCoreJar}, ${birdIdeJar}, ${typepalJar}])`,
		name: "Bird",
		extensions: ["bird"],
		mainModule: "lang::bird::LanguageServer",
		mainFunction: "birdLanguageContributor"
	};
	console.log(language);
	// rascal vscode needs an instance of this class, if you register multiple languages, they can share this vfs instance
	const vfs = new VSCodeUriResolverServer(false);
	// this starts the LSP server and connects it to rascal
	const lsp = new ParameterizedLanguageServer(context,
		vfs,
		calcJarPath(context),
		true,
		"bird", // vscode language ID
		"Bird", // vscode language Title (visible in the right bottom corner)
		language);
	// adding it to the subscriptions makes sure everything is closed down properly
	context.subscriptions.push(lsp);
}

function calcJarPath(context: vscode.ExtensionContext) {
	return context.asAbsolutePath(join('.', 'dist', 'rascal-lsp'));
}

export function deactivate() {}
