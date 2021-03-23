package scw.test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.junit.Test;

import scw.codec.Codec;
import scw.codec.support.AES;
import scw.codec.support.Base64;
import scw.codec.support.ByteHexCodec;
import scw.codec.support.CharsetCodec;
import scw.codec.support.DES;
import scw.codec.support.RSA;

public class CodecTest {
	public static String content = "这是一段加解密测试内容!";
	public static CharsetCodec charsetCodec = CharsetCodec.UTF_8;

	@Test
	public void des() {
		System.out.println("----------------BEGIN DES------------------");
		byte[] secreKey = charsetCodec.encode("12345678");
		byte[] iv = charsetCodec.encode("12345678");
		Codec<String, String> codec = charsetCodec.to(new DES(secreKey, iv))
				.to(ByteHexCodec.DEFAULT);
		String encode = codec.encode(content);
		System.out.println("encode:" + encode);
		String decode = codec.decode(encode);
		System.out.println("decode:" + decode);
		System.out.println("----------------END DES------------------");
	}

	@Test
	public void aes() {
		System.out.println("----------------BEGIN AES------------------");
		byte[] secreKey = charsetCodec.encode("1234567812346578");
		byte[] iv = charsetCodec.encode("1234567812345678");
		Codec<String, String> codec = charsetCodec.to(new AES(secreKey, iv))
				.to(ByteHexCodec.DEFAULT);
		String encode = codec.encode(content);
		System.out.println("encode:" + encode);
		String decode = codec.decode(encode);
		System.out.println("decode:" + decode);
		System.out.println("----------------END AES------------------");
	}

	@Test
	public void rsa() throws NoSuchAlgorithmException {
		System.out.println("----------------BEGIN RSA------------------");
		// KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
		KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");
		// 初始化密钥对生成器，密钥大小为96-1024位
		keyPairGen.initialize(1024, new SecureRandom());
		// 生成一个密钥对，保存在keyPair中
		KeyPair keyPair = keyPairGen.generateKeyPair();
		RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate(); // 得到私钥
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic(); // 得到公钥
		RSA rsa = new RSA(publicKey, privateKey, 128);
		Codec<String, String> codec = charsetCodec.to(rsa).to(Base64.DEFAULT);
		String encode = codec.encode(content);
		System.out.println("encode:" + encode);
		String decode = codec.decode(encode);
		System.out.println("decode:" + decode);
		System.out.println("----------------END RSA------------------");
	}
}