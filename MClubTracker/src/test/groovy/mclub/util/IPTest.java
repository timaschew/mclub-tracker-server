package mclub.util;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class IPTest {
	static{
		IP.load("/tmp/17monipdb.dat");
	}
	private static List<String> l = Arrays.asList(
			"112.10.126.153", 
			"115.238.33.156",
			"115.238.33.158",
			"117.29.100.128",
			"117.29.101.205",
			"120.36.85.166",
			"124.160.217.84",
			"202.107.200.110",
			"211.138.116.182",
			"27.155.100.93",
			"27.155.102.220",
			"27.155.103.90",
			"27.156.5.25",
			"27.156.92.241",
			"36.23.23.97",
			"59.56.7.164");
 
	@Test
	public void testSearchIp() {
		/*
		 * Long st = System.nanoTime(); for (int i = 0; i < 1000000; i++) {
		 * IP.find(IP.randomIp()); } Long et = System.nanoTime();
		 * System.out.println((et - st) / 1000 / 1000);
		 */
		for(String s :l){
			System.out.println(Arrays.toString(IP.find(s)));
		}
		
		System.out.println(Arrays.toString(IP.find("110.251.225.147")));
	}

	
	@Test
	public void testReadAndSearch(){
		File f = new File("/tmp/ips.txt");
		try {
            //使用readLines读取每一行，生成List
            List<String> contents = FileUtils.readLines(f, Charset.forName("UTF-8"));
            //遍历输出contents
            for (String line : contents) {
            	System.out.println(Arrays.toString(IP.find(line)));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
		
	}
}
