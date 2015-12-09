package circuit.operations.primitive;

import java.math.BigInteger;

import util.Util;
import circuit.structure.Wire;

public class ORBasicOp extends BasicOp {

	public ORBasicOp(Wire w1, Wire w2, Wire output, String...desc) {
		super(new Wire[] { w1, w2 }, new Wire[] { output }, desc);
		opcode = "or";
		numMulGates = 1;
	}


	public void checkInputs(BigInteger[] assignment) {
		super.checkInputs(assignment);
		boolean check = Util.isBinary(assignment[inputs[0].getWireId()])
				&& Util.isBinary(assignment[inputs[1].getWireId()]);
		if (!check){			
			System.err.println("Error - Input(s) to OR are not binary. "
					+ this);
			throw new RuntimeException("Error During Evaluation");

		}
	}

	@Override
	public void compute(BigInteger[] assignment) {
		assignment[outputs[0].getWireId()] = assignment[inputs[0].getWireId()].or(
				assignment[inputs[1].getWireId()]);
	}



}