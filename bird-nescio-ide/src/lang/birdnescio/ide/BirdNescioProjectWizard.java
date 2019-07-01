package lang.birdnescio.ide;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.core.project.IBundleProjectDescription;
import org.eclipse.pde.core.project.IBundleProjectService;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.rascalmpl.eclipse.Activator;
import org.rascalmpl.eclipse.IRascalResources;
import org.rascalmpl.eclipse.util.RascalEclipseManifest;
import org.rascalmpl.interpreter.utils.RascalManifest;

/**
 * This code is based on rascal's project wizard.
 */
public class BirdNescioProjectWizard extends BasicNewProjectResourceWizard {

	@Override
	public boolean performFinish() {
		if (!super.performFinish()) {
			return false;
		}
		
		final IProject project = getNewProject();

		IRunnableWithProgress job = new IRunnableWithProgress() {
			@Override
			public void run(IProgressMonitor monitor) throws InvocationTargetException,
					InterruptedException {
				try {
					BundleContext context = Activator.getInstance().getBundle().getBundleContext();
					ServiceReference<IBundleProjectService> ref = context.getServiceReference(IBundleProjectService.class);
					try {
						IBundleProjectService service = context.getService(ref);
						IBundleProjectDescription plugin = service.getDescription(project);
						plugin.setBundleName(project.getName().replaceAll("[^a-zA-Z0-9_]", "_"));
						project.setDefaultCharset("UTF-8", monitor); 
						project.getFolder("mtl").create(true, false, monitor);
						project.getFolder("mvk").create(true, false, monitor);
						project.getFolder("generated").create(true, false, monitor);
						project.getFolder("lib").create(true, false, monitor);


						plugin.setSymbolicName(project.getName().replaceAll("[^a-zA-Z0-9_]", "_"));
						plugin.setNatureIds(new String[] { JavaCore.NATURE_ID, IBundleProjectDescription.PLUGIN_NATURE, IRascalResources.ID_TERM_NATURE});
						plugin.setBundleVersion(Version.parseVersion("1.0.0"));
						plugin.setExecutionEnvironments(new String[] { "JavaSE-1.8"}); // TODO: Is this a constant defined somewhere?

						IProjectDescription description = project.getDescription();
						description.setBuildConfigs(new String[] { "org.eclipse.jdt.core.javabuilder", "org.eclipse.pde.ManifestBuilder", "org.eclipse.pde.SchemaBuilder" });
						project.setDescription(description, monitor);

						createRascalManifest(project);
						plugin.apply(monitor);

						configureJavaProject(plugin, project, monitor);
					}
					finally {
						context.ungetService(ref);
					}
				} catch (CoreException e) {
					Activator.getInstance().logException("could not initialize MTL project", e);
					throw new InterruptedException();
				}
			}

			private void createRascalManifest(IProject project) throws CoreException {
				Manifest man = new RascalEclipseManifest().getDefaultManifest();
				man.getMainAttributes().remove("Courses");
				man.getMainAttributes().remove("Main-Function");
				man.getMainAttributes().remove("Main-Module");
				man.getMainAttributes().put(new Attributes.Name("Source"), "mtl");
				man.getMainAttributes().put(new Attributes.Name("Libraries"), "mvk");
				man.getMainAttributes().put(new Attributes.Name("Target"), "generated");
				man.getMainAttributes().put(new Attributes.Name("DockerImage"), "xlinqreg.azurecr.io/openitems-maverick-develop:latest");
				
				IFolder folder = project.getFolder("META-INF");
				if (!folder.exists()) {
					if (!new File(folder.getLocation().toOSString()).mkdirs()) {
						Activator.log("could not mkdir META-INF", new IOException());
						return;
					}
				}

				IFile rascalMF = project.getFile(new Path(RascalManifest.META_INF_RASCAL_MF)) ;
				if (!rascalMF.exists()) {
					try (FileOutputStream file = new FileOutputStream(rascalMF.getLocation().toOSString())) {
						man.write(file);
					} catch (IOException e) {
						Activator.log("could not create RASCAL.MF", e);
					}
				}

				project.refreshLocal(IProject.DEPTH_INFINITE, new NullProgressMonitor());
			}

			private void configureJavaProject(IBundleProjectDescription plugin, IProject project, IProgressMonitor monitor) throws CoreException {

				new Job("Initializing Java Project " + project.getName()) {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						try {

							ClassLoader cl = getClass().getClassLoader();
							if (cl instanceof BundleReference) {
								Bundle myBundle = ((BundleReference) cl).getBundle();
								URL jarSource = myBundle.getResource("lib/mtl-framework.jar");
								IFile target = project.getFile("lib/mtl-framework.jar");//.create(jarSource.openStream(), true, monitor);
								target.create(new ByteArrayInputStream(new byte[0]), true, null); // force file creation
								Files.copy(jarSource.openStream(), target.getRawLocation().toFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
								project.getFolder("lib").setHidden(true);
							}

							IJavaProject jProject = JavaCore.create(project);
							IClasspathEntry[] oldClasspath = jProject.getRawClasspath();
							IClasspathEntry[] newClasspath = new IClasspathEntry[oldClasspath.length + 3];
							System.arraycopy(oldClasspath, 0, newClasspath, 3, oldClasspath.length);
							newClasspath[0] = JavaRuntime.getDefaultJREContainerEntry();
							newClasspath[1] = JavaCore.newContainerEntry(new Path("org.eclipse.pde.core.requiredPlugins"));
							newClasspath[2] = JavaCore.newLibraryEntry(project.getFile("lib/mtl-framework.jar").getFullPath(), null, null);
							newClasspath[3] = JavaCore.newSourceEntry(project.getFolder("generated").getFullPath(), null);
							jProject.setRawClasspath(newClasspath, monitor);

							IFile cpFile = project.getFile(".classpath");
							if (cpFile.exists())
								cpFile.setHidden(true);
							IFile pFile = project.getFile(".project");
							if (pFile.exists())
								pFile.setHidden(true);
							IFile bpFile = project.getFile("build.properties");
							if (bpFile.exists())
								bpFile.setHidden(true);
							
							
							jProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
							return Status.OK_STATUS;
						} catch (CoreException | IOException e) {
							Activator.getInstance().logException("failed to initialize MTL project with Java nature: " + project.getName(), e);
							return Status.OK_STATUS;
						}
					}
				}.schedule();
			}
		};

		if (project != null) {
			try {
				getContainer().run(true, true, job);
			} catch (InvocationTargetException e) {
				Activator.getInstance().logException("could not initialize new MTL project", e);
				return false;
			} catch (InterruptedException e) {
				return false;
			}
			return true;
		}
		
		return false;
	}

}
