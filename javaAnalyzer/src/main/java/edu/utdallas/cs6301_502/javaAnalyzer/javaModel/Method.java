package edu.utdallas.cs6301_502.javaAnalyzer.javaModel;
import java.util.LinkedHashMap;
import edu.utdallas.cs6301_502.javaAnalyzer.AstVisitor;


public class Method extends Block {

	String name;
	LinkedHashMap<String, String> paramMap = new LinkedHashMap<String, String>();

	public Method(Class clazz, String name) {
		super(null);
		this.parent = clazz;
		this.name = name;
		AstVisitor.log(3, "Creating Method: " + clazz.pkg.getName() + "." + clazz.name + "." + name);
	}

	public String getName() {
		return this.name;
	}

	public void setParamMap(LinkedHashMap<String, String> paramMap) {
		assert (paramMap != null);

		this.paramMap = paramMap;

		for (String name : paramMap.keySet()) {
			AstVisitor.log(0, "Adding Method Parameter: " + name);
			this.addVariable(name, paramMap.get(name));
		}
	}

	@Override
	public void validatePassOne(int depth) {
		AstVisitor.log(depth, "Validating Method: " + getName());
		super.validatePassOne(depth + 1);
	}
	
	
}