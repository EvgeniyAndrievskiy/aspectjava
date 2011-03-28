package edu.spsu.aj.weaver.test;

import java.io.BufferedReader;

import edu.spsu.aj.AspectAction;
import edu.spsu.aj.AspectDescription;

@AspectDescription("Aspect 'Aspect1'")
public class Aspect1 {

	@AspectAction("%instead %call * && %args(arg[0], arg[1])")
	public static long action1(String str1, String str) {
		System.out.println("In action1 of Aspect1. Gathered args: "  + str1 + " " + str);
		return 0;
	}
//	public static long action1() {
//		System.out.println("In action1 of Aspect1. Gathered args: "  +  " ");
//		return 0;
//	}
}
