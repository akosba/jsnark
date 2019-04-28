## jsnark

This is a Java library for building circuits for preprocessing zk-SNARKs. The library uses libsnark as a backend (https://github.com/scipr-lab/libsnark), and can integrate circuits produced by the Pinocchio compiler (https://vc.codeplex.com/SourceControl/latest) when needed by the programmer. The code consists of two main parts:
- `JsnarkCircuitBuilder`: A Java project that has a Gadget library for building/augmenting circuits. (Check the `src/examples` package)
- `libsnark/jsnark_interface`: A C++ interface to libsnark which accepts circuits produced by either the circuit builder or by Pinocchio's compiler directly.

__Updates__: 
- The jsnark library now has several cryptographic gadgets used in earlier work ([Hawk](https://eprint.iacr.org/2015/675.pdf) and [C0C0](https://eprint.iacr.org/2015/1093.pdf)). Some of the gadgets like RSA and AES were improved by techniques from xJsnark. The gadgets can be found in [src/examples/gadgets](https://github.com/akosba/jsnark/tree/master/JsnarkCircuitBuilder/src/examples/gadgets).
- xJsnark, a high-level programming framework for zk-SNARKs is available [here](https://github.com/akosba/xjsnark). xJsnark uses an enhanced version of jsnark in its back end, and aims at reducing the background/effort required by low-level libraries, while generating efficient circuits from the high-level code. Sample examples can be found in this [page](https://github.com/akosba/xjsnark#examples-included).

### Prerequisites

- Libsnark prerequisites
- JDK 8 (Higher versions are also expected to work. We've only tested with JDKs 8 and 12.)
- Junit 4
- BouncyCastle library

For Ubuntu 14.04, the following can be done to install the above:

- To install libsnark prerequisites: 

	`$ sudo apt-get install build-essential cmake git libgmp3-dev libprocps3-dev python-markdown libboost-all-dev libssl-dev`

Note: Don't clone libsnark from `https://github.com/scipr-lab/libsnark`. Make sure to use the modified libsnark submodule within the jsnark cloned repo in the next section.

- To install JDK 8: 

	`$ sudo add-apt-repository ppa:webupd8team/java`

	`$ sudo apt-get update`

	`$ sudo apt-get install oracle-java8-installer`

Verify the installed version by `java -version`. In case it is not 1.8 or later, try `$ sudo update-java-alternatives -s java-8-oracle`

- To install Junit4: 

	`$ sudo apt-get install junit4`
	
- To download BouncyCastle:

	`$ wget https://www.bouncycastle.org/download/bcprov-jdk15on-159.jar`

### jsnark Installation Instructions

- Run `$ git clone --recursive https://github.com/akosba/jsnark.git`

- Run:

	`$ cd jsnark/libsnark`

	`$ git submodule init && git submodule update`

	`$ mkdir build && cd build && cmake ..`

	`$ make`  

The CMakeLists files were modified to produce the needed executable for the interface. The executable will appear under build/libsnark/jsnark_interface

- Compile and test the JsnarkCircuitBuilder project as in the next section..

### Running and Testing JsnarkCircuitBuilder
To compile the JsnarkCircuitBuilder project via command line, from the jsnark directory:  

    $ cd JsnarkCircuitBuilder
    $ mkdir -p bin
    $ javac -d bin -cp /usr/share/java/junit4.jar:bcprov-jdk15on-159.jar  $(find ./src/* | grep ".java$")

The classpaths of junit4 and bcprov-jdk15on-159.jar may need to be adapted in case the jars are located elsewhere. The above command assumes that  bcprov-jdk15on-159.jar was moved to the JsnarkCircuitBuilder directory.

Before running the following, make sure the `PATH_TO_LIBSNARK_EXEC` property in `config.properties` points to the path of the `run_ppzksnark` executable. 

To run a simple example, the following command can be used

    $ java -cp bin examples.generators.SimpleCircuitGenerator

To run one of the JUnit tests available:

    $ java -cp bin:/usr/share/java/junit4.jar org.junit.runner.JUnitCore  examples.tests.hash.SHA256_Test

Some of the examples and tests will require bcprov-jdk15on-159.jar as well to be added to the classpath.	

Note: An IDE, e.g. Eclipse, or possibly the ant tool can be used instead to build and run the Java project more conveniently.


### Examples included

#### Simple Examples

- __Basic Circuit Example__: `SimpleCircuitGenerator.java`. This shows a circuit that computes very simple expressions.
- __Basic Gadget Example__: `DotProductGadget.java`. This is a gadget for computing the dot product of two vectors. The gadget is used in `DotProductCircuitGenerator.java`.
- __Gadgets with witness computations done outside the circuit__: `FieldDivisonGadget.java`, `ModGadget.java`. This way of writing circuits is useful when verification is more efficient than computation, and when the prover witness value can be inferred automatically in the circuit. Note the usage of `specifyProverWitnessComputation(..)`. This must be positioned before the steps where the witness is used. 
- __Pinocchio Integration__: `PinocchioGadget.java` and `AugmentedAuctionCircuitGenerator.java`. The Pinocchio gadget can be used to use compiled circuits by the Pinocchio compiler as gagdets. `AugmentedAuctionCircuitGenerator.java` shows how to use a compiled Auction circuit by Pinocchio, and augment it with other manually-developed gadgets. This can help in the cases where the programmer needs to take care only of the bottleneck parts of the circuits. 

#### Cryptographic Primtives

Several cryptographic gadgets spanning hashes, block ciphers, key exchange, public key encryption and signatures can be found in [src/examples/gadgets](https://github.com/akosba/jsnark/tree/master/JsnarkCircuitBuilder/src/examples/gadgets). We list some notes regarding few gadgets below:

- __SHA256 gadget__: This is a manually-optimized SHA256 Gadget for variable-length input with a padding option. The code is written to be similar to how SHA256 is written in C or Java, except for three main things: keeping track of bitwidth, manual optimizations for computation of ch and maj, and the explicit handling of overflows (A more recent work ([xJsnark](https://github.com/akosba/xjsnark)) reduces/eliminates the need for such differences, while still generating optimized outputs). The current SHA256 gadget implementation in jsnark costs about 25650 constraints for one block depending on the number and the bitwidth of the input wires (Further minor optimizations to the existing implementation in jsnark are possible). 
- __RSA gadgets__: This includes public key encryption gadgets based on both PKCS #1 V2.2 (OAEP) and PKCS #1 v1.5, and also a signature gadget using PKCS #1 v1.5. Internally, these gadgets used the long integer optimizations used in xJsnark.
- __Block cipher gadgets__: This includes optimized implementations for AES, Speck and Chaskey ciphers.  
- __Hybrid Encryption Example__: The circuit main file is in `HybridEncryptionCircuitGenerator.java`. The circuit uses the gadgets defined in `examples/gadgets/diffieHellmanKeyExchange` and `examples/gadgets/blockciphers`, which are for key exchange (using field extension) and symmetric encryption using the speck cipher. Other variants will be added in the future.


### Writing Circuits using jsnark

To summarize the steps needed:
- Extend the `CircuitGenerator` class. 
- Override the `buildCircuit()` method: Identify the inputs, outputs and prover witness wires of your circuit, and instantiate/connect gadgets inside.
- Override the `generateSampleInput()` method: Specify how the values of the input and possibly some of the free prover witness wires are set. This helps in quick testing.
- To run a generator, the following methods should be invoked:
	- `generateCircuit()`: generates the arithmetic circuit and the constraints.
	- `evalCircuit()`: evaluates the circuit.
	- `prepFiles()`: This produces two files: `<circuit name>.arith` and `<circuit name>.in`. The first file specifies the arithemtic circuit in a way that is similar to how Pinocchio outputs arithmetic circuits, but with other kinds of instructions, like: xor, or, pack and assert. The second file outputs a file containing the values for the input and prover free witness wires. This step must be done after calling `evalCircuit()` as some witness values are computed during that step.
	- `runLibsnark()`: This runs the libsnark interface on the two files produced in the last step. By default, this method runs the r1cs_ppzksnark proof system implemented in libsnark. For other options see below.
- Note: In the executing thread, use one CircuitGenerator per thread at a time. If multiple generators are used in parallel, each needs to be in a separate thread, and the corresponding property value in config.properties need to be adapted.

#### Running circuit outputs on libsnark

Given the .arith and the .in files, it's possible to use command line directly to run the jsnark-libsnark interface. You can use the executable interface `run_ppzksnark` that appears in `jsnark/libsnark/build/libsnark/jsnark_interface` to run the libsnark algorithms on the circuit. The executable currently allows to run the proof systems `r1cs_ppzksnark` (default) and `r1cs_gg_ppzksnark` implemented in libsnark. To run the first, the executable just takes two arguments: the arithmetic circuit file path, and a sample input file path. To run the `r1cs_gg_ppzksnark` proof system [Gro16], the first argument should be `gg`, followed by the arithmetic circuit file path, and the sample input file path.
	
#### Further Notes

The gadget library of jsnark shares some similarities with the C++ Gadget library of libsnark, but it has some options that could possibly help with writing optimized circuits quickly without specifying all the details. If the reader is familiar with the gadget libraries of libsnark, here are some key points to minimize confusion:
- No need to maintain a distinction between Variables, LinearCombinations, ... etc. The type Wire can be used to represent Variables, LinearCombinations, Constants, .. etc. The library handles the mapping in a later stage.
- Instead of having the notion of primary input and auxiliary input for representing variables, wires in jsnark can be labeled anywhere as either input, output, prover witness wires. Both the input and output wires are public and seen by the verifier (this corresponds to the primary input in libsnark). The prover witness wires refer to the *free* input variables provided by the prover. This is in some sense similar to the way Pinocchio's compiler classifies wires.  
- Each Gadget in libsnark requires writing and calling two methods: generateConstraints() to specify the r1cs constraints, and generateWitness() to invoke the witness computation. In jsnark's builder, applying primitive operations on wires generates constraints automatically. Additionally, the witness computation is done automatically for primitive operations, and does not need to be explicitly invoked, except in the case of prover witness computation that has to be done outside the circuit, e.g. the FieldDivisionGadget example.
- Jsnark applies caching and other techniques during circuit construction to cancel unneeded constraints. This helps in code reusability when changing input variables wires to carry constant values instead. This also helps in reducing the complexity when writing optimized circuits. One example is the ``maj`` calculation in the SHA256 gadget, in which jsnark detects similar operations across the loop iterations with little effort from the programmer, resulting in more than 1000 gates savings. ``CachingTest.java`` also illustrates what cases caching can help with.

### Running circuits compiled by Pinocchio on libsnark

- To use Pinocchio directly with libsark, run the interface executable `run_ppzksnark` on the `<circuit name>.arith` and `<circuit name>.in` files. The `<circuit name>.in` should specify the hexadecimal value for each input and nizkinput wire ids, in the following format: `id value`, each on a separate line.
- It is important to assign 1 to the wire denoted as the one wire input in the arithmetic file.

### Disclaimer

The code is undergoing more testing and integration of other features. The future versions of this library will include more documentation, examples and optimizations.

### Author
This code is developed and maintained by Ahmed Kosba <akosba@cs.umd.edu>. Please email for any questions.

 
