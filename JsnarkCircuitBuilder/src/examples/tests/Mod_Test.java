package examples.tests;

import java.math.BigInteger;

import junit.framework.TestCase;

import org.junit.Test;

import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import examples.gadgets.ModGadget;


public class Mod_Test extends TestCase {

	@Test
	public void testCase1() {

		int a = 62178522;
		int b = 257;

		CircuitGenerator generator = new CircuitGenerator("Mod_Test1") {

			Wire[] inputWires;

			@Override
			protected void buildCircuit() {

				inputWires = createInputWireArray(2);
				Wire r = new ModGadget(inputWires[0], (int) Math.ceil(Math.log10(a) / Math.log10(2)), inputWires[1],
						(int) Math.ceil(Math.log10(b) / Math.log10(2))).getOutputWires()[0];
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
		generator.printState("end");
		assertEquals(evaluator.getWireValue(rWire), new BigInteger("" + a % b));
	}

}
