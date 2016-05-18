package net.networkdowntime.javaAnalyzer.javaModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class DependentBase {
	private static final Logger LOGGER = LogManager.getLogger("javaModel");

	DependentBase parent;
	String name = "";

	Set<Class> annotationDependencies = new HashSet<Class>();
	Map<String, Class> classDependencies = new HashMap<String, Class>();
	// HashSet<Method> methodDependencies = new HashSet<Method>();

	Set<String> unresolvedAnnotations = new HashSet<String>();
	Set<String> unresolvedClasses = new HashSet<String>();
	Set<String> unresolvedInterfaces = new HashSet<String>();
	private HashMap<String, Integer> unresolvedClassCount = new HashMap<String, Integer>();

	Map<String, Set<String>> unresolvedMethods = new HashMap<String, Set<String>>();

	Map<String, String> varNameTypeMap = new LinkedHashMap<String, String>(); // This is the unqualified Type
	Map<Class, Set<Method>> methodCallMap = new HashMap<Class, Set<Method>>();

	private HashMap<String, Class> varNameClassMap = new LinkedHashMap<String, Class>();

	public void setClass(DependentBase clazz) {
		this.parent = clazz;
	}

	public DependentBase getParent() {
		return parent;
	}

	public Map<String, Class> getClassDependencies() {
		return classDependencies;
	}

	public String getName() {
		if (this instanceof Class) {
			Class currentClass = (Class) this;
			if (currentClass.isAnonymous) {
				return currentClass.anonymousClassDefinedIn.getName() + "." + currentClass.name;
			} else {
				return currentClass.name;
			}
		} else if (this instanceof Method) {
			Method method = (Method) this;
			return method.parent.getName() + "." + method.name;
		} else {
			if (parent != null) {
				return parent.getName();
			} else {
				return "";
			}
		}
	}

	public String getCanonicalName() {
		if (this instanceof Class) {
			Class currentClass = (Class) this;
			if (currentClass.isAnonymous) {
				return currentClass.anonymousClassDefinedIn.getCanonicalName() + "." + currentClass.name;
			} else {
				return currentClass.getPkg().getName() + "." + currentClass.name;
			}
		} else if (this instanceof Method) {
			Method method = (Method) this;
			return method.parent.getCanonicalName() + "." + method.name;
		} else {
			if (parent != null) {
				return parent.getCanonicalName();
			} else {
				return "";
			}
		}
	}

	public void addPotentialClass(int depth, String className) {
		boolean found = false;

		DependentBase base = this;

		while (base != null) {
			if (base.varNameTypeMap.containsKey(className) || base.unresolvedAnnotations.contains(className)) {
				found = true;
			}
			base = base.parent;
		}

		if (!found) {
			addUnresolvedClass(depth, className);
		}
	}

	public void addUnresolvedAnnotations(String annotationName) {
		if (!this.unresolvedAnnotations.contains(annotationName)) {
			this.unresolvedAnnotations.add(annotationName);
			logIndented(3, "Adding unresolved annotation: " + annotationName);
		}
	}

	public void addUnresolvedInterface(String interfaceName) {
		if (!this.unresolvedInterfaces.contains(interfaceName)) {
			this.unresolvedInterfaces.add(interfaceName);
			logIndented(3, "Adding unresolved interface: " + interfaceName);
		}
	}

	public void addUnresolvedClass(int depth, String className) {
		if (className.contains("[")) // remove array notation if needed
			className = className.substring(0, className.indexOf("["));

		if (!isVoid(className) && !isPrimative(className) && !"this".equals(className) && !this.unresolvedClasses.contains(className)) {
			Integer count = this.getUnresolvedClassCount().get(className);
			if (count == null)
				count = 0;
			this.unresolvedClasses.add(className);
			this.getUnresolvedClassCount().put(className, count.intValue() + 1);
			logIndented(depth, "Adding unresolved class: " + className);
		}
	}

	public void addUnresolvedMethodCall(int depth, String typeOrVarName, String methodName) {
		logIndented(depth, "Adding unresolved method call: " + typeOrVarName + " -> " + methodName);

		Set<String> methods = unresolvedMethods.get(varNameTypeMap);
		if (methods == null) {
			methods = new HashSet<String>();
			unresolvedMethods.put(typeOrVarName, methods);
		}
		methods.add(methodName);
	}

	public void addVariable(int depth, String name, String type) {
		if (!varNameTypeMap.containsKey(name)) {
			logIndented(depth, "Adding variable to " + this.getCanonicalName() + ": " + type + " " + name);
			varNameTypeMap.put(name, type);
		}
	}

	public void addResolvedClass(Class clazz) {
		classDependencies.put(clazz.name, clazz);

		if (this instanceof Class) {
			clazz.addReferencedByClass((Class) this);
		}
		if (parent != null) {
			parent.addResolvedClass(clazz);
		}
	}

	public Class searchForUnresolvedClass(int depth, String className) {
		logIndented(depth, "DependentBase.searchForUnresolvedClass(" + className + ")");
		Class matchedClass = classDependencies.get(className);

		if (matchedClass == null) {
			if (parent != null)
				matchedClass = parent.searchForUnresolvedClass(depth + 1, className);
		}
		return matchedClass;
	}

	public Class searchForVariableClass(int depth, String variableName) {
		logIndented(depth, "DependentBase.searchForVariable(" + variableName + ") in " + getCanonicalName());

		for (String varName : getVarNameClassMap().keySet()) {
			logIndented(depth + 1, "Considering variable " + varName + " of type " + getVarNameClassMap().get(varName).getName() + "; matched=" + variableName.trim().equals(varName.trim()));
		}

		Class matchedClass = getVarNameClassMap().get(variableName);

		if (matchedClass == null) {
			if (parent != null)
				matchedClass = parent.searchForVariableClass(depth + 1, variableName);
		}
		return matchedClass;
	}

	public static List<String> splitType(String type) {
		List<String> genericsExpansion = new ArrayList<String>();
		type = type.replaceAll("[<|,>]", " ");
		for (String genericType : type.split("\\s+")) {
			genericsExpansion.add(genericType.trim());
		}
		return genericsExpansion;
	}

	public Class findClass() {

		DependentBase db = this;

		while (db.getParent() != null)
			db = db.getParent();

		if (db instanceof Class) {
			return (Class) db;
		} else {
			return null;
		}
	}

	public void validatePassOne(int depth) {
		Class c = findClass();

		if (this instanceof Method) {
			logIndented(depth, "Validating Method: " + ((Method) this).name + "; method's class: " + c.getCanonicalName());
		} else {
			logIndented(depth, "Validating Block");
		}

		for (String s : this.unresolvedInterfaces) {
			logIndented(depth + 1, "Class " + c.getName() + ": Searching for unresolved interfaces: " + s);
			Class clazz = searchForUnresolvedClass(depth + 2, s);
			if (clazz != null) {
				logIndented(depth + 2, "Matched unresolved interface: " + s + " to " + clazz.getCanonicalName());
				addResolvedClass(clazz);
				this.annotationDependencies.add(clazz);
			}
		}

		for (String s : this.unresolvedAnnotations) {
			logIndented(depth + 1, "Class " + c.getName() + ": Searching for unresolved annotation: " + s);
			Class clazz = searchForUnresolvedClass(depth + 2, s);
			if (clazz != null) {
				logIndented(depth + 2, "Matched unresolved annotation: " + s + " to " + clazz.getCanonicalName());
				addResolvedClass(clazz);
				this.annotationDependencies.add(clazz);
			}
		}

		for (String s : this.unresolvedClasses) {
			logIndented(depth + 1, "Class " + c.getName() + ": Searching for unresolved class: " + s);
			Class clazz = searchForUnresolvedClass(depth + 2, s);
			if (clazz != null) {
				logIndented(depth + 2, "Matched unresolved class: " + s + " to " + clazz.getCanonicalName());
				addResolvedClass(clazz);
			}
		}

		for (String varName : varNameTypeMap.keySet()) {
			for (String type : splitType(varNameTypeMap.get(varName))) {

				logIndented(depth + 1,
						"Class " + c.getName() + ": Searching for class for variable: " + type + " " + varName + "; isPrimative=" + isPrimative(type) + "; is \"this\"=" + "this".equals(type));
				if (!(isPrimative(type) || "this".equals(type))) {
					Class clazz = searchForUnresolvedClass(depth + 2, type);

					if (clazz != null) {
						logIndented(depth + 2, "Matched unresolved class: " + type + " to " + clazz.getCanonicalName());
						if (!"this".equals(type))
							addResolvedClass(clazz);
						getVarNameClassMap().put(varName, clazz);
					}
				}
			}
		}

		if (this instanceof Block) {
			Block b = (Block) this;
			for (Block block : b.childBlocks) {
				block.validatePassOne(depth + 1);
			}
		}

	}

	public int validatePassTwo(int depth) {
		if (this instanceof Block) {
			Block b = (Block) this;
			for (Block block : b.childBlocks) {
				depth = block.validatePassTwo(depth + 1);
			}
		}

		logIndented(depth, this.getClass().getName() + " validatePassTwo(): " + getCanonicalName());

		for (String typeOrVarName : unresolvedMethods.keySet()) {
			Set<String> methodSet = unresolvedMethods.get(typeOrVarName);

			logIndented(depth + 1, "Checking for unresolved method call: " + typeOrVarName + " in " + getCanonicalName());

			String tovn = typeOrVarName;
			Class clazz = null;

			if (tovn.contains(".")) {
				String[] split = tovn.split("\\.");
				tovn = split[0];
			}

			if ("this".equals(tovn)) {
				clazz = findClass();
			} else if ("super".equals(tovn)) {
				// TODO should search the entends heirarchy to see if there is a method match, if not assume immediate extends
				clazz = findClass().getExtnds();
			} else if ("String".equals(tovn)) {
				clazz = searchForUnresolvedClass(depth + 2, "String");
			} else if (tovn.startsWith("\"") && tovn.endsWith("\"")) {
				clazz = searchForUnresolvedClass(depth + 2, "String");
			} else if ("null".equals(tovn) || tovn == null) {
				// nothing to do?
			} else {
				//TODO figure out how to handle chains.  i.e. System.out.println()
				// should add a field to System of type unknown something that has a method called println();
				// this attempts, going up the heirarchy to resolve the typeOrVarName to a defined variable
				clazz = searchForVariableClass(depth + 2, tovn);

				if (clazz == null) {
					clazz = searchForUnresolvedClass(depth + 2, tovn);
				}

			}

			if (clazz != null) {
				if (!"this".equals(tovn)) {
					addResolvedClass(clazz);
				}

				logIndented(depth + 2, "DependentBase.validatePassTwo() for " + findClass().name + ": typeOrVarName " + typeOrVarName + " matched to " + clazz.name);

				for (String methodName : methodSet) {
					Set<Method> methods = methodCallMap.get(clazz);
					if (methods == null) {
						methods = new HashSet<Method>();
						methodCallMap.put(clazz, methods);
					}

					Method method = null;
					if (clazz.fromFile) {
						method = clazz.getMethod(methodName + "()");

					} else {
						method = clazz.getOrCreateAndGetMethod(depth + 3, methodName + "()");
					}

					if (method != null) {
						methods.add(method);
						logIndented(depth + 3, "Found Method Call Reference: " + clazz.getCanonicalName() + "." + method.name);
					}
				}
			}
		}
		return depth + 1;
	}

	protected boolean isVoid(String name) {
		boolean retval = false;

		if ("void".equals(name))
			retval = true;

		return retval;
	}

	protected boolean isPrimative(String name) {
		boolean retval = false;

		if ("boolean".equals(name) || "boolean[]".equals(name))
			retval = true;
		else if ("byte".equals(name) || "byte[]".equals(name))
			retval = true;
		else if ("short".equals(name) || "short[]".equals(name))
			retval = true;
		else if ("int".equals(name) || "int[]".equals(name))
			retval = true;
		else if ("long".equals(name) || "long[]".equals(name))
			retval = true;
		else if ("float".equals(name) || "float[]".equals(name))
			retval = true;
		else if ("double".equals(name) || "double[]".equals(name))
			retval = true;
		else if ("char".equals(name) || "char[]".equals(name))
			retval = true;

		return retval;
	}

	public Map<String, Class> getVarNameClassMap() {
		return varNameClassMap;
	}

	public Map<String, Integer> getUnresolvedClassCount() {
		return unresolvedClassCount;
	}

	public void logIndented(int depth, String str) {
		if (LOGGER.isDebugEnabled()) {
			String retval = "";
			for (int i = 0; i < depth; i++) {
				retval += "    ";
			}
			LOGGER.debug(retval + str);
		}
	}

}