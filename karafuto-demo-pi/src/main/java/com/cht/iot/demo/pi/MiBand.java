package com.cht.iot.demo.pi;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang.RandomStringUtils;

public class MiBand {

	protected static int crc8(byte[] bytes, int len) {
		int crc = 0;
		for (int a = 0; a < len; a++) {
			byte b = bytes[a];
			crc = crc ^ (b & 0xff);
			for (int i = 0; i < 8; i++) {
				if ((crc & 0x01) != 0) {
					crc = (crc >> 1) ^ 0x8c;
				} else {
					crc = crc >> 1;
				}
			}
		}

		return crc;
	}

	public static String genSetUserInfo(String address, String alias, byte gender, byte age, byte height, byte weight, byte type) {
		byte[] bytes = new byte[20];		

		int uid = Integer.parseInt(alias);
		bytes[0] = (byte) uid;
		bytes[1] = (byte) (uid >>> 8);
		bytes[2] = (byte) (uid >>> 16);
		bytes[3] = (byte) (uid >>> 24);
		bytes[4] = gender;
		bytes[5] = age;
		bytes[6] = height;
		bytes[7] = weight;
		bytes[8] = type;

		byte[] as = alias.getBytes();
		for (int i = 9; i < 19; i++) {
			bytes[i] = as[i - 9];
		}

		address = address.substring(address.length() - 2);

		int a = crc8(bytes, 19) ^ Integer.decode("0x" + address).intValue();
		bytes[19] = (byte) a;

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		
		for (byte b : bytes) {
			pw.printf("%02x", b);
		}
		
		pw.flush();
		
		return sw.toString();
	}
	
	public static String genSetUserInfo(String address) {
		String alias = String.format("10%s", RandomStringUtils.randomNumeric(8));
		byte gender = 1;
		byte age = 40;
		byte height = (byte) 180;
		byte weight = 75;
		byte type = 0;
		
		return genSetUserInfo(address, alias, gender, age, height, weight, type);
	}
}
