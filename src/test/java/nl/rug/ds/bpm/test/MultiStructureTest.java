package nl.rug.ds.bpm.test;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.expression.ExpressionBuilder;
import nl.rug.ds.bpm.petrinet.ptnet.PlaceTransitionNet;
import nl.rug.ds.bpm.petrinet.ptnet.element.Place;
import nl.rug.ds.bpm.petrinet.ptnet.element.Transition;
import nl.rug.ds.bpm.specification.jaxb.SpecificationSet;
import nl.rug.ds.bpm.util.exception.MalformedNetException;
import nl.rug.ds.bpm.verification.converter.multi.MultiStructureConverterAction;
import nl.rug.ds.bpm.verification.map.AtomicPropositionMap;
import nl.rug.ds.bpm.verification.model.multi.MultiStructure;
import nl.rug.ds.bpm.verification.model.multi.Partition;
import nl.rug.ds.bpm.verification.model.multi.factory.MultiFactory;
import nl.rug.ds.bpm.verification.model.multi.postprocess.stutter.MultiStutterMergeSplitAction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class MultiStructureTest {
    private static PlaceTransitionNet net = new PlaceTransitionNet();
    private static MultiFactory factory = new MultiFactory();

    @BeforeAll
    public static void createNet() throws MalformedNetException {
        Place p0 = net.addPlace("p0", "p0", 1);
        Transition t0 = net.addTransition("t0", "t0");

        Place p1 = net.addPlace("p1", "p1");
        Transition t1 = net.addTransition("t1", "t1");
        Transition t2 = net.addTransition("t2", "t2");
        Transition t3 = net.addTransition("t3", "t3");

        Place p2 = net.addPlace("p2", "p2");

        Transition t4 = net.addTransition("t4", "t4");

        Place p3 = net.addPlace("p3", "p3");

        Transition t5 = net.addTransition("t5", "t5");

        Place p4 = net.addPlace("p4", "p4");

        net.addArc(p0, t0);
        net.addArc(t0, p1);

        net.addArc(p1, t1);
        net.addArc(p1, t2);
        net.addArc(p1, t3);

        net.addArc(t1, p2);
        net.addArc(t2, p2);
        net.addArc(t3, p2);

        net.addArc(p2, t4);
        net.addArc(t4, p3);

        net.addArc(p3, t5);
        net.addArc(t5, p4);

        t1.setGuard("x>=0");
        t2.setGuard("x<=0 && y==true");
        t3.setGuard("x<0 && y==false");
    }

    @Test
    public void verifiableNetTest() throws MalformedNetException {
        net.getTransition("t1").setGuard("x>=0"); //reset guard
        MultiStructure structure = factory.createStructure();

        assertEquals(0, structure.getAtomicPropositionCount());
        assertEquals(0, structure.getStateCount());
        assertEquals(0, structure.getRelationCount());

        AtomicPropositionMap<CompositeExpression> specificationSetPropositionMap = new AtomicPropositionMap<>("p");
        specificationSetPropositionMap.addSpecificationId(ExpressionBuilder.parseExpression("t0"));
        specificationSetPropositionMap.addSpecificationId(ExpressionBuilder.parseExpression("t5"));
        factory.getAtomicPropositionMap().merge(specificationSetPropositionMap);

        structure.addPartition(new SpecificationSet(), specificationSetPropositionMap.getAPKeys());
        assertFalse(structure.getPartitions().isEmpty());
        Partition partition = structure.getPartitions().iterator().next();

        assertEquals(2, partition.getAtomicPropositionCount());
        assertEquals(0, partition.getStateCount());
        assertEquals(0, partition.getRelationCount());

        MultiStructureConverterAction converterAction = factory.createConverter(net, net.getInitialMarking(), structure);
        converterAction.computeInitial();

        MultiStutterMergeSplitAction splitter = new MultiStutterMergeSplitAction(structure.getPartitions());

        assertEquals(1, structure.getInitial().size());
        assertEquals(6, structure.getAtomicPropositionCount());
        assertEquals(7, structure.getStateCount());
        assertEquals(9, structure.getRelationCount());

        structure.clear();

        assertEquals(1, partition.getInitial().size());
        assertEquals(2, partition.getAtomicPropositionCount());
        assertEquals(5, partition.getStateCount());
        assertEquals(5, partition.getRelationCount());
    }

    @Test
    public void verifiableNetTestwithNoEnabledTransition() {
        net.getTransition("t1").setGuard("x>0"); // Adds case where nothing is enabled

        MultiStructure structure = factory.createStructure();

        assertEquals(0, structure.getAtomicPropositionCount());
        assertEquals(0, structure.getStateCount());
        assertEquals(0, structure.getRelationCount());

        AtomicPropositionMap<CompositeExpression> specificationSetPropositionMap = new AtomicPropositionMap<>("p");
        specificationSetPropositionMap.addSpecificationId(ExpressionBuilder.parseExpression("t0"));
        specificationSetPropositionMap.addSpecificationId(ExpressionBuilder.parseExpression("t5"));
        factory.getAtomicPropositionMap().merge(specificationSetPropositionMap);

        structure.addPartition(new SpecificationSet(), specificationSetPropositionMap.getAPKeys());
        assertFalse(structure.getPartitions().isEmpty());
        Partition partition = structure.getPartitions().iterator().next();

        assertEquals(2, partition.getAtomicPropositionCount());
        assertEquals(0, partition.getStateCount());
        assertEquals(0, partition.getRelationCount());

        MultiStructureConverterAction converterAction = factory.createConverter(net, net.getInitialMarking(), structure);
        converterAction.computeInitial();

        MultiStutterMergeSplitAction splitter = new MultiStutterMergeSplitAction(structure.getPartitions());

        assertEquals(1, structure.getInitial().size());
        assertEquals(6, structure.getAtomicPropositionCount());
        assertEquals(8, structure.getStateCount());
        assertEquals(11, structure.getRelationCount());

        structure.clear();

        assertEquals(1, partition.getInitial().size());
        assertEquals(2, partition.getAtomicPropositionCount());
        assertEquals(6, partition.getStateCount());
        assertEquals(7, partition.getRelationCount());
    }

    @Test
    public void verifiableNetTestwithInference() {
        net.getTransition("t1").setGuard("x>=0"); // Adds case where nothing is enabled

        MultiStructure structure = factory.createStructure();

        assertEquals(0, structure.getAtomicPropositionCount());
        assertEquals(0, structure.getStateCount());
        assertEquals(0, structure.getRelationCount());

        AtomicPropositionMap<CompositeExpression> specificationSetPropositionMap = new AtomicPropositionMap<>("p");
        specificationSetPropositionMap.addSpecificationId(ExpressionBuilder.parseExpression("t0"));
        specificationSetPropositionMap.addSpecificationId(ExpressionBuilder.parseExpression("t5"));
        specificationSetPropositionMap.addSpecificationId(ExpressionBuilder.parseExpression("y==true"));
        factory.getAtomicPropositionMap().merge(specificationSetPropositionMap);

        structure.addPartition(new SpecificationSet(), specificationSetPropositionMap.getAPKeys());
        assertFalse(structure.getPartitions().isEmpty());
        Partition partition = structure.getPartitions().iterator().next();

        assertEquals(3, partition.getAtomicPropositionCount());
        assertEquals(0, partition.getStateCount());
        assertEquals(0, partition.getRelationCount());

        MultiStructureConverterAction converterAction = factory.createConverter(net, net.getInitialMarking(), structure);
        converterAction.computeInitial();

        System.out.println(structure);

        MultiStutterMergeSplitAction splitter = new MultiStutterMergeSplitAction(structure.getPartitions());

        assertEquals(1, structure.getInitial().size());
        assertEquals(7, structure.getAtomicPropositionCount());
        assertEquals(7, structure.getStateCount());
        assertEquals(9, structure.getRelationCount());

        System.out.println("\n" + partition);
        structure.clear();

        assertEquals(1, partition.getInitial().size());
        assertEquals(3, partition.getAtomicPropositionCount());
        assertEquals(6, partition.getStateCount());
        assertEquals(7, partition.getRelationCount());
    }
}
