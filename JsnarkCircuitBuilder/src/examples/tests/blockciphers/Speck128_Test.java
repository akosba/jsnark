/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.tests.blockciphers;

import java.math.BigInteger;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.junit.Test;

import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import examples.gadgets.blockciphers.Speck128CipherGadget;

/**
 * Tests SPECK block cipher @ keysize = 128, blocksize = 128.
 * Test vector obtained from:  https://github.com/inmcm/Simon_Speck_Ciphers/blob/master/Python/SimonSpeckCiphers/tests/test_simonspeck.py			
 */

public class Speck128_Test extends TestCase {

	@Test
	public void testCase1() {
		
		CircuitGenerator generator = new CircuitGenerator("Speck128_Test") {

			Wire[] plaintext; // 2 64-bit words
			Wire[] key; // 2 64-bit words
			Wire[] ciphertext; // 2 64-bit words
			
			@Override
			protected void buildCircuit() {
				plaintext = createInputWireArray(2);
				key = createInputWireArray(2);
				Wire[] expandedKey = Speck128CipherGadget.expandKey(key);
				ciphertext = new Speck128CipherGadget(plaintext, expandedKey).getOutputWires();
				makeOutputArray(ciphertext);
			}

			@Override
			public void generateSampleInput(CircuitEvaluator evaluator) {
				evaluator.setWireValue(key[0], new BigInteger("0706050403020100", 16));
				evaluator.setWireValue(key[1], new BigInteger("0f0e0d0c0b0a0908", 16));
				evaluator.setWireValue(plaintext[0], new BigInteger("7469206564616d20", 16));
				evaluator.setWireValue(plaintext[1], new BigInteger("6c61766975716520", 16));
			}
		};

		generator.generateCircuit();
		generator.evalCircuit();
		CircuitEvaluator evaluator = generator.getCircuitEvaluator();
		ArrayList<Wire> cipherText= generator.getOutWires();
		assertEquals(evaluator.getWireValue(cipherText.get(0)), new BigInteger("7860fedf5c570d18", 16));
		assertEquals(evaluator.getWireValue(cipherText.get(1)), new BigInteger("a65d985179783265", 16));
	}
}
