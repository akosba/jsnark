/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.generators.rsa;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

import javax.crypto.Cipher;

import circuit.auxiliary.LongElement;
import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import circuit.structure.WireArray;
import examples.gadgets.rsa.RSAEncryptionV1_5_Gadget;


// a demo for RSA Encryption PKCS #1, V1.5
public class RSAEncryptionCircuitGenerator extends CircuitGenerator {

	private int rsaKeyLength;
	private int plainTextLength;
	private Wire[] inputMessage;
	private Wire[] randomness;
	private Wire[] cipherText;
	private LongElement rsaModulus;

	private RSAEncryptionV1_5_Gadget rsaEncryptionV1_5_Gadget;

	public RSAEncryptionCircuitGenerator(String circuitName, int rsaKeyLength,
			int plainTextLength) {
		super(circuitName);
		this.rsaKeyLength = rsaKeyLength;
		this.plainTextLength = plainTextLength;
		// constraints on the plaintext length will be checked by the gadget
	}

	@Override
	protected void buildCircuit() {

		inputMessage = createProverWitnessWireArray(plainTextLength); // in bytes
		for(int i = 0; i < plainTextLength;i++){
			inputMessage[i].restrictBitLength(8);
		}
		
		randomness = createProverWitnessWireArray(RSAEncryptionV1_5_Gadget
				.getExpectedRandomnessLength(rsaKeyLength, plainTextLength));
		// constraints on the randomness vector are checked later.

		
		/**
		 * Since an RSA modulus take many wires to present, it could increase
		 * the size of verification key if we divide it into very small chunks,
		 * e.g. 32-bits (which happens by default in this version to minimize
		 * the number of gates later in the circuit). In case the verification
		 * key size is important, e.g. going to be stored in a smart contract, a
		 * possible workaround could be by either assuming the largest possible
		 * bitwidths for the chunks, and then converting them into smaller
		 * chunks, or let the prover provide the key as a witness to the
		 * circuit, and compute its hash, which will be part of the statement.
		 * This way of doing this increases the number of gates a bit, but
		 * reduces the VK size when crucial.
		 * 
		 **/

		rsaModulus = createLongElementInput(rsaKeyLength);

		// The modulus can also be hardcoded by changing the statement above to the following

		// rsaModulus = new LongElement(Util.split(new
		// BigInteger("f0dac4df56945ec31a037c5b736b64192f14baf27f2036feb85dfe45dc99d8d3c024e226e6fd7cabb56f780f9289c000a873ce32c66f4c1b2970ae6b7a3ceb2d7167fbbfe41f7b0ed7a07e3c32f14c3940176d280ceb25ed0bf830745a9425e1518f27de822b17b2b599e0aea7d72a2a6efe37160e46bf7c78b0573c9014380ab7ec12ce272a83aaa464f814c08a0b0328e191538fefaadd236ae10ba9cbb525df89da59118c7a7b861ec1c05e09976742fc2d08bd806d3715e702d9faa3491a3e4cf76b5546f927e067b281c25ddc1a21b1fb12788d39b27ca0052144ab0aad7410dc316bd7e9d2fe5e0c7a1028102454be9c26c3c347dd93ee044b680c93cb",
		// 16), LongElement.CHUNK_BITWIDTH));
		
		// In case of hardcoding the modulus, comment the line that sets the modulus value in generateSampleInput() to avoid an exception

		rsaEncryptionV1_5_Gadget = new RSAEncryptionV1_5_Gadget(rsaModulus, inputMessage,
				randomness, rsaKeyLength);
				
		// since the randomness vector is a witness in this example, verify any needed constraints
		rsaEncryptionV1_5_Gadget.checkRandomnessCompliance();
		
		Wire[] cipherTextInBytes = rsaEncryptionV1_5_Gadget.getOutputWires(); // in bytes
		
		// do some grouping to reduce VK Size	
		cipherText = new WireArray(cipherTextInBytes).packWordsIntoLargerWords(8, 30);
		makeOutputArray(cipherText,
				"Output cipher text");

	}

	@Override
	public void generateSampleInput(CircuitEvaluator evaluator) {

		String msg = "";
		for (int i = 0; i < inputMessage.length; i++) {

			evaluator.setWireValue(inputMessage[i], (int) ('a' + i));
			msg = msg + (char) ('a' + i);
		}
		System.out.println("PlainText:" + msg);

		try {

			// to make sure that the implementation is working fine,
			// encrypt with the underlying java implementation for RSA
			// Encryption in a sample run,
			// extract the randomness (after decryption manually), then run the
			// circuit with the extracted randomness

			SecureRandom random = new SecureRandom();
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(rsaKeyLength, random);
			KeyPair pair = generator.generateKeyPair();
			Key pubKey = pair.getPublic();
			BigInteger modulus = ((RSAPublicKey) pubKey).getModulus();

			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			evaluator.setWireValue(this.rsaModulus, modulus,
					LongElement.CHUNK_BITWIDTH);

			Key privKey = pair.getPrivate();

			cipher.init(Cipher.ENCRYPT_MODE, pubKey, random);
			byte[] cipherText = cipher.doFinal(msg.getBytes());
//			System.out.println("ciphertext : " + new String(cipherText));
			byte[] cipherTextPadded = new byte[cipherText.length + 1];
			System.arraycopy(cipherText, 0, cipherTextPadded, 1, cipherText.length);
			cipherTextPadded[0] = 0;

			byte[][] result = RSAUtil.extractRSARandomness1_5(cipherText,
					(RSAPrivateKey) privKey);
			// result[0] contains the plaintext (after decryption)
			// result[1] contains the randomness

			boolean check = Arrays.equals(result[0], msg.getBytes());
			if (!check) {
				throw new RuntimeException(
						"Randomness Extraction did not decrypt right");
			}

			byte[] sampleRandomness = result[1];
			for (int i = 0; i < sampleRandomness.length; i++) {
				evaluator.setWireValue(randomness[i], (sampleRandomness[i]+256)%256);
			}

		} catch (Exception e) {
			System.err
					.println("Error while generating sample input for circuit");
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws Exception {
		int keyLength = 2048;
		int msgLength = 3;
		RSAEncryptionCircuitGenerator generator = new RSAEncryptionCircuitGenerator(
				"rsa" + keyLength + "_encryption", keyLength, msgLength);
		generator.generateCircuit();
		generator.evalCircuit();
		generator.prepFiles();
		generator.runLibsnark();
	}

}
