/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.structure;

import java.math.BigInteger;
import java.util.Arrays;

import util.Util;
import circuit.eval.Instruction;
import circuit.operations.primitive.AddBasicOp;
import circuit.operations.primitive.PackBasicOp;

public class WireArray {

	protected Wire[] array;
	protected CircuitGenerator generator;

	public WireArray(int n) {
		this(n, CircuitGenerator.getActiveCircuitGenerator());
	}
	
	public WireArray(int n, CircuitGenerator generator) {
		array = new Wire[n];
		this.generator = generator;
	}
	
	public WireArray(Wire[] wireArray) {
		this(wireArray, CircuitGenerator.getActiveCircuitGenerator());
	}
	
	public WireArray(Wire[] wireArray, CircuitGenerator generator) {
		this.array = wireArray;
		this.generator = generator;
	}
	
	public Wire get(int i){
		return array[i];
	}
	
	public void set(int i, Wire w){
		array[i] = w;
	}
	
	public int size(){
		return array.length;
	}
	
	public Wire[] asArray(){
		return array;
	}
	
	public WireArray mulWireArray(WireArray v, int desiredLength, String...desc) {
		Wire[] ws1 = adjustLength( array, desiredLength);
		Wire[] ws2 = adjustLength( v.array, desiredLength);
		Wire[] out = new Wire[desiredLength];
		for (int i = 0; i < out.length; i++) {
			out[i] = ws1[i].mul(ws2[i], desc);
		}
		return new WireArray(out);
	}
	
	
	public Wire sumAllElements(String...desc) {
		boolean allConstant = true;
		Wire output;
		BigInteger sum = BigInteger.ZERO;
		for (Wire w : array) {
			if (!(w instanceof ConstantWire)) {
				allConstant = false;
				break;
			} else {
				sum = sum.add(((ConstantWire) w).getConstant());
			}
		}
		if (allConstant) {
			output = generator.createConstantWire(sum, desc);
		} else {
			output = new LinearCombinationWire(generator.currentWireId++);
			Instruction op = new AddBasicOp(array, output, desc);
//			generator.addToEvaluationQueue(op);
			Wire[] cachedOutputs = generator.addToEvaluationQueue(op);
			if(cachedOutputs == null){
				return output;
			}
			else{
				generator.currentWireId--;
				return cachedOutputs[0];
			}	
		}
		return output;
	}
	
	
	public WireArray addWireArray(WireArray v, int desiredLength, String...desc) {
		Wire[] ws1 = adjustLength(array, desiredLength);
		Wire[] ws2 = adjustLength( v.array, desiredLength);
		Wire[] out = new Wire[desiredLength];
		for (int i = 0; i < out.length; i++) {
			out[i] = ws1[i].add(ws2[i], desc);
		}
		return new WireArray(out);
	}
	
	public WireArray xorWireArray(WireArray v, int desiredLength, String...desc) {
		Wire[] ws1 = adjustLength(array, desiredLength);
		Wire[] ws2 = adjustLength(v.array, desiredLength);
		Wire[] out = new Wire[desiredLength];
		for (int i = 0; i < out.length; i++) {
			out[i] = ws1[i].xor(ws2[i], desc);
		}
		return new WireArray(out);
	}
	
	public WireArray xorWireArray(WireArray v, String...desc) {
		if(size() != v.size()){
			throw new IllegalArgumentException();
		}
		Wire[] ws1 = array;
		Wire[] ws2 = v.array;
		
		Wire[] out = new Wire[size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = ws1[i].xor(ws2[i], desc);
		}
		return new WireArray(out);
	}
	
	public WireArray andWireArray(WireArray v, int desiredLength, String...desc) {
		Wire[] ws1 = adjustLength( array, desiredLength);
		Wire[] ws2 = adjustLength( v.array, desiredLength);
		Wire[] out = new Wire[desiredLength];
		for (int i = 0; i < out.length; i++) {
			out[i] = ws1[i].mul(ws2[i], desc);
		}
		return new WireArray(out);
	}
	
	public WireArray orWireArray(WireArray v, int desiredLength, String...desc) {
		Wire[] ws1 = adjustLength(array, desiredLength);
		Wire[] ws2 = adjustLength(v.array, desiredLength);
		Wire[] out = new Wire[desiredLength];
		for (int i = 0; i < out.length; i++) {
			out[i] = ws1[i].or(ws2[i], desc);
		}
		return new WireArray(out);
	}
	
	
	
	public WireArray invAsBits(int desiredBitWidth, String...desc) {
		Wire[] out = new Wire[desiredBitWidth];
		for(int i = 0; i < desiredBitWidth; i++){
			if(i < array.length){
				out[i] = array[i].invAsBit(desc);
			}
			else{
				out[i] = generator.oneWire;
			}
		}
		return new WireArray(out);
	}	
	
	
	private Wire[] adjustLength(Wire[] ws, int desiredLength) {
		if(ws.length == desiredLength){
			return ws;
		}
		Wire[] newWs = new Wire[desiredLength];
		System.arraycopy(ws, 0, newWs, 0, Math.min(ws.length, desiredLength));
		if (ws.length < desiredLength) {
			for (int i = ws.length; i < desiredLength; i++) {
				newWs[i] = generator.zeroWire;
			}
		}
		return newWs;
	}
	
	public WireArray adjustLength(int desiredLength) {
		if(array.length == desiredLength){
			return this;
		}
		Wire[] newWs = new Wire[desiredLength];
		System.arraycopy(array, 0, newWs, 0, Math.min(array.length, desiredLength));
		if (array.length < desiredLength) {
			for (int i = array.length; i < desiredLength; i++) {
				newWs[i] = generator.zeroWire;
			}
		}
		return new WireArray(newWs);
	}
	
	
	
	public Wire packAsBits(int n, String...desc) {
		return packAsBits(0, n, desc);
	}
	
	public Wire packAsBits(String...desc) {
		return packAsBits(array.length, desc);
	}
	
	protected BigInteger checkIfConstantBits(String...desc){
		boolean allConstant = true;
		BigInteger sum = BigInteger.ZERO;
		for(int i = 0; i < array.length; i++){
			Wire w = array[i];
			if(w instanceof ConstantWire){
				ConstantWire cw = (ConstantWire)w;
				BigInteger v = cw.constant;
				if(v.equals(BigInteger.ONE)){
					sum = sum.add(v.shiftLeft(i));
				}
				else if (!v.equals(BigInteger.ZERO)){
					System.err.println("Warning, one of the bit wires is constant but not binary : " + Util.getDesc(desc));					
				}
				
			}
			else{
				allConstant = false;
			}
		}
		if(allConstant)
			return sum;
		else
			return null;
	}

	public Wire packAsBits(int from, int to, String...desc) {
		
		if (from > to || to > array.length)
			throw new IllegalArgumentException("Invalid bounds: from > to");
		
		Wire[] bits = Arrays.copyOfRange(array, from, to);
		boolean allConstant = true;
		BigInteger sum = BigInteger.ZERO;
		for(int i = 0; i < bits.length; i++){
			Wire w = bits[i];
			if(w instanceof ConstantWire){
				ConstantWire cw = (ConstantWire)w;
				BigInteger v = cw.constant;
				if(v.equals(BigInteger.ONE)){
					sum = sum.add(v.shiftLeft(i));
				}
				else if (!v.equals(BigInteger.ZERO)){
					throw new RuntimeException("Trying to pack non-binary constant bits : " + Util.getDesc(desc));					
				}
				
			}
			else{
				allConstant = false;
			}
		}
		if(!allConstant){
			Wire out = new LinearCombinationWire(generator.currentWireId++);
			out.setBits(new WireArray(bits));
			Instruction op = new PackBasicOp(bits, out, desc);
			Wire[] cachedOutputs = generator.addToEvaluationQueue(op);
			if(cachedOutputs == null){
				return out;		
			}
			else{
				generator.currentWireId--;
				return cachedOutputs[0];
			}
		} else{
			return generator.createConstantWire(sum, desc);

		}
	}
	
	
	public WireArray rotateLeft(int numBits, int s, String...desc) {
		Wire[] bits = adjustLength(array, numBits);
		Wire[] rotatedBits = new Wire[numBits];
		for (int i = 0; i < numBits; i++) {
			if (i < s)
				rotatedBits[i] = bits[i + (numBits - s)];
			else
				rotatedBits[i] = bits[i - s];
		}
		return new WireArray(rotatedBits);
	}
	
	public WireArray rotateRight(int numBits, int s, String...desc) {
		Wire[] bits = adjustLength(array, numBits);
		Wire[] rotatedBits = new Wire[numBits];
		for (int i = 0; i < numBits; i++) {
			if (i >= numBits - s)
				rotatedBits[i] = bits[i - (numBits - s)];
			else
				rotatedBits[i] = bits[i + s];
		}
		return new WireArray(rotatedBits);
	}
	
	

	public WireArray shiftLeft(int numBits, int s, String...desc) {
		Wire[] bits = adjustLength( array, numBits);
		Wire[] shiftedBits = new Wire[numBits];
		for (int i = 0; i < numBits; i++) {
			if (i < s)
				shiftedBits[i] = generator.zeroWire;
			else
				shiftedBits[i] = bits[i - s];
		}
		return new WireArray(shiftedBits);
	}
	
	public WireArray shiftRight(int numBits, int s, String...desc) {
		Wire[] bits = adjustLength(array, numBits);
		Wire[] shiftedBits = new Wire[numBits];
		for (int i = 0; i < numBits; i++) {
			if (i >= numBits - s)
				shiftedBits[i] = generator.zeroWire;
			else
				shiftedBits[i] = bits[i + s];
		}
		return new WireArray(shiftedBits);
	}
		
	public Wire[] packBitsIntoWords(int wordBitwidth, String...desc){
		int numWords = (int)Math.ceil(array.length*1.0/wordBitwidth);
		Wire[] padded = adjustLength( array, wordBitwidth*numWords);
		Wire[] result = new Wire[numWords];
		for(int i = 0; i < numWords; i++){
			result[i] = new WireArray(Arrays.copyOfRange(padded, i*wordBitwidth, (i+1)*wordBitwidth)).packAsBits();
		}
		return result;
	}
	
	public Wire[] packWordsIntoLargerWords(int wordBitwidth, int numWordsPerLargerWord, String...desc){
		int numLargerWords = (int)Math.ceil(array.length*1.0/numWordsPerLargerWord);
		Wire[] result = new Wire[numLargerWords];
		Arrays.fill(result, generator.zeroWire);
		for(int i = 0; i < array.length; i++){
			int subIndex = i % numWordsPerLargerWord;
			result[i/numWordsPerLargerWord] = result[i/numWordsPerLargerWord].add(array[i]
					.mul(new BigInteger("2").pow(subIndex*wordBitwidth)));
 		}
		return result;
		
	}

	public WireArray getBits(int bitwidth, String...desc) {
		Wire[] bits = new Wire[bitwidth * array.length];
		int idx = 0;
		for (int i = 0; i < array.length; i++) {
			Wire[] tmp = array[i].getBitWires(bitwidth, desc).asArray();
			for (int j = 0; j < bitwidth; j++) {
				bits[idx++] = tmp[j];
			}
		}
		return new WireArray(bits);
	}
	
}
