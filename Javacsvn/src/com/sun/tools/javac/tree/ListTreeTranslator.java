package com.sun.tools.javac.tree;

import java.util.Vector;

import com.sun.tools.classfile.Opcode;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCListAccess;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCNewList;
import com.sun.tools.javac.tree.JCTree.JCParens;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;

public class ListTreeTranslator extends TreeTranslator {
	private final Names names ;
	private final Context context;
	private final TreeMaker treeMaker;
	private final Log log;
	private Vector<Boolean> isLefts=new Vector<Boolean>();
	private boolean isLeft=false;
	public ListTreeTranslator(Context context) {
		this.context = context;
		names = Names.instance(context);
		treeMaker = TreeMaker.instance(context);
		log=Log.instance(context);
	}
	@Override
	public void visitAssign(JCAssign tree) {	//isLeft is used to different list[i]=a(true) or a=list[i](false)
		isLefts.add(isLeft);	//save isLeft states, to solve nested problem such as list[list[1]=a]=a;
		isLeft=true;			//set isLeft=true
        tree.lhs = translate(tree.lhs);				//translate left tree of assign
        
        isLeft=false;
        tree.rhs = translate(tree.rhs);				//translate right tree of assign
        isLeft=isLefts.remove(isLefts.size()-1);	//restore states
        result = tree;
    }
	
	@Override
	public void visitIndexed(JCArrayAccess tree) {
		tree.indexed = translate(tree.indexed);
		tree.index = translate(tree.index);

		JCIdent selected = (JCIdent) tree.indexed;
		Name typeName = selected.type.tsym.flatName();

		if (typeName.equals(names.fromString("java.util.List"))) // if list
		{
			//translate k[expr]==> k[expr+((expr<0)?k.size():0)]
			JCLiteral false_part=treeMaker.Literal(4, 0);												//0
			JCFieldAccess list_size_meth=treeMaker.Select(tree.indexed, names.fromString("size"));		//list.size
			JCMethodInvocation true_part=treeMaker.Apply(null, list_size_meth, List.<JCExpression>nil());//list.size()
			
			JCBinary cond_expr_0=treeMaker.Binary(64, treeMaker.Parens(tree.index), treeMaker.Literal(0));//(expr)<0
			JCConditional conditional=treeMaker.Conditional(cond_expr_0, true_part, false_part);		//(expr)<0?list.size():0
			
			JCBinary indexed_binary=treeMaker.Binary(71, treeMaker.Parens(tree.index), treeMaker.Parens(conditional));	// (expr)+((expr)<0?list.size():0)
			
			if(!isLeft)		//list.get(2)
			{
				List<JCExpression> args = List.of((JCExpression)indexed_binary);
				Name get = names.fromString("get");
				JCFieldAccess list_get = treeMaker.Select(selected, get);
				JCMethodInvocation list_get_k = treeMaker.Apply(null, list_get,
						args);
				result = list_get_k;
				
			}else 		//list.set
			{
				result=null;
			}
			
		} else if (typeName.equals(names.fromString("Array"))) {// if array
			result = tree;
		}else {
			log.error(tree.pos(), "array.req.but.found", typeName);
		}
	}

	@Override
	public void visitIndexedL(JCListAccess tree) {
		tree.indexed = translate(tree.indexed);
		tree.term1 = translate(tree.term1);
		tree.term2 = translate(tree.term2);
		tree.term3 = translate(tree.term3);
		// __list_access();
		List<JCExpression> args = List.of(tree.indexed,tree.term1,tree.term2,tree.term3);
		JCIdent list_access = treeMaker.Ident(names.fromString("__list_access"));

		JCMethodInvocation list_access_meth = treeMaker.Apply(null, list_access, args);

		result = list_access_meth;
	}

	@Override
	public void visitNewList(JCNewList tree) {
		tree.elemtype = translate(tree.elemtype);
		tree.elems = translate(tree.elems);
		// result = tree;

		// methord: Arrays.asList()
		JCIdent arrays = treeMaker.Ident(names.fromString("Arrays"));
		Name asList = names.fromString("asList");
		JCFieldAccess arrays_asList = treeMaker.Select(arrays, asList);

		JCMethodInvocation arrays_asListInvocation = treeMaker.Apply(null,
				arrays_asList, tree.elems);

		// NEW: new ArrayList()
		List<JCExpression> args_new = List
				.of((JCExpression) arrays_asListInvocation);

		JCIdent class_new = treeMaker.Ident(names.fromString("ArrayList"));

		JCNewClass newClass = treeMaker.NewClass(null, null, class_new,
				args_new, null);

		result = newClass;
	}

}
