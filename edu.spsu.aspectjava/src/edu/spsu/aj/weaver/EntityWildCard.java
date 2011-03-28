package edu.spsu.aj.weaver;

import java.util.regex.Pattern;

class EntityWildCard {
	public static final int TYPE_WC = 0;
	public static final int METHOD_WC = 1;
	
	// TODO: optimization for primitive types & void: use just "==" of constants
	//	public static final int SPECIAL_WC = 2;
	
	
	private int type;
	private Pattern packageWC;
	private Pattern classWC;
	private Pattern methodWC;
		
	private boolean hasPackage = false;
	private boolean hasClass = false;
	
	EntityWildCard(String wildcard, int t) {
		this.type = t;

		String[] parts = parseEntityString(wildcard);
		if(parts.length == 3){
			hasClass = true;
			hasPackage = true;
			methodWC = Pattern.compile(makeRegexp(parts[2]));
			classWC = Pattern.compile(makeRegexp(parts[1]));
			packageWC = Pattern.compile(makeRegexp(parts[0]));
		}else if(parts.length == 2){
			if(type == TYPE_WC){
				hasPackage = true;
				classWC = Pattern.compile(makeRegexp(parts[1]));
				packageWC = Pattern.compile(makeRegexp(parts[0]));
			}else{
				hasClass = true;
				methodWC = Pattern.compile(makeRegexp(parts[1]));
				classWC = Pattern.compile(makeRegexp(parts[0]));
			}
		}else{
			if(type == TYPE_WC){
				classWC = Pattern.compile(makeRegexp(parts[0]));
			}else{
				methodWC = Pattern.compile(makeRegexp(parts[0]));
			}
		}
	}
	
	private String[] parseEntityString(String entity){
		int dot1 = entity.lastIndexOf('.');
		if(dot1 < 0){
			return new String[]{entity};
		}else{
			String first = entity.substring(dot1 + 1);
			if(type == TYPE_WC){
				return new String[]{entity.substring(0, dot1), first};
			}else{
				int dot2 = entity.substring(0, dot1).lastIndexOf('.');
				if(dot2 < 0){
					return new String[]{entity.substring(0, dot1), first};
				}else{
					return new String[]{entity.substring(0, dot2), 
							entity.substring(dot2 + 1, dot1), first};
				}
			}
		}
	}
	
	private String makeRegexp(String wildcard){
		String result = null;
		result = wildcard.replace(".", "\\.");
		result = result.replace("*", ".*");
		result = result.replace("?", ".?");
		result = result.replace("[", "\\[");
		result = result.replace("]", "\\]");
		return result;
	}
	
	boolean hasPackage() {
		return hasPackage;
	}

	boolean hasClass() {
		return hasClass;
	}

	int getType() {
		return type;
	}

	boolean matches(String entity){
		String[] parts = parseEntityString(entity);
		if(type == TYPE_WC){
			if(hasPackage){
				return packageWC.matcher(parts[0]).matches() &&
					classWC.matcher(parts[1]).matches();
			}else{
				return classWC.matcher(parts[0]).matches();
			}
		}else{
			if(hasPackage){
				return packageWC.matcher(parts[0]).matches() &&
					classWC.matcher(parts[1]).matches() &&
						methodWC.matcher(parts[2]).matches();
			}else if(hasClass){
				return classWC.matcher(parts[0]).matches() &&
					methodWC.matcher(parts[1]).matches();
			}else{
				return methodWC.matcher(parts[0]).matches();
			}
		}
	}
}
