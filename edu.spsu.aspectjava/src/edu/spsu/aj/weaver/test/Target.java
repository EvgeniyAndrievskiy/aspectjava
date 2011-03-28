package edu.spsu.aj.weaver.test;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

public class Target {
	String str = "";
	int a = 123;
	
	public void someMethod() {
		int l1 = -1;
		BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
//		System.out.println("Right before target. (object: "
//				+ r + ")");
//		String s;
		String[] sa = new String[] {"test", "1", "2"};
		try {
			FileOutputStream out = 
				new FileOutputStream("r:/Out.txt");
		} catch (IOException e) {
			target3(new BufferedReader
					(new InputStreamReader(System.in)));
		}
//		target2();
		target1(123 + a, 
				"123", sa[2]);
//		System.out.println("Right after target. (object: "
//				+ (a + l1++) + ")");
		System.out.println(a);
		double d = 1.0;
		
	}
	
	private static long target1(long l, String s1, String s2) {
		System.out.println("In target1 method. Params: " + l + ", " + s1 + ", " + s2);
		return new Random().nextInt();
	}
	
	private static long target2() {
		System.out.println("In target2 method. No params.");
		return 0;
	}
	
	private long target3(BufferedReader o) {
		System.out.println("In target3 method. Params: " + o);
		return new Random().nextInt();
	}

}
