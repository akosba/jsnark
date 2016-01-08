/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.gadgets;

import java.math.BigInteger;

import circuit.eval.CircuitEvaluator;
import circuit.eval.Instruction;
import circuit.operations.Gadget;
import circuit.structure.Wire;

/**
 * This gadget provides the remainder of a % b. 
 * Use within appropriate ranges so that the prover cannot cheat via overflows.
 *
 *
 */

public class ModGadget extends Gadget {

	private Wire a;
	private Wire b;
	private Wire r;
	private Wire q;

	private int bitwidth1;
	private int bitwidth2;

	public ModGadget(Wire a, int bitwidth1,  Wire b, int bitwidth2, String...desc) {
		super(desc);
		this.a = a;
		this.b = b;
		this.bitwidth1 = bitwidth1;
		this.bitwidth2 = bitwidth2;
		if(bitwidth1 < bitwidth2){
			throw new IllegalArgumentException("bitwidth1 < bitwidth2 -- This gadget is most probably not needed.");
		}
		buildCircuit();
	}

	private void buildCircuit() {
		
		r = generator.createProverWitnessWire("mod result");
		q = generator.createProverWitnessWire("division result");

		generator.specifyProverWitnessComputation(new Instruction() {
			@Override
			public void evaluate(CircuitEvaluator evaluator) {
				BigInteger aValue = evaluator.getWireValue(a);
				BigInteger bValue = evaluator.getWireValue(b);
				BigInteger rValue = aValue.mod(bValue);
				evaluator.setWireValue(r, rValue);
				BigInteger qValue = aValue.divide(bValue);
				evaluator.setWireValue(q, qValue);
			}

		});
		
		r.restrictBitLength(bitwidth2);
		q.restrictBitLength(bitwidth1 - bitwidth2 + 1);
		generator.addOneAssertion(r.isLessThan(b, bitwidth2));
		generator.addEqualityAssertion(q.mul(b).add(r), a);
	}

	@Override
	public Wire[] getOutputWires() {
		return new Wire[] { r };
	}

}
