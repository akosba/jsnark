/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.gadgets.hash;

import java.math.BigInteger;
import java.util.Arrays;

import util.Util;
import circuit.config.Config;
import circuit.operations.Gadget;
import circuit.structure.Wire;

public class SubsetSumHashGadget extends Gadget {

	public static final int DIMENSION = 3; // set to 4 for higher security
	public static final int INPUT_LENGTH = 2 * DIMENSION * Config.LOG2_FIELD_PRIME; // length in bits
	private static final BigInteger[][] COEFFS;

	private Wire[] inputWires;
	private Wire[] outWires;
	private boolean binaryOutput;

	static {
		COEFFS = new BigInteger[DIMENSION][INPUT_LENGTH];
		for (int i = 0; i < DIMENSION; i++) {
			for (int k = 0; k < INPUT_LENGTH; k++) {
				COEFFS[i][k] = Util.nextRandomBigInteger(Config.FIELD_PRIME);
			}

		}
	}

	/**
	 * @param ins
	 *            The bitwires of the input.
	 * @param binaryOutput
	 *            Whether the output digest should be splitted into bits or not.
	 * @param desc
	 */
	public SubsetSumHashGadget(Wire[] ins, boolean binaryOutput, String... desc) {

		super(desc);
		int numBlocks = (int) Math.ceil(ins.length * 1.0 / INPUT_LENGTH);

		if (numBlocks > 1) {
			throw new IllegalArgumentException("Only one block is supported at this point");
		}

		int rem = numBlocks * INPUT_LENGTH - ins.length;

		Wire[] pad = new Wire[rem];
		for (int i = 0; i < pad.length; i++) {
			pad[i] = generator.getZeroWire(); // TODO: adjust padding
		}
		inputWires = Util.concat(ins, pad);
		this.binaryOutput = binaryOutput;
		buildCircuit();
	}

	private void buildCircuit() {

		Wire[] outDigest = new Wire[DIMENSION];
		Arrays.fill(outDigest, generator.getZeroWire());

		for (int i = 0; i < DIMENSION; i++) {
			for (int j = 0; j < INPUT_LENGTH; j++) {
				Wire t = inputWires[j].mul(COEFFS[i][j]);
				outDigest[i] = outDigest[i].add(t);
			}
		}
		if (!binaryOutput) {
			outWires = outDigest;
		} else {
			outWires = new Wire[DIMENSION * Config.LOG2_FIELD_PRIME];
			for (int i = 0; i < DIMENSION; i++) {
				Wire[] bits = outDigest[i].getBitWires(Config.LOG2_FIELD_PRIME).asArray();
				for (int j = 0; j < bits.length; j++) {
					outWires[j + i * Config.LOG2_FIELD_PRIME] = bits[j];
				}
			}
		}
	}

	@Override
	public Wire[] getOutputWires() {
		return outWires;
	}
}
