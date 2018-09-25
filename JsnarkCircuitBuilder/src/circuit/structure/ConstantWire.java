/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.structure;

import java.math.BigInteger;

import circuit.config.Config;
import circuit.eval.Instruction;
import circuit.operations.primitive.ConstMulBasicOp;

public class ConstantWire extends Wire {

	protected BigInteger constant;

	public ConstantWire(int wireId, BigInteger value) {
		super(wireId);
		constant = value.mod(Config.FIELD_PRIME);
	}
	
	public BigInteger getConstant() {
		return constant;
	}

	public boolean isBinary() {
		return constant.equals(BigInteger.ONE)
				|| constant.equals(BigInteger.ZERO);
	}

	public Wire mul(Wire w, String... desc) {
		if (w instanceof ConstantWire) {
			return generator.createConstantWire(
					constant.multiply(((ConstantWire) w).constant), desc);
		} else {
			return w.mul(constant, desc);
		}
	}

	public Wire mul(BigInteger b, String... desc) {
		Wire out;
		boolean sign = b.signum() == -1;
		BigInteger newConstant = constant.multiply(b).mod(Config.FIELD_PRIME);
		 	
		out = generator.knownConstantWires.get(newConstant);
		if (out == null) {
			
			if(!sign){
				out = new ConstantWire(generator.currentWireId++, newConstant);
			} else{
				out = new ConstantWire(generator.currentWireId++, newConstant.subtract(Config.FIELD_PRIME));
			}			
			Instruction op = new ConstMulBasicOp(this, out,
					b, desc);
			Wire[] cachedOutputs = generator.addToEvaluationQueue(op);
			if(cachedOutputs == null){
				generator.knownConstantWires.put(newConstant, out);
				return out;
			}
			else{
				// this branch might not be needed
				generator.currentWireId--;
				return cachedOutputs[0];
			}
			
		}
		return out;
	}

	public Wire checkNonZero(Wire w, String... desc) {
		if (constant.equals(BigInteger.ZERO)) {
			return generator.zeroWire;
		} else {
			return generator.oneWire;
		}
	}

	public Wire invAsBit(String... desc) {
		if (!isBinary()) {
			throw new RuntimeException(
					"Trying to invert a non-binary constant!");
		}
		if (constant.equals(BigInteger.ZERO)) {
			return generator.oneWire;
		} else {
			return generator.zeroWire;
		}
	}

	public Wire or(Wire w, String... desc) {
		if (w instanceof ConstantWire) {
			ConstantWire cw = (ConstantWire) w;
			if (isBinary() && cw.isBinary()) {
				if (constant.equals(BigInteger.ZERO)
						&& cw.getConstant().equals(BigInteger.ZERO)) {
					return generator.zeroWire;
				} else {
					return generator.oneWire;
				}
			} else {
				throw new RuntimeException(
						"Trying to OR two non-binary constants");
			}
		} else {
			if (constant.equals(BigInteger.ONE)) {
				return generator.oneWire;
			} else {
				return w;
			}
		}
	}

	public Wire xor(Wire w, String... desc) {
		if (w instanceof ConstantWire) {
			ConstantWire cw = (ConstantWire) w;
			if (isBinary() && cw.isBinary()) {
				if (constant.equals(cw.getConstant())) {
					return generator.zeroWire;
				} else {
					return generator.oneWire;
				}
			} else {
				throw new RuntimeException(
						"Trying to XOR two non-binary constants");
			}
		} else {
			if (constant.equals(BigInteger.ONE)) {
				return w.invAsBit(desc);
			} else {
				return w;
			}
		}
	}

	public WireArray getBitWires(int bitwidth, String... desc) {
		if (constant.bitLength() > bitwidth) {
			throw new RuntimeException("Trying to split a constant of "
					+ constant.bitLength() + " bits into " + bitwidth + "bits");
		} else {
			Wire[] bits = new ConstantWire[bitwidth];
			for (int i = 0; i < bitwidth; i++) {
				bits[i] = constant.testBit(i) ? generator.oneWire : generator.zeroWire;
			}
			return new WireArray(bits);
		}
	}
	
	public void restrictBitLength(int bitwidth, String...desc) {
		getBitWires(bitwidth, desc);
	}
	
	protected void pack(String...desc){
	}
	
}
