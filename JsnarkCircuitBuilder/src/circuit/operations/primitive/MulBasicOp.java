package circuit.operations.primitive;

import java.math.BigInteger;

import circuit.config.Config;
import circuit.structure.Wire;

public class MulBasicOp extends BasicOp {

	public MulBasicOp(Wire w1, Wire w2, Wire output, String...desc) {
		super(new Wire[] { w1, w2 }, new Wire[] { output }, desc);
		opcode = "mul";
		numMulGates = 1;
	}

	@Override
	public void compute(BigInteger[] assignment) {
		BigInteger result =assignment[inputs[0].getWireId()].multiply(
				assignment[inputs[1].getWireId()]);
		if(result.compareTo(Config.FIELD_PRIME) > 0){
			result = result.mod(Config.FIELD_PRIME);
		}
		assignment[outputs[0].getWireId()] = result;
	}


}