package net.networkdowntime.javaAnalyzer.javaModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import net.networkdowntime.javaAnalyzer.JavaAnalyzer;
import net.networkdowntime.javaAnalyzer.viewFilter.JavaFilter;
import net.networkdowntime.renderer.GraphvizRenderer;


public class Class extends DependentBase {

	String name;
	Package pkg;
	boolean isInterface = false;
	boolean isAbstract = false;
	boolean isAnnotation = false;
	boolean isEnum = false;

	List<Package> packageDependencies = new ArrayList<Package>();
	Map<String, Method> methods = new HashMap<String, Method>();
	List<String> imports = new ArrayList<String>();

	Class extnds = null; // this is deferred for now
	String extndsStr = null;
	List<Class> impls = new ArrayList<Class>();

	public Class(Package pkg, String name, boolean isInterface, boolean isAbstract) {
		this.pkg = pkg;
		this.name = name;
		this.isInterface = isInterface;
		this.isAbstract = isAbstract;
		JavaAnalyzer.log(2, "Creating Class: " + pkg.getName() + "." + name);
	}

	public void setExtendsStr(String extndsString) {
		this.extndsStr = extndsString;
	}

	public String getName() {
		return this.name;
	}

	public String getCanonicalName() {
		return this.pkg.getName() + "." + this.name;
	}

	public Method getOrCreateAndGetMethod(String name) {
		Method method = methods.get(name);
		if (method == null) {
			method = new Method(this, name);
			methods.put(name, method);
		}
		return method;
	}

	public void addImport(String name) {
		if (name != null && name.length() > 0) {
			imports.add(name);
			String pkgOrClassName = name.substring(name.lastIndexOf(".") + 1);
			boolean isClass = Character.isUpperCase(pkgOrClassName.charAt(0));

			if (!isClass) {
				Package pkg1 = this.pkg.prj.getOrCreateAndGetPackage(name, false);
				this.packageDependencies.add(pkg1);
			} else {
				String pkgName = name.substring(0, name.lastIndexOf("."));
				String className = pkgOrClassName;
				Package pkg1 = this.pkg.prj.getOrCreateAndGetPackage(pkgName, false);
				this.packageDependencies.add(pkg1);
				pkg1.getOrCreateAndGetClass(className);
			}

		}
	}

	public void setIsInterface(boolean isInterface) {
		this.isInterface = isInterface;
	}

	public void setIsAnnotation(boolean isAnnotation) {
		this.isAnnotation = isAnnotation;
	}

	public void setIsEnum(boolean isEnum) {
		this.isEnum = isEnum;
	}

	public void setIsAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public Class searchForUnresolvedClass(String className) {
		JavaAnalyzer.log(1, "Class.searchForUnresolvedClass(" + className + ")");

		Class matchedClass = super.searchForUnresolvedClass(className);

		if (matchedClass == null) {
			matchedClass = pkg.searchForUnresolvedClass(name, className);
		}
		return matchedClass;
	}

	public void validate() {
		JavaAnalyzer.log(1, "Validating class: " + getCanonicalName());

		if (extndsStr != null) {
			Class clazz = pkg.searchForUnresolvedClass(name, extndsStr);
			if (clazz != null) {
				extnds = clazz;
				addResolvedClass(clazz);
			}
		}

		// ToDo: Need to properly handle implements

		super.validate();

		for (Method method : methods.values()) {
			method.validate();
		}
	}

	public String createGraph(GraphvizRenderer renderer, JavaFilter filter) {
		System.out.println("\tClass: " + this.name);

		HashSet<String> refsToSkip = new HashSet<String>();

		StringBuffer sb = new StringBuffer();

		sb.append(renderer.getBeginRecord(this.pkg.name + "." + this.name, this.name, ""));

		if (filter.isShowFields()) {
			for (String field : this.varNameClassMap.keySet()) {
				Class clazz = this.varNameClassMap.get(field);
				sb.append(renderer.addRecordField(field, field + ": " + clazz.name));
			}
		}
		
		if (filter.isShowMethods()) {
			for (Method method : methods.values()) {
				sb.append(renderer.addRecordField(method.name, method.name));
			}
		}

		sb.append(renderer.getEndRecord());

		if (extnds != null) {
			boolean exclude = false;
			for (String excludePkg : Project.excludePkgs) {
				if (pkg.name.startsWith(excludePkg))
					exclude = true;
			}
			if (!exclude)
				sb.append(renderer.addEdge(this.pkg.name + "." + this.name, extnds.pkg.name + "." + extnds.name, "", true));

			Integer count = this.unresolvedClassCount.get(extnds.name);
			if (count != null && count.intValue() == 1) {
				refsToSkip.add(extnds.name);
			}
		}

		for (Class clazz : this.impls) {
			sb.append(renderer.addEdge(this.pkg.name + "." + this.name, clazz.pkg.name + "." + clazz.name, "", true));
			Integer count = this.unresolvedClassCount.get(clazz.name);
			if (count != null && count.intValue() == 1) {
				boolean exclude = filter.getPackagesToExclude().contains(clazz.pkg.name);
				if (!exclude)
					refsToSkip.add(clazz.name);
			}
		}

		for (Class clazz : this.classDependencies.values()) {
			if (!refsToSkip.contains(clazz.name)) {
				boolean exclude = filter.getPackagesToExclude().contains(clazz.pkg.name);
				if (!exclude)
					sb.append(renderer.addEdge(this.pkg.name + "." + this.name, clazz.pkg.name + "." + clazz.name, ""));
			}
		}

		return sb.toString();
	}

}
