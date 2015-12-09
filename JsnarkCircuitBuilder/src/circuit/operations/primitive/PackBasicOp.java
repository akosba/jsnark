package circuit.operations.primitive;

import java.math.BigInteger;

import util.Util;
import circuit.config.Config;
import circuit.structure.Wire;

public class PackBasicOp extends BasicOp {

	public PackBasicOp(Wire[] inBits, Wire out, String...desc) {
		super(inBits, new Wire[] { out }, desc);
		opcode = "pack";
		numMulGates = 1;
	}

	@Override
	public void checkInputs(BigInteger[] assignment) {
		super.checkInputs(assignment);
		boolean check = true;
		for(int i = 0; i < inputs.length; i++){
			check &= Util.isBinary(assignment[inputs[i].getWireId()]);
		}
		if (!check){
			System.err.println("Error - Input(s) to Pack are not binary. "
					+ this);
			throw new RuntimeException("Error During Evaluation");

		}
	}
	
	@Override
	public void compute(BigInteger[] assignment) {
		BigInteger sum = BigInteger.ZERO;
		for (int i = 0; i < inputs.length; i++) {
			sum = sum.add(assignment[inputs[i].getWireId()].multiply(
					new BigInteger("2").pow(i)));
		}
		assignment[outputs[0].getWireId()]= sum.mod(Config.FIELD_PRIME);
	}



}