/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.tests.blockciphers;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

import junit.framework.TestCase;

import org.junit.Test;

import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import examples.gadgets.blockciphers.AES128CipherGadget;
import examples.gadgets.blockciphers.sbox.AESSBoxGadgetOptimized2;


public class AES128_Test extends TestCase {

	@Test
	public void testCase1() {
		
		// key: "2b7e151628aed2a6abf7158809cf4f3c"
		// plaintext: "ae2d8a571e03ac9c9eb76fac45af8e51"
		// ciphertext: "f5d3d58503b9699de785895a96fdbaaf"
		
		// testing all available sBox implementations
		for(AES128CipherGadget.SBoxOption sboxOption: AES128CipherGadget.SBoxOption.values()){
			
			AES128CipherGadget.sBoxOption = sboxOption;
			CircuitGenerator generator = new CircuitGenerator("AES128_Test1_"+sboxOption) {
	
				private Wire[] plaintext; // 16 bytes
				private Wire[] key; // 16 bytes
				private Wire[] ciphertext; // 16 bytes
	
				@Override
				protected void buildCircuit() {
					plaintext = createInputWireArray(16);
					key = createInputWireArray(16);
					Wire[] expandedKey = AES128CipherGadget.expandKey(key);
					ciphertext = new AES128CipherGadget(plaintext, expandedKey)
							.getOutputWires();
					makeOutputArray(ciphertext);
				}
	
				@Override
				public void generateSampleInput(CircuitEvaluator evaluator) {
	
					BigInteger keyV = new BigInteger(
							"2b7e151628aed2a6abf7158809cf4f3c", 16);
					BigInteger msgV = new BigInteger(
							"ae2d8a571e03ac9c9eb76fac45af8e51", 16);
	
					byte[] keyArray = keyV.toByteArray();
					byte[] msgArray = msgV.toByteArray();
					msgArray = Arrays.copyOfRange(msgArray, msgArray.length - 16,
							msgArray.length);
					keyArray = Arrays.copyOfRange(keyArray, keyArray.length - 16,
							keyArray.length);
	
					for (int i = 0; i < plaintext.length; i++) {
						evaluator.setWireValue(plaintext[i], (msgArray[i] & 0xff));
					}
					for (int i = 0; i < key.length; i++) {
						evaluator.setWireValue(key[i], (keyArray[i] & 0xff));
					}
				}
			};
	
			generator.generateCircuit();
			generator.evalCircuit();
			CircuitEvaluator evaluator = generator.getCircuitEvaluator();
			ArrayList<Wire> cipherText = generator.getOutWires();
	
			BigInteger result = new BigInteger("f5d3d58503b9699de785895a96fdbaaf",
					16);
		
			byte[] resultArray = result.toByteArray();
			resultArray = Arrays.copyOfRange(resultArray, resultArray.length - 16,
					resultArray.length);
	
			for (int i = 0; i < 16; i++) {
				assertEquals(evaluator.getWireValue(cipherText.get(i)),
						BigInteger.valueOf((resultArray[i] + 256) % 256));
			}
		}
	}
	
	
	@Test
	public void testCase2() {
		
		// key: "2b7e151628aed2a6abf7158809cf4f3c"
		// plaintext: "6bc1bee22e409f96e93d7e117393172a"
		// ciphertext: "3ad77bb40d7a3660a89ecaf32466ef97"
		
		// testing all available sBox implementations
		for(AES128CipherGadget.SBoxOption sboxOption: AES128CipherGadget.SBoxOption.values()){
			
			AES128CipherGadget.sBoxOption = sboxOption;
			CircuitGenerator generator = new CircuitGenerator("AES128_Test2_"+sboxOption) {
	
				private Wire[] plaintext; // 16 bytes
				private Wire[] key; // 16 bytes
				private Wire[] ciphertext; // 16 bytes
	
				@Override
				protected void buildCircuit() {
					plaintext = createInputWireArray(16);
					key = createInputWireArray(16);
					Wire[] expandedKey = AES128CipherGadget.expandKey(key);
					ciphertext = new AES128CipherGadget(plaintext, expandedKey)
							.getOutputWires();
					makeOutputArray(ciphertext);
				}
	
				@Override
				public void generateSampleInput(CircuitEvaluator evaluator) {
	
					BigInteger keyV = new BigInteger(
							"2b7e151628aed2a6abf7158809cf4f3c", 16);
					BigInteger msgV = new BigInteger(
							"6bc1bee22e409f96e93d7e117393172a", 16);
	
					byte[] keyArray = keyV.toByteArray();
					byte[] msgArray = msgV.toByteArray();
					msgArray = Arrays.copyOfRange(msgArray, msgArray.length - 16,
							msgArray.length);
					keyArray = Arrays.copyOfRange(keyArray, keyArray.length - 16,
							keyArray.length);
	
					for (int i = 0; i < plaintext.length; i++) {
						evaluator.setWireValue(plaintext[i], (msgArray[i] & 0xff));
					}
					for (int i = 0; i < key.length; i++) {
						evaluator.setWireValue(key[i], (keyArray[i] & 0xff));
					}
				}
			};
	
			generator.generateCircuit();
			generator.evalCircuit();
			CircuitEvaluator evaluator = generator.getCircuitEvaluator();
			ArrayList<Wire> cipherText = generator.getOutWires();
	
			BigInteger result = new BigInteger("3ad77bb40d7a3660a89ecaf32466ef97",
					16);
	
			// expected output:0xf5d3d58503b9699de785895a96fdbaaf
	
			byte[] resultArray = result.toByteArray();
			resultArray = Arrays.copyOfRange(resultArray, resultArray.length - 16,
					resultArray.length);
	
			for (int i = 0; i < 16; i++) {
				assertEquals(evaluator.getWireValue(cipherText.get(i)),
						BigInteger.valueOf((resultArray[i] + 256) % 256));
			}
		}
	}

	@Test
	public void testCase3() {
		
		// key: "2b7e151628aed2a6abf7158809cf4f3c"
		// plaintext: "6bc1bee22e409f96e93d7e117393172a"
		// ciphertext: "3ad77bb40d7a3660a89ecaf32466ef97"
		
		// testing all available sBox implementations
		for(AES128CipherGadget.SBoxOption sboxOption: AES128CipherGadget.SBoxOption.values()){
			
			AES128CipherGadget.sBoxOption = sboxOption;
			CircuitGenerator generator = new CircuitGenerator("AES128_Test3_"+sboxOption) {
	
				private Wire[] plaintext; // 16 bytes
				private Wire[] key; // 16 bytes
				private Wire[] ciphertext; // 16 bytes
	
				@Override
				protected void buildCircuit() {
					plaintext = createInputWireArray(16);
					key = createInputWireArray(16);
					Wire[] expandedKey = AES128CipherGadget.expandKey(key);
					ciphertext = new AES128CipherGadget(plaintext, expandedKey)
							.getOutputWires();
					makeOutputArray(ciphertext);
				}
	
				@Override
				public void generateSampleInput(CircuitEvaluator evaluator) {
	
					BigInteger keyV = new BigInteger(
							"2b7e151628aed2a6abf7158809cf4f3c", 16);
					BigInteger msgV = new BigInteger(
							"30c81c46a35ce411e5fbc1191a0a52ef", 16);
	
					byte[] keyArray = keyV.toByteArray();
					byte[] msgArray = msgV.toByteArray();
					msgArray = Arrays.copyOfRange(msgArray, msgArray.length - 16,
							msgArray.length);
					keyArray = Arrays.copyOfRange(keyArray, keyArray.length - 16,
							keyArray.length);
	
					for (int i = 0; i < plaintext.length; i++) {
						evaluator.setWireValue(plaintext[i], (msgArray[i] & 0xff));
					}
					for (int i = 0; i < key.length; i++) {
						evaluator.setWireValue(key[i], (keyArray[i] & 0xff));
					}
				}
			};
	
			generator.generateCircuit();
			generator.evalCircuit();
			CircuitEvaluator evaluator = generator.getCircuitEvaluator();
			ArrayList<Wire> cipherText = generator.getOutWires();
	
			BigInteger result = new BigInteger("43b1cd7f598ece23881b00e3ed030688",
					16);
	
			byte[] resultArray = result.toByteArray();
			resultArray = Arrays.copyOfRange(resultArray, resultArray.length - 16,
					resultArray.length);
	
			for (int i = 0; i < 16; i++) {
				assertEquals(evaluator.getWireValue(cipherText.get(i)),
						BigInteger.valueOf((resultArray[i] + 256) % 256));
			}
		}
	}


	@Test
	public void testCase4() {
		
		// key: "2b7e151628aed2a6abf7158809cf4f3c"
		// plaintext: "30c81c46a35ce411e5fbc1191a0a52ef"
		// ciphertext: "43b1cd7f598ece23881b00e3ed030688"
		
		// testing all available sBox implementations
		for(AES128CipherGadget.SBoxOption sboxOption: AES128CipherGadget.SBoxOption.values()){
			
			AES128CipherGadget.sBoxOption = sboxOption;
			CircuitGenerator generator = new CircuitGenerator("AES128_Test4_"+sboxOption) {
	
				private Wire[] plaintext; // 16 bytes
				private Wire[] key; // 16 bytes
				private Wire[] ciphertext; // 16 bytes
	
				@Override
				protected void buildCircuit() {
					plaintext = createInputWireArray(16);
					key = createInputWireArray(16);
					Wire[] expandedKey = AES128CipherGadget.expandKey(key);
					ciphertext = new AES128CipherGadget(plaintext, expandedKey)
							.getOutputWires();
					makeOutputArray(ciphertext);
				}
	
				@Override
				public void generateSampleInput(CircuitEvaluator evaluator) {
	
					BigInteger keyV = new BigInteger(
							"2b7e151628aed2a6abf7158809cf4f3c", 16);
					BigInteger msgV = new BigInteger(
							"f69f2445df4f9b17ad2b417be66c3710", 16);
	
					byte[] keyArray = keyV.toByteArray();
					byte[] msgArray = msgV.toByteArray();
					msgArray = Arrays.copyOfRange(msgArray, msgArray.length - 16,
							msgArray.length);
					keyArray = Arrays.copyOfRange(keyArray, keyArray.length - 16,
							keyArray.length);
	
					for (int i = 0; i < plaintext.length; i++) {
						evaluator.setWireValue(plaintext[i], (msgArray[i] & 0xff));
					}
					for (int i = 0; i < key.length; i++) {
						evaluator.setWireValue(key[i], (keyArray[i] & 0xff));
					}
				}
			};
	
			generator.generateCircuit();
			generator.evalCircuit();
			CircuitEvaluator evaluator = generator.getCircuitEvaluator();
			ArrayList<Wire> cipherText = generator.getOutWires();
	
			BigInteger result = new BigInteger("7b0c785e27e8ad3f8223207104725dd4",
					16);

			
			byte[] resultArray = result.toByteArray();
			resultArray = Arrays.copyOfRange(resultArray, resultArray.length - 16,
					resultArray.length);
	
			for (int i = 0; i < 16; i++) {
				assertEquals(evaluator.getWireValue(cipherText.get(i)),
						BigInteger.valueOf((resultArray[i] + 256) % 256));
			}
		}
	}
	
	@Test
	public void testCustomSboxImplementation() {
		
		
		AES128CipherGadget.sBoxOption = AES128CipherGadget.SBoxOption.OPTIMIZED2;
		for(int b = 0; b <= 15; b++){
			
			AESSBoxGadgetOptimized2.setBitCount(b);
			AESSBoxGadgetOptimized2.solveLinearSystems();
			CircuitGenerator generator = new CircuitGenerator("AES128_Test_SBox_Parametrization_"+b) {
	
				private Wire[] plaintext; // 16 bytes
				private Wire[] key; // 16 bytes
				private Wire[] ciphertext; // 16 bytes
	
				@Override
				protected void buildCircuit() {
					plaintext = createInputWireArray(16);
					key = createInputWireArray(16);
					Wire[] expandedKey = AES128CipherGadget.expandKey(key);
					ciphertext = new AES128CipherGadget(plaintext, expandedKey)
							.getOutputWires();
					makeOutputArray(ciphertext);
				}
	
				@Override
				public void generateSampleInput(CircuitEvaluator evaluator) {
	
					BigInteger keyV = new BigInteger(
							"2b7e151628aed2a6abf7158809cf4f3c", 16);
					BigInteger msgV = new BigInteger(
							"f69f2445df4f9b17ad2b417be66c3710", 16);
	
					byte[] keyArray = keyV.toByteArray();
					byte[] msgArray = msgV.toByteArray();
					msgArray = Arrays.copyOfRange(msgArray, msgArray.length - 16,
							msgArray.length);
					keyArray = Arrays.copyOfRange(keyArray, keyArray.length - 16,
							keyArray.length);
	
					for (int i = 0; i < plaintext.length; i++) {
						evaluator.setWireValue(plaintext[i], (msgArray[i] & 0xff));
					}
					for (int i = 0; i < key.length; i++) {
						evaluator.setWireValue(key[i], (keyArray[i] & 0xff));
					}
				}
			};
	
			generator.generateCircuit();
			generator.evalCircuit();
			CircuitEvaluator evaluator = generator.getCircuitEvaluator();
			ArrayList<Wire> cipherText = generator.getOutWires();
	
			BigInteger result = new BigInteger("7b0c785e27e8ad3f8223207104725dd4",
					16);

			
			byte[] resultArray = result.toByteArray();
			resultArray = Arrays.copyOfRange(resultArray, resultArray.length - 16,
					resultArray.length);
	
			for (int i = 0; i < 16; i++) {
				assertEquals(evaluator.getWireValue(cipherText.get(i)),
						BigInteger.valueOf((resultArray[i] + 256) % 256));
			}
		}
	}


	
}
