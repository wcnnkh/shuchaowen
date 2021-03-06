package scw.codec.encoder;

import scw.codec.Encoder;
import scw.codec.support.Base64;
import scw.codec.support.HexCodec;

public interface BytesEncoder<D> extends Encoder<D, byte[]>{
	
	default Encoder<D, String> toBase64(){
		return toEncoder(Base64.DEFAULT);
	}
	
	default Encoder<D, String> toHex(){
		return toEncoder(HexCodec.DEFAULT);
	}
	
	/**
	 * 会直接将结果转换为16进制字符串
	 * @see MD5#DEFAULT
	 * @return
	 */
	default Encoder<D, String> toMD5(){
		return toEncoder(MD5.DEFAULT);
	}
	
	default Encoder<D, String> toSHA1(){
		return toEncoder(SHA1.DEFAULT);
	}
}
