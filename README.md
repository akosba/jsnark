##jsnark

This is a Java library for building circuits for preprocessing zk-SNARKs. The library uses libsnark as a backend (https://github.com/scipr-lab/libsnark), and can integrate circuits produced by the Pinocchio compiler (https://vc.codeplex.com/SourceControl/latest) when needed by the programmer. The code consists of two main parts:
- `JsnarkCircuitBuilder`: A Java project that has a Gadget library for building/augmenting circuits. (Check the `src/examples` package)
- `libsnark/src/interface`: A C++ interface with libsnark which accepts circuits produced by either the circuit builder or by Pinocchio's compiler directly.

### Prerequisites

- Libsnark prerequisites.
- JDK 8. 
- Junit 4.

For Ubuntu 14.04, the following can be done to install the above:

- To install libsnark prerequisites: 

	`$ sudo apt-get install build-essential git libgmp3-dev libprocps3-dev libgtest-dev python-markdown libboost-all-dev libssl-dev`

Note: Don't clone libsnark from `https://github.com/scipr-lab/libsnark`. Make sure to use the modified libsnark submodule within the jsnark cloned repo in the next section.

- To install JDK 8: 

	`$ sudo add-apt-repository ppa:webupd8team/java`

	`$ sudo apt-get update`

	`$ sudo apt-get install oracle-java8-installer`

Verify the installed version by `java -version`. In case it is not 1.8 or later, try `$ sudo update-java-alternatives -s java-8-oracle`

- To install Junit4: 

	`$ sudo apt-get install junit4`

### jsnark Installation Instructions

- Run `$ git clone --recursive https://github.com/akosba/jsnark.git`

- Run:

	`$ cd jsnark/libsnark`

	`$ ./prepare-depends.sh`

	`$ make`  

The makefile has been modified to produce the one needed executable for the interface. The executable will appear under src/interface  

- Compile and test the JsnarkCircuitBuilder project as in the next section..

### Running and Testing JsnarkCircuitBuilder
To compile the JsnarkCircuitBuilder project via command line:

    $ cd JsnarkCircuitBuilder
    $ mkdir -p bin
    $ javac -d bin -cp /usr/share/java/junit4.jar  $(find ./src/* | grep .java)

The classpath of junit4 may need to be adapted accordingly, in case the jar is located elsewhere.

Before running the following, make sure the `PATH_TO_LIBSNARK_EXEC` property in `config.properties` points to the path of the run_libsnark executable. 

To run a simple example, the following command can be used

    $ java -cp bin examples.generators.SimpleCircuitGenerator

To run one of the JUnit tests available:

    $ java -cp bin:/usr/share/java/junit4.jar org.junit.runner.JUnitCore  examples.tests.SHA256_Test

Note: An IDE, e.g. Eclipse, or possibly the ant tool can be used instead to build and run the Java project more conveniently.


### Examples included

- __Basic Circuit Example__: `SimpleExampleCircuitGenerator.java`. This shows a circuit that computes very simple expressions.
- __Basic Gadget Example__: `DotProductGadget.java`. This is a gadget for computing the dot product of two vectors. The gadget is used in `DotProductCircuitGenerator.java`.
- __Gadgets with witness computations done outside the circuit__: `FieldDivisonGadget.java`, `ModGadget.java`. This way of writing circuits is useful when verification is more efficient than computation, and when the prover witness value can be inferred automatically in the circuit. Note the usage of `specifyProverWitnessComputation(..)`. This must be positioned before the steps where the witness is used. 
- __SHA256 Gadget__: This is a manually-optimized SHA256 Gadget for variable-length input with a padding option. The code is written to be similar to how SHA256 is written in C, except for three main things: keeping track of bitwidth, manual optimizations for computation of ch and maj, and the explicit handling of overflows. Making use of jsnark's optimizations, our implementation for the SHA256 gadget costs __26196 constraints for one block__. If padding is applied within the block, the cost can be even lower. 
- __Pinocchio Integration__: `PinocchioGadget.java` and `AugmentedAuctionCircuitGenerator.java`. The Pinocchio gadget can be used to use compiled circuits by the Pinocchio compiler as gagdets. `AugmentedAuctionCircuitGenerator.java` shows how to use a compiled Auction circuit by Pinocchio, and augment it with other manually-developed gadgets. This can help in the cases where the programmer needs to take care only of the bottleneck parts of the circuits. 
- __JUnit Tests__: Some JUnit tests are included for primitive operations and for SHA-256. This can illustrate how to write gadgets and test them.

### Writing Circuits using jsnark

To summarize the steps needed:
- Extend the `CircuitGenerator` class. 
- Override the `buildCircuit()` method: Identify the inputs, outputs and prover witness wires of your circuit, and instantiate/connect gadgets inside.
- Override the `generateSampleInput()` method: Specify how the values of the input and possibly some of the free prover witness wires are set. This helps in quick testing.
- To run a generator, the following methods should be invoked:
	- `generateCircuit()`: generates the arithmetic circuit and the constraints.
	- `evalCircuit()`: evaluates the circuit.
	- `prepFiles()`: This produces two files: `<circuit name>.arith` and `<circuit name>.in`. The first file specifies the arithemtic circuit in a way that is similar to how Pinocchio outputs arithmetic circuits, but with other kinds of instructions, like: xor, or, pack and assert. The second file outputs a file containing the values for the input and prover free witness wires. This step must be done after calling `evalCircuit()` as some witness values are computed during that step.
	- `runLibsnark()`: This runs the libsnark interface with the two files produced in the last step. This can also be done manually outside the circuit if needed.	
- Note: In the executing thread, use one CircuitGenerator per thread at a time. If multiple generators are used in parallel, each needs to be in a separate thread, and the corresponding property value in config.properties need to be adapted.

### Running circuits compiled by Pinocchio on libsnark

- To use Pinocchio directly with libsark, run the interface executable on the `<circuit name>.arith` and `<circuit name>.in` files. The `<circuit name>.in` should specify the hexadecimal value for each input and nizkinput wire ids, in the following format: `id value`, each on a separate line.
- It is important to assign 1 to the wire denoted as the one wire input in the arithmetic file.

### Comparison with libsnark's gadget libraries

The gadget library of jsnark shares some similarities with the C++ Gadget library of libsnark, but it has some options that could possibly help for writing optimized circuits quickly without specifying all details. If the reader is familiar with the gadget libraries of libsnark, and would like to try jsnark, here are some key points to minimize confusion:
- No need to maintain a distinction between Variables, LinearCombinations, ... etc. The type Wire can be used to represent Variables, LinearCombinations, Constants, .. etc. The library handles the mapping in a later stage.
- Instead of having the notion of primary input and auxiliary input for representing variables, the important wires in jsnark can be labeled anywhere as either input, output, prover witness wires. Both the input and output wires are public and seen by the verifier (this corresponds to the primary input in libsnark). The prover witness wires refer to the *free* input variables provided by the prover. This is in some sense similar to the way Pinocchio's compiler classifies wires.  
- Each Gadget in libsnark requires writing and calling two methods: generateConstraints() to specify the r1cs constraints, and generateWitness() to invoke the witness computation. In jsnark's builder, applying primitive operations on wires generate constraints automatically. Additionally, the witness computation is done automatically for primitive operations, and does not need to be explicitly invoked, except in the case of prover witness computation that has to be done outside the circuit, e.g. the FieldDivisionGadget example.
- Jsnark applies caching and other techniques during circuit construction to cancel unneeded constraints. This helps in code reusability when changing input variables wires to carry constant values instead. This also helps in reducing the complexity when writing optimized circuits. One example is the ``maj`` calculation in the SHA256 gadget, in which jsnark detects similar operations across the loop iterations with little effort from the programmer, resulting in more than 1000 gates savings. ``CachingTest.java`` also illustrates what the caching approach can help with.

### Disclaimer

The code is undergoing more testing and integration of other features. The future versions of this library will include more documentation, examples and optimizations.

### Author
This code is developed and maintained by Ahmed Kosba <akosba@cs.umd.edu>. Please email for any questions.

 
