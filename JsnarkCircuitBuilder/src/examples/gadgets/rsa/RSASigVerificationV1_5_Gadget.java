/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.gadgets.rsa;

import circuit.auxiliary.LongElement;
import circuit.operations.Gadget;
import circuit.structure.Wire;
import circuit.structure.WireArray;
import examples.gadgets.math.LongIntegerModGadget;

/**
 * A gadget to check if an RSA signature is valid according to PKCS 1 v1.5 (A
 * gadget based on the latest standard (PSS) will be added in the future).
 * This gadget assumes SHA256 for the message hash, and a public exponent of
 * 0x10001.
 * This gadget can accept a hardcoded or a variable RSA modulus. See the
 * corresponding generator example. 
 * 
 * Implemented according to the standard specs here:
 * https://www.emc.com/collateral/white-
 * papers/h11300-pkcs-1v2-2-rsa-cryptography-standard-wp.pdf
 * 
 * 
 * 
 * 
 */
public class RSASigVerificationV1_5_Gadget extends Gadget {

	private LongElement modulus;
	private LongElement signature;
	private Wire[] msgHash; // 32-bit wires (the output of SHA256 gadget)
	private Wire isValidSignature;
	private int rsaKeyBitLength; // in bits

	public static final byte[] SHA256_IDENTIFIER = new byte[] { 0x30, 0x31,
			0x30, 0x0d, 0x06, 0x09, 0x60, (byte) 0x86, 0x48, 0x01, 0x65, 0x03,
			0x04, 0x02, 0x01, 0x05, 0x00, 0x04, 0x20 };

	public static final int SHA256_DIGEST_LENGTH = 32; // in bytes

	public RSASigVerificationV1_5_Gadget(LongElement modulus, Wire[] msgHash,
			LongElement signature, int rsaKeyBitLength, String... desc) {
		super(desc);
		this.modulus = modulus;
		this.msgHash = msgHash;
		this.signature = signature;
		this.rsaKeyBitLength = rsaKeyBitLength;
		buildCircuit();
	}

	private void buildCircuit() {

		LongElement s = signature;

		for (int i = 0; i < 16; i++) {
			s = s.mul(s);
			s = new LongIntegerModGadget(s, modulus, rsaKeyBitLength, false).getRemainder();
		}
		s = s.mul(signature);
		s = new LongIntegerModGadget(s, modulus, rsaKeyBitLength, true).getRemainder();
		Wire[] sChunks = s.getArray();

		// note that the following can be improved, but for simplicity we
		// are going to compare byte by byte

		// get byte arrays
		Wire[] sBytes = new WireArray(sChunks).getBits(
				LongElement.CHUNK_BITWIDTH).packBitsIntoWords(8);
		Wire[] msgHashBytes = new WireArray(msgHash).getBits(32)
				.packBitsIntoWords(8);

		// reverse the byte array representation of each word of the digest to
		// be compatiable with the endianess
		for (int i = 0; i < 8; i++) {
			Wire tmp = msgHashBytes[4 * i];
			msgHashBytes[4 * i] = msgHashBytes[(4 * i + 3)];
			msgHashBytes[4 * i + 3] = tmp;
			tmp = msgHashBytes[4 * i + 1];
			msgHashBytes[4 * i + 1] = msgHashBytes[4 * i + 2];
			msgHashBytes[4 * i + 2] = tmp;
		}

		int lengthInBytes = (int) (Math.ceil(rsaKeyBitLength * 1.0 / 8));
		Wire sumChecks = generator.getZeroWire();
		sumChecks = sumChecks.add(sBytes[lengthInBytes - 1].isEqualTo(0));
		sumChecks = sumChecks.add(sBytes[lengthInBytes - 2].isEqualTo(1));
		for (int i = 3; i < lengthInBytes - SHA256_DIGEST_LENGTH
				- SHA256_IDENTIFIER.length; i++) {
			sumChecks = sumChecks
					.add(sBytes[lengthInBytes - i].isEqualTo(0xff));
		}
		sumChecks = sumChecks.add(sBytes[SHA256_DIGEST_LENGTH
				+ SHA256_IDENTIFIER.length].isEqualTo(0));

		for (int i = 0; i < SHA256_IDENTIFIER.length; i++) {
			sumChecks = sumChecks.add(sBytes[SHA256_IDENTIFIER.length
					+ SHA256_DIGEST_LENGTH - 1 - i]
					.isEqualTo((int) (SHA256_IDENTIFIER[i] + 256) % 256));
		}
		for (int i = SHA256_DIGEST_LENGTH - 1; i >= 0; i--) {
			sumChecks = sumChecks.add(sBytes[SHA256_DIGEST_LENGTH - 1 - i]
					.isEqualTo(msgHashBytes[i]));
		}

		isValidSignature = sumChecks.isEqualTo(lengthInBytes);

	}

	@Override
	public Wire[] getOutputWires() {
		return new Wire[] { isValidSignature };
	}
}
