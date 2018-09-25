/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.structure;

import java.math.BigInteger;

import circuit.eval.Instruction;
import circuit.operations.primitive.AddBasicOp;
import circuit.operations.primitive.ConstMulBasicOp;
import circuit.operations.primitive.MulBasicOp;
import circuit.operations.primitive.ORBasicOp;
import circuit.operations.primitive.XorBasicOp;


public class BitWire extends Wire {

	public BitWire(int wireId) {
		super(wireId);
	}

	public Wire mul(Wire w, String desc) {
		if (w instanceof ConstantWire) {
			return this.mul(((ConstantWire) w).getConstant(), desc);
		} else {
			Wire output;
			if (w instanceof BitWire)
				output = new VariableBitWire(generator.currentWireId++);
			else
				output = new VariableWire(generator.currentWireId++);
			Instruction op = new MulBasicOp(this, w, output, desc);
			Wire[] cachedOutputs = generator.addToEvaluationQueue(op);
			if(cachedOutputs == null){
				return output;
			}
			else{
				generator.currentWireId--;
				return cachedOutputs[0];
			}
		}
	}

	public Wire mul(BigInteger b, String... desc) {
		Wire out;
		if(b.equals(BigInteger.ZERO)){
			return generator.zeroWire;
		} else if(b.equals(BigInteger.ONE)){
			return this;
		} else{
			out = new LinearCombinationWire(generator.currentWireId++);
			Instruction op = new ConstMulBasicOp(this, out, b, desc);
//			generator.addToEvaluationQueue(op);
//			return out;			
			Wire[] cachedOutputs = generator.addToEvaluationQueue(op);
			if(cachedOutputs == null){
				return out;
			}
			else{
				generator.currentWireId--;
				return cachedOutputs[0];
			}	
		}
	}

	public Wire invAsBit(String...desc) {
//		Wire neg = new Wire(generator.currentWireId++);
//		Instruction op = new ConstMulBasicOp(this, neg, -1, desc);
//		generator.addToEvaluationQueue(op);
		Wire neg = this.mul(-1, desc);
		Wire out = new LinearCombinationBitWire(generator.currentWireId++);
		Instruction op = new AddBasicOp(new Wire[] { generator.oneWire, neg }, out, desc);
//		generator.addToEvaluationQueue(op);
		Wire[] cachedOutputs = generator.addToEvaluationQueue(op);
		if(cachedOutputs == null){
			return out;
		}
		else{
			generator.currentWireId--;
			return cachedOutputs[0];
		}		
	}
	
	public Wire or(Wire w, String...desc) {
		 if (w instanceof ConstantWire) {
			return w.or(this, desc);
		} else {
			Wire out;
			if (w instanceof BitWire) {
				out = new VariableBitWire(generator.currentWireId++);
				Instruction op = new ORBasicOp(this, w, out, desc);
				Wire[] cachedOutputs = generator.addToEvaluationQueue(op);
				if(cachedOutputs == null){
					return out;
				}
				else{
					generator.currentWireId--;
					return cachedOutputs[0];
				}
			} else {
				return super.or(w, desc);
			}	
		}
	}
	
	
	public Wire xor(Wire w, String...desc) {
		 if (w instanceof ConstantWire) {
			return w.xor(this, desc);
		} else {
			Wire out;
			if (w instanceof BitWire) {
				out = new VariableBitWire(generator.currentWireId++);
				Instruction op = new XorBasicOp(this, w, out, desc);
				Wire[] cachedOutputs = generator.addToEvaluationQueue(op);
				if(cachedOutputs == null){
					return out;
				}
				else{
					generator.currentWireId--;
					return cachedOutputs[0];
				}
			} else {
				return super.xor(w, desc);
			}	
		}
	}
	
	public WireArray getBits(Wire w, int bitwidth, String...desc) {
		return new WireArray( new Wire[]{this}).adjustLength(bitwidth);
	}
	
}
