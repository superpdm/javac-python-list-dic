package com.sun.tools.javac.tree;

import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCBlockExp;
import com.sun.tools.javac.tree.JCTree.JCListAccess;
import com.sun.tools.javac.tree.JCTree.JCListComp;
import com.sun.tools.javac.util.Context;

public class MyBlocks {
	
	private final  ParserFactory parserFactory;
	private final  TreeMaker treeMaker;
	private final Symtab syms;
	public MyBlocks(Context context)
	{
		parserFactory=ParserFactory.instance(context);
		treeMaker=TreeMaker.instance(context);
		syms=Symtab.instance(context);
	}
	/**
     *get block code of __list_access from name of them. 
     */
    private String getListAccessCode(JCListAccess tree)
    {
    	String list = tree.indexed.toString();
    	String beg = tree.term1.toString();
    	String end = tree.term2.toString();
    	String step = tree.term3.toString();
    	String code_list_access
    	= "{"
    		+"		List list = " + list + ";"
    		+"		int beg = " + beg + ";"
    		+"      int end = " + end + ";"
    		+"      int step = " + step + ";"
    		+ "		int len = list.size();                                           	"
    		+ "		java.util.List tmpList = new java.util.ArrayList();              	"
    		+ "                                                                         "
    		+ "		if (step == Integer.MAX_VALUE)                                   	"
    		+ "			step = 1;                                                    	"
    		+ "		if (step > 0) {                                                  	"
    		+ "			beg += (beg < 0) ? len : 0;                                  	"
    		+ "			end += (end < 0) ? len : 0;                                     "
    		+ "                                                                      	"
    		+ "			if (beg == Integer.MAX_VALUE)                                	"
    		+ "				beg = 0;                                                 	"
    		+ "			if (end == Integer.MAX_VALUE)                                	"
    		+ "				end = len;                                               	"
    		+ "			for (int i = beg; i < end; i += step) {                      	"
    		+ "				tmpList.add(list.get(i));                                	"
    		+ "			}                                                            	"
    		+ "		} else {                                                         	"
    		+ "			beg += (beg < 0) ? len : 0;                                  	"
    		+ "			end += (end < 0) ? len : 0;                                  	"
    		+ "                                                                      	"
    		+ "			if (beg == Integer.MAX_VALUE)                                	"
    		+ "				beg = len - 1;                                           	"
    		+ "			if (end == Integer.MAX_VALUE)                                	"
    		+ "				end = -1;                                                	"
    		+ "                                                                         "
    		+ "			for (int i = beg; i > end; i += step) {                         "
    		+ "				tmpList.add(list.get(i));                                   "
    		+ "			}                                                               "
    		+ "		}                                                                   "
    		+ "		tmpList=tmpList;                                                     "
    		+ "	} end ";
    	return code_list_access;
    }


    private JCBlockExp parseBlockExp(String code)
    {
    	JavacParser parser=(JavacParser) parserFactory.newParser(code.subSequence(0, code.length()-1), false, false, false);
    	JCBlock block=parser.block();
    	JCBlockExp blockExp=treeMaker.BlockExp(block);
    	return blockExp;
    }
    public JCBlockExp getListAccessBlock(JCListAccess tree)
    {
    	String code=getListAccessCode(tree);
    	JCBlockExp blockExp=parseBlockExp(code);
    	return blockExp;
    }
    
    private String getListCompCode(JCListComp tree)
    {
    	String decl=tree.decl.toString();
    	String expr=tree.expr.toString();
    	String listExpr=tree.listExpr.toString();
    	String ifExpr="";
    	if(tree.ifExpr!=null)
    		ifExpr=tree.ifExpr.toString();
    	
    	String code="{"
    		+"java.util.List tmpList = new ArrayList();"
    		+"for("+decl+":"+listExpr+")";
    	if(ifExpr!="")
    		code+=
    			"	if("+ifExpr+")";
    	code+=
    		"		tmpList.add("+expr+");"	
    		+"tmpList=tmpList;"
    		+"} end";
    	return code;
    }
    public JCBlockExp getListCompBlock(JCListComp tree)
    {
    	String code=getListCompCode(tree);
    	JCBlockExp blockExp=parseBlockExp(code);
    	return blockExp;
    }

    private String getListAddCode(JCBinary tree)
    {
    	String code="{"
    		+"java.util.List __tmp=new ArrayList("+tree.lhs.toString()+");"
    		+"__tmp.addAll("+tree.rhs.toString()+");"
    		+"__tmp=__tmp;"
    		+"} end";
    	return code;
    }
    
    public JCBlockExp getListAddBlock(JCBinary tree)
    {
    	String code=getListAddCode(tree);
    	JCBlockExp blockExp=parseBlockExp(code);
    	return blockExp;
    }
    private String getListMulCode(JCBinary tree)
    {
    	JCTree listTree,intTree;
    	if(tree.lhs.type.tsym==syms.listType.tsym){
    		listTree=tree.lhs;
    		intTree=tree.rhs;
    	}else
    	{
    		listTree=tree.rhs;
    		intTree=tree.lhs;
    	}
    	String code="{"
    		+"java.util.List __list="+listTree.toString()+";"
    		+"java.util.List __tmp=new ArrayList(__list);"
    		+"for(int i=0;i<"+intTree.toString()+"-1;i++)"
    		+"   __tmp.addAll(__list);"
    		+"__tmp=__tmp;"
    		+"} end";
    	return code;
    }
    public JCBlockExp getListMulBlock(JCBinary tree)
    {
    	String code=getListMulCode(tree);
    	JCBlockExp blockExp=parseBlockExp(code);
    	return blockExp;
    }
}
