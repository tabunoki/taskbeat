package com.binarysprite.taskbeat;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Properties;

/**
 * アプリケーションの設定を管理します。
 * 
 * @author Tabunoki
 *
 */
public enum ClockProperties {
	
	FONT,
	
	WINDOW_WIDTH,
	WINDOW_HEIGHT,
	
	USERNAME,
	PASSWORD,
	GRAPH_NAME,
	
	WAITING_FOREGROUND_COLOR_R,
	WAITING_FOREGROUND_COLOR_G,
	WAITING_FOREGROUND_COLOR_B,
	
	WAITING_BACKGROUND_COLOR_R,
	WAITING_BACKGROUND_COLOR_G,
	WAITING_BACKGROUND_COLOR_B,

	RUNNING_FOREGROUND_COLOR_R,
	RUNNING_FOREGROUND_COLOR_G,
	RUNNING_FOREGROUND_COLOR_B,

	RUNNING_BACKGROUND_COLOR_R,
	RUNNING_BACKGROUND_COLOR_G,
	RUNNING_BACKGROUND_COLOR_B;
	
	public static final String BASEDIR;
	
	private static final Properties PROPERTIES;
	
	static {
		
		final String propertiesFileName = "clock.properties";
		
		File propertiesFile = new File(System.getProperty("user.dir"), propertiesFileName);
		if (propertiesFile.isFile() == false) {
			try {
				propertiesFile = new File(ClassLoader.getSystemResource("./META-INF").toURI().getPath(), propertiesFileName);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
		
		BASEDIR = propertiesFile.getParent();
		
		System.out.println("ユーザーディレクトリ: " + System.getProperty("user.dir"));
		System.out.println("システムディレクトリ: " + ClassLoader.getSystemResource("./META-INF").getPath());
		System.out.println("プロパティファイル: " + propertiesFile.getAbsolutePath());
		
		PROPERTIES = new Properties();
		
		InputStream inputStream = null;
		try {
			inputStream = new BufferedInputStream(new FileInputStream(propertiesFile));
			
			PROPERTIES.load(inputStream);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public String get() {
		return PROPERTIES.getProperty(this.toString());
	}
	
	public void set(String value) {
		PROPERTIES.setProperty(this.toString(), value);
	}
}
