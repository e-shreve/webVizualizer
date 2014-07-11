package net.networkdowntime.javaAnalyzer.javaModel;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.networkdowntime.javaAnalyzer.JavaAnalyzer;
import net.networkdowntime.javaAnalyzer.viewFilter.JavaFilter;
import net.networkdowntime.renderer.GraphvizDotRenderer;
import net.networkdowntime.renderer.GraphvizRenderer;


public class Project {

	private Set<File> files = new HashSet<File>();
	private HashSet<String> scannedFiles = new HashSet<String>();
	Map<String, Package> packages = new HashMap<String, Package>();
	
	public Project() {
		getOrCreateAndGetPackage("java.lang", false);
	}

	public List<File> getFiles() {
		List<File> retval = new ArrayList<File>();
		retval.addAll(this.files);
		retval.sort(new Comparator<File>() {

			@Override
			public int compare(File file1, File file2) {
				return file1.compareTo(file2);
			}

		});
		return retval;
	}

	/**
	 * Adds a file or directory containing files to the list of files to be scanned.
	 * 
	 * @param file
	 */
	public void addFile(File file) {
		if (file.exists()) {
			files.add(file);
			scanFile(file);
		} else {
			JavaAnalyzer.log(0, file.getAbsolutePath() + " does not exist");
		}
	}

	/**
	 * Removes the specified file or directory from the scanned files.
	 * 
	 * @param file
	 */
	public void removeFile(File file) {
		if (file.exists()) {
			files.remove(file);
			deindexFile(file);
		} else {
			JavaAnalyzer.log(0, file.getAbsolutePath() + " does not exist");
		}
	}
	
	/**
	 * Scans the selected file or directory and adds it to the project
	 * 
	 * Preconditions: file exists
	 * @param fileToScan
	 */
	private void scanFile(File fileToScan) {
	
		List<File> filesToScan = new ArrayList<File>();
		filesToScan.addAll(getFiles(fileToScan));
	
		for (File f : filesToScan) {
			try {
				if (f.getName().endsWith(".java")) {
					CompilationUnit cu = JavaParser.parse(f);
					if (cu.getTypes() == null) {
						JavaAnalyzer.log(0, f.getAbsolutePath() + " has no classes");
					} else {
						JavaAnalyzer.processTypeDeclarations(0, this, null, cu, cu.getTypes());
						scannedFiles.add(f.getAbsolutePath());
					}
				}
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		System.out.println("\n");
	
	}

	private void deindexFile(File fileToDeindex) {
		// To Do Implement better deindexing
		// Simple implementation is to just redo everything
		
		scannedFiles = new HashSet<String>();
		packages = new HashMap<String, Package>();
		getOrCreateAndGetPackage("java.lang", false);

		for (File file : files) {
			scanFile(file);
		}
	}

	private static List<File> getFiles(File baseDir) {
		List<File> fileList = new ArrayList<File>();

		if (!baseDir.getAbsolutePath().contains(".svn")) {
			System.out.println(baseDir.getAbsolutePath() + ": exists " + baseDir.exists());

			String[] files = baseDir.list();
			String path = baseDir.getPath();

			for (String s : files) {
				File file = new File(path + File.separator + s);
				if (file.isDirectory()) {
					fileList.addAll(getFiles(file));
				} else {
					fileList.add(file);
				}
			}
		}
		return fileList;

	}

	public void addPackage(Package pkg) {
		if (!packages.containsKey(pkg.getName())) {
			packages.put(pkg.getName(), pkg);
		}
	}

	public Package getPackage(String name) {
		return packages.get(name);
	}

	public List<String> getPackageNames() {
		List<String> retval = new ArrayList<String>();
		
		for (Package p : packages.values()) {
			retval.add(p.name);
		}
		return retval;
	}

	public List<String> getClassNames() {
		List<String> retval = new ArrayList<String>();
		
		for (Package p : packages.values()) {
			for (Class c : p.classes.values())
				retval.add(c.name);
		}
		return retval;
	}

	public Package getOrCreateAndGetPackage(String name, boolean inPath) {
		Package pkg = packages.get(name);
		if (pkg == null) {
			pkg = new Package(name, inPath);
			pkg.setProject(this);
			packages.put(name, pkg);
		}

		if (inPath) {
			pkg.inPath = inPath;
		}

		return pkg;
	}

	public void validate() {
		for (Package pkg : packages.values()) {
			pkg.validate();
		}
	}

	public Class searchForClass(String pkgDoingSearch, String name) {
		// System.out.println("Project: Searching for unresolved class: " + name);
		Class clazz = null;
		for (String pkgName : packages.keySet()) {
			if (!pkgDoingSearch.equals(pkgName)) {
				Package pkg = packages.get(pkgName);
				clazz = pkg.searchForUnresolvedClass(null, name);
				if (clazz != null)
					break;
			}
		}

		if (clazz == null) {
			Package pkg = getOrCreateAndGetPackage("java.lang", false);
			clazz = pkg.getOrCreateAndGetClass(name);
		}
		return clazz;
	}

	public static final String[] excludePkgs = { "java.", "javax." };

	public String createGraph(JavaFilter filter) {
		GraphvizRenderer renderer = new GraphvizDotRenderer();

		StringBuffer sb = new StringBuffer();

		sb.append(renderer.getHeader());

		for (String pkgName : packages.keySet()) {
			Package pkg = packages.get(pkgName);
			// if (pkg.inPath) {
			boolean exclude = filter.getPackagesToExclude().contains(pkg.name);
			
			if (!exclude)
				sb.append(pkg.createGraph(renderer, filter));
		}

		sb.append(renderer.getFooter());

		return sb.toString();
	}

}
