/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.operations.primitive;

import java.math.BigInteger;

import circuit.structure.Wire;

public class NonZeroCheckBasicOp extends BasicOp {

	public NonZeroCheckBasicOp(Wire w, Wire out1, Wire out2 , String...desc) {
		super(new Wire[] { w }, new Wire[]{out1, out2}, desc);
		opcode = "zerop";
		numMulGates = 2;
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

}
