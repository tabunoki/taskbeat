package com.binarysprite.taskbeat;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;

/**
 * WSSE認証のためのユーティリティクラスです。
 * 
 * @author Tabunoki
 *
 */
public class WSSE {
	
	/**
	 * WSSEの文字エンコードです。
	 */
	public static final String ENCODING = "UTF-8";
	
	/**
	 * ユーティリティクラスのためインスタンスを生成することはできません。
	 * 開発者はコンストラクタを作成しないでください。
	 */
	private WSSE() {
		
	}
	
	/**
	 * WSSE認証のためのヘッダー値を返します。
	 * 
	 * @param username ユーザー名
	 * @param password パスワード
	 * @return WSSEヘッダーの値
	 */
	public static String getHeaderValue(String username, String password) {
		
		byte[] nonceBytes = new byte[8];
		try {
			SecureRandom.getInstance("SHA1PRNG").nextBytes(nonceBytes);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		
		String created = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(new Date());
		
		byte[] createdBytes = null;
		try {
			createdBytes = created.getBytes(ENCODING);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		byte[] passwordBytes = null;
		try {
			passwordBytes = password.getBytes(ENCODING);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		byte[] message = new byte[nonceBytes.length + createdBytes.length + passwordBytes.length];
		System.arraycopy(nonceBytes, 0, message, 0, nonceBytes.length);
		System.arraycopy(createdBytes, 0, message, nonceBytes.length, createdBytes.length);
		System.arraycopy(passwordBytes, 0, message, nonceBytes.length + createdBytes.length, passwordBytes.length);
		
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("SHA1");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		md.update(message);
		byte[] digestBytes = md.digest();
		
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("UsernameToken Username=\"");
		stringBuffer.append(username);
		stringBuffer.append("\", PasswordDigest=\"");
		stringBuffer.append(new String(Base64.encodeBase64(digestBytes)));
		stringBuffer.append("\", Nonce=\"");
		stringBuffer.append(new String(Base64.encodeBase64(nonceBytes)));
		stringBuffer.append("\", Created=\"");
		stringBuffer.append(created);
		stringBuffer.append('"');
		
		return stringBuffer.toString();
	}
}
