/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/

package examples.gadgets.blockciphers;

import circuit.operations.Gadget;
import circuit.structure.Wire;

/**
 * Implements the light weight cipher Chaskey128, the LTS version with 16 rounds
 * https://eprint.iacr.org/2014/386.pdf.
 * 
 * The gadget follows the reference implementation from this project:
 * https://www.nist.gov/sites/default/files/documents/2016/10/18/perrin-paper-lwc2016.pdf
 * https://www.cryptolux.org/index.php/FELICS
 */
public class ChaskeyLTS128CipherGadget extends Gadget {

	private Wire[] plaintext; // 4 32-bit words
	private Wire[] key; // 4 32-bit words
	private Wire[] ciphertext; // 4 32-bit words

	public ChaskeyLTS128CipherGadget(Wire[] inputs, Wire[] key, String... desc) {
		super(desc);
		if (inputs.length != 4 || key.length != 4) {
			throw new IllegalArgumentException("Invalid Input");
		}
		this.plaintext = inputs;
		this.key = key;

		buildCircuit();

	}

	protected void buildCircuit() {

		Wire[] v = new Wire[4];
		for (int i = 0; i < 4; i++) {
			v[i] = (plaintext[i].xorBitwise(key[i], 32));
		}

		for (int i = 0; i < 16; i++) {

			v[0] = v[0].add(v[1]);
			v[0] = v[0].trimBits(33, 32);
			v[1] = v[1].rotateLeft(32, 5).xorBitwise(v[0], 32);
			v[0] = v[0].rotateLeft(32, 16);

			v[2] = v[2].add(v[3]).trimBits(33, 32);
			v[3] = v[3].rotateLeft(32, 8).xorBitwise(v[2], 32);

			v[0] = v[0].add(v[3]).trimBits(33, 32);
			v[3] = v[3].rotateLeft(32, 13).xorBitwise(v[0], 32);

			v[2] = v[2].add(v[1]).trimBits(33, 32);
			;
			v[1] = v[1].rotateLeft(32, 7).xorBitwise(v[2], 32);
			v[2] = v[2].rotateLeft(32, 16);

		}

		for (int i = 0; i < 4; i++) {
			v[i] = v[i].xorBitwise(key[i], 32);
		}
		ciphertext = v;
	}

	@Override
	public Wire[] getOutputWires() {
		return ciphertext;
	}

}
