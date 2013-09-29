package com.binarysprite.taskbeat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;


/**
 * はてなグラフAPIとの通信を行うクライアントです。
 * 
 * @author Tabunoki
 *
 */
public class HatenaGraph {
	
	/**
	 * はてなグラフAPIのエンドポイントURLです。
	 */
	private final String endpointURL = "http://graph.hatena.ne.jp/api/data";
	
	/**
	 * はてなグラフのユーザー名です。
	 */
	private final String username;
	
	/**
	 * はてなグラフのパスワードです。
	 */
	private final String password;
	
	/**
	 * 
	 */
	private static final Pattern ROW_PATTERN = Pattern.compile("\"([0-9]{4}-[0-9]{2}-[0-9]{2})\":\"([0-9]+.[0-9]{2})\"");
	
	/**
	 * 
	 */
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	
	/**
	 * 
	 * @author Tabunoki
	 *
	 */
	public enum DataType {
		JSON,
		YAML;
	}
	
	
	/**
	 * はてなグラフクライアントを生成します。
	 * @param username ユーザー名
	 * @param password パスワード
	 */
	public HatenaGraph(String username, String password) {
		
		if (username == null || password == null) {
			throw new NullPointerException("username or password is null.");
		}
		
		this.username = username;
		this.password = password;
	}
	
	/**
	 * 現在の日付で指定のグラフに値を登録します。
	 * @param graphname グラフ名
	 * @param value 値
	 */
	public void post(String graphname, double value) {
		
		this.post(graphname, new Date(), value);
	}
	
	/**
	 * 指定の日付で指定のグラフに値を登録します。
	 * @param graphname グラフ名
	 * @param date 日付
	 * @param value 値
	 */
	public void post(String graphname, Date date, double value) {
		
		HttpClient client = new HttpClient();
		PostMethod method = new PostMethod(this.endpointURL);
		
		method.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		method.addRequestHeader("X-WSSE", WSSE.getHeaderValue(this.username, this.password));
		method.setParameter("graphname", graphname);
		method.setParameter("date", new SimpleDateFormat("yyyy-MM-dd").format(date));
		method.setParameter("value", String.valueOf(value));
		
		try {
			client.executeMethod(method);
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 指定のグラフを取得します。
	 * @param graphname グラフ名
	 * @param type データタイプ（YAMLもしくはJSON）
	 * @return グラフデータ
	 */
	public String get(String graphname, DataType type) {
		
		return this.get(graphname, this.username, type);
	}
	
	/**
	 * 指定のユーザーのグラフを取得します。
	 * @param graphname グラフ名
	 * @param username ユーザー名
	 * @param type データタイプ（YAMLもしくはJSON）
	 * @return グラフデータ
	 */
	public String get(String graphname, String username, DataType type) {
		
		HttpClient client = new HttpClient();
		GetMethod method = new GetMethod(
				this.endpointURL + 
				"?graphname=" + graphname + 
				"&username=" + username + 
				"&type=" + type);
		
		method.setRequestHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		method.addRequestHeader("X-WSSE", WSSE.getHeaderValue(this.username, this.password));
		
		try {
			client.executeMethod(method);
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String data = null;
		try {
			data = method.getResponseBodyAsString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return data;
	}
	
	/**
	 * 
	 * @param date
	 * @param graphname
	 * @return
	 */
	public double getValue(Date date, String graphname) {
		
		return this.getValue(date, graphname, this.username);
	}
	
	/**
	 * 指定の日時のデータを取得します。
	 * @param date
	 * @param graphname
	 * @param username
	 * @return
	 */
	public double getValue(Date date, String graphname, String username) {
		
		System.out.println(date + ", " + graphname + ", " + username);
		
		Matcher matcher = ROW_PATTERN.matcher(this.get(graphname, username, DataType.JSON));
		
		while (matcher.find()) {
			if (matcher.group(1).equals(DATE_FORMAT.format(date))) {
				return Double.valueOf(matcher.group(2));
			}
		}
		
		return 0;
	}
}
