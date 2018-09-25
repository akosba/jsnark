/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.operations.primitive;

import java.math.BigInteger;

import circuit.config.Config;
import circuit.structure.Wire;

public class SplitBasicOp extends BasicOp {

	public SplitBasicOp(Wire w, Wire[] outs, String...desc) {
		super(new Wire[] { w }, outs, desc);
	}

	public String getOpcode(){
		return "split";
	}
	
	protected void checkInputs(BigInteger[] assignment) {
		super.checkInputs(assignment);
		if (outputs.length < assignment[inputs[0].getWireId()].bitLength()) {
			System.err
					.println("Error in Split --- The number of bits does not fit -- Input: "
							+ assignment[inputs[0].getWireId()].toString(16) + "\n\t" + this);

			throw new RuntimeException("Error During Evaluation -- " + this);
		}
	}

	@Override
	protected void compute(BigInteger[] assignment) {

		BigInteger inVal = assignment[inputs[0].getWireId()];
		if (inVal.compareTo(Config.FIELD_PRIME) > 0) {
			inVal = inVal.mod(Config.FIELD_PRIME);
		}
		for (int i = 0; i < outputs.length; i++) {
			assignment[outputs[i].getWireId()] = inVal.testBit(i) ? BigInteger.ONE
					: BigInteger.ZERO;
		}
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj)
			return true;
		if (!(obj instanceof SplitBasicOp)) {
			return false;
		}
		SplitBasicOp op = (SplitBasicOp) obj;
		return inputs[0].equals(op.inputs[0]) && outputs.length == op.outputs.length;

	}
	
	@Override
	public int getNumMulGates() {
		return outputs.length + 1;
	}

}
