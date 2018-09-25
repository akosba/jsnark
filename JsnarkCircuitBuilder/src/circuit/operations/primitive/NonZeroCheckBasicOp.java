/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.operations.primitive;

import java.math.BigInteger;

import circuit.structure.Wire;

public class NonZeroCheckBasicOp extends BasicOp {

	public NonZeroCheckBasicOp(Wire w, Wire out1, Wire out2 , String...desc) {
		super(new Wire[] { w }, new Wire[]{out1, out2}, desc);
	}

	public String getOpcode(){
		return "zerop";
	}
	@Override
	public void compute(BigInteger[] assignment) {

		if (assignment[inputs[0].getWireId()].signum() == 0) {
			assignment[outputs[1].getWireId()] = BigInteger.ZERO;
		} else {
			assignment[outputs[1].getWireId()] = BigInteger.ONE;
		}
		assignment[outputs[0].getWireId()] = BigInteger.ZERO; // a dummy value
	}
	
	@Override
	public boolean equals(Object obj) {

		if (this == obj)
			return true;
		if (!(obj instanceof NonZeroCheckBasicOp)) {
			return false;
		}
		NonZeroCheckBasicOp op = (NonZeroCheckBasicOp) obj;
		return inputs[0].equals(op.inputs[0]);

	}

	@Override
	public int getNumMulGates() {
		return 2;
	}

}
