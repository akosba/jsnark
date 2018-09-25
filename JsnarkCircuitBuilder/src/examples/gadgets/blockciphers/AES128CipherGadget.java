/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/

package examples.gadgets.blockciphers;

import java.util.Arrays;

import circuit.operations.Gadget;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import circuit.structure.WireArray;
import examples.gadgets.blockciphers.sbox.AESSBoxComputeGadget;
import examples.gadgets.blockciphers.sbox.AESSBoxGadgetOptimized1;
import examples.gadgets.blockciphers.sbox.AESSBoxGadgetOptimized2;
import examples.gadgets.blockciphers.sbox.AESSBoxNaiveLookupGadget;

/**
 * Implements an AES 128-bit block cipher. The gadget applies an improved
 * read-only memory lookup from xjsnark (to appear) to reduce the cost of the
 * S-box access. (See the sbox package for the improved lookup implementation)
 *
 */
public class AES128CipherGadget extends Gadget {

	//
	public enum SBoxOption {
		LINEAR_SCAN, COMPUTE, OPTIMIZED1, OPTIMIZED2
	}

	public static SBoxOption sBoxOption = SBoxOption.OPTIMIZED2;

	private Wire[] plaintext; // array of 16 bytes
	private Wire[] expandedKey; // array of 176 bytes (call expandKey(..))
	private Wire[] ciphertext; // array of 16 bytes

	private static int nb = 4;
	private static int nk = 4;
	private static int nr = 6 + nk;

	public static int RCon[] = new int[] { 0x8d, 0x01, 0x02, 0x04, 0x08, 0x10,
			0x20, 0x40, 0x80, 0x1b, 0x36 };

	public static int SBox[] = { 0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f,
			0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76, 0xca, 0x82,
			0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c,
			0xa4, 0x72, 0xc0, 0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc,
			0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15, 0x04, 0xc7, 0x23,
			0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27,
			0xb2, 0x75, 0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52,
			0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84, 0x53, 0xd1, 0x00, 0xed,
			0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58,
			0xcf, 0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9,
			0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8, 0x51, 0xa3, 0x40, 0x8f, 0x92,
			0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
			0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e,
			0x3d, 0x64, 0x5d, 0x19, 0x73, 0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a,
			0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb, 0xe0,
			0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62,
			0x91, 0x95, 0xe4, 0x79, 0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e,
			0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08, 0xba, 0x78,
			0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b,
			0xbd, 0x8b, 0x8a, 0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e,
			0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e, 0xe1, 0xf8, 0x98,
			0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55,
			0x28, 0xdf, 0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41,
			0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16 };

	/**
	 * 
	 * @param inputs
	 *            : array of 16 bytes (each wire represents a byte)
	 * @param expandedKey
	 *            : array of 176 bytes (each wire represents a byte) -- call
	 *            expandKey() to get it
	 */

	public AES128CipherGadget(Wire[] inputs, Wire[] expandedKey, String... desc) {

		super(desc);
		if (inputs.length != 4 * nb || expandedKey.length != 4 * nb * (nr + 1)) {
			throw new IllegalArgumentException("Invalid Input");
		}
		this.plaintext = inputs;
		this.expandedKey = expandedKey;
		buildCircuit();
	}

	// follows the outline in http://www.cs.utsa.edu/~wagner/laws/AESintro.html
	protected void buildCircuit() {
		ciphertext = new Wire[4 * nb];
		Wire[][] state = new Wire[4][nb];
		int i = 0;
		for (int j = 0; j < nb; j++) {
			for (int k = 0; k < 4; k++) {
				state[k][j] = plaintext[i++];
			}
		}

		state = addRoundKey(state, 0, nb - 1);
		int round = 0;
		for (round = 1; round < nr; round++) {
			subBytes(state);
			state = shiftRows(state);
			state = mixColumns(state);
			state = addRoundKey(state, round * nb * 4, (round + 1) * nb * 4 - 1);
		}

		subBytes(state);
		state = shiftRows(state);
		state = addRoundKey(state, round * nb * 4, (round + 1) * nb * 4 - 1);

		i = 0;
		for (int j = 0; j < nb; j++) {
			for (int k = 0; k < 4; k++) {
				ciphertext[i++] = state[k][j];
			}
		}
	}

	private void subBytes(Wire[][] state) {
		for (int i = 0; i < state.length; i++) {
			for (int j = 0; j < state[i].length; j++) {
				state[i][j] = randomAccess(generator, state[i][j]);
			}
		}
	}

	private Wire[][] mixColumns(Wire[][] state) {

		Wire a[] = new Wire[4];
		int c;

		for (c = 0; c < 4; c++) {
			for (int i = 0; i < 4; i++) {
				a[i] = state[i][c];
			}
			state[0][c] = galoisMulConst(a[0], 2)
					.xorWireArray(galoisMulConst(a[1], 3))
					.xorWireArray(a[2].getBitWires(8))
					.xorWireArray(a[3].getBitWires(8)).packAsBits();

			state[1][c] = a[0].getBitWires(8)
					.xorWireArray(galoisMulConst(a[1], 2))
					.xorWireArray(galoisMulConst(a[2], 3))
					.xorWireArray(a[3].getBitWires(8)).packAsBits();

			state[2][c] = a[0].getBitWires(8).xorWireArray(a[1].getBitWires(8))
					.xorWireArray(galoisMulConst(a[2], 2))
					.xorWireArray(galoisMulConst(a[3], 3)).packAsBits();
			state[3][c] = galoisMulConst(a[0], 3)
					.xorWireArray(a[1].getBitWires(8))
					.xorWireArray(a[2].getBitWires(8))
					.xorWireArray(galoisMulConst(a[3], 2)).packAsBits();

		}
		return state;
	}

	private WireArray galoisMulConst(Wire wire, int i) {

		Wire p = generator.getZeroWire();
		int counter;
		Wire hiBitSet;

		for (counter = 0; counter < 8; counter++) {
			if ((i & 1) != 0) {
				p = p.xorBitwise(wire, 8);
			}
			i >>= 1;
			if (i == 0)
				break;
			hiBitSet = wire.getBitWires(8).get(7);
			wire = wire.shiftLeft(8, 1);
			Wire tmp = wire.xorBitwise(generator.createConstantWire(0x1bL), 8);
			wire = wire.add(hiBitSet.mul(tmp.sub(wire)));
		}
		return p.getBitWires(8);
	}

	private Wire[][] shiftRows(Wire[][] state) {
		Wire[][] newState = new Wire[4][nb];
		newState[0] = Arrays.copyOf(state[0], nb);
		for (int j = 0; j < nb; j++) {
			newState[1][j] = state[1][(j + 1) % nb];
			newState[2][j] = state[2][(j + 2) % nb];
			newState[3][j] = state[3][(j + 3) % nb];
		}
		return newState;
	}

	private Wire[][] addRoundKey(Wire[][] state, int from, int to) {
		Wire[][] newState = new Wire[4][nb];
		int idx = 0;
		for (int j = 0; j < nb; j++) {
			for (int i = 0; i < 4; i++) {
				newState[i][j] = state[i][j].xorBitwise(
						expandedKey[from + idx], 8);
				idx++;
			}
		}
		return newState;
	}

	@Override
	public Wire[] getOutputWires() {
		return ciphertext;
	}

	// key is a 16-byte array. Each wire represents a byte.
	public static Wire[] expandKey(Wire[] key) {

		Wire[][] w = new Wire[nb * (nr + 1)][4];
		Wire[] temp;
		int i = 0;
		while (i < nk) {
			w[i] = new Wire[] { key[4 * i], key[4 * i + 1], key[4 * i + 2],
					key[4 * i + 3] };
			i++;
		}

		CircuitGenerator generator = CircuitGenerator
				.getActiveCircuitGenerator();
		i = nk;
		while (i < nb * (nr + 1)) {
			temp = w[i - 1];
			if (i % nk == 0) {
				temp = subWord(generator, rotateWord(generator, temp));
				temp[0] = temp[0].xorBitwise(
						generator.createConstantWire(RCon[i / nk]), 8);
			} else if (nk > 6 && (i % nk) == 4) {
				temp = subWord(generator, temp);
			}

			for (int v = 0; v < 4; v++) {
				w[i][v] = w[i - nk][v].xorBitwise(temp[v], 8);

			}

			i++;

		}
		Wire[] expanded = new Wire[nb * (nr + 1) * 4];
		int idx = 0;
		for (int k = 0; k < nb * (nr + 1); k++) {
			for (i = 0; i < 4; i++) {
				expanded[idx++] = w[k][i];
			}
		}

		return expanded;
	}

	private static Wire[] subWord(CircuitGenerator generator, Wire[] w) {
		for (int i = 0; i < w.length; i++) {
			w[i] = randomAccess(generator, w[i]);
		}
		return w;
	}

	private static Wire[] rotateWord(CircuitGenerator generator, Wire[] w) {
		Wire[] newW = new Wire[w.length];
		for (int j = 0; j < w.length; j++) {
			newW[j] = w[(j + 1) % w.length];
		}
		return newW;
	}

	private static Wire randomAccess(CircuitGenerator generator, Wire wire) {

		Gadget g = null;
		switch (sBoxOption) {
		case LINEAR_SCAN:
			g = new AESSBoxNaiveLookupGadget(wire);
			break;
		case COMPUTE:
			g = new AESSBoxComputeGadget(wire);
			break;
		case OPTIMIZED1:
			g = new AESSBoxGadgetOptimized1(wire);
			break;
		case OPTIMIZED2:
			g = new AESSBoxGadgetOptimized2(wire);
			break;
		}

		return g.getOutputWires()[0];
	}

}
