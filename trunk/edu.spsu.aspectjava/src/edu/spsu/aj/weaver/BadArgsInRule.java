package edu.spsu.aj.weaver;

public class BadArgsInRule extends Exception {
	private AspectRule rule;
	private AbstractConditionClause clause;
	
	BadArgsInRule(AspectRule rule, AbstractConditionClause clause, String message) {
		super(message);
		this.rule = rule;
		this.clause = clause;
	}
	
	BadArgsInRule(AspectRule rule, AbstractConditionClause clause) {
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
