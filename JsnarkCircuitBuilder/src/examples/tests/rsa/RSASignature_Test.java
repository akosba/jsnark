/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.tests.rsa;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;

import junit.framework.TestCase;

import org.junit.Test;

import circuit.auxiliary.LongElement;
import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import examples.gadgets.hash.SHA256Gadget;
import examples.gadgets.rsa.RSASigVerificationV1_5_Gadget;



//Tests RSA PKCS #1, V1.5 Signature

public class RSASignature_Test extends TestCase {

	/*
	 * Note that these tests are for ensuring the basic functionality. To verify
	 * that the gadget cannot allow *any* invalid signatures to pass, this requires more than testing few cases, e.g. a
	 * careful review of the code  to ensure that there are no
	 * missing/incorrect constraints that a cheating prover could make use of.
	 */

	@Test
	public void testValidSignatureDifferentKeyLengths() {

		String inputStr = "abc";

		// testing commonly used rsa key lengths in addition to non-power of two
		// ones

		// might need to increase memory heap to run this test on some platforms

		int[] keySizeArray = new int[] { 1024, 2048, 3072, 4096, 2047, 2049 };

		for (int keySize : keySizeArray) {
			CircuitGenerator generator = new CircuitGenerator("RSA" + keySize
					+ "_SIG_TestValid") {

				int rsaKeyLength = keySize;
				Wire[] inputMessage;
				LongElement signature;
				LongElement rsaModulus;

				SHA256Gadget sha2Gadget;
				RSASigVerificationV1_5_Gadget rsaSigVerificationV1_5_Gadget;

				@Override
				protected void buildCircuit() {
					inputMessage = createInputWireArray(inputStr.length());
					sha2Gadget = new SHA256Gadget(inputMessage, 8,
							inputMessage.length, false, true);
					Wire[] digest = sha2Gadget.getOutputWires();
					rsaModulus = createLongElementInput(rsaKeyLength);
					signature = createLongElementInput(rsaKeyLength);
					rsaSigVerificationV1_5_Gadget = new RSASigVerificationV1_5_Gadget(
							rsaModulus, digest, signature, rsaKeyLength);
					makeOutput(rsaSigVerificationV1_5_Gadget.getOutputWires()[0]);
				}

				@Override
				public void generateSampleInput(CircuitEvaluator evaluator) {

					for (int i = 0; i < inputMessage.length; i++) {
						evaluator.setWireValue(inputMessage[i],
								inputStr.charAt(i));
					}
					try {
						KeyPairGenerator keyGen = KeyPairGenerator
								.getInstance("RSA");
						keyGen.initialize(rsaKeyLength, new SecureRandom());
						KeyPair keyPair = keyGen.generateKeyPair();
						Signature signature = Signature
								.getInstance("SHA256withRSA");
						signature.initSign(keyPair.getPrivate());

						byte[] message = inputStr.getBytes();
						signature.update(message);

						byte[] sigBytes = signature.sign();

						// pad an extra zero byte to avoid having a negative big
						// integer
						byte[] signaturePadded = new byte[sigBytes.length + 1];
						System.arraycopy(sigBytes, 0, signaturePadded, 1,
								sigBytes.length);
						signaturePadded[0] = 0;
						BigInteger modulus = ((RSAPublicKey) keyPair
								.getPublic()).getModulus();
						BigInteger sig = new BigInteger(signaturePadded);

						evaluator.setWireValue(this.rsaModulus, modulus,
								LongElement.CHUNK_BITWIDTH);
						evaluator.setWireValue(this.signature, sig,
								LongElement.CHUNK_BITWIDTH);

					} catch (Exception e) {
						System.err
								.println("Error while generating sample input for circuit");
						e.printStackTrace();
					}
				}
			};

			generator.generateCircuit();
			generator.evalCircuit();
			CircuitEvaluator evaluator = generator.getCircuitEvaluator();
			assertEquals(BigInteger.ONE,
					evaluator.getWireValue(generator.getOutWires().get(0)));
		}

	}

	@Test
	public void testInvalidSignatureDifferentKeyLengths() {

		
		String inputStr = "abc";

		// testing commonly used rsa key lengths in addition to non-power of two
		// ones


		int[] keySizeArray = new int[] { 1024, 2048, 3072, 4096, 2047, 2049 };

		for (int keySize : keySizeArray) {
			CircuitGenerator generator = new CircuitGenerator("RSA" + keySize
					+ "_SIG_TestInvalid") {

				int rsaKeyLength = keySize;
				Wire[] inputMessage;
				LongElement signature;
				LongElement rsaModulus;

				SHA256Gadget sha2Gadget;
				RSASigVerificationV1_5_Gadget rsaSigVerificationV1_5_Gadget;

				@Override
				protected void buildCircuit() {
					inputMessage = createInputWireArray(inputStr.length());
					sha2Gadget = new SHA256Gadget(inputMessage, 8,
							inputMessage.length, false, true);
					Wire[] digest = sha2Gadget.getOutputWires();
					rsaModulus = createLongElementInput(rsaKeyLength);
					signature = createLongElementInput(rsaKeyLength);
					rsaSigVerificationV1_5_Gadget = new RSASigVerificationV1_5_Gadget(
							rsaModulus, digest, signature, rsaKeyLength);
					makeOutput(rsaSigVerificationV1_5_Gadget.getOutputWires()[0]);
				}

				@Override
				public void generateSampleInput(CircuitEvaluator evaluator) {

					for (int i = 0; i < inputMessage.length; i++) {
						evaluator.setWireValue(inputMessage[i],
								inputStr.charAt(i));
					}
					try {
						KeyPairGenerator keyGen = KeyPairGenerator
								.getInstance("RSA");
						keyGen.initialize(rsaKeyLength, new SecureRandom());
						KeyPair keyPair = keyGen.generateKeyPair();
						Signature signature = Signature
								.getInstance("SHA256withRSA");
						signature.initSign(keyPair.getPrivate());

						byte[] message = inputStr.getBytes();
						signature.update(message);

						byte[] sigBytes = signature.sign();

						// pad an extra zero byte to avoid having a negative big
						// integer
						byte[] signaturePadded = new byte[sigBytes.length + 1];
						System.arraycopy(sigBytes, 0, signaturePadded, 1,
								sigBytes.length);
						signaturePadded[0] = 0;
						BigInteger modulus = ((RSAPublicKey) keyPair
								.getPublic()).getModulus();
						BigInteger sig = new BigInteger(signaturePadded);

						evaluator.setWireValue(this.rsaModulus, modulus,
								LongElement.CHUNK_BITWIDTH);

						// input the modulus itself instead of the signature
						evaluator.setWireValue(this.signature, sig.subtract(BigInteger.ONE),
								LongElement.CHUNK_BITWIDTH);

					} catch (Exception e) {
						System.err
								.println("Error while generating sample input for circuit");
						e.printStackTrace();
					}
				}
			};

			generator.generateCircuit();
			generator.evalCircuit();
			CircuitEvaluator evaluator = generator.getCircuitEvaluator();
			assertEquals(BigInteger.ZERO,
					evaluator.getWireValue(generator.getOutWires().get(0)));
		}

	}
	
	// This test checks the robustness of the code when the chunk bitwidth changes
	
	@Test
	public void testValidSignatureDifferentChunkBitwidth() {

		String inputStr = "abc";

		int keySize = 1024;
		int defaultBitwidth = LongElement.CHUNK_BITWIDTH ;

		int[] chunkBiwidthArray = new int[106];
		for(int b = 16; b-16 < chunkBiwidthArray.length; b++){
			
			LongElement.CHUNK_BITWIDTH = b;
			CircuitGenerator generator = new CircuitGenerator("RSA" + keySize
					+ "_SIG_TestValid_ChunkB_"+b) {

				int rsaKeyLength = keySize;
				Wire[] inputMessage;
				LongElement signature;
				LongElement rsaModulus;

				SHA256Gadget sha2Gadget;
				RSASigVerificationV1_5_Gadget rsaSigVerificationV1_5_Gadget;

				@Override
				protected void buildCircuit() {
					inputMessage = createInputWireArray(inputStr.length());
					sha2Gadget = new SHA256Gadget(inputMessage, 8,
							inputMessage.length, false, true);
					Wire[] digest = sha2Gadget.getOutputWires();
					rsaModulus = createLongElementInput(rsaKeyLength);
					signature = createLongElementInput(rsaKeyLength);
					rsaSigVerificationV1_5_Gadget = new RSASigVerificationV1_5_Gadget(
							rsaModulus, digest, signature, rsaKeyLength);
					makeOutput(rsaSigVerificationV1_5_Gadget.getOutputWires()[0]);
				}

				@Override
				public void generateSampleInput(CircuitEvaluator evaluator) {

					for (int i = 0; i < inputMessage.length; i++) {
						evaluator.setWireValue(inputMessage[i],
								inputStr.charAt(i));
					}
					try {
						KeyPairGenerator keyGen = KeyPairGenerator
								.getInstance("RSA");
						keyGen.initialize(rsaKeyLength, new SecureRandom());
						KeyPair keyPair = keyGen.generateKeyPair();
						Signature signature = Signature
								.getInstance("SHA256withRSA");
						signature.initSign(keyPair.getPrivate());

						byte[] message = inputStr.getBytes();
						signature.update(message);

						byte[] sigBytes = signature.sign();

						// pad an extra zero byte to avoid having a negative big
						// integer
						byte[] signaturePadded = new byte[sigBytes.length + 1];
						System.arraycopy(sigBytes, 0, signaturePadded, 1,
								sigBytes.length);
						signaturePadded[0] = 0;
						BigInteger modulus = ((RSAPublicKey) keyPair
								.getPublic()).getModulus();
						BigInteger sig = new BigInteger(signaturePadded);

						evaluator.setWireValue(this.rsaModulus, modulus,
								LongElement.CHUNK_BITWIDTH);
						evaluator.setWireValue(this.signature, sig,
								LongElement.CHUNK_BITWIDTH);

					} catch (Exception e) {
						System.err
								.println("Error while generating sample input for circuit");
						e.printStackTrace();
					}
				}
			};

			generator.generateCircuit();
			generator.evalCircuit();
			CircuitEvaluator evaluator = generator.getCircuitEvaluator();
			assertEquals(BigInteger.ONE,
					evaluator.getWireValue(generator.getOutWires().get(0)));
			
			LongElement.CHUNK_BITWIDTH = defaultBitwidth; // needed for running all tests together
		}

	}

}
