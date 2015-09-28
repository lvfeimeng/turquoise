package sviolet.liba.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Utils {

	protected static char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6',
			'7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	protected static MessageDigest messagedigest = null;

	static {
		try {
			messagedigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 获得file的MD5值
	 * 
	 * @param file
	 * @return
	 */
	public static String getFileMD5String(final File file) {
		if(sviolet.turquoise.utils.sys.DeviceUtils.getVersionSDK() < 11)
			return getFileMD5ByIO(file);//API10使用普通IO(NIO很慢)
		else
			return getFileMD5ByNIO(file);//API11以上使用NIO,效率高
	}
	
	/**
	 * 使用NIO方式读取文件计算MD5
	 * 
	 * @param file
	 * @return
	 */
	private static String getFileMD5ByNIO(final File file){
		FileInputStream is = null;
		try {
			is = new FileInputStream(file);
			FileChannel ch = is.getChannel();
			MappedByteBuffer byteBuffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
			messagedigest.update(byteBuffer);
			return bufferToHex(messagedigest.digest());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 使用普通IO方式读取文件计算MD5
	 * 
	 * @param file
	 * @return
	 * 
	 * @author S.Violet(zhuqinchao)
	 */
	private static String getFileMD5ByIO(final File file){
		FileInputStream in = null;
		try {
			in = new FileInputStream(file);
			byte[] buff = new byte[1024];
			int size = 0;
			while((size = in.read(buff)) != -1){  
				messagedigest.update(buff, 0, size);
			}
			return bufferToHex(messagedigest.digest());
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}finally{
			try {
				if(in != null)
					in.close();
			} catch (IOException e) {
				e.printStackTrace();
			} 
		}
	}

	public static String getMD5String(String s) {
		return getMD5String(s.getBytes());
	}

	public static String getMD5String(byte[] bytes) {
		messagedigest.update(bytes);
		return bufferToHex(messagedigest.digest());
	}

	private static String bufferToHex(byte bytes[]) {
		return bufferToHex(bytes, 0, bytes.length);
	}

	private static String bufferToHex(byte bytes[], int m, int n) {
		StringBuffer stringbuffer = new StringBuffer(2 * n);
		int k = m + n;
		for (int l = m; l < k; l++) {
			appendHexPair(bytes[l], stringbuffer);
		}
		return stringbuffer.toString();
	}

	private static void appendHexPair(byte bt, StringBuffer stringbuffer) {
		char c0 = hexDigits[(bt & 0xf0) >> 4];
		char c1 = hexDigits[bt & 0xf];
		stringbuffer.append(c0);
		stringbuffer.append(c1);
	}

	public static boolean checkPassword(String password, String md5PwdStr) {
		String s = getMD5String(password);
		return s.equals(md5PwdStr);
	}

}