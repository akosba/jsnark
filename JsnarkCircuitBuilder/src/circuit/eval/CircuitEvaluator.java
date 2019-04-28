/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package circuit.eval;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Scanner;

import util.Util;
import circuit.auxiliary.LongElement;
import circuit.config.Config;
import circuit.operations.WireLabelInstruction;
import circuit.operations.WireLabelInstruction.LabelType;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;
import circuit.structure.WireArray;

public class CircuitEvaluator {

	private CircuitGenerator circuitGenerator;
	private BigInteger[] valueAssignment;

	public CircuitEvaluator(CircuitGenerator circuitGenerator) {
		this.circuitGenerator = circuitGenerator;
		valueAssignment = new BigInteger[circuitGenerator.getNumWires()];
		valueAssignment[circuitGenerator.getOneWire().getWireId()] = BigInteger.ONE;
	}

	public void setWireValue(Wire w, BigInteger v) {
		if(v.signum() < 0 || v.compareTo(Config.FIELD_PRIME) >=0){
			throw new IllegalArgumentException("Only positive values that are less than the modulus are allowed for this method.");
		}
		valueAssignment[w.getWireId()] = v;
	}

	public BigInteger getWireValue(Wire w) {
		BigInteger v = valueAssignment[w.getWireId()];
		if (v == null) {
			WireArray bits = w.getBitWiresIfExistAlready();
			if (bits != null) {
				BigInteger sum = BigInteger.ZERO;
				for (int i = 0; i < bits.size(); i++) {
					sum = sum.add(valueAssignment[bits.get(i).getWireId()]
							.shiftLeft(i));
				}
				v = sum;
			}
		}
		return v;
	}

	public BigInteger[] getWiresValues(Wire[] w) {
		BigInteger[] values = new BigInteger[w.length];
		for (int i = 0; i < w.length; i++) {
			values[i] = getWireValue(w[i]);
		}
		return values;
	}

	public BigInteger getWireValue(LongElement e, int bitwidthPerChunk) {
		return Util.combine(valueAssignment, e.getArray(), bitwidthPerChunk);
	}

	public void setWireValue(LongElement e, BigInteger value,
			int bitwidthPerChunk) {
		Wire[] array = e.getArray();
		setWireValue(array, Util.split(value, bitwidthPerChunk));
	}

	public void setWireValue(Wire wire, long v) {
		if(v < 0){
			throw new IllegalArgumentException("Only positive values that are less than the modulus are allowed for this method.");
		}
		setWireValue(wire, BigInteger.valueOf(v));
	}

	public void setWireValue(Wire[] wires, BigInteger[] v) {
		for (int i = 0; i < v.length; i++) {
			setWireValue(wires[i], v[i]);
		}
		for (int i = v.length; i < wires.length; i++) {
			setWireValue(wires[i], BigInteger.ZERO);
		}
	}

	public void evaluate() {

		System.out.println("Running Circuit Evaluator for < "
				+ circuitGenerator.getName() + " >");
		LinkedHashMap<Instruction, Instruction> evalSequence = circuitGenerator
				.getEvaluationQueue();

		for (Instruction e : evalSequence.keySet()) {
			e.evaluate(this);
			e.emit(this);
		}
		// check that each wire has been assigned a value
		for (int i = 0; i < valueAssignment.length; i++) {
			if (valueAssignment[i] == null) {
				throw new RuntimeException("Wire#" + i + "is without value");
			}
		}
		System.out.println("Circuit Evaluation Done for < "
				+ circuitGenerator.getName() + " >\n\n");

	}

	public void writeInputFile() {
		try {
			LinkedHashMap<Instruction, Instruction> evalSequence = circuitGenerator
					.getEvaluationQueue();

			PrintWriter printWriter = new PrintWriter(
					circuitGenerator.getName() + ".in");
			for (Instruction e : evalSequence.keySet()) {
				if (e instanceof WireLabelInstruction
						&& (((WireLabelInstruction) e).getType() == LabelType.input || ((WireLabelInstruction) e)
								.getType() == LabelType.nizkinput)) {
					int id = ((WireLabelInstruction) e).getWire().getWireId();
					printWriter.println(id + " "
							+ valueAssignment[id].toString(16));
				}
			}
			printWriter.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * An independent old method for testing.
	 * 
	 * @param circuitFilePath
	 * @param inFilePath
	 * @throws Exception
	 */

	public static void eval(String circuitFilePath, String inFilePath)
			throws Exception {

		Scanner circuitScanner = new Scanner(new BufferedInputStream(
				new FileInputStream(circuitFilePath)));
		Scanner inFileScanner = new Scanner(new File(inFilePath));

		int totalWires = Integer.parseInt(circuitScanner.nextLine().replace(
				"total ", ""));

		BigInteger[] assignment = new BigInteger[totalWires];

		ArrayList<Integer> wiresToReport = new ArrayList<Integer>();
		HashSet<Integer> ignoreWires = new HashSet<Integer>();

		// Hashtable<Integer, BigInteger> assignment = new Hashtable<>();
		while (inFileScanner.hasNextInt()) {
			int wireNumber = inFileScanner.nextInt();
			String num = inFileScanner.next();
			assignment[wireNumber] = new BigInteger(num, 16);
			wiresToReport.add(wireNumber);
			// assignment.put(wireNumber, new BigInteger(num));
		}

		BigInteger prime = new BigInteger(
				"21888242871839275222246405745257275088548364400416034343698204186575808495617");

		circuitScanner.nextLine();
		while (circuitScanner.hasNext()) {
			String line = circuitScanner.nextLine();
			if (line.contains("#")) {
				line = line.substring(0, line.indexOf("#"));
				line = line.trim();
			}
			if (line.startsWith("input") || line.startsWith("nizkinput")) {

				continue;
			} else if (line.startsWith("output ")) {
				line = line.replace("output ", "");
				System.out.println(Integer.parseInt(line) + "::"
						+ assignment[Integer.parseInt(line)].toString(16));
				wiresToReport.add(Integer.parseInt(line));
			} else if (line.startsWith("DEBUG ")) {
				line = line.replace("DEBUG ", "");
				Scanner scanner = new Scanner(line);
				int id = Integer.parseInt(scanner.next());
				System.out.println(id + "::" + assignment[id].toString(16)
						+ " >> " + scanner.nextLine());
				scanner.close();
			} else {
				ArrayList<Integer> ins = getInputs(line);
				for (int in : ins) {
					if (assignment[in] == null) {
						System.err
								.println("Undefined value for a used wire, at line "
										+ line);
					}
				}
				ArrayList<Integer> outs = getOutputs(line);
				if (line.startsWith("mul ")) {
					BigInteger out = BigInteger.ONE;
					for (int w : ins) {
						out = out.multiply(assignment[w]);
					}
					wiresToReport.add(outs.get(0));
					assignment[outs.get(0)] = out.mod(prime);
					;
				} else if (line.startsWith("add ")) {
					BigInteger out = BigInteger.ZERO;
					for (int w : ins) {
						out = out.add(assignment[w]);
					}
					assignment[outs.get(0)] = out.mod(prime);
				} else if (line.startsWith("xor ")) {
					BigInteger out = assignment[ins.get(0)]
							.equals(assignment[ins.get(1)]) ? BigInteger.ZERO
							: BigInteger.ONE;
					assignment[outs.get(0)] = out;
					wiresToReport.add(outs.get(0));

				} else if (line.startsWith("zerop ")) {
					ignoreWires.add(outs.get(0));
					if (assignment[ins.get(0)].signum() == 0) {
						assignment[outs.get(1)] = BigInteger.ZERO;
					} else {

						assignment[outs.get(1)] = BigInteger.ONE;
					}
					wiresToReport.add(outs.get(1));

				} else if (line.startsWith("split ")) {
					if (outs.size() < assignment[ins.get(0)].bitLength()) {

						System.err.println("Error in Split");
						System.out.println(assignment[ins.get(0)].toString(16));
						System.out.println(line);
					}
					for (int i = 0; i < outs.size(); i++) {
						assignment[outs.get(i)] = assignment[ins.get(0)]
								.testBit(i) ? BigInteger.ONE : BigInteger.ZERO;
						wiresToReport.add(outs.get(i));

					}

				} else if (line.startsWith("pack ")) {

					BigInteger sum = BigInteger.ZERO;
					for (int i = 0; i < ins.size(); i++) {
						sum = sum.add(assignment[ins.get(i)]
								.multiply(new BigInteger("2").pow(i)));
					}
					wiresToReport.add(outs.get(0));
					assignment[outs.get(0)] = sum;
				} else if (line.startsWith("const-mul-neg-")) {
					String constantStr = line.substring(
							"const-mul-neg-".length(), line.indexOf(" "));
					BigInteger constant = prime.subtract(new BigInteger(
							constantStr, 16));
					assignment[outs.get(0)] = assignment[ins.get(0)].multiply(
							constant).mod(prime);
				} else if (line.startsWith("const-mul-")) {
					String constantStr = line.substring("const-mul-".length(),
							line.indexOf(" "));
					BigInteger constant = new BigInteger(constantStr, 16);
					assignment[outs.get(0)] = assignment[ins.get(0)].multiply(
							constant).mod(prime);
				} else {
					System.err.println("Unknown Circuit Statement");
				}

			}
		}

		for (int i = 0; i < totalWires; i++) {
			if (assignment[i] == null && !ignoreWires.contains(i)) {
				System.out.println("Wire " + i + " is Null");
			}
		}

		circuitScanner.close();
		inFileScanner.close();

		PrintWriter printWriter = new PrintWriter(inFilePath + ".full.2");
		for (int id : wiresToReport) {
			printWriter.println(id + " " + assignment[id].toString(16));
		}
		printWriter.close();
	}

	private static ArrayList<Integer> getOutputs(String line) {
		// System.out.println(line);
		Scanner scanner = new Scanner(line.substring(line.lastIndexOf("<") + 1,
				line.lastIndexOf(">")));
		ArrayList<Integer> outs = new ArrayList<>();
		while (scanner.hasNextInt()) {
			int v = scanner.nextInt();
			// System.out.println(v);
			outs.add(v);
		}
		scanner.close();
		return outs;
	}

	private static ArrayList<Integer> getInputs(String line) {
		Scanner scanner = new Scanner(line.substring(line.indexOf("<") + 1,
				line.indexOf(">")));
		ArrayList<Integer> ins = new ArrayList<>();
		while (scanner.hasNextInt()) {
			ins.add(scanner.nextInt());
		}
		scanner.close();
		return ins;
	}

	public BigInteger[] getAssignment() {
		return valueAssignment;
	}

}
