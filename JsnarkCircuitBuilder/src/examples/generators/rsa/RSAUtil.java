/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/

package examples.generators.rsa;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.util.Arrays;

/**
 * Utility methods to extract sample randomness used by standard implementations
 * for RSA Encryption. In absence of test vectors, the extracted randomness is
 * used to test our RSA gadgets to make sure the RSA circuits match the standard
 * implementations.
 *
 */

public class RSAUtil {

	public static byte[][] extractRSARandomness1_5(byte[] cipherText,
			RSAPrivateKey privateKey) {

		BigInteger modulus = privateKey.getModulus();
		int keySize = modulus.bitLength();
		BigInteger d = privateKey.getPrivateExponent();

		byte[] cipherTextPadded = new byte[cipherText.length + 1];
		System.arraycopy(cipherText, 0, cipherTextPadded, 1, cipherText.length);
		cipherTextPadded[0] = 0;

		BigInteger c = new BigInteger(cipherText);
		c = new BigInteger(cipherTextPadded);
		BigInteger product = BigInteger.ONE;
		for (int i = keySize - 1; i >= 0; i--) {
			product = product.multiply(product).mod(modulus);
			boolean bit = d.testBit(i);
			if (bit)
				product = product.multiply(c).mod(modulus);
		}

//		System.out.println("After decryption manually = "
//				+ product.toString(16));

		byte[] paddedPlaintext = product.toByteArray();
		if (paddedPlaintext.length != keySize / 8 - 1) {
			System.out.println("Error");
			return null;
		}
		byte[] plaintext = null;
		byte[] randomness = null;

		if (paddedPlaintext[0] != 2) {
			System.out.println("Error");
		} else {
			for (int i = 1; i < keySize / 8 - 2; i++) {
				if (paddedPlaintext[i] != 0) {
					continue;
				} else {
					plaintext = new byte[(keySize / 8 - 2) - i];
					randomness = new byte[i - 1];
					System.arraycopy(paddedPlaintext, i + 1, plaintext, 0,
							plaintext.length);
					System.arraycopy(paddedPlaintext, 1, randomness, 0,
							randomness.length);

					break;
				}
			}
		}
		byte[][] result = new byte[][] { plaintext, randomness };
		return result;
	}

	private static final byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16),
				(byte) (value >>> 8), (byte) value };
	}

	private static byte[] mgf(byte[] array, int maskLen, int hlen) {

		byte[] v = new byte[0];
		for (int i = 0; i <= ((int) Math.ceil(maskLen * 1.0 / hlen)) - 1; i++) {
			byte[] c = intToByteArray(i);
			MessageDigest hash = null;
			try {
				hash = MessageDigest.getInstance("SHA-256");
			} catch (Exception e) {
				e.printStackTrace();
			}
			hash.update(concat(array, c));
			byte[] digest = hash.digest();
			hash.reset();
			v = concat(v, digest);
		}
		return v;
	}

	private static byte[] concat(byte[] a1, byte[] a2) {
		int l = a1.length + a2.length;
		byte[] result = new byte[l];
		for (int i = 0; i < a1.length; i++) {
			result[i] = a1[i];
		}
		for (int i = 0; i < a2.length; i++) {
			result[i + a1.length] = a2[i];
		}
		return result;
	}

	public static byte[][] extractRSAOAEPSeed(byte[] cipherText,
			RSAPrivateKey privateKey) {

		BigInteger modulus = privateKey.getModulus();
		int keySize = modulus.bitLength();
		BigInteger d = privateKey.getPrivateExponent();

		byte[] cipherTextPadded = new byte[cipherText.length + 1];
		System.arraycopy(cipherText, 0, cipherTextPadded, 1, cipherText.length);
		cipherTextPadded[0] = 0;

		BigInteger c = new BigInteger(cipherText);
		c = new BigInteger(cipherTextPadded);

		BigInteger product = BigInteger.ONE;
		for (int i = keySize - 1; i >= 0; i--) {
			product = product.multiply(product).mod(modulus);
			boolean bit = d.testBit(i);
			if (bit)
				product = product.multiply(c).mod(modulus);
		}

		int hlen = 32;
		int maskedDBLength = keySize / 8 - hlen - 1;

		byte[] encodedMessageBytes = product.toByteArray();

		if (encodedMessageBytes.length > keySize / 8) {
			encodedMessageBytes = Arrays.copyOfRange(encodedMessageBytes, 1,
					encodedMessageBytes.length);
		} else {
			while (encodedMessageBytes.length < keySize / 8) {
				encodedMessageBytes = concat(new byte[] { 0 },
						encodedMessageBytes);
			}
		}

		byte[] maskedSeed = Arrays
				.copyOfRange(encodedMessageBytes, 1, hlen + 1);
		byte[] maskedDb = Arrays.copyOfRange(encodedMessageBytes, hlen + 1,
				encodedMessageBytes.length);

		byte[] seedMask = mgf(maskedDb, hlen, hlen);
		byte[] seed = Arrays.copyOf(seedMask, hlen);
		for (int i = 0; i < hlen; i++) {
			seed[i] ^= maskedSeed[i];
		}

		byte[] dbMask = mgf(seed, keySize / 8 - hlen - 1, hlen);
		dbMask= Arrays.copyOf(dbMask, keySize/8-hlen-1);

		byte[] DB = new byte[dbMask.length + 1]; // appending a zero to the left, to avoid sign issues in the BigInteger
		System.arraycopy(maskedDb, 0, DB, 1, maskedDBLength);
		for (int i = 0; i < maskedDBLength; i++) {
			DB[i + 1] ^= dbMask[i];
		}
//		BigInteger dbInt = new BigInteger(DB);

		int shift1 = 0;
		while (DB[shift1] == 0) {
			shift1++;
		}
		int idx = 32 + shift1;
		while (DB[idx] == 0) {
			idx++;
		}
		byte[] plaintext = Arrays.copyOfRange(DB, idx + 1, DB.length);
		byte[][] result = new byte[][] { plaintext, seed };
		return result;
	}

}
