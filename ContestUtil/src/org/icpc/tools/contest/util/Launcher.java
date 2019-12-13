package org.icpc.tools.contest.util;

import java.lang.reflect.Method;

import org.icpc.tools.contest.Trace;

public class Launcher {

	public static void main(String[] args) {
		Trace.init("ICPC Contest Utilities", "contestUtil", args);
		if (args == null || args.length == 0) {
			showHelp();
			return;
		}

		Class<?> c = null;
		try {
			c = Class.forName("org.icpc.tools.contest.util." + args[0]);
		} catch (ClassNotFoundException e) {
			// ignore
		}

		if (c == null) {
			Trace.trace(Trace.ERROR, "Class not found: " + args[0]);
			return;
		}

		try {
			Method m = c.getMethod("main", String[].class);
			String[] args2 = new String[args.length - 1];
			System.arraycopy(args, 1, args2, 0, args.length - 1);
			m.invoke(null, (Object) args2);
		} catch (Exception e) {
			Trace.trace(Trace.ERROR, "Error launching " + args[0], e);
			showHelp();
		}
	}

	protected static void showHelp() {
		System.out.println("Usage: contestUtil.bat/sh [class] [options]");
		System.out.println();
		System.out.println("Where [class] is one of:");
		System.out.println("     FloorGenerator2014");
		System.out.println("     FloorGenerator2015");
		System.out.println("     FloorGenerator2016");
		System.out.println("     ImagesGenerator");
		System.out.println("     LogoUtil");
		System.out.println("     RescaleImages");
		System.out.println("     ScreenMaskUtil");
		System.out.println("");
		System.out.println("ex: contestUtil.bat LogoUtil");
	}
}