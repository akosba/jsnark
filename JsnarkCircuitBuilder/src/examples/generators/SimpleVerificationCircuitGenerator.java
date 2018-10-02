package examples.generators;

import circuit.eval.CircuitEvaluator;
import circuit.structure.CircuitGenerator;
import circuit.structure.Wire;

/**
 * This is a very simple verification circuit which a prover can use to
 * prove that given z, the prover knows x, y such that x + y = z.
 * This is the example explained in the zcash blog part 1. */
public class SimpleVerificationCircuitGenerator extends CircuitGenerator {

    private Wire[] privateInputs;
    private Wire expOutput;

    public SimpleVerificationCircuitGenerator(String circuitName) {
        super(circuitName);
    }

    @Override
    protected void buildCircuit() {
        expOutput = createInputWire();
        privateInputs = createProverWitnessWireArray(2);
        Wire result = privateInputs[0].add(privateInputs[1]);
        addEqualityAssertion(expOutput, result);

    }

    @Override
    public void generateSampleInput(CircuitEvaluator evaluator) {
        evaluator.setWireValue(privateInputs[0], 2);
        evaluator.setWireValue(privateInputs[1], 5);
        evaluator.setWireValue(expOutput, 7);
    }

    public static void main(String[] args) {
        SimpleVerificationCircuitGenerator myGen = new SimpleVerificationCircuitGenerator("zcash_blog_example");
        myGen.generateCircuit();
        myGen.evalCircuit();
        myGen.prepFiles();
        myGen.runLibsnark();
    }
}
