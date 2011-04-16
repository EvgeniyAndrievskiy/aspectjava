package edu.spsu.aj.weaver;

import java.util.Iterator;

import org.objectweb.asm.Type;

public class AspectRule {
	private String description;  // from AspectDescription annot
	private RuleCondition condition;
	private RuleAction action;
		
	public AspectRule(RuleCondition condition, RuleAction action) throws BadArgsInRuleExc {
		this.condition = condition;
		this.action = action;
		description = null;
		checkArgs();
	}
	
	public AspectRule(String desc, RuleCondition condition, RuleAction action) throws BadArgsInRuleExc {
		this.condition = condition;
		this.action = action;
		this.description = desc;
		checkArgs();
	}
	
	private void checkArgs() throws BadArgsInRuleExc{
		Iterator<AbstractConditionClause> iter = condition.getCondClauses();
		while(iter.hasNext()){
			AbstractConditionClause clause = iter.next();
			ArgsInfo argsInfo = clause.getArgsInfo();
			Type[] actArgTypes = Type.getArgumentTypes(action.getDescriptor());
			
			// Case %args(..), that means all arguments from target instr are passed into action.
			// This case is equivalent to argsInfo.getArgsCount() == -1 and/or argsInfo.getInfo() == null.
			if(argsInfo != null && argsInfo.getArgsCount() == -1) {
				// CALL joinpoint case.
				if(clause instanceof CallConditionClause){
					int[] info = new int[actArgTypes.length];
					for(int i= 0; i < actArgTypes.length; i++) {
						info[i] = i;
					}
					argsInfo.setInfo(info);
					argsInfo.setArgsCount(info.length);
				}// USE & ASSIGN joinpoint cases.
				else{
					// TODO: fill in args info for %args(..) case for USE & ASSIGN.
				}
				continue;
			}
			
			if(clause.getArgsInfo() != null && 
					actArgTypes.length != clause.getArgsInfo().getArgsCount()){
				throw new BadArgsInRuleExc(this, clause, "Disparity between args count in " +
						"args info & action args count.");
			}
			if(clause.getArgsInfo() == null && 
					Type.getArgumentTypes(action.getDescriptor()).length != 0){
				throw new BadArgsInRuleExc(this, clause, "Missed args info.");
			}
			
		}
	}

	public String getDescription() {
		return description;
	}

	public RuleCondition getCondition() {
		return condition;
	}

	public RuleAction getAction() {
		return action;
	}
	
	public String toString(){	
		return condition + " -> " + action;
	}

}
