/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package editor.domain.play;

import common.Triple;
import common.Tuple;
import editor.domain.elements.ClockVar;
import editor.domain.elements.DtaEdge;
import editor.domain.elements.DtaLocation;
import editor.domain.elements.Place;
import editor.domain.elements.Transition;
import editor.domain.grammar.ColorVarsBinding;
import editor.domain.grammar.DomainElement;
import editor.domain.grammar.ExpressionLanguage;
import editor.domain.grammar.ParserContext;
import editor.domain.values.Bound;
import editor.domain.values.EvaluatedFormula;
import editor.domain.values.IntScalarValue;
import editor.domain.values.MultiSet;
import editor.domain.values.RealScalarValue;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** A state of a GSPN and optionally a state of a DTA.
 *
 * @author elvio
 */
public class JointState implements Serializable, AbstractMarking {
    // Place markings 
    public Map<Place, EvaluatedFormula> marking = new HashMap<>();
    
    // GSPN transitions that can fire
    public Set<FirableWithBindings<Transition>> enabledTransitions 
            = new HashSet<>();
    
    // The flow of the continuous places
    public Map<Place, EvaluatedFormula> flowFunction = new HashMap<>();
    
    // The priority level of the enabled transitions (0 = timed)
    public int priorityLevel;
    
    // DTA location
    public DtaLocation currLoc;
    
    // DTA Inner/Boundary edges that can be triggered
    public Set<DtaEdge> enabledDtaEdges = new HashSet<>();
    
    // Is this a state where a DTA Inner edge must be triggered
    public boolean isDtaInnerMove = false;
    
    // The value of the continous variables (clocks)
    public Map<ClockVar, Double> continuousVars = new HashMap<>();
    
    // Marking representation
    public String markingText;
    
    // != null if there is some unrecoverable error.
    public String errorDescription;
    
    // Cardinal number of this state in a path trace
    public int stateNum;
    
    // Remembers if this is a timed simulation or not.
    public boolean isTimedSimulation;
    
    // All the following fields are used only during a timed simulation.
    
    // Elapsed time from the beginning of the simulation
    public double time;
    
    // Time boundary of the next event that changes the enabling of the transitions,
    // or -1 if there is no time bound.
    public double nextTimeBound;
    
    // Suggested time instant for the next timed event
    public double avgTimeNextTransition;
    
    // Age of the general transitions
    public Map<Tuple<Transition, ColorVarsBinding>, Double> ageOfTransitions 
            = new HashMap<>();
    
    // Timed events are urgent (i.e. behave like immediate events)
    // This happens when general (deterministic) transitions have no time left before firing
    public boolean timedEventsAreUrgent = false;
    
    // Boundary DTA edges are urgent
    public boolean boundaryDtaEdgesAreUrgent = false;
    
    // Fixed time step for those events that are triggered at specific time points,
    // like boundary DTA edges (fire when x = value) or general transitions.
    // Exponential transitions do not have a fixed time-to-fire.
    public Map<Tuple<Firable, ColorVarsBinding>, Bound> firingTimeSupport 
            = new HashMap<>();
    
    
    
    // Special singleton bottom location instance.
    public static final DtaLocation BOTTOM_LOCATION = 
            new DtaLocation("BOT", false, DtaLocation.FinalType.REJECTING, "", new Point2D.Double());
    
    public static final Map<DomainElement, EvaluatedFormula> EMPTY_MAP 
            = new HashMap<>();
    
    @Override
    public EvaluatedFormula getMarkingOfPlace(Place place) {
        EvaluatedFormula m = marking.get(place);
        if (m != null)
            return m;
        if (place.isInNeutralDomain())
            return (place.isDiscrete() ? IntScalarValue.ZERO : RealScalarValue.ZERO);
        else
            return MultiSet.makeNew(place.isDiscrete() ? EvaluatedFormula.Type.INT :EvaluatedFormula.Type.REAL,
                                    place.getColorDomain(), EMPTY_MAP);
    }

    @Override
    public double getValueOfClockVariable(ClockVar clockVar) {
        return continuousVars.get(clockVar);
    }
    
    
    public void setErrorState(String errDescr) {
        marking.clear();
        enabledTransitions.clear();
        currLoc = null;
        enabledDtaEdges.clear();
        enabledTransitions.clear();
        markingText = "<<ERROR>> " + errDescr;
        errorDescription = errDescr;
    }
    
    public boolean isErrorState() {
        return (errorDescription != null);
    }
    
    public boolean isTangible() { 
        return priorityLevel == 0 && !isDtaMove() && !timedEventsAreUrgent;    
    }
    
    public boolean isVanishing() {
        return !isTangible();//priorityLevel > 0;
    }
    
    public boolean isDtaMove() {
        return isDtaInnerMove;
    }
    
    public boolean isTop() {
        return (currLoc != null) && (currLoc.isFinalAccepting());
    }
    
    public boolean isBot() {
        if (currLoc == BOTTOM_LOCATION)
            return true;
        return (currLoc != null) && (currLoc.isFinalRejecting());
    }

    public String getMarkingRepr(ParserContext gspnContext) {
        // Prepare the marking text representation
        StringBuilder sb = new StringBuilder();
        if (isTimedSimulation)
            sb.append("@").append(String.format(Locale.US, "%.4f", time)).append(": ");
        if (currLoc != null) {
            sb.append("[");
            sb.append(currLoc.getUniqueName());
            if (isTimedSimulation) {
                for (Map.Entry<ClockVar, Double> e : continuousVars.entrySet())
                    sb.append(", ").append(e.getKey().getUniqueName())
                      .append("=").append(String.format(Locale.US, "%.2f", e.getValue()));
            }
            sb.append("] ");
        }
        boolean emptyMarking = true;
        int i = 0;
        Iterator<Map.Entry<Place, EvaluatedFormula>> it = marking.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Place, EvaluatedFormula> tkn = it.next();
            if (tkn.getValue().equalsZero())
                continue;
            if (i++ > 0)
                sb.append(", ");
            sb.append(tkn.getKey().getUniqueName());
            sb.append("=");
            sb.append(tkn.getValue().toStringFormat(ExpressionLanguage.PNPRO, "%.2f"));
            emptyMarking = false;
        }
        if (emptyMarking)
            sb.append("<<empty marking>>");
        
        return sb.toString();
    }
    
    public int countBindingsWithLimitedTimeSupport(Firable event, boolean includeImpulses) {
        int count = 0;
        for (Map.Entry<Tuple<Firable, ColorVarsBinding>, Bound> e : firingTimeSupport.entrySet()) {
            if (e.getKey().x == event) {
                if (e.getValue().isImpulse() && !includeImpulses)
                    continue;
                count++;
            }
        }
        return count;
    }
    public int countBindingsWithLimitedTimeSupport(Firable event) {
        return countBindingsWithLimitedTimeSupport(event, true);
    }
    
    public String firstTimeOfEvent(Firable event) {
        int count = 0;
        double min = Double.MAX_VALUE, max = Double.MIN_VALUE;
        for (Map.Entry<Tuple<Firable, ColorVarsBinding>, Bound> e : firingTimeSupport.entrySet()) {
            if (e.getKey().x == event) {
                if (min > e.getValue().a)
                    min = e.getValue().a;
                if (max < e.getValue().b)
                    max = e.getValue().b;
                count++;
            }
        }
        assert count > 0;
        String minStr = String.format(Locale.US, "%.4f", min + time);
        String maxStr = String.format(Locale.US, "%.4f", max + time);
        if (count == 1) {
            if (min == max)
                return "[fires at "+minStr+"]";
            else
                return "[fires in range: "+minStr+", "+maxStr+"]";
        }
        else {
            if (min == max)
                return "[first fires at "+minStr+"]";
            else
                return "[first fires in range: "+minStr+", "+maxStr+"]";
        }
    }
    
//    public String timeOfEvent(Firable event, ColorVarsBinding binding) {
//        assert fixedTimeToFire.containsKey(new Tuple(event, binding));
//        return String.format(Locale.US, "%.4f", fixedTimeToFire.get(new Tuple(event, binding)) + time);
//    }
    
    
    // Should show the "let time elapse" event in the GUI?
    public boolean allowTimeElapse() {
        boolean ok = isTangible() && 
                     isTimedSimulation &&
                     !enabledTransitions.isEmpty();
        return ok;
    }
    
    public FirableWithBindings<Transition> getEnabledFirableBindingsOf(Transition trn) {
        for (FirableWithBindings<Transition> fwb : enabledTransitions)
            if (fwb.firable == trn)
                return fwb;
        return null; // trn is not enabled
    }
    
    public String ConvertToString() {
        StringBuilder sb = new StringBuilder();
        if (isTimedSimulation)
            sb.append("@").append(String.format(Locale.US, "%.4f", time)).append(": ");
        if (currLoc != null) {
            sb.append("[");
            sb.append(currLoc.getUniqueName());
            if (isTimedSimulation) {
                for (Map.Entry<ClockVar, Double> e : continuousVars.entrySet())
                    sb.append(", ").append(e.getKey().getUniqueName())
                      .append("=").append(String.format(Locale.US, "%.2f", e.getValue()));
            }
            sb.append("] ");
        }
        boolean emptyMarking = true;
        int i = 0;
        Iterator<Map.Entry<Place, EvaluatedFormula>> it = marking.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Place, EvaluatedFormula> tkn = it.next();
            if (tkn.getValue().equalsZero())
                continue;
            if (i++ > 0)
                sb.append(", ");
            sb.append(tkn.getKey().getUniqueName());
            sb.append("=");
            sb.append(tkn.getValue().toStringFormat(ExpressionLanguage.PNPRO, "%.2f"));
            emptyMarking = false;
        }
        if (emptyMarking)
            sb.append("<<empty marking>>");
        
        return sb.toString();
    }
    
    
//    // Old code: selects the next event to fire randomly, ignoring transition weights, rates, etc...
//    public Tuple<Firable, ColorVarsBinding> selectNextFirableEventRandomlyOLD(Random randomGenerator) {
//        // Make the list of firable transitions
//        ArrayList<Tuple<Firable, ColorVarsBinding>> firables 
//                = new ArrayList<>();
//        for (FirableWithBindings<Transition> fwb : enabledTransitions)
//            if (fwb.firable.isFiringInstantaneous())
//                for (ColorVarsBinding binding : fwb.bindings)
//                    firables.add(new Tuple<Firable, ColorVarsBinding>(fwb.firable, binding));
//        for (DtaEdge edge : enabledDtaEdges)
//            firables.add(new Tuple<Firable, ColorVarsBinding>(edge, null));
//        if (allowTimeElapse())
//            firables.add(new Tuple<Firable, ColorVarsBinding>(TimeElapse.SINGLETON, null));
//
//        if (firables.isEmpty())
//            return null; // nothing to fire
//        
//        // Select the firing transition
//        int firedIndex = randomGenerator.nextInt(firables.size());
//        return firables.get(firedIndex);
//    }
    
//    // Generate a random sample from the negative exponential distribution
//    private static double negExp(double mean, Random randomGenerator) {
//        return -Math.log(randomGenerator.nextDouble()) * mean;
//    }
//    // Generate the firingTime of an exponential transition with a given rate
//    private static double randomExpFiringTime(double rate, Random randomGenerator) {
//        return negExp(1.0 / rate, randomGenerator);
//    }
    
    
    public Triple<Firable, ColorVarsBinding, Double> 
        selectNextFirableEvent(ParserContext gspnContext) 
    {
        Triple<Firable, ColorVarsBinding, Double> nextEvent = null;
        
        //-------------------------------------
        // Select next DTA inner edge randomly. 
        if (isDtaMove()) {
            // If the DTA is properly deterministic, there should be just one edge.
            ArrayList<Tuple<Firable, ColorVarsBinding>> firables = new ArrayList<>();
            for (DtaEdge edge : enabledDtaEdges)
                firables.add(new Tuple<>(edge, null));
            
            int firedIndex = StatisticalDistributions.randIntN(firables.size());
            Tuple<Firable, ColorVarsBinding> tcvb = firables.get(firedIndex);
            nextEvent = new Triple<>(tcvb.x, tcvb.y, -1.0);
        }
        //-------------------------------------
        // Select next immediate transition
        else if (isVanishing()) {
            // Use Gillespie algorithm over the immediate transition's weights
            ArrayList<Tuple<Transition, ColorVarsBinding>> firables = new ArrayList<>();
            ArrayList<Double> weights = new ArrayList<>();
            for (FirableWithBindings<Transition> fwb : enabledTransitions) {
                if (fwb.firable.isFiringInstantaneous()) {
                    for (ColorVarsBinding binding : fwb.bindings) {
                        firables.add(new Tuple<>(fwb.firable, binding));
                        double w = fwb.firable.evaluateWeight(gspnContext, this).getScalarReal();
                        weights.add(w);
                        //System.out.println("IMM "+fwb.firable.getUniqueName()+" w="+w);
                    }
                }
            }
            Tuple<Transition, ColorVarsBinding> tcvb;
            tcvb = firables.get(gillespie(weights));            
            nextEvent = new Triple<>(tcvb.x, tcvb.y, -1.0);
        }
        //-------------------------------------
        // Select next (timed) event
        else if (isTangible()) {
            // Get the enabled non-immediate transitions
            ArrayList<Tuple<Transition, ColorVarsBinding>> firables = new ArrayList<>();
            for (FirableWithBindings<Transition> fwb : enabledTransitions) {
                if (fwb.firable.isFiringInstantaneous()) {
                    for (ColorVarsBinding binding : fwb.bindings) {
                        firables.add(new Tuple<>(fwb.firable, binding));
                    }
                }
            }
            if (!isTimedSimulation) {
                // there are no rates nor times, select randomly
                int firedIndex = StatisticalDistributions.randIntN(firables.size());
                Tuple<Transition, ColorVarsBinding> tcvb = firables.get(firedIndex);
                nextEvent = new Triple<>(tcvb.x, tcvb.y, -1.0);
            }
            else {
                // Draw firing times from the transition distributions
                int firedIndex = 0;
                double minTime = Double.MAX_VALUE;
                for (int i=0; i<firables.size(); i++) {
                    Transition trn = (Transition)firables.get(i).x;
                    double firingTime;
                    if (trn.isExponential()) {
                        double rate = trn.evaluateDelay(gspnContext, this, firables.get(i).y).getScalarReal();
                        // Draw  new sample from the negative exponential distribution of the specified rate
                        firingTime = StatisticalDistributions.randomExpFiringTime(rate);
//                        System.out.println("randomExpFiringTime["+trn.getUniqueName()+", lambda="+rate+"] = "+firingTime);
                    }
                    else {
                        firingTime = trn.evaluateGeneralFiringTime(gspnContext, this, firables.get(i).y);
                        Double age = ageOfTransitions.get(firables.get(i));
                        if (age != null)
                            firingTime -= age;
                    }
                    if (firingTime < minTime) {
                        firedIndex = i;
                        minTime = firingTime;
                    }
                }
                Tuple<Transition, ColorVarsBinding> tcvb = firables.get(firedIndex);
                nextEvent = new Triple<>(tcvb.x, tcvb.y, minTime);
            }
        }
        
        return nextEvent;
    }
    
    
    private int gillespie(ArrayList<Double> weights) {
        double weightsSum = 0;
        for (double w : weights)
            weightsSum += w;
        
        // select randomly between 0 and w
        double sel = StatisticalDistributions.uniform01() * weightsSum;
        double reachedSel = 0;
        int i = 0;
        while (i < weights.size() - 1) {
            reachedSel += weights.get(i);
            if (reachedSel > sel)
                break;
            i++;
        }
        System.out.println("gillespie weightsSum="+weightsSum+" sel="+sel+" reachedSel="+reachedSel+" i="+i);
        return i;
    }
    
    /*public static void main(String[] args) {
        Random randGen = new Random();
        for (int i=0; i<10; i++) {
            System.out.println(randomExpFiringTime(10, randGen));
        }
    }*/
}
