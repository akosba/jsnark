/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.generators.rsa;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;

import circuit.auxiliary.LongElement;
import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import examples.gadgets.hash.SHA256Gadget;
import examples.gadgets.rsa.RSASigVerificationV1_5_Gadget;

//a demo for RSA Signatures PKCS #1, V1.5
public class RSASigVerCircuitGenerator extends CircuitGenerator {

	private int rsaKeyLength;
	private Wire[] inputMessage;
	private LongElement signature;
	private LongElement rsaModulus;

	private SHA256Gadget sha2Gadget;
	private RSASigVerificationV1_5_Gadget rsaSigVerificationV1_5_Gadget;

	public RSASigVerCircuitGenerator(String circuitName, int rsaKeyLength) {
		super(circuitName);
		this.rsaKeyLength = rsaKeyLength;
	}

	@Override
	protected void buildCircuit() {

		// a sample input message of 3 byte
		inputMessage = createInputWireArray(3);
		sha2Gadget = new SHA256Gadget(inputMessage, 8, inputMessage.length,
				false, true);
		Wire[] digest = sha2Gadget.getOutputWires();

		/**
		 * Since an RSA modulus take many wires to present, it could increase
		 * the size of verification key if we divide it into very small chunks,
		 * e.g. 32-bits (which happens by default in this version to minimize
		 * the number of gates later in the circuit). In case the verification
		 * key size is important, e.g. going to be stored in a smart contract,
		 * there is a workaround, by first assuming the largest possible
		 * bitwidths for the chunks, and then converting them into smaller
		 * chunks. Even better, let the prover provide the key as a witness to
		 * the circuit, and compute their hash, which will be part of the
		 * statement. This way of doing this increases the number of gates a
		 * bit, but reduces the VK size when needed.
		 * 
		 **/

		rsaModulus = createLongElementInput(rsaKeyLength);

		// The modulus can also be hardcoded by changing the statement above to the following

		// rsaModulus = new LongElement(Util.split(new
		// BigInteger("f0dac4df56945ec31a037c5b736b64192f14baf27f2036feb85dfe45dc99d8d3c024e226e6fd7cabb56f780f9289c000a873ce32c66f4c1b2970ae6b7a3ceb2d7167fbbfe41f7b0ed7a07e3c32f14c3940176d280ceb25ed0bf830745a9425e1518f27de822b17b2b599e0aea7d72a2a6efe37160e46bf7c78b0573c9014380ab7ec12ce272a83aaa464f814c08a0b0328e191538fefaadd236ae10ba9cbb525df89da59118c7a7b861ec1c05e09976742fc2d08bd806d3715e702d9faa3491a3e4cf76b5546f927e067b281c25ddc1a21b1fb12788d39b27ca0052144ab0aad7410dc316bd7e9d2fe5e0c7a1028102454be9c26c3c347dd93ee044b680c93cb",
		// 16), LongElement.CHUNK_BITWIDTH));
		
		// In case of hardcoding the modulus, comment the line that sets the modulus value in generateSampleInput() to avoid an exception

		signature = createLongElementProverWitness(rsaKeyLength);

		// since the signature is provided as a witness, verify some properties
		// about it
		signature.restrictBitwidth();
		signature.assertLessThan(rsaModulus); // might not be really necessary in that
										      // case

		rsaSigVerificationV1_5_Gadget = new RSASigVerificationV1_5_Gadget(
				rsaModulus, digest, signature, rsaKeyLength);
		makeOutput(rsaSigVerificationV1_5_Gadget.getOutputWires()[0],
				"Is Signature valid?");

	}

	@Override
	public void generateSampleInput(CircuitEvaluator evaluator) {
		String inputStr = "abc";
		for (int i = 0; i < inputMessage.length; i++) {
			evaluator.setWireValue(inputMessage[i], inputStr.charAt(i));
		}

		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(rsaKeyLength, new SecureRandom());
			KeyPair keyPair = keyGen.generateKeyPair();

			Signature signature = Signature.getInstance("SHA256withRSA");
			signature.initSign(keyPair.getPrivate());

			byte[] message = inputStr.getBytes();
			signature.update(message);

			byte[] sigBytes = signature.sign();
			byte[] signaturePadded = new byte[sigBytes.length + 1];
			System.arraycopy(sigBytes, 0, signaturePadded, 1, sigBytes.length);
			signaturePadded[0] = 0;
			BigInteger modulus = ((RSAPublicKey) keyPair.getPublic())
					.getModulus();
//			System.out.println(modulus.toString(16));
			BigInteger sig = new BigInteger(signaturePadded);

			// if (!minimizeVerificationKey) {
			evaluator.setWireValue(this.rsaModulus, modulus,
					LongElement.CHUNK_BITWIDTH);
			evaluator.setWireValue(this.signature, sig,
					LongElement.CHUNK_BITWIDTH);
			// } else {
			// evaluator.setWireValue(this.rsaModulusWires,
			// Util.split(modulus, Config.LOG2_FIELD_PRIME - 1));
			// evaluator.setWireValue(this.signatureWires,
			// Util.split(sig, Config.LOG2_FIELD_PRIME - 1));
			// }
		} catch (Exception e) {
			System.err
					.println("Error while generating sample input for circuit");
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws Exception {
		int keyLength = 2048;
		RSASigVerCircuitGenerator generator = new RSASigVerCircuitGenerator(
				"rsa" + keyLength + "_sha256_sig_verify", keyLength);
		generator.generateCircuit();
		generator.evalCircuit();
		generator.prepFiles();
		generator.runLibsnark();
	}

}
