package edu.spsu.aj.weaver;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class AbstractConditionClause {
	public static enum Context{
		BEFORE, AFTER, INSTEAD
	}
	
	protected String condClause;
	protected String pattern;
	protected String argsInfoStr = null;
	private List<Restriction> restrictions = null;
	private Context context;
	protected ArgsInfo argsInfo;
	
	AbstractConditionClause(String condClause) throws BadCondClauseFormat {
		this.condClause = condClause.trim();
		
		/** Context **/
		int ws = condClause.indexOf(' ');
		if(ws < 0){
			throw new BadCondClauseFormat(condClause);
		}
		String cont = condClause.substring(0, ws).toLowerCase();
		if(cont.equals("%before")){
			context = Context.BEFORE;
		}else if(cont.equals("%after")){
			context = Context.AFTER;
		}else if(cont.equals("%instead")){
			context = Context.INSTEAD;
		}else{
			throw new BadCondClauseFormat(condClause, 
					"Unexpected weaving context: " + cont);
		}
		
		/** Pattern **/	
		String s = condClause.substring(ws + 1).trim();
		// now 's' contains all after Context; it starts with no whitespace

		ws = s.indexOf(' ');
		// there must be at least 1 whitespace between JoinPointKind & Pattern 
		if(ws < 0){	
			throw new BadCondClauseFormat(condClause);
		}
		String string = s.substring(ws + 1).trim();
		// now 'string' contains Pattern + rest

		ws = string.indexOf("&&");
		if(ws < 0){
			pattern = string;
		}else{
			pattern = string.substring(0, ws).trim();
		}
		
		/** ArgsInfo & Restrictions **/
		if(ws >= 0) {
			string = string.substring(ws + 2).trim();
			// now 'string' contains ArgsInfo and (or) Restrictions
			
			if(string.startsWith("%args")){
				ws = string.indexOf("&&");
				if(ws < 0) {
					argsInfoStr = string;
				}else{
					argsInfoStr = string.substring(0, ws).trim();
					restrictions = new LinkedList<Restriction>();
					String[] restrs = (string.substring(ws + 2)).split("&&");
					for(String res : restrs){
						restrictions.add(new Restriction(res));
					}
				}
			}else {
				restrictions = new LinkedList<Restriction>();
				String[] restrs = string.split("&&");
				for(String res : restrs){
					restrictions.add(new Restriction(res));
				}
			}		
		}
	}
	
	abstract boolean accepts(AbstractInsnNode instr, MethodNode method, ClassNode class1);
	
	ArgsInfo getArgsInfo(){
		return argsInfo;
	}
	
	protected boolean checkRestrictions(AbstractInsnNode instr, MethodNode method, ClassNode class1){
		if(restrictions == null){
			return true;
		}
		for(Restriction restriction : restrictions){
			if(!restriction.accepts(instr, method, class1)){
				return false;
			}
		}
		return true;
	}
	
	public Context getContext(){ 
		return context;
	}
	
	@Override
	public String toString() {
		return condClause;
	}
	
//	protected static boolean mathes(EntityWildCard wc, MethodNode meth, ClassNode cl){
//		String classString = cl.name;
//		String methString = meth.name;
//		if(wc.hasClass() && wc.hasPackage()){
//			String methFull = classString.replace('/', '.')
//			 + '.' + methString;
//			return wc.matches(methFull);
//		}else if(wc.hasClass()){
//			int lastSlash = classString.lastIndexOf('/');
//			String methClass = classString.substring(lastSlash + 1)
//			 + '.' + methString;
//			return wc.matches(methClass);
//		}else{
//			return wc.matches(methString);
//		}
//		
//	}
	
	// Class-wrapper
	private class Restriction {
		private static final int WITHIN = 0;
		private static final int NOT_WITHIN = 1;
		private static final int WITHINCODE = 2;
		private static final int NOT_WITHINCODE = 3;
		
		private String restriction;
		private int type;
		private EntityWildCard entityWC;
		
		Restriction(String restr) throws BadCondClauseFormat{
			restriction = restr.trim();
			int br = restriction.indexOf('(');
			if(br < 0){
				throw new BadCondClauseFormat(condClause, 
						"Bad format of restriction: " + restriction);
			}
			String typeString = restriction.substring(0, br).
				trim().toLowerCase();
			if(typeString.equals("%within")){
				type = WITHIN;
			}else if(typeString.equals("%!within")){
				type = NOT_WITHIN;
			}else if(typeString.equals("%withincode")){
				type = WITHINCODE;
			}else if(typeString.equals("%!withincode")){
				type = NOT_WITHINCODE;
			}else{
				throw new BadCondClauseFormat(condClause, 
						"Bad format of restriction: " + restriction);
			}
			// remove last symbol (')') and then trim
			String entityWCString = restriction.substring(br + 1,
					restriction.length() - 1).trim();
			int entityWCType;
			if((type == WITHIN) || (type == NOT_WITHIN)){
				entityWCType = EntityWildCard.TYPE_WC;
			}else{
				entityWCType = EntityWildCard.METHOD_WC;
			}
			try {
				entityWC = new EntityWildCard(entityWCString, entityWCType);
			} catch (PatternSyntaxException e) {
				throw new BadCondClauseFormat(condClause, "Bad format of restriction: "
						+ restriction);
			}
		}
		
		boolean accepts(AbstractInsnNode instr, MethodNode method, ClassNode class1){
			String classString = class1.name;
			String methString = method.name;
			
			switch (type) {
			case WITHIN:
				if(entityWC.hasPackage()){
					return entityWC.matches(classString.replace('/', '.'));
				}else{
					int lastSlash = classString.lastIndexOf('/');
					return entityWC.matches(classString.substring(lastSlash + 1));
				}
//				break;
			case NOT_WITHIN:
				if(entityWC.hasPackage()){
					return !entityWC.matches(classString.replace('/', '.'));
				}else{
					int lastSlash = classString.lastIndexOf('/');
					return !entityWC.matches(classString.substring(lastSlash + 1));
				}
//				break;
			case WITHINCODE:
				if(entityWC.hasClass() && entityWC.hasPackage()){
					String methFull = classString.replace('/', '.')
					 + '.' + methString;
					return entityWC.matches(methFull);
				}else if(entityWC.hasClass()){
					int lastSlash = classString.lastIndexOf('/');
					String methClass = classString.substring(lastSlash + 1)
					 + '.' + methString;
					return entityWC.matches(methClass);
				}else{
					return entityWC.matches(methString);
				}
//				break;
			case NOT_WITHINCODE:
				if(entityWC.hasClass() && entityWC.hasPackage()){
					String methFull = classString.replace('/', '.')
					 + '.' + methString;
					return !entityWC.matches(methFull);
				}else if(entityWC.hasClass()){
					int lastSlash = classString.lastIndexOf('/');
					String methClass = classString.substring(lastSlash + 1)
					 + '.' + methString;
					return !entityWC.matches(methClass);
				}else{
					return !entityWC.matches(methString);
				}
//				break;
			default:
				break;
			}
			
			return false;
		}
		
		@Override
		public String toString() {
			return restriction;
		}
	}
}
