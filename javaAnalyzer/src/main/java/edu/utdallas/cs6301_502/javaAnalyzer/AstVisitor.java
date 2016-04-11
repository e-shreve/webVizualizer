package edu.utdallas.cs6301_502.javaAnalyzer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.TypeParameter;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EmptyMemberDeclaration;
import com.github.javaparser.ast.body.EmptyTypeDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.MultiTypeParameter;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.body.VariableDeclaratorId;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.IntegerLiteralMinValueExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralMinValueExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.QualifiedNameExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ForeachStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.TypeDeclarationStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import edu.utdallas.cs6301_502.javaAnalyzer.javaModel.Package;
import edu.utdallas.cs6301_502.javaAnalyzer.javaModel.Project;
import edu.utdallas.cs6301_502.javaAnalyzer.javaModel.Class;

public class AstVisitor extends VoidVisitorAdapter {
	// begin dump the AST
	int depth = 0;
	BufferedWriter astDumpWriter = null;
	// end dump the AST
	
	private static final boolean LOG = true;
	CompilationUnit cu = null;
	
	Set<String> imports = new LinkedHashSet<String>();
	Project project = null;
	Stack<Class> classStack = new Stack<Class>();
	Class base = null;
	private Package pkg;
	
	public static void main(String[] args) {
		Project prj = new Project();

		long time = System.currentTimeMillis();
		
		prj.addFile(new File("src/test/java/testClasses/"));
		
		System.out.println("Time to parse files (ms): " + (System.currentTimeMillis() - time));
		time = System.currentTimeMillis();
		
		prj.validate();
		System.out.println("Time to validate (ms): " + (System.currentTimeMillis() - time));
	}

	public AstVisitor(int depth, String fileName, Project prj, Class base, CompilationUnit cu, List<TypeDeclaration> typeDeclarations) {
		try {
			File dir = new File("astDumps");
			if (!dir.exists()) dir.mkdir();
			fileName = "astDumps/" + fileName.replace(".java", ".txt");
			File astDump = new File(fileName);
			astDumpWriter = new BufferedWriter(new FileWriter(astDump));
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.depth = depth;
		this.project = prj;
		
		this.visit(cu, null);
	}

	public static void processTypeDeclarations(int depth, String fileName, Project prj, Class base, CompilationUnit cu, List<TypeDeclaration> typeDeclarations) {
		new AstVisitor(depth, fileName, prj, base, cu, typeDeclarations);
	}

	public void logAST(int depth, String str) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < depth; i++) {
			sb.append("\t");
		}
		sb.append(str);
		sb.append("\r");
		try {
			astDumpWriter.write(sb.toString());
			astDumpWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void log(int depth, String str) {
		if (LOG) {
			for (int i = 0; i < depth; i++) {
				System.out.print("\t");
			}
			System.out.println(str);
		}
	}

	private void addImports() {
		for (String importStr : imports) {
			base.addImport(3, importStr);
		}
	}
	@Override
	public void visit(AnnotationDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.getNameExpr().getName());

		if (n.getParentNode() instanceof CompilationUnit) {
			base = pkg.getOrCreateAndGetClass(1, n.getName(), true);
		} else {
			ClassOrInterfaceDeclaration parent = (ClassOrInterfaceDeclaration) n.getParentNode();
			base = pkg.getOrCreateAndGetClass(1, parent.getName() + "." + n.getName(), true);
		}
		base.setIsAnnotation(true);
		addImports();
		
		classStack.push(base);

		depth++;
		super.visit(n, arg);
		depth--;
		
		base = classStack.pop();
	}

	@Override
	public void visit(AnnotationMemberDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(ArrayAccessExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(ArrayCreationExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(ArrayInitializerExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(AssertStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(AssignExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(BinaryExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(BlockComment n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(BlockStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(BooleanLiteralExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(BreakStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(CastExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(CatchClause n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(CharLiteralExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(ClassExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(ClassOrInterfaceDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.getNameExpr().getName());

		if (n.getParentNode() instanceof CompilationUnit) {
			base = pkg.getOrCreateAndGetClass(1, n.getName(), true);
		} else {
			ClassOrInterfaceDeclaration parent = (ClassOrInterfaceDeclaration) n.getParentNode();
			base = pkg.getOrCreateAndGetClass(1, parent.getName() + "." + n.getName(), true);
		}
		base.setIsInterface(n.isInterface());
		addImports();
		
		for (ClassOrInterfaceType type : n.getExtends()) {
			base.setExtendsStr(type.getName());
			base.addUnresolvedClass(type.getName());
		}
		
		for (ClassOrInterfaceType type : n.getImplements()) {
			base.addImplsStr(type.getName());
			base.addUnresolvedInterface(type.getName());
		}
		
		classStack.push(base);
		
		depth++;
		super.visit(n, arg);
		depth--;


		base = classStack.pop();
	}

	@Override
	public void visit(ClassOrInterfaceType n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		String typeStr = n.toString();
		if (typeStr.contains("<")) { // Generics

			typeStr = typeStr.substring(0, typeStr.indexOf("<"));
//			log(depth + 1, "Generic Type - " + genericizedClass);
//			base.addUnresolvedClass(genericizedClass);
//
		} // else {
		base.addUnresolvedClass(typeStr);
//		}

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(CompilationUnit n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(ConditionalExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(ConstructorDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(ContinueStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(DoStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(DoubleLiteralExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(EmptyMemberDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(EmptyStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(EmptyTypeDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(EnclosedExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(EnumConstantDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(EnumDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.getNameExpr().getName());

		if (n.getParentNode() instanceof CompilationUnit) {
			base = pkg.getOrCreateAndGetClass(1, n.getName(), true);
		} else {
			ClassOrInterfaceDeclaration parent = (ClassOrInterfaceDeclaration) n.getParentNode();
			base = pkg.getOrCreateAndGetClass(1, parent.getName() + "." + n.getName(), true);
		}
		base.setIsEnum(true);
		addImports();

		classStack.push(base);

		depth++;
		super.visit(n, arg);
		depth--;

		
		base = classStack.pop();
	}

	@Override
	public void visit(ExplicitConstructorInvocationStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(ExpressionStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(FieldAccessExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(FieldDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		
		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(ForeachStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(ForStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(IfStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(ImportDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(InitializerDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(InstanceOfExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(IntegerLiteralExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(IntegerLiteralMinValueExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(JavadocComment n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(LabeledStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(LambdaExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(LineComment n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(LongLiteralExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(LongLiteralMinValueExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(MarkerAnnotationExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(MemberValuePair n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(MethodCallExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(MethodDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(MethodReferenceExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(MultiTypeParameter n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(NameExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString() + "; " + n.getParentNode().getClass().getName());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(NormalAnnotationExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(NullLiteralExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(ObjectCreationExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(PackageDeclaration n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString().trim());
		
		this.pkg = project.getOrCreateAndGetPackage(1, n.getName().getName(), true, true);
		
		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(Parameter n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(PrimitiveType n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		// I think these can be safely ignored

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(QualifiedNameExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		if (n.getParentNode() instanceof ImportDeclaration) {
			imports.add(n.toString());
		}
		
		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(ReferenceType n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		// I think these can be safely ignored
		
		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(ReturnStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(SingleMemberAnnotationExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(StringLiteralExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(SuperExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(SwitchEntryStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(SwitchStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(SynchronizedStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(ThisExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(ThrowStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(TryStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(TypeDeclarationStmt n, Object arg) {
		System.out.println(n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(TypeExpr n, Object arg) {
		System.out.println(n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(TypeParameter n, Object arg) {
		System.out.println(n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(UnaryExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(UnknownType n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(VariableDeclarationExpr n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(VariableDeclarator n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(VariableDeclaratorId n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(VoidType n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		// I think these can be safely ignored

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(WhileStmt n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		depth++;
		super.visit(n, arg);
		depth--;

	}

	@Override
	public void visit(WildcardType n, Object arg) {
		logAST(depth, n.getClass().getName() + "(" + n.getBeginLine() + "): " + n.toString());

		// I think these can be safely ignored

		depth++;
		super.visit(n, arg);
		depth--;

	}

}
