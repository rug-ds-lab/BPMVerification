package nl.rug.ds.bpm.test;

import nl.rug.ds.bpm.expression.CompositeExpression;
import nl.rug.ds.bpm.expression.ExpressionBuilder;
import nl.rug.ds.bpm.petrinet.ptnet.PlaceTransitionNet;
import nl.rug.ds.bpm.petrinet.ptnet.element.Place;
import nl.rug.ds.bpm.petrinet.ptnet.element.Transition;
import nl.rug.ds.bpm.util.comparator.ComparableComparator;
import nl.rug.ds.bpm.util.exception.MalformedNetException;
import nl.rug.ds.bpm.verification.converter.kripke.KripkeStructureConverterAction;
import nl.rug.ds.bpm.verification.map.AtomicPropositionMap;
import nl.rug.ds.bpm.verification.model.generic.optimizer.proposition.PropositionOptimizer;
import nl.rug.ds.bpm.verification.model.kripke.KripkeStructure;
import nl.rug.ds.bpm.verification.model.kripke.factory.KripkeFactory;
import nl.rug.ds.bpm.verification.model.kripke.postprocess.stutter.StutterOptimizer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StutterStructureTest {
    private static PlaceTransitionNet net = new PlaceTransitionNet();
    private static KripkeFactory factory = new KripkeFactory();

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
    public void verifiableNetTest() {
        net.getTransition("t1").setGuard("x>=0"); //reset guard

        KripkeStructure structure = factory.createStructure();

        AtomicPropositionMap<CompositeExpression> specificationSetPropositionMap = new AtomicPropositionMap<>("p");
        specificationSetPropositionMap.addSpecificationId(ExpressionBuilder.parseExpression("t0"));
        specificationSetPropositionMap.addSpecificationId(ExpressionBuilder.parseExpression("t5"));
        factory.getAtomicPropositionMap().merge(specificationSetPropositionMap);

        assertEquals(0, structure.getAtomicPropositionCount());
        assertEquals(0, structure.getStateCount());
        assertEquals(0, structure.getRelationCount());

        assertEquals("1p0", net.getInitialMarking().toString());

        Set<Transition> enabled = new HashSet<>();
        enabled.add(net.getTransition("t0"));
        assertEquals(enabled, net.getEnabledTransitions(net.getInitialMarking()));

        KripkeStructureConverterAction converterAction = factory.createConverter(net, net.getInitialMarking(), structure);
        converterAction.computeInitial();

        assertEquals(1, structure.getInitial().size());
        assertEquals(6, structure.getAtomicPropositionCount());
        assertEquals(7, structure.getStateCount());
        assertEquals(9, structure.getRelationCount());

        TreeSet<String> unusedAP = new TreeSet<>(new ComparableComparator<String>());
        unusedAP.addAll(structure.getAtomicPropositions());
        unusedAP.removeAll(specificationSetPropositionMap.getAPKeys());

        PropositionOptimizer propositionOptimizer = new PropositionOptimizer(structure, unusedAP);

        assertEquals(2, structure.getAtomicPropositionCount());

        StutterOptimizer stutterOptimizer = new StutterOptimizer(structure);
        stutterOptimizer.linearPreProcess();
        stutterOptimizer.optimize();

        assertEquals(1, structure.getInitial().size());
        assertEquals(4, structure.getStateCount());
        assertEquals(4, structure.getRelationCount());
    }

    @Test
    public void verifiableNetTestwithNoEnabledTransition() {
        net.getTransition("t1").setGuard("x>0"); // Adds case where nothing is enabled

        KripkeStructure structure = factory.createStructure();

        AtomicPropositionMap<CompositeExpression> specificationSetPropositionMap = new AtomicPropositionMap<>("p");
        specificationSetPropositionMap.addSpecificationId(ExpressionBuilder.parseExpression("t0"));
        specificationSetPropositionMap.addSpecificationId(ExpressionBuilder.parseExpression("t5"));
        factory.getAtomicPropositionMap().merge(specificationSetPropositionMap);

        KripkeStructureConverterAction converterAction2 = factory.createConverter(net, net.getInitialMarking(), structure);
        converterAction2.computeInitial();

        assertEquals(1, structure.getInitial().size());
        assertEquals(6, structure.getAtomicPropositionCount());
        assertEquals(8, structure.getStateCount());
        assertEquals(11, structure.getRelationCount());

        TreeSet<String> unusedAP = new TreeSet<>(new ComparableComparator<String>());
        unusedAP.addAll(structure.getAtomicPropositions());
        unusedAP.removeAll(specificationSetPropositionMap.getAPKeys());

        PropositionOptimizer propositionOptimizer = new PropositionOptimizer(structure, unusedAP);

        assertEquals(2, structure.getAtomicPropositionCount());

        StutterOptimizer stutterOptimizer = new StutterOptimizer(structure);
        stutterOptimizer.linearPreProcess();
        stutterOptimizer.optimize();

        assertEquals(1, structure.getInitial().size());
        assertEquals(5, structure.getStateCount());
        assertEquals(6, structure.getRelationCount());
    }

    @Test
    public void verifiableNetTestwithInference() {
        net.getTransition("t1").setGuard("x>=0"); // Adds case where nothing is enabled

        KripkeStructure structure = factory.createStructure();

        AtomicPropositionMap<CompositeExpression> specificationSetPropositionMap = new AtomicPropositionMap<>("p");
        specificationSetPropositionMap.addSpecificationId(ExpressionBuilder.parseExpression("t0"));
        specificationSetPropositionMap.addSpecificationId(ExpressionBuilder.parseExpression("t5"));
        specificationSetPropositionMap.addSpecificationId(ExpressionBuilder.parseExpression("y==true"));
        factory.getAtomicPropositionMap().merge(specificationSetPropositionMap);

        assertEquals(0, structure.getAtomicPropositionCount());
        assertEquals(0, structure.getStateCount());
        assertEquals(0, structure.getRelationCount());

        KripkeStructureConverterAction converterAction = factory.createConverter(net, net.getInitialMarking(), structure);
        converterAction.computeInitial();

        TreeSet<String> unusedAP = new TreeSet<>(new ComparableComparator<String>());
        unusedAP.addAll(structure.getAtomicPropositions());
        unusedAP.removeAll(specificationSetPropositionMap.getAPKeys());

        PropositionOptimizer propositionOptimizer = new PropositionOptimizer(structure, unusedAP);

        assertEquals(3, structure.getAtomicPropositionCount());

        StutterOptimizer stutterOptimizer = new StutterOptimizer(structure);
        stutterOptimizer.linearPreProcess();
        stutterOptimizer.optimize();

        assertEquals(1, structure.getInitial().size());
        assertEquals(3, structure.getAtomicPropositionCount());
        assertEquals(5, structure.getStateCount());
        assertEquals(6, structure.getRelationCount());
    }
}
