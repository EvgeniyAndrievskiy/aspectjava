package edu.spsu.aj.weaver;

abstract class ArgsInfo {
	// String presentation
	protected String toString;
	
	ArgsInfo(String argsInfo) throws BadCondClauseExc {
		this.toString = argsInfo.trim();
		parseArgsInfoString(toString);
	}
	
	abstract protected void parseArgsInfoString(String str) throws BadCondClauseExc;
	
	// Special case %args(..) is equivalent to getArgsCount() == -1.
	// It means that all (possible) arguments from target instruction are passed into action.
	// It's supposed to be filled by actual data on rule creating stage when action is known.
	abstract int getArgsCount();
	
	abstract void setArgsCount(int argsCount);
	
	// Special case %args(..) is equivalent to getInfo() == null.
	// It means that all (possible) arguments from target instruction are passed into action.
	// It's supposed to be filled by actual data on rule creating stage when action is known.
	abstract  Object getInfo();
	
	abstract  void setInfo(Object info);
	
	public String toString(){
		return toString;
	}

}
