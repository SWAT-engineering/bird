package lang.birdnescio.ide;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.rascalmpl.eclipse.Activator;
import org.rascalmpl.eclipse.IRascalResources;
import org.rascalmpl.eclipse.util.RascalEclipseManifest;
import org.rascalmpl.uri.ProjectURIResolver;
import org.rascalmpl.uri.URIResolverRegistry;

import io.usethesource.vallang.ISourceLocation;

public class NewNescioFile extends Wizard implements INewWizard {
	private NewNescioFilePage page;
	private ISelection selection;
	private String moduleName;
	
	public NewNescioFile() {
		super();
		setNeedsProgressMonitor(true);
		setWindowTitle("Create a new Nescio module file");
	}
	
	@Override
	public void addPages() {
		page = new NewNescioFilePage(selection);
		addPage(page);
	}
	
	@Override
	public boolean performFinish() {
		String container = page.getContainerName();
		if (container.endsWith("/")) {
		  container = container.substring(0, container.length() - 1);
		}
		final String containerName = container;
		final String filename = page.getFileName();
		IRunnableWithProgress op = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				try {
					IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
					IPath path = new Path(containerName);
					IProject project = root.getFolder(path).getProject();
					
					int till = containerName.substring(1).indexOf("/");
					String containerToPutFileIn = containerName;
					String fileToCreate = filename;
					if(till != -1){
						containerToPutFileIn = containerName.substring(0, till + 1);
						fileToCreate = containerName.substring(till + 1) + "/" + filename;
					}
					
					fileToCreate = fileToCreate.endsWith(".nescio") ? fileToCreate : fileToCreate + ".nescio";
					
					List<String> srcs = new RascalEclipseManifest().getSourceRoots(project);
					moduleName = fileToCreate;
					
					for (String src : srcs) {
					  if (moduleName.startsWith("/" + src)) {
					    moduleName = moduleName.substring(src.length() + 1);
					    break;
					  }
					}

					if (moduleName.startsWith("/")) {
						moduleName = moduleName.substring(1);
					}
					
					moduleName = moduleName.substring(0, moduleName.length() - ".nescio".length());

					doFinish(containerToPutFileIn, fileToCreate, monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(true, false, op);
		} catch (InterruptedException e) {
			return false;
		} catch (InvocationTargetException e) {
			Throwable realException = e.getTargetException();
			MessageDialog.openError(getShell(), "Error", realException.getMessage());
			return false;
		}
		return true;
	}
	
	private void doFinish(
		String containerName,
		String fileName,
		IProgressMonitor monitor)
		throws CoreException {
		// create a sample file
		monitor.beginTask("Creating " + fileName, 2);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		Path path = new Path(containerName);
		IResource resource = path.segmentCount() > 1 ? 
				root.getProject(path.segment(0)).getFolder(path.removeFirstSegments(1))
		: root.getProject(path.segment(0));
				
		
		if (!resource.exists() || !(resource instanceof IContainer)) {
			throwCoreException("Container \"" + containerName + "\" does not exist.");
		}
		IContainer container = (IContainer) resource;
		final IFile file = container.getFile(new Path(fileName));
		ISourceLocation loc = ProjectURIResolver.constructProjectURI(file.getFullPath());
		
		try (OutputStream out = URIResolverRegistry.getInstance().getOutputStream(loc, false)) {
			try (PrintWriter w = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
				w.println("module " + moduleName.replace("/", "::") );
				w.println("	forLanguage bird   // one can specify another language if there is a nescio plugin for it");
				w.println("	rootNode Foo");
				w.println();
				w.println("// import external format module(s)");
				w.println();
				w.println("rule foo:");
				w.println("    /**/Foo => toZeros // matches all the nodes whose type is Foo and apply transformation");
				w.println();
				w.println("// Description of the transformation");
				w.println("@(engineering.swat.nescio.algorithms.ToZeros.apply)"); 
			    w.println("algorithm toZeros()");
			    w.println();
			}
			
			monitor.worked(1);
			monitor.setTaskName("Opening file for editing...");
			getShell().getDisplay().asyncExec(new Runnable() {
				@Override
				public void run() {
					IWorkbenchPage page =
						PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					try {
						IDE.openEditor(page, file, true);
					} catch (PartInitException e) {
					}
				}
			});
			monitor.worked(1);
		} 
		catch (IOException e) {
			Activator.log("could not create new Control module", e);
		}
	}
	
	private void throwCoreException(String message) throws CoreException {
		IStatus status =
			new Status(IStatus.ERROR, IRascalResources.ID_RASCAL_ECLIPSE_PLUGIN, IStatus.OK, message, null);
		throw new CoreException(status);
	}
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}
}
