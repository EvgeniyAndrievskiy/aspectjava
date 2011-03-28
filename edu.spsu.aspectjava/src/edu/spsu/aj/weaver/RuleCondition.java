package edu.spsu.aj.weaver;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class RuleCondition {
	private String condition;	
	private List<AbstractConditionClause> condClauses;
	
	public RuleCondition(String condition) throws BadCondClauseFormat{
		this.condition = condition.trim();
		condClauses = new LinkedList<AbstractConditionClause>();
		
		parseConditionString(condition);
	}
	
	private void parseConditionString(String cond) throws BadCondClauseFormat {
		cond = cond.trim();
		String[] clauses = cond.split("\\|\\|");
		for(String s : clauses){
			AbstractConditionClause clause = 
				CondClausesFactory.createClause(s);
			condClauses.add(clause);
		}
	}

	AbstractConditionClause accept(AbstractInsnNode instr, MethodNode method, ClassNode class1){
		for(AbstractConditionClause clause : condClauses){
			if(clause.accepts(instr, method, class1)){
				return clause;
			}
		}
		return null;
	}
	
	public Iterator<AbstractConditionClause> getCondClauses(){
		return condClauses.iterator();
	}

	@Override
	public String toString() {
		return condition;
	}

}
