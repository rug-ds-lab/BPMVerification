## Business process verification package
The business process verification package provides generic verification tools for Petri net based business process models.
The Petri net based business process models used are specified in the [BPMPetriNet](https://github.com/rug-ds-lab/BPMPetriNetv) 
package, which supports the standard [pnml](http://www.pnml.org/) place/transition net file format. 

The package provides the following functionality:
* Generation of a declarative process specification describing the common behavior of one or more related [pnml](http://www.pnml.org/) process model variants.
* Verification of a pnml process model against a generated or custom specification using the [NuSMV2](http://nusmv.fbk.eu/)/[NuXMV](https://nuxmv.fbk.eu/) model checker.

### Structure
The package is structured as followed:

* eventStructure
* specification
  * jaxb
  * map
  * marshaller
  * parser
* variability
  * utils
* verification
  * checker
  * converter
  * map
  * model.kripke
  * optimizer

### Usage
The package provides two core functionalities contained within the following classes:

* nl.rug.ds.bpm.variability.VariabilitySpecification
* nl.rug.ds.bpm.verification.Verifier

Self explanatory examples using these classes can be found in test/main.

### Custom specifications
Specifications can be either generated automatically or defined manually.

    <bpmSpecification>
        <specificationSets>
            <specificationSet>
                <specifications>
                    <specification id="s1" type="AlwaysResponse">
                        <inputElements>
                            <inputElement target="p">start</inputElement>
                            <inputElement target="q">transition3</inputElement>
                            <inputElement target="q">transition4</inputElement>
                        </inputElements>
                    </specification>
                    <specification id="s2" type="AlwaysNext">
                        <inputElements>
                            <inputElement target="p">transition1</inputElement>
                            <inputElement target="q">transition3</inputElement>
                        </inputElements>
                    </specification>
                </specifications>
            </specificationSet>
        </specificationSets>

        <elementGroups>
            <group id="start">
                <elements>
                    <element>transition1</element>
                    <element>transition2</element>
                </elements>
            </group>
        </elementGroups>

        <specificationTypes>
            <specificationType id="AlwaysNext">
                <inputs>
                    <input type="or">p</input>
                    <input type="or">q</input>
                </inputs>
                <formulas>
                    <formula language="CTLSPEC">AG(p -> A[p U q])</formula>
                </formulas>
            </specificationType>
        </specificationTypes>
    </bpmSpecification>

For example, the listing above forms the following specification:

    AG((transition1 | transition2) -> AF(transition3 | transition4))
    AG(transition1 -> A[transition1 U transition3])

Specifications are divided into specificationSets to ensure effective model reduction. For each specificationSet a
separate model is generated, reduced, and verified. Reduction is accomplished by calculating a stutter equivalent model
with respect to the used input elements for each set.

Each specification is defined by an id, type, and a list of inputElements. Each id should be unique and is used for
feedback purposes. The type refers to a specificationType which is either predefined in resources/specificationTypes.xml
or defined custom. The list of inputElements target the inputs of the specificationType. In case of overloading a
target, the inputElements form a dis/con-junction as defined by that target's type.

The optional elementGroups block defines sets of elements belonging to a group given an id. This id can then be used as
an input element throughout the specification. Elements within a group form a disjunction.

The optional specificationTypes block defines custom specificationTypes by a unique id, one or more formulas, and its
inputs. The language of a formula can be either _CTLSPEC_, _LTLSPEC_, or _JUSTICE_ as defined by the
[NuSMV2](http://nusmv.fbk.eu/)/[NuXMV](https://nuxmv.fbk.eu/) model checker.

### Related publications
For more information on the inner workings of this package, please see the following publications, or when incorporating this package into your work, please cite the following publications.

[1] H. Groefsema **(2016)** _Business Process Variability: A Study into Process Management and Verification._ PhD thesis.

[2] H. Groefsema, N.R.T.P. van Beest, and M. Aiello **(2016)** _A Formal Model for Compliance Verification of Service Compositions._ IEEE Transactions on Service Computing.

[3] H. Groefsema and N.R.T.P. van Beest **(2015)** _Design-time Compliance of Service Compositions in Dynamic Service Environments._ In IEEE International Conference on Service Oriented Computing & Applications, 108–115.

[4] H. Groefsema, N.R.T.P. van Beest and A. Armas-Cervantes **(2017)** _Automated compliance verification of business processes in Apromore._ In Proceedings of the BPM Demo Track 2017.

[5] N.R.T.P. van Beest, H. Groefsema, L. García-Bañuelos and M. Aiello **(2019)** _Variability in business processes: Automatically obtaining a generic specification._ In Information Systems, volume 80.