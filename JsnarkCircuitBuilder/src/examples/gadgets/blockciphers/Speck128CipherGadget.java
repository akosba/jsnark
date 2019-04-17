/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/

package examples.gadgets.blockciphers;

import circuit.operations.Gadget;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;

/**
 * Implements the Speck lightweight block cipher
 * https://eprint.iacr.org/2015/585.pdf
 *
 */

public class Speck128CipherGadget extends Gadget {

	private Wire[] plaintext;
	private Wire[] expandedKey;
	private Wire[] ciphertext;

	
	/**
	 * 
	 * @param inputs
	 *            : Array of 2 64-bit elements.
	 * @param expandedKey
	 *            : Array of 32 64-bit elements. (Call expandKey(..))
	 * @param desc
	 */
	public Speck128CipherGadget(Wire[] plaintext, Wire[] expandedKey,
			String... desc) {
		super(desc);
		if (plaintext.length != 2 || expandedKey.length != 32) {
			throw new IllegalArgumentException("Invalid Input");
		}
		this.plaintext = plaintext;
		this.expandedKey = expandedKey;
		buildCircuit();
	}

	protected void buildCircuit() {

		Wire x, y;
		x = plaintext[1];
		y = plaintext[0];
		ciphertext = new Wire[2];
		for (int i = 0; i <= 31; i++) {
			x = x.rotateRight(64, 8).add(y);
			x = x.trimBits(65, 64);
			x = x.xorBitwise(expandedKey[i], 64);
			y = y.rotateLeft(64, 3).xorBitwise(x, 64);
		}
		ciphertext[1] = x;
		ciphertext[0] = y;
	}

	/**
	 * 
	 * @param key
	 *            : 2 64-bit words
	 * @return
	 */
	public static Wire[] expandKey(Wire[] key) {
		CircuitGenerator generator = CircuitGenerator
				.getActiveCircuitGenerator();
		Wire[] k = new Wire[32];
		Wire[] l = new Wire[32];
		k[0] = key[0];
		l[0] = key[1];
		for (int i = 0; i <= 32 - 2; i++) {
			l[i + 1] = k[i].add(l[i].rotateLeft(64, 56));
			l[i + 1] = l[i + 1].trimBits(65, 64);
			l[i + 1] = l[i + 1].xorBitwise(generator.createConstantWire(i), 64);
			k[i + 1] = k[i].rotateLeft(64, 3).xorBitwise(l[i + 1], 64);
		}
		return k;
	}

	@Override
	public Wire[] getOutputWires() {
		return ciphertext;
	}
}
