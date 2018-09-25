/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.tests;

import java.math.BigInteger;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.junit.Test;

import util.Util;
import circuit.config.Config;
import circuit.eval.CircuitEvaluator;
import circuit.eval.Instruction;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import circuit.structure.WireArray;

public class PrimitiveOpTest extends TestCase {

	@Test
	public void testAddition() {

		int numIns = 100;
		BigInteger[] inVals1 = Util.randomBigIntegerArray(numIns, Config.FIELD_PRIME);
		BigInteger[] inVals2 = Util.randomBigIntegerArray(numIns, Config.FIELD_PRIME);

		ArrayList<BigInteger> result = new ArrayList<BigInteger>();
		result.add(inVals1[0].add(inVals1[1]).mod(Config.FIELD_PRIME));
		BigInteger s = BigInteger.ZERO;
		for (int i = 0; i < numIns; i++) {
			s = s.add(inVals1[i]);
		}
		result.add(s.mod(Config.FIELD_PRIME));
		for (int i = 0; i < numIns; i++) {
			result.add(inVals1[i].add(inVals2[i]).mod(Config.FIELD_PRIME));
		}

		CircuitGenerator generator = new CircuitGenerator("addition") {
			WireArray inputs1;
			WireArray inputs2;

			@Override
			protected void buildCircuit() {
				inputs1 = new WireArray(createInputWireArray(numIns));
				inputs2 = new WireArray(createInputWireArray(numIns));

				Wire result1 = inputs1.get(0).add(inputs1.get(1), "");
				Wire result2 = inputs1.sumAllElements();
				WireArray resultArray = inputs1.addWireArray(inputs2, inputs1.size());

				makeOutput(result1, "");
				makeOutput(result2, "");
				makeOutputArray(resultArray.asArray(), "");
			}

			@Override
			public void generateSampleInput(CircuitEvaluator evaluator) {
				evaluator.setWireValue(inputs1.asArray(), inVals1);
				evaluator.setWireValue(inputs2.asArray(), inVals2);

			}
		};

		generator.generateCircuit();
		CircuitEvaluator evaluator = new CircuitEvaluator(generator);
		generator.generateSampleInput(evaluator);
		evaluator.evaluate();

		int idx = 0;
		for (Wire output : generator.getOutWires()) {
			assertEquals(evaluator.getWireValue(output), result.get(idx++));
		}
		assertEquals(generator.getNumOfConstraints(), numIns + 2);

	}

	@Test
	public void testMultiplication() {

		int numIns = 100;
		BigInteger[] inVals1 = Util.randomBigIntegerArray(numIns, Config.FIELD_PRIME);
		BigInteger[] inVals2 = Util.randomBigIntegerArray(numIns, Config.FIELD_PRIME);

		ArrayList<BigInteger> result = new ArrayList<BigInteger>();
		result.add(inVals1[0].multiply(inVals1[1]).mod(Config.FIELD_PRIME));
		for (int i = 0; i < numIns; i++) {
			result.add(inVals1[i].multiply(inVals2[i]).mod(Config.FIELD_PRIME));
		}

		CircuitGenerator generator = new CircuitGenerator("multiplication") {
			WireArray inputs1;
			WireArray inputs2;

			@Override
			protected void buildCircuit() {
				inputs1 = new WireArray(createInputWireArray(numIns));
				inputs2 = new WireArray(createInputWireArray(numIns));

				Wire result1 = inputs1.get(0).mul(inputs1.get(1), "");
				WireArray resultArray = inputs1.mulWireArray(inputs2, numIns);

				makeOutput(result1, "");
				makeOutputArray(resultArray.asArray(), "");
			}

			@Override
			public void generateSampleInput(CircuitEvaluator evaluator) {
				evaluator.setWireValue(inputs1.asArray(), inVals1);
				evaluator.setWireValue(inputs2.asArray(), inVals2);

			}
		};
		generator.generateCircuit();
		CircuitEvaluator evaluator = new CircuitEvaluator(generator);
		generator.generateSampleInput(evaluator);
		evaluator.evaluate();
		int idx = 0;
		for (Wire output : generator.getOutWires()) {
			assertEquals(evaluator.getWireValue(output), result.get(idx++));
		}
		assertEquals(generator.getNumOfConstraints(), numIns + 1);
	}

	@Test
	public void testComparison() {

		int numIns = 10000;
		int numBits = 10;
		BigInteger[] inVals1 = Util.randomBigIntegerArray(numIns, numBits);
		BigInteger[] inVals2 = Util.randomBigIntegerArray(numIns, numBits);

		ArrayList<Integer> result = new ArrayList<Integer>();
		for (int i = 0; i < numIns; i++) {
			result.add(inVals1[i].compareTo(inVals2[i]));
		}

		final Wire[] result1 = new Wire[numIns];
		final Wire[] result2 = new Wire[numIns];
		final Wire[] result3 = new Wire[numIns];
		final Wire[] result4 = new Wire[numIns];
		final Wire[] result5 = new Wire[numIns];

		CircuitGenerator generator = new CircuitGenerator("comparison") {

			Wire[] inputs1;
			Wire[] inputs2;

			@Override
			protected void buildCircuit() {

				inputs1 = createInputWireArray(numIns);
				inputs2 = createInputWireArray(numIns);

				for (int i = 0; i < numIns; i++) {
					result1[i] = inputs1[i].isLessThan(inputs2[i], numBits);
					result2[i] = inputs1[i].isLessThanOrEqual(inputs2[i], numBits);
					result3[i] = inputs1[i].isGreaterThan(inputs2[i], numBits);
					result4[i] = inputs1[i].isGreaterThanOrEqual(inputs2[i], numBits);
					result5[i] = inputs1[i].isEqualTo(inputs2[i]);
				}
			}

			@Override
			public void generateSampleInput(CircuitEvaluator evaluator) {
				evaluator.setWireValue(inputs1, inVals1);
				evaluator.setWireValue(inputs2, inVals2);

			}
		};
		generator.generateCircuit();
		CircuitEvaluator evaluator = new CircuitEvaluator(generator);
		generator.generateSampleInput(evaluator);
//		generator.printCircuit();
		evaluator.evaluate();
		for (int i = 0; i < numIns; i++) {
			int r = result.get(i);
			if (r == 0) {
				assertEquals(evaluator.getWireValue(result1[i]), BigInteger.ZERO);
				assertEquals(evaluator.getWireValue(result2[i]), BigInteger.ONE);
				assertEquals(evaluator.getWireValue(result3[i]), BigInteger.ZERO);
				assertEquals(evaluator.getWireValue(result4[i]), BigInteger.ONE);
				assertEquals(evaluator.getWireValue(result5[i]), BigInteger.ONE);
			} else if (r == 1) {
				assertEquals(evaluator.getWireValue(result1[i]), BigInteger.ZERO);
				assertEquals(evaluator.getWireValue(result2[i]), BigInteger.ZERO);
				assertEquals(evaluator.getWireValue(result3[i]), BigInteger.ONE);
				assertEquals(evaluator.getWireValue(result4[i]), BigInteger.ONE);
				assertEquals(evaluator.getWireValue(result5[i]), BigInteger.ZERO);
			} else if (r == -1) {
				assertEquals(evaluator.getWireValue(result1[i]), BigInteger.ONE);
				assertEquals(evaluator.getWireValue(result2[i]), BigInteger.ONE);
				assertEquals(evaluator.getWireValue(result3[i]), BigInteger.ZERO);
				assertEquals(evaluator.getWireValue(result4[i]), BigInteger.ZERO);
				assertEquals(evaluator.getWireValue(result5[i]), BigInteger.ZERO);
			}
		}
	}

	@Test
	public void testBooleanOperations() {

		int numIns = Config.LOG2_FIELD_PRIME;
		BigInteger[] inVals1 = Util.randomBigIntegerArray(numIns, Config.FIELD_PRIME);
		BigInteger[] inVals2 = Util.randomBigIntegerArray(numIns, Config.FIELD_PRIME);
		BigInteger[] inVals3 = Util.randomBigIntegerArray(numIns, 32);

		BigInteger[] shiftedRightVals = new BigInteger[numIns];
		BigInteger[] shiftedLeftVals = new BigInteger[numIns];
		BigInteger[] rotatedRightVals = new BigInteger[numIns];
		BigInteger[] rotatedLeftVals = new BigInteger[numIns];
		BigInteger[] xoredVals = new BigInteger[numIns];
		BigInteger[] oredVals = new BigInteger[numIns];
		BigInteger[] andedVals = new BigInteger[numIns];
		BigInteger[] invertedVals = new BigInteger[numIns];

		BigInteger mask = new BigInteger("2").pow(Config.LOG2_FIELD_PRIME).subtract(BigInteger.ONE);
		
		for (int i = 0; i < numIns; i++) {
			shiftedRightVals[i] = inVals1[i].shiftRight(i).mod(Config.FIELD_PRIME);
			shiftedLeftVals[i] = inVals1[i].shiftLeft(i).and(mask).mod(Config.FIELD_PRIME);
			rotatedRightVals[i] = BigInteger.valueOf(Integer.rotateRight(inVals3[i].intValue(), i % 32) & 0x00000000ffffffffL);
			rotatedLeftVals[i] = BigInteger.valueOf(Integer.rotateLeft(inVals3[i].intValue(), i % 32) & 0x00000000ffffffffL );
			xoredVals[i] = inVals1[i].xor(inVals2[i]).mod(Config.FIELD_PRIME);
			oredVals[i] = inVals1[i].or(inVals2[i]).mod(Config.FIELD_PRIME);
			andedVals[i] = inVals1[i].and(inVals2[i]).mod(Config.FIELD_PRIME);
			invertedVals[i] = BigInteger.valueOf(~inVals3[i].intValue() & 0x00000000ffffffffL);
		}

		CircuitGenerator generator = new CircuitGenerator("boolean_operations") {
			Wire[] inputs1;
			Wire[] inputs2;
			Wire[] inputs3;

			@Override
			protected void buildCircuit() {

				inputs1 = createInputWireArray(numIns);
				inputs2 = createInputWireArray(numIns);
				inputs3 = createInputWireArray(numIns);

				Wire[] shiftedRight = new Wire[numIns];
				Wire[] shiftedLeft = new Wire[numIns];
				Wire[] rotatedRight = new Wire[numIns];
				Wire[] rotatedLeft = new Wire[numIns];
				Wire[] xored = new Wire[numIns];
				Wire[] ored = new Wire[numIns];
				Wire[] anded = new Wire[numIns];
				Wire[] inverted = new Wire[numIns];

				for (int i = 0; i < numIns; i++) {
					shiftedRight[i] = inputs1[i].shiftRight(Config.LOG2_FIELD_PRIME, i);
					shiftedLeft[i] = inputs1[i].shiftLeft(Config.LOG2_FIELD_PRIME, i);
					rotatedRight[i] = inputs3[i].rotateRight(32, i % 32);
					rotatedLeft[i] = inputs3[i].rotateLeft(32, i % 32);
					xored[i] = inputs1[i].xorBitwise(inputs2[i], Config.LOG2_FIELD_PRIME);
					ored[i] = inputs1[i].orBitwise(inputs2[i], Config.LOG2_FIELD_PRIME);
					anded[i] = inputs1[i].andBitwise(inputs2[i], Config.LOG2_FIELD_PRIME);

					inverted[i] = inputs3[i].invBits(32);
				}

				makeOutputArray(shiftedRight);
				makeOutputArray(shiftedLeft);
				makeOutputArray(rotatedRight);
				makeOutputArray(rotatedLeft);
				makeOutputArray(xored);
				makeOutputArray(ored);
				makeOutputArray(anded);
				makeOutputArray(inverted);
			}

			@Override
			public void generateSampleInput(CircuitEvaluator evaluator) {
				evaluator.setWireValue(inputs1, inVals1);
				evaluator.setWireValue(inputs2, inVals2);
				evaluator.setWireValue(inputs3, inVals3);
			}
		};
		generator.generateCircuit();
		CircuitEvaluator evaluator = new CircuitEvaluator(generator);
		generator.generateSampleInput(evaluator);
		evaluator.evaluate();

		ArrayList<Wire> outWires = generator.getOutWires();
		int i, outputIndex = 0;
		for (i = 0; i < numIns; i++) 
			assertEquals(shiftedRightVals[i], evaluator.getWireValue(outWires.get(i + outputIndex)));

		outputIndex += numIns;
		for (i = 0; i < numIns; i++) 
			assertEquals(shiftedLeftVals[i], evaluator.getWireValue(outWires.get(i + outputIndex)));

		outputIndex += numIns;
		for (i = 0; i < numIns; i++)
			assertEquals(rotatedRightVals[i], evaluator.getWireValue(outWires.get(i + outputIndex)));
		
		outputIndex += numIns;
		for (i = 0; i < numIns; i++)
			assertEquals(rotatedLeftVals[i], evaluator.getWireValue(outWires.get(i + outputIndex)));

		outputIndex += numIns;
		for (i = 0; i < numIns; i++)
			assertEquals(xoredVals[i], evaluator.getWireValue(outWires.get(i + outputIndex)));

		outputIndex += numIns;
		for (i = 0; i < numIns; i++)
			assertEquals(oredVals[i], evaluator.getWireValue(outWires.get(i + outputIndex)));

		outputIndex += numIns;
		for (i = 0; i < numIns; i++)
			assertEquals(andedVals[i], evaluator.getWireValue(outWires.get(i + outputIndex)));

		outputIndex += numIns;
		for (i = 0; i < numIns; i++)
			assertEquals(invertedVals[i], evaluator.getWireValue(outWires.get(i + outputIndex)));

	}
	
	
	@Test
	public void testAssertion() {

		int numIns = 100;
		BigInteger[] inVals1 = Util.randomBigIntegerArray(numIns, Config.FIELD_PRIME);
		BigInteger[] inVals2 = Util.randomBigIntegerArray(numIns, Config.FIELD_PRIME);
		
		
		ArrayList<BigInteger> result = new ArrayList<BigInteger>();
		result.add(inVals1[0].multiply(inVals1[0]).mod(Config.FIELD_PRIME));
		for (int i = 0; i < numIns; i++) {
			result.add(inVals1[i].multiply(inVals2[i]).mod(Config.FIELD_PRIME));
		}

		CircuitGenerator generator = new CircuitGenerator("assertions") {
			WireArray inputs1;
			WireArray inputs2;
			WireArray solutions; // provide solutions as witnesses

			@Override
			protected void buildCircuit() {
				inputs1 = new WireArray(createInputWireArray(numIns));
				inputs2 = new WireArray(createInputWireArray(numIns));
				solutions = new WireArray(createProverWitnessWireArray(numIns+1));

				specifyProverWitnessComputation(new Instruction() {
					
					@Override
					public void evaluate(CircuitEvaluator evaluator) {
						evaluator.setWireValue(solutions.get(0),result.get(0));
						for(int i =0; i < numIns;i++){
							evaluator.setWireValue(solutions.get(i+1),result.get(i+1));
						}
					}
				});
				
				addAssertion(inputs1.get(0), inputs1.get(0), solutions.get(0));
				for(int i = 0; i < numIns;i++){
					addAssertion(inputs1.get(i), inputs2.get(i), solutions.get(i+1));
				}
				
				// constant assertions will not add constraints
				addZeroAssertion(zeroWire);
				addOneAssertion(oneWire);
				addAssertion(zeroWire, oneWire, zeroWire);
				addAssertion(oneWire, oneWire, oneWire);
				addBinaryAssertion(zeroWire);
				addBinaryAssertion(oneWire);
				
				// won't add a constraint
				addEqualityAssertion(inputs1.get(0), inputs1.get(0));

				// will add a constraint
				addEqualityAssertion(inputs1.get(0), inVals1[0]);

				
			}

			@Override
			public void generateSampleInput(CircuitEvaluator evaluator) {
				evaluator.setWireValue(inputs1.asArray(), inVals1);
				evaluator.setWireValue(inputs2.asArray(), inVals2);

			}
		};
		generator.generateCircuit();
		CircuitEvaluator evaluator = new CircuitEvaluator(generator);
		generator.generateSampleInput(evaluator);
		evaluator.evaluate(); // no exception will be thrown
		assertEquals(generator.getNumOfConstraints(), numIns + 2);
	}
}
