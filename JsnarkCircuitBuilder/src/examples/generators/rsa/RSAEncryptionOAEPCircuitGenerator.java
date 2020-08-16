/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.generators.rsa;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;

import javax.crypto.Cipher;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import circuit.auxiliary.LongElement;
import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import circuit.structure.WireArray;
import examples.gadgets.rsa.RSAEncryptionOAEPGadget;

public class RSAEncryptionOAEPCircuitGenerator extends CircuitGenerator {

	private int rsaKeyLength;
	private int plainTextLength;
	private Wire[] inputMessage;
	private Wire[] seed;
	private Wire[] cipherText;
	private LongElement rsaModulus;

	private RSAEncryptionOAEPGadget rsaEncryptionOAEPGadget;

	public RSAEncryptionOAEPCircuitGenerator(String circuitName, int rsaKeyLength,
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
		
		seed = createProverWitnessWireArray(RSAEncryptionOAEPGadget.SHA256_DIGEST_LENGTH);
		// constraints on the seed are checked later.
		
		rsaModulus = createLongElementInput(rsaKeyLength);

		// The modulus can also be hardcoded by changing the statement above to the following

		// rsaModulus = new LongElement(Util.split(new
		// BigInteger("f0dac4df56945ec31a037c5b736b64192f14baf27f2036feb85dfe45dc99d8d3c024e226e6fd7cabb56f780f9289c000a873ce32c66f4c1b2970ae6b7a3ceb2d7167fbbfe41f7b0ed7a07e3c32f14c3940176d280ceb25ed0bf830745a9425e1518f27de822b17b2b599e0aea7d72a2a6efe37160e46bf7c78b0573c9014380ab7ec12ce272a83aaa464f814c08a0b0328e191538fefaadd236ae10ba9cbb525df89da59118c7a7b861ec1c05e09976742fc2d08bd806d3715e702d9faa3491a3e4cf76b5546f927e067b281c25ddc1a21b1fb12788d39b27ca0052144ab0aad7410dc316bd7e9d2fe5e0c7a1028102454be9c26c3c347dd93ee044b680c93cb",
		// 16), LongElement.CHUNK_BITWIDTH));

		// In case of hardcoding, comment the line that sets the modulus value in generateSampleInput() 

		
		rsaEncryptionOAEPGadget = new RSAEncryptionOAEPGadget(rsaModulus, inputMessage,
				seed, rsaKeyLength);
		
		// since seed is a witness in this example, verify any needed constraints
		// If the key or the msg are witnesses, similar constraints are needed
		rsaEncryptionOAEPGadget.checkSeedCompliance();
		
		Wire[] cipherTextInBytes = rsaEncryptionOAEPGadget.getOutputWires(); // in bytes
		
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
			// encrypt with the BouncyCastle RSA OAEP encryption in a sample run,
			// extract the seed (after decryption manually), then run the
			// circuit with the extracted seed
			
			// The BouncyCastle implementation is used at is supports SHA-256 for the MGF, while the native Java implementation uses SHA-1 by default.

			Security.addProvider(new BouncyCastleProvider());  
			Cipher cipher = Cipher
					.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding", "BC");
			
			SecureRandom random = new SecureRandom();
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(rsaKeyLength, random);
			KeyPair pair = generator.generateKeyPair();
			Key pubKey = pair.getPublic();
			BigInteger modulus = ((RSAPublicKey) pubKey).getModulus();

			evaluator.setWireValue(this.rsaModulus, modulus,
					LongElement.CHUNK_BITWIDTH);

			Key privKey = pair.getPrivate();

			cipher.init(Cipher.ENCRYPT_MODE, pubKey, random);
			byte[] cipherText = cipher.doFinal(msg.getBytes());
//			System.out.println("ciphertext : " + new String(cipherText));
			byte[] cipherTextPadded = new byte[cipherText.length + 1];
			System.arraycopy(cipherText, 0, cipherTextPadded, 1, cipherText.length);
			cipherTextPadded[0] = 0;


			byte[][] result = RSAUtil.extractRSAOAEPSeed(cipherText,
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
				evaluator.setWireValue(seed[i], (sampleRandomness[i]+256)%256);
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
		RSAEncryptionOAEPCircuitGenerator generator = new RSAEncryptionOAEPCircuitGenerator(
				"rsa" + keyLength + "_oaep_encryption", keyLength, msgLength);
		generator.generateCircuit();
		generator.evalCircuit();
		generator.prepFiles();
		generator.runLibsnark();
	}

}
