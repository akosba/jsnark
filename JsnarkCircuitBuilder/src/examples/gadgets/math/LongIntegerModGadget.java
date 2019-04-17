/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.gadgets.math;

import java.math.BigInteger;
import java.util.Arrays;

import util.Util;
import circuit.auxiliary.LongElement;
import circuit.eval.CircuitEvaluator;
import circuit.eval.Instruction;
import circuit.operations.Gadget;
import circuit.structure.Wire;

/**
 * This gadget provides a % b, when both operands are represented as long
 * elements. You can check the RSA gadgets/circuit generators for an example.
 * Most of the optimizations that reduce the cost of this step are more visible
 * in the LongElement class methods called by this gadget.
 * 
 */
public class LongIntegerModGadget extends Gadget {

	private final LongElement a;
	private final LongElement b;

	private LongElement r;
	private LongElement q;
	private boolean restrictRange;

	/**
	 * 
	 *
	 * 
	 * @param a
	 * @param b
	 * @param restrictRange
	 *            if true, the output will be forced to be less than b,
	 *            otherwise the output remainder will only be guaranteed to have
	 *            the same bitwidth as b, but not necessarily less than b. The
	 *            second case is helpful when the purpose is just to reduce the
	 *            range, while having consistent output. As an example (in a
	 *            short integer case for simplicity): assume we are interested
	 *            in this operation 3001 % 10. The output should be 1 in normal
	 *            cases, but to save some operations, we might skip checking
	 *            that the result is less than the modulus and just check that
	 *            it has the same bitwidth as the modulus, which we must do
	 *            anyway since the result is provided as a witness. In that
	 *            case, the output of this gadget could be 1 or 11, which in
	 *            some contexts would be ok, e.g. in intermediate operations.
	 *            See the RSA encryption gadget for an illustration.
	 * @param desc
	 */

	public LongIntegerModGadget(LongElement a, LongElement b,
			boolean restrictRange, String... desc) {
		super(desc);
		this.a = a;
		this.b = b;
		this.restrictRange = restrictRange;
		buildCircuit();
	}

	private void buildCircuit() {

		int aBitwidth = a.getMaxVal(LongElement.CHUNK_BITWIDTH).bitLength();
		int bBitwidth = b.getMaxVal(LongElement.CHUNK_BITWIDTH).bitLength();

		int rBitwidth = bBitwidth;
		int qBitwidth = aBitwidth - bBitwidth + 1;

		int rChunkLength = (int) Math.ceil(rBitwidth * 1.0
				/ LongElement.CHUNK_BITWIDTH);
		int qChunkLength = (int) Math.ceil(qBitwidth * 1.0
				/ LongElement.CHUNK_BITWIDTH);

		Wire[] rWires = generator.createProverWitnessWireArray(rChunkLength);
		Wire[] qWires = generator.createProverWitnessWireArray(qChunkLength);

		int[] rChunkBitwidths = new int[rChunkLength];
		int[] qChunkBitwidths = new int[qChunkLength];

		Arrays.fill(rChunkBitwidths, LongElement.CHUNK_BITWIDTH);
		Arrays.fill(qChunkBitwidths, LongElement.CHUNK_BITWIDTH);

		if (rBitwidth % LongElement.CHUNK_BITWIDTH != 0) {
			rChunkBitwidths[rChunkLength - 1] = rBitwidth
					% LongElement.CHUNK_BITWIDTH;
		}
		if (qBitwidth % LongElement.CHUNK_BITWIDTH != 0) {
			qChunkBitwidths[qChunkLength - 1] = qBitwidth
					% LongElement.CHUNK_BITWIDTH;
		}

		r = new LongElement(rWires, rChunkBitwidths);
		q = new LongElement(qWires, qChunkBitwidths);

		//
		// r = generator.createProverWitnessWire("mod result");
		// q = generator.createProverWitnessWire("division result");
		//
		//
		generator.specifyProverWitnessComputation(new Instruction() {
			@Override
			public void evaluate(CircuitEvaluator evaluator) {
				BigInteger aValue = evaluator.getWireValue(a,
						LongElement.CHUNK_BITWIDTH);
				BigInteger bValue = evaluator.getWireValue(b,
						LongElement.CHUNK_BITWIDTH);
				BigInteger rValue = aValue.mod(bValue);
				BigInteger qValue = aValue.divide(bValue);

				evaluator.setWireValue(r.getArray(),
						Util.split(rValue, LongElement.CHUNK_BITWIDTH));
				evaluator.setWireValue(q.getArray(),
						Util.split(qValue, LongElement.CHUNK_BITWIDTH));
			}
		});

		r.restrictBitwidth();
		q.restrictBitwidth();

		LongElement res = q.mul(b).add(r);

		// implements the improved long integer equality assertion from xjsnark
		res.assertEquality(a);
		
		if (restrictRange) {
			r.assertLessThan(b);
		}

	}

	@Override
	public Wire[] getOutputWires() {
		return r.getArray();
	}

	public LongElement getRemainder() {
		return r;
	}

}