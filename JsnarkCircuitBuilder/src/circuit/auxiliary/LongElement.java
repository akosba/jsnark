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
	// This represents the size of smaller chunks used to represent long
	// elements
	public static int CHUNK_BITWIDTH = 32;

	public LongElement(Wire w, int currentBitwidth) {
		array = new Wire[] { w };
		this.currentBitwidth = new int[] { currentBitwidth };
		this.currentMaxValues = new BigInteger[] { Util
				.computeMaxValue(currentBitwidth) };
		generator = CircuitGenerator.getActiveCircuitGenerator();
	}

	public LongElement(WireArray bits) {
		if (CHUNK_BITWIDTH >= bits.size()) {
			array = new Wire[] { bits.packAsBits(bits.size()) };
			this.currentMaxValues = new BigInteger[] { Util
					.computeMaxValue(bits.size()) };
			this.currentBitwidth = new int[] { bits.size() };

		} else {
			BigInteger maxChunkVal = Util.computeMaxValue(CHUNK_BITWIDTH);
			BigInteger maxLastChunkVal = maxChunkVal;
			int size = bits.size();
			if (size % CHUNK_BITWIDTH != 0) {
				bits = bits.adjustLength(size
						+ (CHUNK_BITWIDTH - size % CHUNK_BITWIDTH));
				maxLastChunkVal = Util.computeMaxValue(size % CHUNK_BITWIDTH);
			}
			this.array = new Wire[bits.size() / CHUNK_BITWIDTH];
			this.currentMaxValues = new BigInteger[array.length];
			this.currentBitwidth = new int[array.length];

			for (int i = 0; i < this.array.length; i++) {
				this.array[i] = new WireArray(Arrays.copyOfRange(
						bits.asArray(), i * CHUNK_BITWIDTH, (i + 1)
								* CHUNK_BITWIDTH)).packAsBits();
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
					BigInteger[] a = evaluator.getWiresValues(array1);
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
	
	public LongElement align(int totalNumChunks) {
		
		Wire[] newArray = Arrays.copyOfRange(array, 0, totalNumChunks);
		for(int i = 0; i < newArray.length; i++){
			if(newArray[i] == null){
				newArray[i]= generator.getZeroWire();
			}
		}
		BigInteger[] newMaxValues = new BigInteger[totalNumChunks];
		Arrays.fill(newMaxValues, BigInteger.ZERO);
		System.arraycopy(currentMaxValues, 0, newMaxValues, 0, Math.min(totalNumChunks, currentMaxValues.length));
		BigInteger maxAlignedChunkValue = Util.computeMaxValue(CHUNK_BITWIDTH);
		
		for (int i = 0; i < totalNumChunks; i++) {
			if (newMaxValues[i].bitLength() > CHUNK_BITWIDTH) {
				Wire[] chunkBits = newArray[i].getBitWires(newMaxValues[i].bitLength())
						.asArray();
				newArray[i] = new WireArray(Arrays.copyOfRange(chunkBits, 0,
						CHUNK_BITWIDTH)).packAsBits();
				Wire rem = new WireArray(Arrays.copyOfRange(chunkBits,
						CHUNK_BITWIDTH, newMaxValues[i].bitLength())).packAsBits();
				if (i != totalNumChunks - 1) {
					newMaxValues[i + 1] = newMaxValues[i].shiftRight(
							CHUNK_BITWIDTH).add(newMaxValues[i + 1]);
					newArray[i + 1] = rem.add(newArray[i + 1]);
				}
				newMaxValues[i] = maxAlignedChunkValue;
			}
		}
		return new LongElement(newArray, newMaxValues);
	}


	// This method extracts (some of) the bit wires corresponding to a long
	// element based on the totalBitwidth argument.
	// If totalBitwidth is -1, all bits are returned.
	// See restrictBitwidth for restricting the bitwidth of all the long element
	// chunks

	public WireArray getBits(int totalBitwidth) {

		if (bits != null) {
			return bits.adjustLength(totalBitwidth == -1? bits.size():totalBitwidth);
		}
		if (array.length == 1) {
			bits = array[0].getBitWires(currentMaxValues[0].bitLength());
			return bits.adjustLength(totalBitwidth == -1? bits.size():totalBitwidth);
		} else {
			if (totalBitwidth <= CHUNK_BITWIDTH && totalBitwidth >= 0) {
				WireArray out = array[0].getBitWires(currentMaxValues[0]
						.bitLength());
				return out.adjustLength(totalBitwidth);
			} else {
				Wire[] bitWires;
				int limit = totalBitwidth;
				BigInteger maxVal = getMaxVal(CHUNK_BITWIDTH);

				if (totalBitwidth != -1) {
					bitWires = new Wire[totalBitwidth];
				} else {
					bitWires = new Wire[maxVal.bitLength()];
					limit = maxVal.bitLength();
				}
				Arrays.fill(bitWires, generator.getZeroWire());

				int newLength = (int) Math.ceil(getMaxVal(CHUNK_BITWIDTH)
						.bitLength() * 1.0 / CHUNK_BITWIDTH);
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
					if (newMaxValues[chunkIndex].bitLength() > CHUNK_BITWIDTH) {

						Wire[] chunkBits = newArray[chunkIndex].getBitWires(
								newMaxValues[chunkIndex].bitLength()).asArray();

						alignedChunkBits = Arrays.copyOfRange(chunkBits, 0,
								CHUNK_BITWIDTH);
						Wire rem = new WireArray(Arrays.copyOfRange(chunkBits,
								CHUNK_BITWIDTH,
								newMaxValues[chunkIndex].bitLength()))
								.packAsBits();

						if (chunkIndex != newArray.length - 1) {
							newMaxValues[chunkIndex + 1] = newMaxValues[chunkIndex]
									.shiftRight(CHUNK_BITWIDTH).add(
											newMaxValues[chunkIndex + 1]);
							newArray[chunkIndex + 1] = rem
									.add(newArray[chunkIndex + 1]);
						}
					} else {
						alignedChunkBits = newArray[chunkIndex].getBitWires(
								CHUNK_BITWIDTH).asArray();
					}
					System.arraycopy(alignedChunkBits, 0, bitWires, idx,
							Math.min(alignedChunkBits.length, limit - idx));
					chunkIndex++;
					idx += alignedChunkBits.length;
				}
				WireArray out = new WireArray(bitWires);
				if(limit >= maxVal.bitLength()){
					bits = out.adjustLength(maxVal.bitLength());
				} 
				return out;
			}

		}

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
			if (newArray[i] instanceof ConstantWire) {
				newMaxValues[i] = ((ConstantWire) newArray[i]).getConstant();
			}

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
		BigInteger[] constants = new BigInteger[array.length];
		for (int i = 0; i < array.length; i++) {
			if (!(array[i] instanceof ConstantWire))
				return null;
			else {
				constants[i] = ((ConstantWire) array[i]).getConstant();
			}
		}
		return Util.group(constants, bitwidth_per_chunk);
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

	// This asserts that the current bitwidth conditions are satisfied
	public void restrictBitwidth() {
		if (!isAligned()) {
			System.err
					.println("Warning [restrictBitwidth()]: Might want to align before checking bitwidth constraints");
			if (Config.printStackTraceAtWarnings) {
				Thread.dumpStack();
			}
		}
		for (int i = 0; i < array.length; i++) {
			array[i].restrictBitLength(currentBitwidth[i]);
		}
	}

	public boolean isAligned() {
		boolean check = true;
		for (int i = 0; i < array.length; i++) {
			check &= currentBitwidth[i] <= CHUNK_BITWIDTH;
		}
		return check;
	}

	public void assertEqualityNaive(LongElement a) {

		WireArray bits1 = a.getBits(a.getMaxVal(CHUNK_BITWIDTH).bitLength());
		WireArray bits2 = getBits(getMaxVal(CHUNK_BITWIDTH).bitLength());
		LongElement v1 = new LongElement(bits1);
		LongElement v2 = new LongElement(bits2);
		for (int i = 0; i < v1.array.length; i++) {
			generator.addEqualityAssertion(v1.array[i], v2.array[i]);
		}
	}

	// an improved equality assertion algorithm from xjsnark
	public void assertEquality(LongElement e) {

		Wire[] a1 = array;
		Wire[] a2 = e.array;
		BigInteger[] bounds1 = currentMaxValues;
		BigInteger[] bounds2 = e.currentMaxValues;

		int limit = Math.max(a1.length, a2.length);

		// padding
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

		// To make the equality check more efficient, group the chunks together
		// while ensuring that there are no overflows.

		ArrayList<Wire> group1 = new ArrayList<Wire>();
		ArrayList<BigInteger> group1_bounds = new ArrayList<BigInteger>();
		ArrayList<Wire> group2 = new ArrayList<Wire>();
		ArrayList<BigInteger> group2_bounds = new ArrayList<BigInteger>();

		// This array will store how many chunks were grouped together for every
		// wire in group1 or group2
		// The grouping needs to happen in the same way for the two operands, so
		// it's one steps array
		ArrayList<Integer> steps = new ArrayList<Integer>();

		BigInteger shift = new BigInteger("2").pow(CHUNK_BITWIDTH);
		int i = 0;
		while (i < limit) {
			int step = 1;
			Wire w1 = a1[i];
			Wire w2 = a2[i];
			BigInteger b1 = bounds1[i];
			BigInteger b2 = bounds2[i];
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
			group1_bounds.add(b1);
			group2.add(w2);
			group2_bounds.add(b2);
			steps.add(step);
			i += step;
		}

		if(group1.size() == 1) {
			generator.addEqualityAssertion(group1.get(0), group2.get(0),
					"Equality assertion of long elements | Case 3 | Group Index 0");
			return;
		}
		
		int numOfGroupedChunks = group1.size();

		// After grouping, subtraction will be needed to compare the grouped
		// chunks and propagate carries.
		// To avoid dealing with cases where the first operand in the
		// subtraction is less than the second operand,
		// we introduce an auxiliary constant computed based on the bounds of
		// the second operand. The chunks
		// of this auxConstant will be added to the chunks of the first operand
		// before subtraction.

		BigInteger auxConstant = BigInteger.ZERO;
		BigInteger[] auxConstantChunks = new BigInteger[numOfGroupedChunks];

		Wire[] carries = generator
				.createProverWitnessWireArray(numOfGroupedChunks - 1);
		int[] carriesBitwidthBounds = new int[carries.length];

		// computing the auxConstantChunks, and the total auxConstant
		int accumStep = 0;
		for (int j = 0; j < auxConstantChunks.length - 1; j++) {
			auxConstantChunks[j] = BigInteger.valueOf(2).pow(
					group2_bounds.get(j).bitLength());
			auxConstant = auxConstant.add(auxConstantChunks[j].multiply(shift
					.pow(accumStep)));
			accumStep += steps.get(j);
			carriesBitwidthBounds[j] = Math.max(auxConstantChunks[j]
					.bitLength(), group1_bounds.get(j).bitLength())
					- steps.get(j) * CHUNK_BITWIDTH + 1;
		}

		// since the two elements should be equal, we should not need any aux
		// chunk in the last step
		auxConstantChunks[auxConstantChunks.length - 1] = BigInteger.ZERO;

		// Note: the previous auxConstantChunks are not aligned. We compute an
		// aligned version as follows.

		// First split the aux constant into small chunks based on
		// CHUNK_BITWIDTH
		BigInteger[] alignedAuxConstantSmallChunks = Util.split(auxConstant,
				CHUNK_BITWIDTH);

		// second, group the small aux chunks based on the steps array computed
		// earlier to get the alignedAuxConstantChunks
		// alignedAuxConstantChunks is the grouped version of
		// alignedAuxConstantSmallChunks

		BigInteger[] alignedAuxConstantChunks = new BigInteger[numOfGroupedChunks];
		Arrays.fill(alignedAuxConstantChunks, BigInteger.ZERO);

		int idx = 0;
		loop1: for (int j = 0; j < numOfGroupedChunks; j++) {
			for (int k = 0; k < steps.get(j); k++) {
				alignedAuxConstantChunks[j] = alignedAuxConstantChunks[j]
						.add(alignedAuxConstantSmallChunks[idx].multiply(shift
								.pow(k)));
				idx++;
				if (idx == alignedAuxConstantSmallChunks.length) {
					break loop1;
				}
			}
		}
		if (idx != alignedAuxConstantSmallChunks.length) {
			if (idx == alignedAuxConstantSmallChunks.length - 1) {
				alignedAuxConstantChunks[numOfGroupedChunks - 1] = alignedAuxConstantChunks[numOfGroupedChunks - 1]
						.add(alignedAuxConstantSmallChunks[idx].multiply(shift
								.pow(steps.get(numOfGroupedChunks - 1))));
			} else {
				throw new RuntimeException("Case not expected. Please report.");
			}
		}

		// specify how the values of carries are obtained during runtime
		generator.specifyProverWitnessComputation(new Instruction() {

			@Override
			public void evaluate(CircuitEvaluator evaluator) {

				BigInteger prevCarry = BigInteger.ZERO;
				for (int i = 0; i < carries.length; i++) {
					BigInteger a = evaluator.getWireValue(group1.get(i));
					BigInteger b = evaluator.getWireValue(group2.get(i));
					BigInteger carryValue = auxConstantChunks[i].add(a)
							.subtract(b).subtract(alignedAuxConstantChunks[i])
							.add(prevCarry);
					carryValue = carryValue.shiftRight(steps.get(i)
							* CHUNK_BITWIDTH);
					evaluator.setWireValue(carries[i], carryValue);
					prevCarry = carryValue;
				}
			}
		});

		// We must make sure that the carries values are bounded.

		for (int j = 0; j < carries.length; j++) {
			// carries[j].getBitWires(carriesBitwidthBounds[j]);
			carries[j].restrictBitLength(carriesBitwidthBounds[j]);

			// Note: in this context restrictBitLength and getBitWires will be
			// the same, but it's safer to use restrictBitLength
			// when enforcing constraints.
		}

		// Now apply the main constraints

		Wire prevCarry = generator.getZeroWire();
		BigInteger prevBound = BigInteger.ZERO;

		// recall carries.length = numOfGroupedChunks - 1
		for (int j = 0; j < carries.length + 1; j++) {
			Wire auxConstantChunkWire = generator
					.createConstantWire(auxConstantChunks[j]);
			Wire alignedAuxConstantChunkWire = generator
					.createConstantWire(alignedAuxConstantChunks[j]);

			// the last carry value must be zero
			Wire currentCarry = j == carries.length ? generator.getZeroWire()
					: carries[j];

			// overflow check for safety
			if (auxConstantChunks[j].add(group1_bounds.get(j)).add(prevBound)
					.compareTo(Config.FIELD_PRIME) >= 0) {
				System.err.println("Overflow possibility @ ForceEqual()");
			}

			Wire w1 = auxConstantChunkWire
					.add(group1.get(j).sub(group2.get(j))).add(prevCarry);
			Wire w2 = alignedAuxConstantChunkWire.add(currentCarry.mul(shift
					.pow(steps.get(j))));

			// enforce w1 = w2
			// note: in the last iteration, both auxConstantChunkWire and
			// currentCarry will be zero,
			// i.e., there will be no more values to be checked.

			generator
					.addEqualityAssertion(w1, w2,
							"Equality assertion of long elements | Case 3 | Group Index " + j);

			prevCarry = currentCarry;
			if (j != carries.length) {
				prevBound = Util.computeMaxValue(carriesBitwidthBounds[j]);
			}

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
		final Wire[] paddedA1 = Util.padWireArray(a1, length,
				generator.getZeroWire());
		final Wire[] paddedA2 = Util.padWireArray(a2, length,
				generator.getZeroWire());

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
		// Only one bit should be set.
		generator.addOneAssertion(new WireArray(helperBits).sumAllElements());

		// verify "the greater than condition" for the specified chunk
		Wire chunk1 = generator.getZeroWire();
		Wire chunk2 = generator.getZeroWire();

		for (int i = 0; i < helperBits.length; i++) {
			chunk1 = chunk1.add(paddedA1[i].mul(helperBits[i]));
			chunk2 = chunk2.add(paddedA2[i].mul(helperBits[i]));
		}
		generator.addOneAssertion(chunk1.isLessThan(chunk2,
				LongElement.CHUNK_BITWIDTH));

		// check that the other more significant chunks are equal
		Wire[] helperBits2 = new Wire[helperBits.length];
		helperBits2[0] = generator.getZeroWire();
		for (int i = 1; i < helperBits.length; i++) {
			helperBits2[i] = helperBits2[i - 1].add(helperBits[i - 1]);
//			generator.addZeroAssertion(helperBits2[i].mul(paddedA1[i]
//					.sub(paddedA2[i])));
			generator.addAssertion(helperBits2[i], paddedA1[i].sub(paddedA2[i]), generator.getZeroWire());	
		}

		// no checks needed for the less significant chunks
	}

}
