/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.auxiliary;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

import util.Util;
import circuit.config.Config;
import circuit.eval.CircuitEvaluator;
import circuit.eval.Instruction;
import circuit.structure.CircuitGenerator;
import circuit.structure.ConstantWire;
import circuit.structure.Wire;
import circuit.structure.WireArray;

/**
 * An auxiliary class that handles the operations of long integers, such as the
 * ones used in RSA operations. It applies some of the long integer
 * optimizations from xjsnark (to appear). This is a preliminary version. More
 * Other features and detailed tests will be added in the future.
 * 
 * Usage examples exist in the RSA examples gadgets.
 *
 */

public class LongElement {

	private Wire[] array;
	private int[] currentBitwidth;
	private BigInteger[] currentMaxValues;
	private WireArray bits;
	private CircuitGenerator generator;

	// Should be declared as final, but left non-final for testing purposes.
	// Don't change in the middle of circuit generation.
	public static int BITWIDTH_PER_CHUNK = 32;

	public LongElement(Wire w, int currentBitwidth) {
		array = new Wire[] { w };
		this.currentBitwidth = new int[] { currentBitwidth };
		this.currentMaxValues = new BigInteger[] { Util
				.computeMaxValue(currentBitwidth) };
		generator = CircuitGenerator.getActiveCircuitGenerator();
	}

	public LongElement(WireArray bits) {
		if (BITWIDTH_PER_CHUNK >= bits.size()) {
			array = new Wire[] { bits.packAsBits(BITWIDTH_PER_CHUNK) };
			this.currentMaxValues = new BigInteger[] { Util
					.computeMaxValue(bits.size()) };
			this.currentBitwidth = new int[] { bits.size() };

		} else {
			BigInteger maxChunkVal = Util.computeMaxValue(BITWIDTH_PER_CHUNK);
			BigInteger maxLastChunkVal = maxChunkVal;
			int size= bits.size();
			if (size % BITWIDTH_PER_CHUNK != 0) {
				bits = bits.adjustLength(size
						+ (BITWIDTH_PER_CHUNK - size
								% BITWIDTH_PER_CHUNK));
				maxLastChunkVal = Util.computeMaxValue(size
						% BITWIDTH_PER_CHUNK);
			}
			this.array = new Wire[bits.size() / BITWIDTH_PER_CHUNK];
			this.currentMaxValues = new BigInteger[array.length];
			this.currentBitwidth = new int[array.length];

			for (int i = 0; i < this.array.length; i++) {
				this.array[i] = new WireArray(Arrays.copyOfRange(
						bits.asArray(), i * BITWIDTH_PER_CHUNK, (i + 1)
								* BITWIDTH_PER_CHUNK)).packAsBits();
				if (i == array.length - 1) {
					this.currentMaxValues[i] = maxLastChunkVal;
					this.currentBitwidth[i] = maxLastChunkVal.bitLength();
				} else {
					this.currentMaxValues[i] = maxChunkVal;
					this.currentBitwidth[i] = maxChunkVal.bitLength();
				}
			}
		}
		this.bits = bits;
		generator = CircuitGenerator.getActiveCircuitGenerator();
	}

	public LongElement(Wire w, BigInteger currentMaxValue) {
		array = new Wire[] { w };
		this.currentMaxValues = new BigInteger[] { currentMaxValue };
		this.currentBitwidth = new int[] { currentMaxValue.bitLength() };
		generator = CircuitGenerator.getActiveCircuitGenerator();
	}

	public LongElement(Wire[] w, int[] currentBitwidth) {
		array = w;
		this.currentBitwidth = currentBitwidth;
		this.currentMaxValues = new BigInteger[w.length];
		for (int i = 0; i < w.length; i++) {
			currentMaxValues[i] = Util.computeMaxValue(currentBitwidth[i]);
		}
		generator = CircuitGenerator.getActiveCircuitGenerator();
	}

	public void makeOutput(String... desc) {
		for (Wire w : getArray()) {
			generator.makeOutput(w, desc);
		}
	}

	// public LongElement(Wire[] w, int currentBitwidth) {
	// array = w;
	// this.currentBitwidth = new int[w.length];
	// Arrays.fill(this.currentBitwidth, currentBitwidth);
	// ;
	// this.currentMaxValues = new BigInteger[w.length];
	// for (int i = 0; i < w.length; i++) {
	// currentMaxValues[i] = Util.computeMaxValue(this.currentBitwidth[i]);
	// }
	// generator = CircuitGenerator.getActiveCircuitGenerator();
	// }

	/**
	 * A long element representing a constant.
	 */
	public LongElement(BigInteger[] chunks) {
		this.currentMaxValues = chunks;
		this.currentBitwidth = new int[chunks.length];
		for (int i = 0; i < chunks.length; i++) {
			currentBitwidth[i] = currentMaxValues[i].bitLength();
		}
		generator = CircuitGenerator.getActiveCircuitGenerator();
		array = generator.createConstantWireArray(chunks);
	}

	public LongElement(Wire[] w, BigInteger[] currentMaxValues) {
		array = w;
		this.currentMaxValues = currentMaxValues;
		this.currentBitwidth = new int[w.length];
		for (int i = 0; i < w.length; i++) {
			currentBitwidth[i] = currentMaxValues[i].bitLength();
		}
		generator = CircuitGenerator.getActiveCircuitGenerator();
	}

	public boolean addOverflowCheck(LongElement o) {
		int length = Math.min(array.length, o.array.length);
		boolean overflow = false;
		for (int i = 0; i < length; i++) {
			BigInteger max1 = i < array.length ? currentMaxValues[i]
					: BigInteger.ZERO;
			BigInteger max2 = i < o.array.length ? o.currentMaxValues[i]
					: BigInteger.ZERO;
			if (max1.add(max2).compareTo(Config.FIELD_PRIME) >= 0) {
				overflow = true;
				break;
			}
		}
		return overflow;
	}

	public boolean mulOverflowCheck(LongElement o) {
		int length = array.length + o.array.length - 1;
		boolean overflow = false;
		BigInteger[] newMaxValues = new BigInteger[length];
		Arrays.fill(newMaxValues, BigInteger.ZERO);
		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < o.array.length; j++) {
				newMaxValues[i + j] = newMaxValues[i + j]
						.add(currentMaxValues[i]
								.multiply(o.currentMaxValues[j]));
			}
		}
		for (int i = 0; i < length; i++) {
			if (newMaxValues[i].compareTo(Config.FIELD_PRIME) >= 0) {
				overflow = true;
				break;
			}
		}
		return overflow;
	}

	public LongElement add(LongElement o) {

		if (addOverflowCheck(o)) {
			System.err.println("Warning: Addition overflow could happen");
		}

		int length = Math.max(array.length, o.array.length);
		Wire[] w1 = new WireArray(array).adjustLength(length).asArray();
		Wire[] w2 = new WireArray(o.array).adjustLength(length).asArray();
		Wire[] result = new Wire[length];
		BigInteger[] newMaxValues = new BigInteger[length];
		for (int i = 0; i < length; i++) {
			result[i] = w1[i].add(w2[i]);
			BigInteger max1 = i < array.length ? currentMaxValues[i]
					: BigInteger.ZERO;
			BigInteger max2 = i < o.array.length ? o.currentMaxValues[i]
					: BigInteger.ZERO;

			newMaxValues[i] = max1.add(max2);
		}
		return new LongElement(result, newMaxValues);
	}

	/**
	 * Implements the improved long integer multiplication from xjsnark
	 * 
	 */

	public LongElement mul(LongElement o) {

		if (mulOverflowCheck(o)) {
			System.err.println("Warning: Mul overflow could happen");

		}
		int length = array.length + o.array.length - 1;
		Wire[] result;

		// check if we can just apply the simple non-costly multiplication
		if (o.array.length == 1 || array.length == 1 || isConstant()
				|| o.isConstant()) {
			result = new Wire[length];
			Arrays.fill(result, generator.getZeroWire());

			// O(n*m) multiplication. Fine to apply if any of the operands has
			// dim 1
			// or any of them is constant
			for (int i = 0; i < array.length; i++) {
				for (int j = 0; j < o.array.length; j++) {
					result[i + j] = result[i + j].add(array[i].mul(o.array[j]));
				}
			}
		} else {

			// implement the optimization

			result = generator.createProverWitnessWireArray(length);
			
			// for safety
			final Wire[] array1 = this.array;
			final Wire[] array2 = o.array;
			generator.specifyProverWitnessComputation(new Instruction() {
				@Override
				public void evaluate(CircuitEvaluator evaluator) {
					BigInteger[] a = evaluator
							.getWiresValues(array1);
					BigInteger[] b = evaluator.getWiresValues(array2);
					BigInteger[] resultVals = multiplyPolys(a, b);
					evaluator.setWireValue(result, resultVals);
				}
			});

			Wire zeroWire = generator.getZeroWire();
			for (int k = 0; k < length; k++) {
				BigInteger constant = new BigInteger((k + 1) + "");
				Wire v1 = zeroWire;
				Wire v2 = zeroWire;
				Wire v3 = zeroWire;
				BigInteger coeff = BigInteger.ONE;

				Wire[] vector1 = new Wire[array.length];
				Wire[] vector2 = new Wire[o.array.length];
				Wire[] vector3 = new Wire[length];
				for (int i = 0; i < length; i++) {
					if (i < array.length) {
						vector1[i] = array[i].mul(coeff);
					}
					if (i < o.array.length) {
						vector2[i] = o.array[i].mul(coeff);
					}
					vector3[i] = result[i].mul(coeff);
					coeff = coeff.multiply(constant).mod(Config.FIELD_PRIME);
				}

				// for(int i = array.length-1; i>=0; i--){
				// v1 = v1.mul(constant).add(array[i]);
				// }
				// for(int i = o.array.length-1; i>=0; i--){
				// v2 = v2.mul(constant).add(o.array[i]);
				// }
				// for(int i = length-1; i>=0; i--){
				// v3 = v3.mul(constant).add(result[i]);
				// }

				v1 = new WireArray(vector1).sumAllElements();
				v2 = new WireArray(vector2).sumAllElements();
				v3 = new WireArray(vector3).sumAllElements();
				generator.addAssertion(v1, v2, v3);
			}
		}

		BigInteger[] newMaxValues = new BigInteger[length];
		Arrays.fill(newMaxValues, BigInteger.ZERO);
		for (int i = 0; i < array.length; i++) {
			for (int j = 0; j < o.array.length; j++) {
				newMaxValues[i + j] = newMaxValues[i + j]
						.add(currentMaxValues[i]
								.multiply(o.currentMaxValues[j]));
			}
		}
		return new LongElement(result, newMaxValues);
	}

	private boolean isConstant() {
		boolean isConstant = true;
		if (array != null) {
			for (int i = 0; i < array.length; i++) {
				isConstant &= array[i] instanceof ConstantWire;
			}
		}
		return isConstant;
	}

	public int getSize() {
		return array.length;
	}

	public void align(int totalNumChunks) {

		array = Arrays.copyOfRange(array, 0, totalNumChunks);
		for (int i = 0; i < array.length; i++) {
			if (array[i] == null) {
				array[i] = generator.getZeroWire();
			}
		}

		BigInteger[] newMaxValues = new BigInteger[totalNumChunks];
		System.arraycopy(currentMaxValues, 0, newMaxValues, 0, totalNumChunks);

		for (int i = 0; i < totalNumChunks; i++) {
			if (newMaxValues[i].bitLength() > BITWIDTH_PER_CHUNK) {
				Wire[] chunkBits = array[i].getBitWires(currentBitwidth[i])
						.asArray();
				array[i] = new WireArray(Arrays.copyOfRange(chunkBits, 0,
						BITWIDTH_PER_CHUNK)).packAsBits();
				Wire rem = new WireArray(Arrays.copyOfRange(chunkBits,
						BITWIDTH_PER_CHUNK, currentBitwidth[i])).packAsBits();
				if (i != totalNumChunks - 1) {
					newMaxValues[i + 1] = newMaxValues[i].shiftRight(
							BITWIDTH_PER_CHUNK).add(newMaxValues[i + 1]);
					array[i + 1] = rem.add(array[i + 1]);
				}
				newMaxValues[i] = Util.computeMaxValue(BITWIDTH_PER_CHUNK);
				currentBitwidth[i] = BITWIDTH_PER_CHUNK;
			}
		}
		currentMaxValues = newMaxValues;
	}

	public WireArray getBits(int totalBitwidth) {
		if (bits != null) {
			return bits.adjustLength(totalBitwidth);
		} else if (totalBitwidth == BITWIDTH_PER_CHUNK) {
			if (array.length == 1) {
				System.out.println("bit length at split = "
						+ currentMaxValues[0].bitLength());
				bits = array[0].getBitWires(currentMaxValues[0].bitLength());
				bits = bits.adjustLength(totalBitwidth);
			} else {
				// TODO : pack in this special case
				// System.exit(0);
				// System.out.println(array.length);
				// System.exit(-1);
				return null;
			}
		} else {
			Wire[] bitWires;
			int limit = totalBitwidth;
			if (totalBitwidth != -1) {
				bitWires = new Wire[totalBitwidth];

			} else {
				BigInteger maxVal = getMaxVal(BITWIDTH_PER_CHUNK);
				bitWires = new Wire[maxVal.bitLength()];
				limit = maxVal.bitLength();
			}
			Arrays.fill(bitWires, generator.getZeroWire());

			int newLength = (int) Math.ceil(getMaxVal(BITWIDTH_PER_CHUNK)
					.bitLength() * 1.0 / BITWIDTH_PER_CHUNK);
			// Wire[] newArray = new Wire[array.length];
			// BigInteger[] newMaxValues = new
			// BigInteger[currentMaxValues.length];
			// System.out.println("new Length = " + newLength);
			Wire[] newArray = new Wire[newLength];
			BigInteger[] newMaxValues = new BigInteger[newLength];
			Arrays.fill(newMaxValues, BigInteger.ZERO);
			Arrays.fill(newArray, generator.getZeroWire());

			System.arraycopy(currentMaxValues, 0, newMaxValues, 0,
					currentMaxValues.length);
			System.arraycopy(array, 0, newArray, 0, array.length);

			int idx = 0;
			int chunkIndex = 0;
			while (idx < limit && chunkIndex < newLength) {
				Wire[] alignedChunkBits;
				if (newMaxValues[chunkIndex].bitLength() > BITWIDTH_PER_CHUNK) {

					Wire[] chunkBits = newArray[chunkIndex].getBitWires(
							newMaxValues[chunkIndex].bitLength()).asArray();

					alignedChunkBits = Arrays.copyOfRange(chunkBits, 0,
							BITWIDTH_PER_CHUNK);
					Wire rem = new WireArray(Arrays.copyOfRange(chunkBits,
							BITWIDTH_PER_CHUNK,
							newMaxValues[chunkIndex].bitLength())).packAsBits();

					if (chunkIndex != newArray.length - 1) {
						newMaxValues[chunkIndex + 1] = newMaxValues[chunkIndex]
								.shiftRight(BITWIDTH_PER_CHUNK).add(
										newMaxValues[chunkIndex + 1]);

						newArray[chunkIndex + 1] = rem
								.add(newArray[chunkIndex + 1]);

					}
				} else {
					alignedChunkBits = newArray[chunkIndex].getBitWires(
							newMaxValues[chunkIndex].bitLength()).asArray();
				}
				System.arraycopy(alignedChunkBits, 0, bitWires, idx,
						Math.min(alignedChunkBits.length, limit - idx));
				chunkIndex++;
				idx += alignedChunkBits.length;
			}

			bits = new WireArray(bitWires);
			// generator.addDebugInstruction(bits.array, "aligned Bits");
		}
		return bits;
	}

	public BigInteger getMaxVal(int bitwidth) {
		return Util.group(currentMaxValues, bitwidth);
	}

	private BigInteger[] multiplyPolys(BigInteger[] aiVals, BigInteger[] biVals) {

		BigInteger[] solution = new BigInteger[aiVals.length + biVals.length
				- 1];
		Arrays.fill(solution, BigInteger.ZERO);
		for (int i = 0; i < aiVals.length; i++) {
			for (int j = 0; j < biVals.length; j++) {
				solution[i + j] = solution[i + j].add(
						aiVals[i].multiply(biVals[j])).mod(Config.FIELD_PRIME);
			}
		}
		return solution;

	}

	public LongElement muxBit(LongElement other, Wire w) {

		int length = Math.max(array.length, other.array.length);
		Wire[] newArray = new Wire[length];
		BigInteger[] newMaxValues = new BigInteger[length];
		for (int i = 0; i < length; i++) {

			BigInteger b1 = i < array.length ? currentMaxValues[i]
					: BigInteger.ZERO;
			BigInteger b2 = i < other.array.length ? other.currentMaxValues[i]
					: BigInteger.ZERO;
			newMaxValues[i] = b1.compareTo(b2) == 1 ? b1 : b2;

			Wire w1 = i < array.length ? array[i] : generator.getZeroWire();
			Wire w2 = i < other.array.length ? other.array[i] : generator
					.getZeroWire();
			newArray[i] = w1.add(w.mul(w2.sub(w1)));

		}
		return new LongElement(newArray, newMaxValues);
	}

	public Wire[] getArray() {
		return array;
	}

	public int[] getCurrentBitwidth() {
		return currentBitwidth;
	}

	public BigInteger[] getCurrentMaxValues() {
		return currentMaxValues;
	}

	public WireArray getBits() {
		return bits;
	}

	public BigInteger getConstant(int bitwidth_per_chunk) {
		for (Wire w : array) {
			if (!(w instanceof ConstantWire))
				return null;
		}
		return Util.group(currentMaxValues, bitwidth_per_chunk);
	}

	// the equals java method to compare objects (this is NOT for circuit
	// equality check)
	public boolean equals(Object o) {
		if (o == null || !(o instanceof LongElement)) {
			return false;
		}
		LongElement v = (LongElement) o;
		if (v.array.length != array.length) {
			return false;
		}
		boolean check = true;
		for (int i = 0; i < array.length; i++) {
			if (!v.array[i].equals(array[i])) {
				check = false;
				break;
			}
		}
		return check;
	}


//	public LongElement addsub(BigInteger[] a, LongElement o) {
//
//		int length = Math.max(array.length, Math.max(a.length, o.array.length));
//
//		Wire[] w1 = new WireArray(array).adjustLength(length).asArray();
//		Wire[] w2 = new WireArray(o.array).adjustLength(length).asArray();
//
//		Wire[] result = new Wire[length];
//		BigInteger[] newMaxValues = new BigInteger[length];
//		for (int i = 0; i < length; i++) {
//			result[i] = w1[i].sub(w2[i]);
//			if (i < a.length) {
//				result[i] = result[i].add(a[i]);
//			}
//			BigInteger max1 = i < array.length ? currentMaxValues[i]
//					: BigInteger.ZERO;
//			BigInteger max2 = i < a.length ? a[i] : BigInteger.ZERO;
//
//			newMaxValues[i] = max1.add(max2);
//		}
//		return new LongElement(result, newMaxValues);
//	}

	// This asserts that the current bitwidth conditions are satisfied
	public void forceBitwidth() {
		if(!isAligned()){
			System.err.println("Warning [forceBitwidth()]: Might want to align before checking bitwidth constraints");
		}
		for (int i = 0; i < array.length; i++) {
			array[i].restrictBitLength(currentBitwidth[i]);
		}
	}

	public boolean isAligned() {
		boolean check = true;
		for (int i = 0; i < array.length; i++) {
			check &= currentBitwidth[i] <= BITWIDTH_PER_CHUNK;
		}
		return check;
	}

	public void assertEqualityNaive(LongElement a) {

		// generator.addDebugInstruction(a.array, "1 - before packing");
		// generator.addDebugInstruction(array, "2 - before packing");
		WireArray bits1 = a
				.getBits(a.getMaxVal(BITWIDTH_PER_CHUNK).bitLength());
		WireArray bits2 = getBits(getMaxVal(BITWIDTH_PER_CHUNK).bitLength());
		LongElement v1 = new LongElement(bits1);
		LongElement v2 = new LongElement(bits2);
		for (int i = 0; i < v1.array.length; i++) {
			generator.addEqualityAssertion(v1.array[i], v2.array[i]);

		}
	}

	
	// an improved equality checking algorithm from xjsnark
	public void assertEquality(LongElement e) {

		ArrayList<Wire> group1 = new ArrayList<Wire>();
		ArrayList<BigInteger> group1_bound = new ArrayList<BigInteger>();
		ArrayList<Wire> group2 = new ArrayList<Wire>();
		ArrayList<BigInteger> group2_bound = new ArrayList<BigInteger>();
		ArrayList<Integer> steps = new ArrayList<Integer>();
		
		Wire[] a1 = array;
		Wire[] a2 = e.array;

		BigInteger[] bounds1 = currentMaxValues;
		BigInteger[] bounds2 = e.currentMaxValues;

		int limit = Math.max(a1.length, a2.length);
		if (e.array.length != limit) {
			a2 = new WireArray(a2).adjustLength(limit).asArray();
			bounds2 = new BigInteger[limit];
			Arrays.fill(bounds2, BigInteger.ZERO);
			System.arraycopy(e.currentMaxValues, 0, bounds2, 0,
					e.currentMaxValues.length);
		}
		if (array.length != limit) {
			a1 = new WireArray(a1).adjustLength(limit).asArray();
			bounds1 = new BigInteger[limit];
			Arrays.fill(bounds1, BigInteger.ZERO);
			System.arraycopy(currentMaxValues, 0, bounds1, 0,
					currentMaxValues.length);
		}

		// simple equality assertion cases
		if (a1.length == a2.length && a1.length == 1) {
			generator.addEqualityAssertion(a1[0], a2[0],
					"Equality assertion of long elements | case 1");
			return;
		} else if (isAligned() && e.isAligned()) {
			for (int i = 0; i < limit; i++) {
				generator.addEqualityAssertion(a1[i], a2[i],
						"Equality assertion of long elements | case 2 | index "
								+ i);
			}
			return;
		}

		BigInteger shift = new BigInteger("2").pow(BITWIDTH_PER_CHUNK);
		int i = 0;
		while (i < limit) {
			int step = 1;
			Wire w1 = a1[i];
			Wire w2 = a2[i];
			BigInteger b1 = bounds1[i];
			BigInteger b2 = bounds2[i];
			// System.out.println(b1);
			// System.out.println(b2);
			while (i + step <= limit - 1) {
				BigInteger delta = shift.pow(step);
				if (b1.add(bounds1[i + step].multiply(delta)).bitLength() < Config.LOG2_FIELD_PRIME - 2
						&& b2.add(bounds2[i + step].multiply(delta))
								.bitLength() < Config.LOG2_FIELD_PRIME - 2) {
					w1 = w1.add(a1[i + step].mul(delta));
					w2 = w2.add(a2[i + step].mul(delta));
					b1 = b1.add(bounds1[i + step].multiply(delta));
					b2 = b2.add(bounds2[i + step].multiply(delta));
					step++;
				} else {
					break;
				}
			}
			group1.add(w1);
			group1_bound.add(b1);
			group2.add(w2);
			group2_bound.add(b2);
			// System.out.println("Step = " + step);
			steps.add(step);
			i += step;
		}

		int newChunkSize = group1.size();
		Wire[] carries = generator
				.createProverWitnessWireArray(newChunkSize - 1);
		BigInteger[] auxConstantChunks = new BigInteger[newChunkSize];
		BigInteger auxConstant = BigInteger.ZERO;

		int accumStep = 0;
		int[] carriesBitwidthBounds = new int[carries.length];
		for (int j = 0; j < auxConstantChunks.length - 1; j++) {
			auxConstantChunks[j] = new BigInteger("2").pow(group2_bound.get(j)
					.bitLength());
			auxConstant = auxConstant.add(auxConstantChunks[j].multiply(shift
					.pow(accumStep)));
			accumStep += steps.get(j);
			// System.out.println(steps.get(j) + "," +
			// auxConstantChunks[j].bitLength());
			// carriesBitBounds[j] = auxConstantChunks[j].bitLength() -
			// steps.get(j)*BITWIDTH_PER_CHUNK + 1;
			carriesBitwidthBounds[j] = Math.max(auxConstantChunks[j]
					.bitLength(), group1_bound.get(j).bitLength())
					- steps.get(j) * BITWIDTH_PER_CHUNK + 1;
		}
		auxConstantChunks[auxConstantChunks.length - 1] = BigInteger.ZERO;

		BigInteger[] alignedCoeffs = new BigInteger[newChunkSize];
		Arrays.fill(alignedCoeffs, BigInteger.ZERO);
		BigInteger[] smallerAlignedCoeffs = Util.split(auxConstant,
				BITWIDTH_PER_CHUNK);
		int idx = 0;

		loop1: for (int j = 0; j < newChunkSize; j++) {
			for (int k = 0; k < steps.get(j); k++) {
				alignedCoeffs[j] = alignedCoeffs[j]
						.add(smallerAlignedCoeffs[idx].multiply(shift.pow(k)));
				idx++;
				if (idx == smallerAlignedCoeffs.length) {
					break loop1;
				}
			}
		}
		if (idx != smallerAlignedCoeffs.length) {
			if (idx == smallerAlignedCoeffs.length - 1) {
				alignedCoeffs[newChunkSize - 1] = alignedCoeffs[newChunkSize - 1]
						.add(smallerAlignedCoeffs[idx].multiply(shift.pow(steps
								.get(newChunkSize - 1) + 0)));
			} else {
				throw new RuntimeException(
						"Case not expected. Investigate why!");
			}

		}

		generator.specifyProverWitnessComputation(new Instruction() {

			@Override
			public void evaluate(CircuitEvaluator evaluator) {

				BigInteger prevCarry = BigInteger.ZERO;
				for (int i = 0; i < carries.length; i++) {
					BigInteger a = evaluator.getWireValue(group1.get(i));
					BigInteger b = evaluator.getWireValue(group2.get(i));
					BigInteger carryValue = auxConstantChunks[i].add(a)
							.subtract(b).subtract(alignedCoeffs[i])
							.add(prevCarry);
					carryValue = carryValue.shiftRight(steps.get(i)
							* BITWIDTH_PER_CHUNK);
					evaluator.setWireValue(carries[i], carryValue);
					prevCarry = carryValue;
				}
			}
		});

		for (int j = 0; j < carries.length; j++) {
			carries[j].getBitWires(carriesBitwidthBounds[j]);
		}
		Wire prevCarry = generator.getZeroWire();
		BigInteger prevBound = BigInteger.ZERO;
		for (int j = 0; j < carries.length + 1; j++) {
			Wire auxConstantChunkWire = generator
					.createConstantWire(auxConstantChunks[j]);
			Wire alignedCoeffWire = generator
					.createConstantWire(alignedCoeffs[j]);
			Wire currentCarry = j == carries.length ? generator.getZeroWire()
					: carries[j];

			// overflow check
			if (auxConstantChunks[j].add(group1_bound.get(j)).add(prevBound)
					.compareTo(Config.FIELD_PRIME) >= 0) {
				System.err.println("Overflow possibility @ ForceEqual()");
			}

			Wire w1 = auxConstantChunkWire
					.add(group1.get(j).sub(group2.get(j))).add(prevCarry);
			Wire w2 = alignedCoeffWire.add(currentCarry.mul(shift.pow(steps
					.get(j))));

			generator
					.addEqualityAssertion(w1, w2,
							"Equality assertion of long elements | case 3 | index "
									+ j);

			prevCarry = currentCarry;
			if (j != carries.length)
				prevBound = Util.computeMaxValue(carriesBitwidthBounds[j]);
		}
	}

	// applies an improved technique to assert comparison
	public void assertLessThan(LongElement other) {

		// first verify that both elements are aligned
		if (!isAligned() || !other.isAligned()) {
			throw new IllegalArgumentException("input chunks are not aligned");
		}

		
		Wire[] a1 = this.getArray();
		Wire[] a2 = other.getArray();
		final int length = Math.max(a1.length, a2.length);
		final Wire[] paddedA1 = Util.padWireArray(a1, length, generator.getZeroWire());
		final Wire[] paddedA2 = Util.padWireArray(a2, length, generator.getZeroWire());
		
		/*
		 * Instead of doing the comparison naively (which will involve all the
		 * bits) let the prover help us by pointing to the first chunk in the
		 * other element that is more than the corresponding chunk in this
		 * element.
		 */
		Wire[] helperBits = generator.createProverWitnessWireArray(length);
		// set the value of the helperBits outside the circuits

		generator.specifyProverWitnessComputation(new Instruction() {
			@Override
			public void evaluate(CircuitEvaluator evaluator) {

				boolean found = false;
				for (int i = length - 1; i >= 0; i--) {
					BigInteger v1 = evaluator.getWireValue(paddedA1[i]);
					BigInteger v2 = evaluator.getWireValue(paddedA2[i]);

					boolean check = v2.compareTo(v1) > 0 && !found;
					evaluator.setWireValue(helperBits[i],
							check ? BigInteger.ONE : BigInteger.ZERO);
					if (check)
						found = true;
				}
			}
		});

		// verify constraints about helper bits. 
		for (Wire w : helperBits) {
			generator.addBinaryAssertion(w);
		}
//		Only one bit should be set.
		generator.addOneAssertion(new WireArray(helperBits).sumAllElements());

		// verify "the greater than condition" for the specified chunk
		Wire chunk1 = generator.getZeroWire();
		Wire chunk2 = generator.getZeroWire();


		for (int i = 0; i < helperBits.length; i++) {
			chunk1 = chunk1.add(paddedA1[i].mul(helperBits[i]));
			chunk2 = chunk2.add(paddedA2[i].mul(helperBits[i]));

		}
		generator.addOneAssertion(chunk1.isLessThan(chunk2,
				LongElement.BITWIDTH_PER_CHUNK));

		// check that the other more significant chunks are equal
		Wire[] helperBits2 = new Wire[helperBits.length];
		helperBits2[0] = generator.getZeroWire();
		for (int i = 1; i < helperBits.length; i++) {
			helperBits2[i] = helperBits2[i - 1].add(helperBits[i - 1]);
			generator.addZeroAssertion(helperBits2[i].mul(paddedA1[i]
					.sub(paddedA2[i])));
		}

		// no checks needed for the less significant chunks
	}

}
