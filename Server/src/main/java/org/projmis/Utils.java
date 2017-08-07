package org.projmis;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

public class Utils {
	private static Object object = new Object();
		
	public static void log(String place, String... message) {
		try {
			PrintWriter writer = new PrintWriter(new FileOutputStream(new File("server-log.txt"), true));
			writer.append("" + new Date() + "\t" + place);
			for (int i = 0; i < message.length; i++) {
				writer.append("\t" + message[i]);
			}
			writer.append("\n");
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void saveException(String local, Exception exception) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		exception.printStackTrace(pw);
		String exceptionString = sw.toString();
		pw.close();
		try {
			sw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		synchronized (object) {
			try {
				PrintWriter writer = new PrintWriter(new FileOutputStream(new File("server-exceptions.txt"), true));
				writer.append(String.valueOf(new Date()) + "\t" + local + "\t" + exceptionString + "\n");
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void saveException(String local, String exceptionString) {
		synchronized (object) {
			try {
				PrintWriter writer = new PrintWriter(new FileOutputStream(new File("server-exceptions.txt"), true));
				writer.append(String.valueOf(new Date()) + "\t" + local + "\t" + exceptionString + "\n");
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static String exceptionToString(Exception exception) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		exception.printStackTrace(pw);
		String exceptionString = sw.toString();
		pw.close();
		return exceptionString;
	}
}
