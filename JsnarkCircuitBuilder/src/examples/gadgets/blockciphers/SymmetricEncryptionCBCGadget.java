/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/

package examples.gadgets.blockciphers;

import java.util.Arrays;

import util.Util;
import circuit.operations.Gadget;
import circuit.structure.Wire;
import circuit.structure.WireArray;

/**
 * Performs symmetric encryption in the CBC mode. 
 * Only supports one cipher (speck128) as an example at the moment. Other ciphers will be integrated soon.
 *
 */
public class SymmetricEncryptionCBCGadget extends Gadget {

	private Wire[] ciphertext;
	private String cipherName;

	private Wire[] keyBits;
	private Wire[] plaintextBits;
	private Wire[] ivBits;

	private int blocksize = 128;
	private int keysize = 128;

	public SymmetricEncryptionCBCGadget(Wire[] plaintextBits, Wire[] keyBits,
			Wire[] ivBits, String cipherName, String... desc) {
		super(desc);
		if(keyBits.length != keysize || ivBits.length != keysize){
			throw new IllegalArgumentException("Key and IV bit vectors should be of length 128");
		}
		this.plaintextBits = plaintextBits;
		this.ivBits = ivBits;
		this.keyBits = keyBits;
		this.cipherName = cipherName;
		buildCircuit();
	}

	protected void buildCircuit() {

		int numBlocks = (int) Math.ceil(plaintextBits.length * 1.0 / blocksize);
		plaintextBits = new WireArray(plaintextBits).adjustLength(numBlocks * blocksize)
				.asArray();

		Wire[] preparedKey = prepareKey();
		WireArray prevCipher = new WireArray(ivBits);

		ciphertext = new Wire[0];
		for (int i = 0; i < numBlocks; i++) {
			WireArray msgBlock = new WireArray(Arrays.copyOfRange(plaintextBits, i
					* blocksize, (i + 1) * blocksize));
			Wire[] xored = msgBlock.xorWireArray(prevCipher).asArray();
			if (cipherName.equals("speck128")) {
				Wire[] tmp = new WireArray(xored).packBitsIntoWords(64);
				Gadget gadget = new Speck128CipherGadget(tmp, preparedKey, "");
				Wire[] outputs = gadget.getOutputWires();
				prevCipher = new WireArray(outputs).getBits(64);
			} else {
				throw new UnsupportedOperationException("Other Ciphers not supported in this version!");
			}
			ciphertext = Util.concat(ciphertext,
					prevCipher.packBitsIntoWords(64));
		}
	}

	private Wire[] prepareKey() {

		Wire[] preparedKey;
		if (cipherName.equals("speck128")) {
			Wire[] packedKey = new WireArray(keyBits).packBitsIntoWords(64);
			preparedKey = Speck128CipherGadget.expandKey(packedKey);
		} else {
			throw new UnsupportedOperationException("Other Ciphers not supported in this version!");
		}
		return preparedKey;
	}

	@Override
	public Wire[] getOutputWires() {
		return ciphertext;
	}

}
