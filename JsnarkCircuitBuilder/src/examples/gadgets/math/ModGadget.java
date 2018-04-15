/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.gadgets.math;

import java.math.BigInteger;

import circuit.eval.CircuitEvaluator;
import circuit.eval.Instruction;
import circuit.operations.Gadget;
import circuit.structure.Wire;

/**
 * This gadget provides the remainder of a % b. 
 *
 *
 */

public class ModGadget extends Gadget {

	private final Wire a;
	private final Wire b;
	private Wire r;
	private Wire q;

	private int bitwidth; // bitwidth for both a, b

	public ModGadget(Wire a,  Wire b, int bitwidth, String...desc) {
		super(desc);
		this.a = a;
		this.b = b;
		this.bitwidth = bitwidth;
		if(bitwidth > 126){
			throw new IllegalArgumentException("Bitwidth not supported yet.");
		}
		buildCircuit();
	}

	private void buildCircuit() {
		
		r = generator.createProverWitnessWire("mod result");
		q = generator.createProverWitnessWire("division result");

		
		// notes about how to use this code block can be found in FieldDivisionGadget
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
		
		r.restrictBitLength(bitwidth);
		q.restrictBitLength(bitwidth);
		generator.addOneAssertion(r.isLessThan(b, bitwidth));
		generator.addEqualityAssertion(q.mul(b).add(r), a);
	}

	@Override
	public Wire[] getOutputWires() {
		return new Wire[] { r };
	}

}
