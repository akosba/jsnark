/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.operations.primitive;

import java.math.BigInteger;

import circuit.config.Config;
import circuit.structure.Wire;

public class AssertBasicOp extends BasicOp {

	public AssertBasicOp(Wire w1, Wire w2, Wire output, String...desc) {
		super(new Wire[] { w1, w2 }, new Wire[] { output }, desc);
		opcode = "assert";
		numMulGates = 1;
	}

	@Override
	protected void compute(BigInteger[] assignment) {
		BigInteger leftSide = assignment[inputs[0].getWireId()].multiply(
				assignment[inputs[1].getWireId()]).mod(
						Config.FIELD_PRIME);
		BigInteger rightSide = assignment[outputs[0].getWireId()];
		boolean check = leftSide.equals(rightSide);
		if (!check) {
			System.err.println("Error - Assertion Failed " + this);
			System.out.println(assignment[inputs[0].getWireId()] + "*"
					+ assignment[inputs[1].getWireId()] + "!="
					+ assignment[outputs[0].getWireId()]);
			throw new RuntimeException("Error During Evaluation");
		}
	}

	@Override
	protected void checkOutputs(BigInteger[] assignment) {
		// do nothing
	}
}