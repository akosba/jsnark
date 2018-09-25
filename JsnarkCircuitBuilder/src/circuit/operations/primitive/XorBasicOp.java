/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.operations.primitive;

import java.math.BigInteger;

import util.Util;
import circuit.structure.Wire;

public class XorBasicOp extends BasicOp {

	public XorBasicOp(Wire w1, Wire w2, Wire output, String...desc) {
		super(new Wire[] { w1, w2 }, new Wire[] { output }, desc);
	}

	public String getOpcode(){
		return "xor";
	}

	public void checkInputs(BigInteger[] assignment) {
		super.checkInputs(assignment);
		boolean check = Util.isBinary(assignment[inputs[0].getWireId()])
				&& Util.isBinary(assignment[inputs[1].getWireId()]);
		if (!check){
			System.err.println("Error - Input(s) to XOR are not binary. "
					+ this);
			throw new RuntimeException("Error During Evaluation");

		}
	}

	@Override
	public void compute(BigInteger[] assignment) {
		assignment[outputs[0].getWireId()] = assignment[inputs[0].getWireId()].xor(
				assignment[inputs[1].getWireId()]);
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj)
			return true;
		if (!(obj instanceof XorBasicOp)) {
			return false;
		}
		XorBasicOp op = (XorBasicOp) obj;

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