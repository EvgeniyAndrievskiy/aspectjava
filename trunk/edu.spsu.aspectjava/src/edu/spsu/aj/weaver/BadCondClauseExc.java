package edu.spsu.aj.weaver;

public class BadCondClauseExc extends Exception {
	private String condClause;
	
	BadCondClauseExc(String condClause, String message) {
		super(message);
		this.condClause = condClause;
	}
	
	BadCondClauseExc(String condClause) {
		super();
		this.condClause = condClause;
	}

	public String getCondClause() {
		return condClause;
	}
	
}
