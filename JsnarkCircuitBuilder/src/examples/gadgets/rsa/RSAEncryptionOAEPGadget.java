/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/

package examples.gadgets.rsa;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

import util.Util;
import circuit.auxiliary.LongElement;
import circuit.operations.Gadget;
import circuit.structure.Wire;
import circuit.structure.WireArray;
import examples.gadgets.hash.SHA256Gadget;
import examples.gadgets.math.LongIntegerModGadget;

/**
 * A gadget for RSA encryption according to PKCS#1 v2.2. The gadget assumes a
 * hardcoded public exponent of 0x10001, and uses SHA256 as the hash function
 * for mask generation function (mgf).
 * This gadget can accept a hardcoded or a variable RSA modulus. See the
 * corresponding generator example. 
 * 
 * This gadget is costly in comparison with the PKCS v1.5 RSA encryption gadget
 * due to many SHA256 calls during mask generation.
 * 
 * The implementation of this gadget follows the standard specs in:
 * https://www.emc.com/collateral/white-
 * papers/h11300-pkcs-1v2-2-rsa-cryptography-standard-wp.pdf
 */

public class RSAEncryptionOAEPGadget extends Gadget {

	private LongElement modulus;

	// every wire represents a byte in the following three arrays
	private Wire[] plainText;
	private Wire[] seed;

	private Wire[] ciphertext;

	private int rsaKeyBitLength; // in bits (assumed to be divisible by 8)
	public static final int SHA256_DIGEST_LENGTH = 32; // in bytes

	public static final byte[] lSHA256_HASH = new byte[] { (byte) 0xe3,
			(byte) 0xb0, (byte) 0xc4, 0x42, (byte) 0x98, (byte) 0xfc, 0x1c,
			0x14, (byte) 0x9a, (byte) 0xfb, (byte) 0xf4, (byte) 0xc8,
			(byte) 0x99, 0x6f, (byte) 0xb9, 0x24, 0x27, (byte) 0xae, 0x41,
			(byte) 0xe4, 0x64, (byte) 0x9b, (byte) 0x93, 0x4c, (byte) 0xa4,
			(byte) 0x95, (byte) 0x99, 0x1b, 0x78, 0x52, (byte) 0xb8, 0x55 };

	public RSAEncryptionOAEPGadget(LongElement modulus, Wire[] plainText,
			Wire[] seed, int rsaKeyBitLength, String... desc) {
		super(desc);

		if (rsaKeyBitLength % 8 != 0) {
			throw new IllegalArgumentException(
					"RSA Key bit length is assumed to be a multiple of 8");
		}

		if (plainText.length > rsaKeyBitLength / 8 - 2 * SHA256_DIGEST_LENGTH - 2) {
			System.err.println("Message too long");
			throw new IllegalArgumentException(
					"Invalid message length for RSA Encryption");
		}

		if (seed.length != SHA256_DIGEST_LENGTH) {
			System.err
					.println("Seed must have the same length as the hash function output ");
			throw new IllegalArgumentException(
					"Invalid seed dimension for RSA Encryption");
		}

		this.seed = seed;
		this.plainText = plainText;
		this.modulus = modulus;
		this.rsaKeyBitLength = rsaKeyBitLength;
		buildCircuit();
	}

	private void buildCircuit() {

		int mLen = plainText.length;
		int hLen = SHA256_DIGEST_LENGTH;
		int keyLen = rsaKeyBitLength / 8; // in bytes
		Wire[] paddingString = new Wire[keyLen - mLen - 2 * hLen - 2];
		Arrays.fill(paddingString, generator.getZeroWire());

		Wire[] db = new Wire[keyLen - hLen - 1];
		for (int i = 0; i < keyLen - hLen - 1; i++) {
			if (i < hLen) {
				db[i] = generator
						.createConstantWire((lSHA256_HASH[i] + 256) % 256);
			} else if (i < hLen + paddingString.length) {
				db[i] = paddingString[i - hLen];
			} else if (i < hLen + paddingString.length + 1) {
				db[i] = generator.getOneWire();
			} else {
				db[i] = plainText[i - (hLen + paddingString.length + 1)];
			}
		}

		Wire[] dbMask = mgf1(seed, keyLen - hLen - 1);
		Wire[] maskedDb = new Wire[keyLen - hLen - 1];
		for (int i = 0; i < keyLen - hLen - 1; i++) {
			maskedDb[i] = dbMask[i].xorBitwise(db[i], 8);
		}

		Wire[] seededMask = mgf1(maskedDb, hLen);
		Wire[] maskedSeed = new Wire[hLen];
		for (int i = 0; i < hLen; i++) {
			maskedSeed[i] = seededMask[i].xorBitwise(seed[i], 8);
		}
		
		Wire[] paddedByteArray = Util.concat(maskedSeed, maskedDb); // Big-Endian
		
		// The LongElement implementation is LittleEndian, so we will process the array in reverse order
		
		LongElement paddedMsg = new LongElement(
				new BigInteger[] { BigInteger.ZERO });
		for (int i = 0; i < paddedByteArray.length; i++) {
			LongElement e = new LongElement(paddedByteArray[paddedByteArray.length-i-1], 8);
			LongElement c = new LongElement(Util.split(
					BigInteger.ONE.shiftLeft(8 * i),
					LongElement.CHUNK_BITWIDTH));
			paddedMsg = paddedMsg.add(e.mul(c));
		}
		
		// do modular exponentiation
		LongElement s = paddedMsg;
		for (int i = 0; i < 16; i++) {
			s = s.mul(s);
			s = new LongIntegerModGadget(s, modulus, rsaKeyBitLength, false).getRemainder();
		}
		s = s.mul(paddedMsg);
		s = new LongIntegerModGadget(s, modulus, rsaKeyBitLength, true).getRemainder();

		// return the cipher text as byte array
		ciphertext = s.getBits(rsaKeyBitLength).packBitsIntoWords(8);
	}

	public void checkSeedCompliance() {
		for (int i = 0; i < seed.length; i++) {
			// Verify that the seed wires are bytes
			// This is also checked already by the sha256 gadget in the mgf1 calls, but added here for clarity
			seed[i].restrictBitLength(8);
		}
	}
	
	private Wire[] mgf1(Wire[] in, int length) {

		ArrayList<Wire> mgfOutputList = new ArrayList<Wire>();
		for (int i = 0; i <= ((int) Math.ceil(length * 1.0
				/ SHA256_DIGEST_LENGTH)) - 1; i++) {

			// the standard follows a Big Endian format
			Wire[] counter = generator.createConstantWireArray(new long[] {
					(byte) (i >>> 24), (byte) (i >>> 16), (byte) (i >>> 8),
					(byte) i });

			Wire[] inputToHash = Util.concat(in, counter);
			SHA256Gadget shaGadget = new SHA256Gadget(inputToHash, 8,
					inputToHash.length, false, true);
			Wire[] digest = shaGadget.getOutputWires();

			Wire[] msgHashBytes = new WireArray(digest).getBits(32)
					.packBitsIntoWords(8);
			// reverse the byte array representation of each word of the digest
			// to
			// be compatible with the endianess
			for (int j = 0; j < 8; j++) {
				Wire tmp = msgHashBytes[4 * j];
				msgHashBytes[4 * j] = msgHashBytes[(4 * j + 3)];
				msgHashBytes[4 * j + 3] = tmp;
				tmp = msgHashBytes[4 * j + 1];
				msgHashBytes[4 * j + 1] = msgHashBytes[4 * j + 2];
				msgHashBytes[4 * j + 2] = tmp;
			}
			for (int j = 0; j < msgHashBytes.length; j++) {
				mgfOutputList.add(msgHashBytes[j]);
			}
		}
		Wire[] out = mgfOutputList.toArray(new Wire[] {});
		return Arrays.copyOf(out, length);
	}

	@Override
	public Wire[] getOutputWires() {
		return ciphertext;
	}

}
