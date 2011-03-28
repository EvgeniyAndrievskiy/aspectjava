package edu.spsu.aj.weaver;

public class BadCondClauseFormat extends Exception {
	private String condClause;
	
	BadCondClauseFormat(String condClause, String message) {
		super(message);
		this.condClause = condClause;
	}
	
	BadCondClauseFormat(String condClause) {
		super();
		this.condClause = condClause;
	}

	public String getCondClause() {
		return condClause;
	}
	
}
