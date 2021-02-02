/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.structure;

import java.math.BigInteger;

import circuit.config.Config;
import circuit.eval.Instruction;
import circuit.operations.primitive.ConstMulBasicOp;
import circuit.operations.primitive.MulBasicOp;
import circuit.operations.primitive.NonZeroCheckBasicOp;
import circuit.operations.primitive.ORBasicOp;
import circuit.operations.primitive.PackBasicOp;
import circuit.operations.primitive.SplitBasicOp;
import circuit.operations.primitive.XorBasicOp;

public class Wire {

	protected int wireId = -1;
	protected CircuitGenerator generator;

	public Wire(int wireId) {
		this.generator = CircuitGenerator.getActiveCircuitGenerator();
		if (wireId < 0) {
			throw new IllegalArgumentException("wire id cannot be negative");
		}
		this.wireId = wireId;
	}

	protected Wire(WireArray bits) {
		this.generator = CircuitGenerator.getActiveCircuitGenerator();
		setBits(bits);
	}

	public String toString() {
		return wireId + "";
	}

	public int getWireId() {
		return wireId;
	}

	WireArray getBitWires() {
		return null;
	}

	void setBits(WireArray bits) {
		// method overriden in subclasses
		// default action:
		System.err.println(
				"Warning --  you are trying to set bits for either a constant or a bit wire." + " -- Action Ignored");
	}

	public Wire mul(BigInteger b, String... desc) {
		packIfNeeded(desc);
		if (b.equals(BigInteger.ONE))
			return this;
		if (b.equals(BigInteger.ZERO))
			return generator.zeroWire;
		Wire out = new LinearCombinationWire(generator.currentWireId++);
		Instruction op = new ConstMulBasicOp(this, out, b, desc);
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

	public Wire mul(long l, String... desc) {
		return mul(new BigInteger(l + ""), desc);
	}

	public Wire mul(long base, int exp, String... desc) {
		BigInteger b = new BigInteger(base + "");
		b = b.pow(exp);
		return mul(b, desc);
	}

	public Wire mul(Wire w, String... desc) {
		if (w instanceof ConstantWire) {
			return this.mul(((ConstantWire) w).getConstant(), desc);
		} else {
			packIfNeeded(desc);
			w.packIfNeeded(desc);
			Wire output = new VariableWire(generator.currentWireId++);
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

	public Wire add(Wire w, String... desc) {
		packIfNeeded(desc);
		w.packIfNeeded(desc);
		return new WireArray(new Wire[] { this, w }).sumAllElements(desc);
	}

	public Wire add(long v, String... desc) {
		return add(generator.createConstantWire(v, desc), desc);
	}

	public Wire add(BigInteger b, String... desc) {
		return add(generator.createConstantWire(b, desc), desc);

	}

	public Wire sub(Wire w, String... desc) {
		packIfNeeded(desc);
		w.packIfNeeded(desc);
		Wire neg = w.mul(-1, desc);
		return add(neg, desc);
	}

	public Wire sub(long v, String... desc) {
		return sub(generator.createConstantWire(v, desc), desc);

	}

	public Wire sub(BigInteger b, String... desc) {
		return sub(generator.createConstantWire(b, desc), desc);
	}

	public Wire checkNonZero(String... desc) {
		packIfNeeded(desc);
		/**
		 * this wire is not currently used for anything - It's for compatibility
		 * with earlier experimental versions when the target was Pinocchio
		 **/
		Wire out1 = new Wire(generator.currentWireId++);
		Wire out2 = new VariableBitWire(generator.currentWireId++);
		Instruction op = new NonZeroCheckBasicOp(this, out1, out2, desc);
		Wire[] cachedOutputs = generator.addToEvaluationQueue(op);
		if(cachedOutputs == null){
			return out2;
		}
		else{
			generator.currentWireId-=2;
			return cachedOutputs[1];
		}		
	}

	public Wire invAsBit(String... desc) {
		packIfNeeded(desc); // just a precaution .. should not be really needed
		Wire w1 = this.mul(-1, desc);
		Wire out = generator.oneWire.add(w1, desc);
		return out;
	}

	public Wire or(Wire w, String... desc) {
		if (w instanceof ConstantWire) {
			return w.or(this, desc);
		} else {
			packIfNeeded(desc); // just a precaution .. should not be really
								// needed
			Wire out = new VariableWire(generator.currentWireId++);
			Instruction op = new ORBasicOp(this, w, out, desc);
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


	public Wire xor(Wire w, String... desc) {
		if (w instanceof ConstantWire) {
			return w.xor(this, desc);
		} else {
			packIfNeeded(desc); // just a precaution .. should not be really
								// needed
			Wire out = new VariableWire(generator.currentWireId++);
			Instruction op = new XorBasicOp(this, w, out, desc);
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

	public Wire and(Wire w, String... desc) {
		return mul(w, desc);
	}

	public WireArray getBitWires(int bitwidth, String... desc) {
		WireArray bitWires = getBitWires();
		if (bitWires == null) {
			bitWires = forceSplit(bitwidth, desc);
			setBits(bitWires);
			return bitWires;
		} else {
			if(bitwidth < bitWires.size() && !(this instanceof ConstantWire)){
				System.err.println("Warning: getBitWires() was called with different arguments on the same wire more than once");
				System.out.println("\t It was noted that the argument in the second call was less than the first.");
				System.out.println("\t If this was called for enforcing a bitwidth constraint, you must use restrictBitLengh(), otherwise you can ignore this.");
				if(Config.printStackTraceAtWarnings){
					Thread.dumpStack();
				} else{
					System.out.println("\t You can view the stack trace by setting Config.printStackTraceAtWarnings to true in the code.");
				}
			}
			return bitWires.adjustLength(bitwidth);
		}
	}
	
	public WireArray getBitWiresIfExistAlready(){
		return getBitWires();
	}

	protected WireArray forceSplit(int bitwidth, String... desc) {
		Wire[] ws = new VariableBitWire[bitwidth];
		for (int i = 0; i < bitwidth; i++) {
			ws[i] = new VariableBitWire(generator.currentWireId++);
		}
		Instruction op = new SplitBasicOp(this, ws, desc);
		Wire[] cachedOutputs = generator.addToEvaluationQueue(op);
		
		if(cachedOutputs == null){
			WireArray bitWires = new WireArray(ws);
			return bitWires;
		}
		else{
			generator.currentWireId-=bitwidth;
			return new WireArray(cachedOutputs).adjustLength(bitwidth);
		}		


	}

	public void restrictBitLength(int bitWidth, String... desc) {
		WireArray bitWires = getBitWires();
		if (bitWires == null) {
			getBitWires(bitWidth, desc);
		} else {
			if (bitWires.size() > bitWidth) {
				bitWires = forceSplit(bitWidth, desc);
				setBits(bitWires);
			} else {
				// nothing to be done.
			}
		}
	}

	public Wire xorBitwise(Wire w, int numBits, String... desc) {
		WireArray bits1 = getBitWires(numBits, desc);
		WireArray bits2 = w.getBitWires(numBits, desc);
		WireArray result = bits1.xorWireArray(bits2, numBits, desc);
		BigInteger v = result.checkIfConstantBits(desc);
		if (v == null) {
			return new LinearCombinationWire(result);
		} else {
			return generator.createConstantWire(v);
		}
	}

	public Wire xorBitwise(long v, int numBits, String... desc) {
		return xorBitwise(generator.createConstantWire(v, desc), numBits, desc);
	}

	public Wire xorBitwise(BigInteger b, int numBits, String... desc) {
		return xorBitwise(generator.createConstantWire(b, desc), numBits, desc);
	}

	public Wire andBitwise(Wire w, int numBits, String... desc) {
		WireArray bits1 = getBitWires(numBits, desc);
		WireArray bits2 = w.getBitWires(numBits, desc);
		WireArray result = bits1.andWireArray(bits2, numBits, desc);
		BigInteger v = result.checkIfConstantBits(desc);
		if (v == null) {
			return new LinearCombinationWire(result);
		} else {
			return generator.createConstantWire(v);
		}
	}

	public Wire andBitwise(long v, int numBits, String... desc) {
		return andBitwise(generator.createConstantWire(v, desc), numBits, desc);
	}

	public Wire andBitwise(BigInteger b, int numBits, String... desc) {
		return andBitwise(generator.createConstantWire(b, desc), numBits, desc);
	}

	public Wire orBitwise(Wire w, int numBits, String... desc) {
		WireArray bits1 = getBitWires(numBits, desc);
		WireArray bits2 = w.getBitWires(numBits, desc);
		WireArray result = bits1.orWireArray(bits2, numBits, desc);
		BigInteger v = result.checkIfConstantBits(desc);
		if (v == null) {
			return new LinearCombinationWire(result);
		} else {
			return generator.createConstantWire(v);
		}
	}

	public Wire orBitwise(long v, int numBits, String... desc) {
		return orBitwise(generator.createConstantWire(v, desc), numBits, desc);
	}

	public Wire orBitwise(BigInteger b, int numBits, String... desc) {
		return orBitwise(generator.createConstantWire(b, desc), numBits, desc);
	}

	public Wire isEqualTo(Wire w, String... desc) {
		packIfNeeded(desc);
		w.packIfNeeded(desc);
		Wire s = sub(w, desc);
		return s.checkNonZero(desc).invAsBit(desc);
	}

	public Wire isEqualTo(BigInteger b, String... desc) {
		return isEqualTo(generator.createConstantWire(b, desc));
	}

	public Wire isEqualTo(long v, String... desc) {
		return isEqualTo(generator.createConstantWire(v, desc));
	}

	/**
	 * Note: This method performs unsigned comparison and assumes that the values on this wire and w can fit into bitwidth bits. 
	 * It's the responsibility of the programmer to ensure that this is the case. (The enforcement could be added within the method, but this is not needed for many applications.)
	 * For example, if any of the wires is an unconstrained witness wire by the prover, restrict bitlength should be called first.
	 * Existing gadgets that show examples of using comparisons in the case of witnesses are the Mod gadgets in the examples.gadgets.math package.
	 */
	public Wire isLessThanOrEqual(Wire w, int bitwidth, String... desc) {
		packIfNeeded(desc);
		w.packIfNeeded(desc);
		BigInteger p = new BigInteger("2").pow(bitwidth);
		Wire pWire = generator.createConstantWire(p, desc);
		Wire sum = pWire.add(w, desc).sub(this, desc);
		WireArray bitWires = sum.getBitWires(bitwidth + 1, desc);
		return bitWires.get(bitwidth);
	}

	
	/**
	 * Note: This method performs unsigned comparison and assumes that the values on this wire and v can fit into bitwidth bits. 
	 * It's the responsibility of the programmer to ensure that this is the case. (The enforcement could be added within the method, but this is not needed for many applications.)
	 * For example, if any of the wires is an unconstrained witness wire by the prover, restrict bitlength should be called first.
	 * Existing gadgets that show examples of using comparisons in the case of witnesses are the Mod gadgets in the examples.gadgets.math package.
	 */
	public Wire isLessThanOrEqual(long v, int bitwidth, String... desc) {
		if(v < 0) {
			throw new IllegalArgumentException("This method performs unsigned comparisons only.");
		} 
		if(BigInteger.valueOf(v).bitLength() > bitwidth) {
			throw new IllegalArgumentException("The constant argument must fit within the given bitwidth. Also, see other comments in the code.");
		}
		// Note: the above checks are not sufficient for the correct usage of the method. See the above comments.
		return isLessThanOrEqual(generator.createConstantWire(v, desc), bitwidth, desc);
	}

	
	/**
	 * Note: This method performs unsigned comparison and assumes that the values on this wire and b can fit into bitwidth bits. 
	 * It's the responsibility of the programmer to ensure that this is the case. (The enforcement could be added within the method, but this is not needed for many applications.)
	 * For example, if any of the wires is an unconstrained witness wire by the prover, restrict bitlength should be called first.
	 * Existing gadgets that show examples of using comparisons in the case of witnesses are the Mod gadgets in the examples.gadgets.math package.
	 */
	public Wire isLessThanOrEqual(BigInteger b, int bitwidth, String... desc) {
		if(b.signum() < 0) {
			throw new IllegalArgumentException("This method performs unsigned comparisons only.");
		} 
		if(b.bitLength() > bitwidth) {
			throw new IllegalArgumentException("The constant argument must fit within the given bitwidth. Also, see other comments in the code.");
		}
		// Note: the above checks are not sufficient for the correct usage of the method. See the above comments.
		return isLessThanOrEqual(generator.createConstantWire(b, desc), bitwidth, desc);
	}

	/**
	 * Note: This method performs unsigned comparison and assumes that the values on this wire and w can fit into bitwidth bits. 
	 * It's the responsibility of the programmer to ensure that this is the case. (The enforcement could be added within the method, but this is not needed for many applications.)
	 * For example, if any of the wires is an unconstrained witness wire by the prover, restrict bitlength should be called first.
	 * Existing gadgets that show examples of using comparisons in the case of witnesses are the Mod gadgets in the examples.gadgets.math package.
	 */
	public Wire isLessThan(Wire w, int bitwidth, String... desc) {
		packIfNeeded(desc);
		w.packIfNeeded(desc);
		BigInteger p = new BigInteger("2").pow(bitwidth);
		Wire pWire = generator.createConstantWire(p, desc);
		Wire sum = pWire.add(this, desc).sub(w, desc);
		WireArray bitWires = sum.getBitWires(bitwidth + 1, desc);
		return bitWires.get(bitwidth).invAsBit(desc);
	}

	/**
	 * Note: This method performs unsigned comparison and assumes that the values on this wire and v can fit into bitwidth bits. 
	 * It's the responsibility of the programmer to ensure that this is the case. (The enforcement could be added within the method, but this is not needed for many applications.)
	 * For example, if any of the wires is an unconstrained witness wire by the prover, restrict bitlength should be called first.
	 * Existing gadgets that show examples of using comparisons in the case of witnesses are the Mod gadgets in the examples.gadgets.math package.
	 */
	public Wire isLessThan(long v, int bitwidth, String... desc) {
		if(v < 0) {
			throw new IllegalArgumentException("This method performs unsigned comparisons only.");
		} 
		if(BigInteger.valueOf(v).bitLength() > bitwidth) {
			throw new IllegalArgumentException("The constant argument must fit within the given bitwidth. Also, see other comments in the code.");
		}
		// Note: the above checks are not sufficient for the correct usage of the method. See the above comments.
		return isLessThan(generator.createConstantWire(v, desc), bitwidth, desc);

	}

	/**
	 * Note: This method performs unsigned comparison and assumes that the values on this wire and b can fit into bitwidth bits. 
	 * It's the responsibility of the programmer to ensure that this is the case. (The enforcement could be added within the method, but this is not needed for many applications.)
	 * For example, if any of the wires is an unconstrained witness wire by the prover, restrict bitlength should be called first.
	 * Existing gadgets that show examples of using comparisons in the case of witnesses are the Mod gadgets in the examples.gadgets.math package.
	 */
	public Wire isLessThan(BigInteger b, int bitwidth, String... desc) {
		if(b.signum() < 0) {
			throw new IllegalArgumentException("This method performs unsigned comparisons only.");
		} 
		if(b.bitLength() > bitwidth) {
			throw new IllegalArgumentException("The constant argument must fit within the given bitwidth. Also, see other comments in the code.");
		}
		// Note: the above checks are not sufficient for the correct usage of the method. See the above comments.
		return isLessThan(generator.createConstantWire(b, desc), bitwidth, desc);
	}

	/**
	 * Note: This method performs unsigned comparison and assumes that the values on this wire and w can fit into bitwidth bits. 
	 * It's the responsibility of the programmer to ensure that this is the case. (The enforcement could be added within the method, but this is not needed for many applications.)
	 * For example, if any of the wires is an unconstrained witness wire by the prover, restrict bitlength should be called first.
	 * Existing gadgets that show examples of using comparisons in the case of witnesses are the Mod gadgets in the examples.gadgets.math package.
	 */
	public Wire isGreaterThanOrEqual(Wire w, int bitwidth, String... desc) {
		packIfNeeded(desc);
		w.packIfNeeded(desc);
		BigInteger p = new BigInteger("2").pow(bitwidth);
		Wire pWire = generator.createConstantWire(p, desc);
		Wire sum = pWire.add(this, desc).sub(w, desc);
		WireArray bitWires = sum.getBitWires(bitwidth + 1, desc);
		return bitWires.get(bitwidth);
	}

	/**
	 * Note: This method performs unsigned comparison and assumes that the values on this wire and w can fit into bitwidth bits. 
	 * It's the responsibility of the programmer to ensure that this is the case. (The enforcement could be added within the method, but this is not needed for many applications.)
	 * For example, if any of the wires is an unconstrained witness wire by the prover, restrict bitlength should be called first.
	 * Existing gadgets that show examples of using comparisons in the case of witnesses are the Mod gadgets in the examples.gadgets.math package.
	 */
	public Wire isGreaterThanOrEqual(long v, int bitwidth, String... desc) {
		if(v < 0) {
			throw new IllegalArgumentException("This method performs unsigned comparisons only.");
		} 
		if(BigInteger.valueOf(v).bitLength() > bitwidth) {
			throw new IllegalArgumentException("The constant argument must fit within the given bitwidth. Also, see other comments in the code.");
		}
		// Note: the above checks are not sufficient for the correct usage of the method. See the above comments.
		return isGreaterThanOrEqual(generator.createConstantWire(v, desc), bitwidth, desc);
	}

	/**
	 * Note: This method performs unsigned comparison and assumes that the values on this wire and b can fit into bitwidth bits. 
	 * It's the responsibility of the programmer to ensure that this is the case. (The enforcement could be added within the method, but this is not needed for many applications.)
	 * For example, if any of the wires is an unconstrained witness wire by the prover, restrict bitlength should be called first.
	 * Existing gadgets that show examples of using comparisons in the case of witnesses are the Mod gadgets in the examples.gadgets.math package.
	 */
	public Wire isGreaterThanOrEqual(BigInteger b, int bitwidth, String... desc) {
		if(b.signum() < 0) {
			throw new IllegalArgumentException("This method performs unsigned comparisons only.");
		} 
		if(b.bitLength() > bitwidth) {
			throw new IllegalArgumentException("The constant argument must fit within the given bitwidth. Also, see other comments in the code.");
		}
		// Note: the above checks are not sufficient for the correct usage of the method. See the above comments.
		return isGreaterThanOrEqual(generator.createConstantWire(b, desc), bitwidth, desc);
	}

	/**
	 * Note: This method performs unsigned comparison and assumes that the values on this wire and w can fit into bitwidth bits. 
	 * It's the responsibility of the programmer to ensure that this is the case. (The enforcement could be added within the method, but this is not needed for many applications.)
	 * For example, if any of the wires is an unconstrained witness wire by the prover, restrict bitlength should be called first.
	 * Existing gadgets that show examples of using comparisons in the case of witnesses are the Mod gadgets in the examples.gadgets.math package.
	 */
	public Wire isGreaterThan(Wire w, int bitwidth, String... desc) {
		packIfNeeded(desc);
		w.packIfNeeded(desc);
		BigInteger p = new BigInteger("2").pow(bitwidth);
		Wire pWire = generator.createConstantWire(p, desc);
		Wire sum = pWire.add(w, desc).sub(this, desc);
		WireArray bitWires = sum.getBitWires(bitwidth + 1, desc);
		return bitWires.get(bitwidth).invAsBit(desc);
	}

	/**
	 * Note: This method performs unsigned comparison and assumes that the values on this wire and v can fit into bitwidth bits. 
	 * It's the responsibility of the programmer to ensure that this is the case. (The enforcement could be added within the method, but this is not needed for many applications.)
	 * For example, if any of the wires is an unconstrained witness wire by the prover, restrict bitlength should be called first.
	 * Existing gadgets that show examples of using comparisons in the case of witnesses are the Mod gadgets in the examples.gadgets.math package.
	 */
	public Wire isGreaterThan(long v, int bitwidth, String... desc) {
		if(v < 0) {
			throw new IllegalArgumentException("This method performs unsigned comparisons only.");
		} 
		if(BigInteger.valueOf(v).bitLength() > bitwidth) {
			throw new IllegalArgumentException("The constant argument must fit within the given bitwidth. Also, see other comments in the code.");
		}
		// Note: the above checks are not sufficient for the correct usage of the method. See the above comments.
		return isGreaterThan(generator.createConstantWire(v, desc), bitwidth, desc);
	}

	/**
	 * Note: This method performs unsigned comparison and assumes that the values on this wire and b can fit into bitwidth bits.
	 * It's the responsibility of the programmer to ensure that this is the case. (The enforcement could be added within the method, but this is not needed for many applications.)
	 * For example, if any of the wires is an unconstrained witness wire by the prover, restrict bitlength should be called first.
	 * Existing gadgets that show examples of using comparisons in the case of witnesses are the Mod gadgets in the examples.gadgets.math package.
	 */
	public Wire isGreaterThan(BigInteger b, int bitwidth, String... desc) {
		if(b.signum() < 0) {
			throw new IllegalArgumentException("This method performs unsigned comparisons only.");
		} 
		if(b.bitLength() > bitwidth) {
			throw new IllegalArgumentException("The constant argument must fit within the given bitwidth. Also, see other comments in the code.");
		}
		// Note: the above checks are not sufficient for the correct usage of the method. See the above comments.
		return isGreaterThan(generator.createConstantWire(b, desc), bitwidth, desc);
	}

	public Wire rotateLeft(int numBits, int s, String... desc) {
		WireArray bits = getBitWires(numBits, desc);
		Wire[] rotatedBits = new Wire[numBits];
		for (int i = 0; i < numBits; i++) {
			if (i < s)
				rotatedBits[i] = bits.get(i + (numBits - s));
			else
				rotatedBits[i] = bits.get(i - s);
		}
		WireArray result = new WireArray(rotatedBits);
		BigInteger v = result.checkIfConstantBits(desc);
		if (v == null) {
			return new LinearCombinationWire(result);
		} else {
			return generator.createConstantWire(v);
		}
	}

	public Wire rotateRight(int numBits, int s, String... desc) {
		WireArray bits = getBitWires(numBits, desc);
		Wire[] rotatedBits = new Wire[numBits];
		for (int i = 0; i < numBits; i++) {
			if (i >= numBits - s)
				rotatedBits[i] = bits.get(i - (numBits - s));
			else
				rotatedBits[i] = bits.get(i + s);
		}
		WireArray result = new WireArray(rotatedBits);
		BigInteger v = result.checkIfConstantBits(desc);
		if (v == null) {
			return new LinearCombinationWire(result);
		} else {
			return generator.createConstantWire(v);
		}
	}

	public Wire shiftLeft(int numBits, int s, String... desc) {
		WireArray bits = getBitWires(numBits, desc);
		Wire[] shiftedBits = new Wire[numBits];
		for (int i = 0; i < numBits; i++) {
			if (i < s)
				shiftedBits[i] = generator.zeroWire;
			else
				shiftedBits[i] = bits.get(i - s);
		}
		WireArray result = new WireArray(shiftedBits);
		BigInteger v = result.checkIfConstantBits(desc);
		if (v == null) {
			return new LinearCombinationWire(result);
		} else {
			return generator.createConstantWire(v);
		}
	}

	public Wire shiftRight(int numBits, int s, String... desc) {
		WireArray bits = getBitWires(numBits, desc);
		Wire[] shiftedBits = new Wire[numBits];
		for (int i = 0; i < numBits; i++) {
			if (i >= numBits - s)
				shiftedBits[i] = generator.zeroWire;
			else
				shiftedBits[i] = bits.get(i + s);
		}
		WireArray result = new WireArray(shiftedBits);
		BigInteger v = result.checkIfConstantBits(desc);
		if (v == null) {
			return new LinearCombinationWire(result);
		} else {
			return generator.createConstantWire(v);
		}
	}

	public Wire invBits(int bitwidth, String... desc) {
		Wire[] bits = getBitWires(bitwidth, desc).asArray();
		Wire[] resultBits = new Wire[bits.length];
		for (int i = 0; i < resultBits.length; i++) {
			resultBits[i] = bits[i].invAsBit(desc);
		}
		return new LinearCombinationWire(new WireArray(resultBits));
	}

	public Wire trimBits(int currentNumOfBits, int desiredNumofBits, String... desc) {
		WireArray bitWires = getBitWires(currentNumOfBits, desc);
		WireArray result = bitWires.adjustLength(desiredNumofBits);
		BigInteger v = result.checkIfConstantBits(desc);
		if (v == null) {
			return new LinearCombinationWire(result);
		} else {
			return generator.createConstantWire(v);
		}
	}

	protected void packIfNeeded(String... desc) {
		if (wireId == -1) {
			pack();
		}
	}

	protected void pack(String... desc) {
		if (wireId == -1) {
			WireArray bits = getBitWires();
			if (bits == null) {
				throw new RuntimeException("A Pack operation is tried on a wire that has no bits.");
			}
			wireId = generator.currentWireId++;
//			Instruction op = new PackBasicOp(bits.array, this, desc);
//			generator.addToEvaluationQueue(op);
			
			Instruction op = new PackBasicOp(bits.array, this,  desc);
			Wire[] cachedOutputs = generator.addToEvaluationQueue(op);
			
			if(cachedOutputs != null){
				generator.currentWireId--;
				wireId = cachedOutputs[0].getWireId();
			}		

		}
	}
	
	@Override
	public int hashCode() {
		return wireId;
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj){
			return true;
		}
		else if(!(obj instanceof Wire)){
			return false;
		}
		else{
			Wire w = (Wire)obj;
			return w.wireId == wireId && w.generator==generator;
		}
	}

}
