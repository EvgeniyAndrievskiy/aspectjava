package edu.spsu.aj.weaver;

class CondClausesFactory {
	
	static AbstractConditionClause createClause(String clause) throws BadCondClauseFormat{
		clause = clause.trim();
		AbstractConditionClause result = null;
		// takes substring(1) to avoid first '%' before context
		int k = clause.substring(1).indexOf('%');
		if(k < 0){
			throw new BadCondClauseFormat(clause);
		}
		// takes substring(k + 2) because of int k = clause.SUBSTRING(1).indexOf('%');
		String str = clause.substring(k + 2);
		int w = str.indexOf(' ');
		if(w < 0){
			throw new BadCondClauseFormat(clause);
		}
		String jpKind = str.substring(0, w).toLowerCase();
		
		// TODO: point to implement ASSIGN & USE 
		if(jpKind.equals("call")){
			result = new CallConditionClause(clause);
		}else {
			throw new BadCondClauseFormat(clause, 
					"Unexpected joinpoint kind: " + jpKind);
		}
		return result;
	}
}
