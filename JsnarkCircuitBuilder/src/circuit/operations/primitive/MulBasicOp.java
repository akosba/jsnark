/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.operations.primitive;

import java.math.BigInteger;

import circuit.config.Config;
import circuit.structure.Wire;

public class MulBasicOp extends BasicOp {

	public MulBasicOp(Wire w1, Wire w2, Wire output, String... desc) {
		super(new Wire[] { w1, w2 }, new Wire[] { output }, desc);
	}

	public String getOpcode(){
		return "mul";
	}
	
	@Override
	public void compute(BigInteger[] assignment) {
		BigInteger result = assignment[inputs[0].getWireId()]
				.multiply(assignment[inputs[1].getWireId()]);
		if (result.compareTo(Config.FIELD_PRIME) > 0) {
			result = result.mod(Config.FIELD_PRIME);
		}
		assignment[outputs[0].getWireId()] = result;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj)
			return true;
		if (!(obj instanceof MulBasicOp)) {
			return false;
		}
		MulBasicOp op = (MulBasicOp) obj;

		boolean check1 = inputs[0].equals(op.inputs[0])
				&& inputs[1].equals(op.inputs[1]);
		boolean check2 = inputs[1].equals(op.inputs[0])
				&& inputs[0].equals(op.inputs[1]);
		return check1 || check2;

	}
	
	@Override
	public int getNumMulGates() {
		return 1;
	}


}