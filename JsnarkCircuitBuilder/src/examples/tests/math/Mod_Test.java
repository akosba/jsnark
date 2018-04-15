/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.tests.math;

import java.math.BigInteger;

import junit.framework.TestCase;

import org.junit.Test;

import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import examples.gadgets.math.ModConstantGadget;
import examples.gadgets.math.ModGadget;


public class Mod_Test extends TestCase {

	// TODO; add more tests
	@Test
	public void testCase1() {

		int a = 1262178522;
		int b = 257; // b will be an input to the circuit

		CircuitGenerator generator = new CircuitGenerator("Mod_Test1") {

			Wire[] inputWires;

			@Override
			protected void buildCircuit() {

				inputWires = createInputWireArray(2);
//				Wire r = new ModGadget(inputWires[0], (int) Math.ceil(Math.log10(a) / Math.log10(2)), inputWires[1],
//						(int) Math.ceil(Math.log10(b) / Math.log10(2))).getOutputWires()[0];
				
				Wire r = new ModGadget(inputWires[0],  inputWires[1], 32).getOutputWires()[0];
				makeOutput(r);
			}

			@Override
			public void generateSampleInput(CircuitEvaluator e) {
				e.setWireValue(inputWires[0], a);
				e.setWireValue(inputWires[1], b);

			}
		};

		generator.generateCircuit();
		CircuitEvaluator evaluator = new CircuitEvaluator(generator);
		generator.generateSampleInput(evaluator);
		evaluator.evaluate();
		Wire rWire = generator.getOutWires().get(0);
		assertEquals(evaluator.getWireValue(rWire), BigInteger.valueOf(a % b));
	}
	
	@Test
	public void testCase2() {

		int a = 1262178522;
		int b = 257; // b will be a constant

		CircuitGenerator generator = new CircuitGenerator("Mod_Test2") {

			Wire[] inputWires;

			@Override
			protected void buildCircuit() {

				inputWires = createInputWireArray(1);
				Wire r = new ModConstantGadget(inputWires[0], 32, BigInteger.valueOf(b)).getOutputWires()[0];
				makeOutput(r);
			}

			@Override
			public void generateSampleInput(CircuitEvaluator e) {
				e.setWireValue(inputWires[0], a);
			}
		};

		generator.generateCircuit();
		CircuitEvaluator evaluator = new CircuitEvaluator(generator);
		generator.generateSampleInput(evaluator);
		evaluator.evaluate();
		Wire rWire = generator.getOutWires().get(0);
		assertEquals(evaluator.getWireValue(rWire), BigInteger.valueOf(a % b));
	}
	

}
