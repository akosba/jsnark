/*******************************************************************************
 * Author: Ahmed Kosba <akosba@cs.umd.edu>
 *******************************************************************************/
package examples.gadgets.augmenter;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Scanner;

import circuit.operations.Gadget;
import circuit.structure.Wire;

public class PinocchioGadget extends Gadget {

	private Wire[] inputWires;
	private Wire[] proverWitnessWires;
	private Wire[] outputWires;

	public PinocchioGadget(Wire[] inputWires, String pathToArithFile, String... desc) {
		super(desc);
		this.inputWires = inputWires;
		try {
			buildCircuit(pathToArithFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void buildCircuit(String path) throws FileNotFoundException {

		ArrayList<Wire> proverWitnessWires = new ArrayList<Wire>();
		ArrayList<Wire> outputWires = new ArrayList<Wire>();

		Wire[] wireMapping;
		Scanner scanner = new Scanner(new File(path));

		if (!scanner.next().equals("total")) {
			scanner.close();
			throw new RuntimeException("Expected total %d in the first line");
		}
		int numWires = scanner.nextInt();
		scanner.nextLine();
		wireMapping = new Wire[numWires];

		int inputCount = 0;
		while (scanner.hasNext()) {
			String line = scanner.nextLine();
			// remove comments
			if (line.contains("#")) {
				line = line.substring(0, line.indexOf("#"));
			}
			if (line.equals("")) {
				continue;
			} else if (line.startsWith("input")) {
				String[] tokens = line.split("\\s+");
				int wireIndex = Integer.parseInt(tokens[1]);
				if (wireMapping[wireIndex] != null) {
					throwParsingError(scanner, "Wire assigned twice! " + wireIndex);
				}
				if (inputCount < inputWires.length) {
					wireMapping[wireIndex] = inputWires[inputCount];
				} else {
					// the last input wire is assumed to be the one wire
					wireMapping[wireIndex] = generator.getOneWire();
				}
				inputCount++;
			} else if (line.startsWith("output")) {
				String[] tokens = line.split("\\s+");
				int wireIndex = Integer.parseInt(tokens[1]);
				outputWires.add(wireMapping[wireIndex]);
			} else if (line.startsWith("nizk")) {
				String[] tokens = line.split("\\s+");
				int wireIndex = Integer.parseInt(tokens[1]);
				if (wireMapping[wireIndex] != null) {
					throwParsingError(scanner, "Wire assigned twice! " + wireIndex);
				}
				Wire w = generator.createProverWitnessWire();
				proverWitnessWires.add(w);
				wireMapping[wireIndex] = w;
			} else {
				ArrayList<Integer> ins = getInputs(line);
				for (int in : ins) {
					if (wireMapping[in] == null) {
						throwParsingError(scanner, "Undefined input wire " + in + " at line " + line);
					}
				}
				ArrayList<Integer> outs = getOutputs(line);
				if (line.startsWith("mul ")) {
					wireMapping[outs.get(0)] = wireMapping[ins.get(0)].mul(wireMapping[ins.get(1)]);
				} else if (line.startsWith("add ")) {
					Wire result = wireMapping[ins.get(0)];
					for (int i = 1; i < ins.size(); i++) {
						result = result.add(wireMapping[ins.get(i)]);
					}
					wireMapping[outs.get(0)] = result;
				} else if (line.startsWith("zerop ")) {
					wireMapping[outs.get(1)] = wireMapping[ins.get(0)].checkNonZero();
				} else if (line.startsWith("split ")) {
					Wire[] bits = wireMapping[ins.get(0)].getBitWires(outs.size()).asArray();
					for (int i = 0; i < outs.size(); i++) {
						wireMapping[outs.get(i)] = bits[i];
					}
				} else if (line.startsWith("const-mul-neg-")) {
					String constantStr = line.substring("const-mul-neg-".length(), line.indexOf(" "));
					BigInteger constant = new BigInteger(constantStr, 16);
					wireMapping[outs.get(0)] = wireMapping[ins.get(0)].mul(constant.negate());
				} else if (line.startsWith("const-mul-")) {
					String constantStr = line.substring("const-mul-".length(), line.indexOf(" "));
					BigInteger constant = new BigInteger(constantStr, 16);
					wireMapping[outs.get(0)] = wireMapping[ins.get(0)].mul(constant);
				} else {
					throwParsingError(scanner, "Unsupport Circuit Line " + line);
				}

			}
		}

		scanner.close();

		this.proverWitnessWires = new Wire[proverWitnessWires.size()];
		proverWitnessWires.toArray(this.proverWitnessWires);
		this.outputWires = new Wire[outputWires.size()];
		outputWires.toArray(this.outputWires);
	}

	private ArrayList<Integer> getOutputs(String line) {
		Scanner scanner = new Scanner(line.substring(line.lastIndexOf("<") + 1, line.lastIndexOf(">")));
		ArrayList<Integer> outs = new ArrayList<>();
		while (scanner.hasNextInt()) {
			int v = scanner.nextInt();
			outs.add(v);
		}
		scanner.close();
		return outs;
	}

	private ArrayList<Integer> getInputs(String line) {
		Scanner scanner = new Scanner(line.substring(line.indexOf("<") + 1, line.indexOf(">")));
		ArrayList<Integer> ins = new ArrayList<>();
		while (scanner.hasNextInt()) {
			ins.add(scanner.nextInt());
		}
		scanner.close();
		return ins;
	}

	@Override
	public Wire[] getOutputWires() {
		return outputWires;
	}

	public Wire[] getProverWitnessWires() {
		return proverWitnessWires;
	}

	private void throwParsingError(Scanner s, String m) {
		s.close();
		throw new RuntimeException(m);
	}
}
