package scw.codec.support;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import scw.codec.CodecException;
import scw.codec.MultipleCodec;

public abstract class SecurityCodec implements BytesCodec<byte[]>, MultipleCodec<byte[]> {
	
	public static KeyFactory getKeyFactory(String algorithm) {
		try {
			return KeyFactory.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new CodecException(algorithm, e);
		}
	}

	public static PrivateKey getPrivateKey(String algorithm, byte[] privateKey) {
		EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKey);
		KeyFactory keyFactory = getKeyFactory(algorithm);
		try {
			return keyFactory.generatePrivate(keySpec);
		} catch (InvalidKeySpecException e) {
			throw new CodecException(algorithm, e);
		}
	}

	public static PublicKey getPublicKey(String algorithm, byte[] publicKey) {
		EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKey);
		KeyFactory keyFactory = getKeyFactory(algorithm);
		try {
			return keyFactory.generatePublic(keySpec);
		} catch (InvalidKeySpecException e) {
			throw new CodecException(algorithm, e);
		}
	}
}
