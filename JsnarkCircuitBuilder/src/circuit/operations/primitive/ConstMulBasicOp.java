/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.operations.primitive;

import java.math.BigInteger;

import circuit.config.Config;
import circuit.structure.Wire;

public class ConstMulBasicOp extends BasicOp {

	private BigInteger constInteger;
	private boolean inSign;
	
	public ConstMulBasicOp(Wire w, Wire out, BigInteger constInteger,
			String...desc) {
		super(new Wire[] { w }, new Wire[] { out }, desc);
		inSign = constInteger.signum() == -1;
		if (!inSign) {
			constInteger = constInteger.mod(Config.FIELD_PRIME);
			this.constInteger =constInteger;
		} else {
			constInteger = constInteger.negate();
			constInteger = constInteger.mod(Config.FIELD_PRIME);
			this.constInteger = Config.FIELD_PRIME.subtract(constInteger);
		}
	}

	public String getOpcode(){
		if (!inSign) {
			return "const-mul-" + constInteger.toString(16);
		} else{
			return "const-mul-neg-" + Config.FIELD_PRIME.subtract(constInteger).toString(16);
		}
	}
	
	@Override
	public void compute(BigInteger[] assignment) {
		BigInteger result = assignment[inputs[0].getWireId()].multiply(constInteger);
		if (result.bitLength() >= Config.LOG2_FIELD_PRIME) {
			result = result.mod(Config.FIELD_PRIME);
		}
		assignment[outputs[0].getWireId()] = result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof ConstMulBasicOp)) {
			return false;
		}
		ConstMulBasicOp op = (ConstMulBasicOp) obj;
		return inputs[0].equals(op.inputs[0]) && constInteger.equals(op.constInteger);

	}
	
	@Override
	public int getNumMulGates() {
		return 0;
	}


	@Override
	public int hashCode() {
		int h = constInteger.hashCode();
		for(Wire in:inputs){
			h+=in.hashCode();
		}
		return h;
	}
	
	
}