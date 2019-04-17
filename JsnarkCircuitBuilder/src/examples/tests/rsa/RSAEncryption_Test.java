/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.tests.rsa;

import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;

import javax.crypto.Cipher;

import junit.framework.TestCase;

import org.junit.Test;

import circuit.auxiliary.LongElement;
import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import circuit.structure.WireArray;
import examples.gadgets.rsa.RSAEncryptionV1_5_Gadget;
import examples.generators.rsa.RSAUtil;


// Tests RSA PKCS #1, V1.5

public class RSAEncryption_Test extends TestCase {

	
	@Test
	public void testEncryptionDifferentKeyLengths() throws Exception{

		
		String plainText = "abc";

		// testing commonly used rsa key lengths

		// might need to increase memory heap to run this test on some platforms
		
		int[] keySizeArray = new int[] { 1024, 2048, 3072, 4096};

		for (int keySize : keySizeArray) {

			final byte[] cipherTextBytes = new byte[keySize/8];			
			SecureRandom random = new SecureRandom();
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(keySize, random);
			KeyPair keyPair = keyGen.generateKeyPair();
			Key pubKey = keyPair.getPublic();
			BigInteger rsaModulusValue = ((RSAPublicKey) pubKey).getModulus();
			
			CircuitGenerator generator = new CircuitGenerator("RSA" + keySize
					+ "_Enc_TestEncryption") {

				 int rsaKeyLength = keySize;
				 int plainTextLength = plainText.length();
				 Wire[] inputMessage;
				 Wire[] randomness;
				 Wire[] cipherText;
				 LongElement rsaModulus;

				 RSAEncryptionV1_5_Gadget rsaEncryptionV1_5_Gadget;

				@Override
				protected void buildCircuit() {
					inputMessage = createProverWitnessWireArray(plainTextLength); // in bytes
					for(int i = 0; i < plainTextLength;i++){
						inputMessage[i].restrictBitLength(8);
					}
					
					rsaModulus = createLongElementInput(rsaKeyLength);
					randomness = createProverWitnessWireArray(RSAEncryptionV1_5_Gadget
							.getExpectedRandomnessLength(rsaKeyLength, plainTextLength));
					rsaEncryptionV1_5_Gadget = new RSAEncryptionV1_5_Gadget(rsaModulus, inputMessage,
							randomness, rsaKeyLength);
					
					// since randomness is a witness
					rsaEncryptionV1_5_Gadget.checkRandomnessCompliance();
					Wire[] cipherTextInBytes = rsaEncryptionV1_5_Gadget.getOutputWires(); // in bytes
					
					// group every 8 bytes together
					cipherText = new WireArray(cipherTextInBytes).packWordsIntoLargerWords(8, 8);
					makeOutputArray(cipherText,
							"Output cipher text");
				}

				@Override
				public void generateSampleInput(CircuitEvaluator evaluator) {

					for (int i = 0; i < inputMessage.length; i++) {
						evaluator.setWireValue(inputMessage[i],
								plainText.charAt(i));
					}
					try {
						Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
						evaluator.setWireValue(this.rsaModulus, rsaModulusValue,
								LongElement.CHUNK_BITWIDTH);

						Key privKey = keyPair.getPrivate();

						cipher.init(Cipher.ENCRYPT_MODE, pubKey, random);
						byte[] tmp = cipher.doFinal(plainText.getBytes());
						System.arraycopy(tmp, 0, cipherTextBytes, 0, keySize/8);
						
						byte[] cipherTextPadded = new byte[cipherTextBytes.length + 1];
						System.arraycopy(cipherTextBytes, 0, cipherTextPadded, 1, cipherTextBytes.length);
						cipherTextPadded[0] = 0;

						byte[][] result = RSAUtil.extractRSARandomness1_5(cipherTextBytes,
								(RSAPrivateKey) privKey);

						boolean check = Arrays.equals(result[0], plainText.getBytes());
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
			};

			generator.generateCircuit();
			generator.evalCircuit();
			CircuitEvaluator evaluator = generator.getCircuitEvaluator();
			
			// retrieve the ciphertext from the circuit, and verify that it matches the expected ciphertext and that it decrypts correctly (using the Java built-in RSA decryptor)
			ArrayList<Wire> cipherTextList = generator.getOutWires();
			BigInteger t = BigInteger.ZERO;
			int i = 0;
			for(Wire w:cipherTextList){
				BigInteger val = evaluator.getWireValue(w);
				t = t.add(val.shiftLeft(i*64));
				i++;
			}
		
			// extract the bytes
			byte[] cipherTextBytesFromCircuit = t.toByteArray();

			// ignore the sign byte if any was added
			if(t.bitLength() == keySize && cipherTextBytesFromCircuit.length == keySize/8+1){
				cipherTextBytesFromCircuit=Arrays.copyOfRange(cipherTextBytesFromCircuit, 1, cipherTextBytesFromCircuit.length);
			}
			
			for(int k = 0; k < cipherTextBytesFromCircuit.length;k++){
				assertEquals(cipherTextBytes[k], cipherTextBytesFromCircuit[k]);

			}
			
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
			byte[] cipherTextDecrypted = cipher.doFinal(cipherTextBytesFromCircuit);
			assertTrue(Arrays.equals(plainText.getBytes(), cipherTextDecrypted));
		}

	}
}
