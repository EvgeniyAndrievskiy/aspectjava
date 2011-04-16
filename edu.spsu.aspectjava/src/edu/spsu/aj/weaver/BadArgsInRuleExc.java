package edu.spsu.aj.weaver;

public class BadArgsInRuleExc extends Exception {
	private AspectRule rule;
	private AbstractConditionClause clause;
	
	BadArgsInRuleExc(AspectRule rule, AbstractConditionClause clause, String message) {
		super(message);
		this.rule = rule;
		this.clause = clause;
	}
	
	BadArgsInRuleExc(AspectRule rule, AbstractConditionClause clause) {
		super();
		this.rule = rule;
		this.clause = clause;
	}

	public AspectRule getRule() {
		return rule;
	}

	public AbstractConditionClause getClause() {
		return clause;
	}
	
}
