/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.tests;

import java.math.BigInteger;
import java.util.ArrayList;

import org.junit.Test;

import circuit.config.Config;
import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import junit.framework.TestCase;
import util.Util;

public class CachingTest extends TestCase {

	
	@Test
	public void testCaching() {

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
		BigInteger[] multipliedVals = new BigInteger[numIns];
		BigInteger[] addedVals = new BigInteger[numIns];  // I don't cache additions now, but just testing there are no side effects


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
			multipliedVals[i] = inVals1[i].multiply(inVals2[i]).mod(Config.FIELD_PRIME);
			addedVals[i] = inVals1[i].add(inVals2[i]).mod(Config.FIELD_PRIME);
			
		}

		CircuitGenerator generator = new CircuitGenerator("Caching_Test") {
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

				Wire[] multiplied = new Wire[numIns];
				Wire[] added = new Wire[numIns];

				for (int i = 0; i < numIns; i++) {
					shiftedRight[i] = inputs1[i].shiftRight(Config.LOG2_FIELD_PRIME, i);
					shiftedLeft[i] = inputs1[i].shiftLeft(Config.LOG2_FIELD_PRIME, i);
					rotatedRight[i] = inputs3[i].rotateRight(32, i % 32);
					rotatedLeft[i] = inputs3[i].rotateLeft(32, i % 32);
					xored[i] = inputs1[i].xorBitwise(inputs2[i], Config.LOG2_FIELD_PRIME);
					ored[i] = inputs1[i].orBitwise(inputs2[i], Config.LOG2_FIELD_PRIME);
					anded[i] = inputs1[i].andBitwise(inputs2[i], Config.LOG2_FIELD_PRIME);
					inverted[i] = inputs3[i].invBits(32);
					multiplied[i] = inputs1[i].mul(inputs2[i]);
					added[i] = inputs1[i].add(inputs2[i]);
				}
				
				int currentCost = getNumOfConstraints();
				
				// repeat everything again, and verify that the number of multiplication gates will not be affected
				for (int i = 0; i < numIns; i++) {
					shiftedRight[i] = inputs1[i].shiftRight(Config.LOG2_FIELD_PRIME, i);
					shiftedLeft[i] = inputs1[i].shiftLeft(Config.LOG2_FIELD_PRIME, i);
					rotatedRight[i] = inputs3[i].rotateRight(32, i % 32);
					rotatedLeft[i] = inputs3[i].rotateLeft(32, i % 32);
					xored[i] = inputs1[i].xorBitwise(inputs2[i], Config.LOG2_FIELD_PRIME);
					ored[i] = inputs1[i].orBitwise(inputs2[i], Config.LOG2_FIELD_PRIME);
					anded[i] = inputs1[i].andBitwise(inputs2[i], Config.LOG2_FIELD_PRIME);
					inverted[i] = inputs3[i].invBits(32);
					multiplied[i] = inputs1[i].mul(inputs2[i]);
					added[i] = inputs1[i].add(inputs2[i]);
				}
				
				assertTrue(getNumOfConstraints() == currentCost);
				
				// repeat binary operations again while changing the order of the operands, and verify that the number of multiplication gates will not be affected
				for (int i = 0; i < numIns; i++) {
					xored[i] = inputs2[i].xorBitwise(inputs1[i], Config.LOG2_FIELD_PRIME);
					ored[i] = inputs2[i].orBitwise(inputs1[i], Config.LOG2_FIELD_PRIME);
					anded[i] = inputs2[i].andBitwise(inputs1[i], Config.LOG2_FIELD_PRIME);
					multiplied[i] = inputs2[i].mul(inputs1[i]);
					added[i] = inputs2[i].add(inputs1[i]);
				}

				assertTrue(getNumOfConstraints() == currentCost);

				
				makeOutputArray(shiftedRight);
				makeOutputArray(shiftedLeft);
				makeOutputArray(rotatedRight);
				makeOutputArray(rotatedLeft);
				makeOutputArray(xored);
				makeOutputArray(ored);
				makeOutputArray(anded);
				makeOutputArray(inverted);
				makeOutputArray(multiplied);
				makeOutputArray(added);
				
				currentCost = getNumOfConstraints();
				
				// repeat labeling as output (although not really meaningful) and make sure no more constraints are added
				makeOutputArray(shiftedRight);
				makeOutputArray(shiftedLeft);
				makeOutputArray(rotatedRight);
				makeOutputArray(rotatedLeft);
				makeOutputArray(xored);
				makeOutputArray(ored);
				makeOutputArray(anded);
				makeOutputArray(inverted);
				makeOutputArray(multiplied);
				makeOutputArray(added);
				
				assertTrue(getNumOfConstraints() == currentCost);
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
		
		outputIndex += numIns;
		for (i = 0; i < numIns; i++)
			assertEquals(multipliedVals[i], evaluator.getWireValue(outWires.get(i + outputIndex)));
		
		outputIndex += numIns;
		for (i = 0; i < numIns; i++)
			assertEquals(addedVals[i], evaluator.getWireValue(outWires.get(i + outputIndex)));

	}
	
}
