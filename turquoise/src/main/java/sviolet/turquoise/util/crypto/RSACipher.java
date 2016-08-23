/*
 * Copyright (C) 2015-2016 S.Violet
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Project GitHub: https://github.com/shepherdviolet/turquoise
 * Email: shepherdviolet@163.com
 */

package sviolet.turquoise.util.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * <p>RSA加密工具</p>
 *
 * <p>PC端JDK默认加密填充方式为RSA/None/PKCS1Padding，Android默认为RSA/None/NoPadding</p>
 *
 */
public class RSACipher {

    public static final String SIGN_ALGORITHM_RSA_MD5 = "MD5withRSA";
    public static final String SIGN_ALGORITHM_RSA_SHA1 = "SHA1withRSA";

    public static final String CRYPTO_TRANSFORMATION_RSA_ECB_PKCS1 = "RSA/ECB/PKCS1Padding";
    public static final String CRYPTO_TRANSFORMATION_RSA_ECB_NOPADDING = "RSA/ECB/NoPadding";
    public static final String CRYPTO_TRANSFORMATION_RSA_NONE_PKCS1PADDING = "RSA/None/PKCS1Padding";
    public static final String CRYPTO_TRANSFORMATION_RSA_NONE_NOPADDING = "RSA/None/NoPadding";
    public static final String CRYPTO_TRANSFORMATION_RSA = "RSA";
	  
    /**
     * 用私钥对信息生成数字签名<p>
     *  
     * @param data 需要签名的数据
     * @param privateKey 私钥
     * @param signAlgorithm 签名逻辑: RSACipher.SIGN_ALGORITHM_RSA_MD5 / RSACipher.SIGN_ALGORITHM_RSA_SHA1
     *  
     * @return 数字签名
     * @throws NoSuchAlgorithmException 无效的signAlgorithm
     * @throws InvalidKeyException 无效的私钥
     * @throws SignatureException 签名异常
     */  
    public static byte[] sign(byte[] data, RSAPrivateKey privateKey, String signAlgorithm) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException{
        Signature signature = Signature.getInstance(signAlgorithm);
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    /**
     * <p>用私钥对信息生成数字签名(NIO)</p>
     *
     * <p>ByteBuffer示例:</p>
     *
     * <pre>{@code
     *         FileInputStream inputStream = new FileInputStream(file);
     *         FileChannel channel = inputStream.getChannel();
     *         MappedByteBuffer byteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
     * }</pre>
     *
     * @param data 需要签名的数据
     * @param privateKey 私钥
     * @param signAlgorithm 签名逻辑: RSACipher.SIGN_ALGORITHM_RSA_MD5 / RSACipher.SIGN_ALGORITHM_RSA_SHA1
     *
     * @return 数字签名
     * @throws NoSuchAlgorithmException 无效的signAlgorithm
     * @throws InvalidKeyException 无效的私钥
     * @throws SignatureException 签名异常
     */
    public static byte[] sign(ByteBuffer data, RSAPrivateKey privateKey, String signAlgorithm) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException{
        Signature signature = Signature.getInstance(signAlgorithm);
        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }
  
    /**
     * <p>用公钥验证数字签名</p>
     *  
     * @param data 被签名的数据
     * @param sign 数字签名
     * @param publicKey 公钥
     * @param signAlgorithm 签名逻辑: RSACipher.SIGN_ALGORITHM_RSA_MD5 / RSACipher.SIGN_ALGORITHM_RSA_SHA1
     *  
     * @return true:数字签名有效
     * @throws NoSuchAlgorithmException 无效的signAlgorithm
     * @throws InvalidKeyException 无效的私钥
     * @throws SignatureException 签名异常
     *  
     */  
    public static boolean verify(byte[] data, byte[] sign, RSAPublicKey publicKey, String signAlgorithm) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException{
        Signature signature = Signature.getInstance(signAlgorithm);
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(sign);  
    }

    /**
     * <p>用公钥验证数字签名(NIO)</p>
     *
     * <p>ByteBuffer示例:</p>
     *
     * <pre>{@code
     *         FileInputStream inputStream = new FileInputStream(file);
     *         FileChannel channel = inputStream.getChannel();
     *         MappedByteBuffer byteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
     * }</pre>
     *
     * @param data 被签名的数据
     * @param sign 数字签名
     * @param publicKey 公钥
     * @param signAlgorithm 签名逻辑: RSACipher.SIGN_ALGORITHM_RSA_MD5 / RSACipher.SIGN_ALGORITHM_RSA_SHA1
     *
     * @return true:数字签名有效
     * @throws NoSuchAlgorithmException 无效的signAlgorithm
     * @throws InvalidKeyException 无效的私钥
     * @throws SignatureException 签名异常
     *
     */
    public static boolean verify(ByteBuffer data, byte[] sign, RSAPublicKey publicKey, String signAlgorithm) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException{
        Signature signature = Signature.getInstance(signAlgorithm);
        signature.initVerify(publicKey);
        signature.update(data);
        return signature.verify(sign);
    }

    /**
     * <p>私钥解密</p>
     *  
     * @param data 已加密数据
     * @param privateKey 私钥
     * @param cryptoTransformation 加密算法/填充方式: RSACipher.CRYPTO_TRANSFORMATION_RSA_ECB_PKCS1 / RSACipher.CRYPTO_TRANSFORMATION_RSA_ECB_NOPADDING
     *
     * @return 解密的数据
     * @throws NoSuchPaddingException 填充方式无效(cryptoTransformation)
     * @throws NoSuchAlgorithmException 加密算法无效(cryptoTransformation)
     * @throws InvalidKeyException 无效的私钥
     * @throws BadPaddingException 填充错误(密码错误)
     * @throws IllegalBlockSizeException 无效的块大小(密码错误?)
     * @throws IOException IO错误
     */  
    public static byte[] decrypt(byte[] data, RSAPrivateKey privateKey, String cryptoTransformation) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException{
    	
        Cipher cipher = Cipher.getInstance(cryptoTransformation);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        
        int dataLength = data.length;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int offset = 0;
        byte[] buffer;
        int blockSize = privateKey.getModulus().bitLength() / 8;//解密块和密钥等长
        
        // 对数据分段解密
        while (dataLength - offset > 0) {
            if (dataLength - offset > blockSize) {
                buffer = cipher.doFinal(data, offset, blockSize);
            } else {  
                buffer = cipher.doFinal(data, offset, dataLength - offset);
            }  
            outputStream.write(buffer, 0, buffer.length);
            offset += blockSize;
        }  
        return outputStream.toByteArray();
    }  
 
    /**
     * <p>公钥加密</p>
     *  
     * @param data 源数据 
     * @param publicKey 公钥
     * @param cryptoTransformation 加密算法/填充方式: RSACipher.CRYPTO_TRANSFORMATION_RSA_ECB_PKCS1 / RSACipher.CRYPTO_TRANSFORMATION_RSA_ECB_NOPADDING
     *
     * @return 加密后的数据
     * @throws NoSuchPaddingException 填充方式无效(cryptoTransformation)
     * @throws NoSuchAlgorithmException 加密算法无效(cryptoTransformation)
     * @throws InvalidKeyException 无效的私钥
     * @throws BadPaddingException 填充错误(密码错误?)
     * @throws IllegalBlockSizeException 无效的块大小(密码错误?)
     * @throws IOException IO错误
     */  
	public static byte[] encrypt(byte[] data, RSAPublicKey publicKey, String cryptoTransformation) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException{
        Cipher cipher = Cipher.getInstance(cryptoTransformation);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        
        int dataLength = data.length;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int offSet = 0;  
        byte[] buffer;
        int blockSize = publicKey.getModulus().bitLength() / 8 - 11;//加密块比密钥长度小11
        
        // 对数据分段加密  
        while (dataLength - offSet > 0) {
            if (dataLength - offSet > blockSize) {
                buffer = cipher.doFinal(data, offSet, blockSize);
            } else {  
                buffer = cipher.doFinal(data, offSet, dataLength - offSet);
            }  
            outputStream.write(buffer, 0, buffer.length);
            offSet += blockSize;
        }  
        return outputStream.toByteArray();
    }  
    
    /**
     * <p>公钥解密</p>
     *  
     * @param data 已加密数据
     * @param publicKey 公钥
     * @param cryptoTransformation 加密算法/填充方式: RSACipher.CRYPTO_TRANSFORMATION_RSA_ECB_PKCS1 / RSACipher.CRYPTO_TRANSFORMATION_RSA_ECB_NOPADDING
     *
     * @return 解密的数据
     * @throws NoSuchPaddingException 填充方式无效(cryptoTransformation)
     * @throws NoSuchAlgorithmException 加密算法无效(cryptoTransformation)
     * @throws InvalidKeyException 无效的私钥
     * @throws BadPaddingException 填充错误(密码错误)
     * @throws IllegalBlockSizeException 无效的块大小(密码错误?)
     * @throws IOException IO错误
     */  
    public static byte[] decrypt(byte[] data, RSAPublicKey publicKey, String cryptoTransformation) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException{
    	
        Cipher cipher = Cipher.getInstance(cryptoTransformation);
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        
        int dataLength = data.length;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int offSet = 0;  
        byte[] buffer;
        int blockSize = publicKey.getModulus().bitLength() / 8;//解密块和密钥等长
        
        // 对数据分段解密
        while (dataLength - offSet > 0) {
            if (dataLength - offSet > blockSize) {
                buffer = cipher.doFinal(data, offSet, blockSize);
            } else {
                buffer = cipher.doFinal(data, offSet, dataLength - offSet);
            }  
            outputStream.write(buffer, 0, buffer.length);
            offSet += blockSize;
        }  
        return outputStream.toByteArray();
    }  
 
    /**
     * <p>私钥加密</p>
     *  
     * @param data 源数据 
     * @param privateKey 私钥
     * @param cryptoTransformation 加密算法/填充方式: RSACipher.CRYPTO_TRANSFORMATION_RSA_ECB_PKCS1 / RSACipher.CRYPTO_TRANSFORMATION_RSA_ECB_NOPADDING
     *
     * @return 加密后的数据
     * @throws NoSuchPaddingException 填充方式无效(cryptoTransformation)
     * @throws NoSuchAlgorithmException 加密算法无效(cryptoTransformation)
     * @throws InvalidKeyException 无效的私钥
     * @throws BadPaddingException 填充错误(密码错误)
     * @throws IllegalBlockSizeException 无效的块大小(密码错误?)
     * @throws IOException IO错误
     */  
    public static byte[] encrypt(byte[] data, RSAPrivateKey privateKey, String cryptoTransformation) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, IOException{
        Cipher cipher = Cipher.getInstance(cryptoTransformation);
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        
        int dataLength = data.length;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int offSet = 0;  
        byte[] buffer;
        int blockSize = privateKey.getModulus().bitLength() / 8 - 11;//加密块比密钥长度小11
        
        // 对数据分段加密  
        while (dataLength - offSet > 0) {
            if (dataLength - offSet > blockSize) {
                buffer = cipher.doFinal(data, offSet, blockSize);
            } else {  
                buffer = cipher.doFinal(data, offSet, dataLength - offSet);
            }  
            outputStream.write(buffer, 0, buffer.length);
            offSet += blockSize;
        }  
        return outputStream.toByteArray();
    }  
}