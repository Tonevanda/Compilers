package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2024.ast.Kind;

import java.util.ArrayList;
import java.util.HashMap;

public class RegAlloc {

    ClassUnit cfg;
    int maxRegisters;

    // Each instruction has a list of sets of variables, with each set being a list of strings
    // First set is liveIn, second set is liveOut
    // TODO: Probably change this
    HashMap<Instruction, ArrayList<ArrayList<Element>>> sets;

    public RegAlloc(ClassUnit cfg, int maxRegisters){
        this.cfg = cfg;
        this.maxRegisters = maxRegisters;
        this.sets = new HashMap<>();
    }

    private void buildSets(){
        var methods = cfg.getMethods();
        for(var method : methods){
            method.getBeginNode();
            // For every method, visit every instruction and initialize the liveIn and liveOut sets as empty
            // And create the use and def sets
            // TODO: Devia percorrer os nós, não as instruções
            //  Fazer isso através do método getSucc e assim
            var instructions = method.getInstructions();
            for(var instruction : instructions){
                var sets = getSets(instruction);

                this.sets.put(instruction, sets);
            }
        }
        for(var method : methods){
            // For every method, visit every instruction and build the 4 sets for each instruction
            var instructions = method.getInstructions();
            for(var instruction : instructions){
                var sets = new ArrayList<ArrayList<Element>>();
                var instructionSets = new ArrayList<Element>();


                sets.add(instructionSets);

                this.sets.put(instruction, sets);
            }
        }
    }

    // TODO: O use e o def não precisam de ser guardados no field sets, aquilo devia ser só live-in e live-out
    //  Talvez esta função deva retornar o use e def sets, portanto
    private static ArrayList<ArrayList<Element>> getSets(Instruction instruction) {
        var sets = new ArrayList<ArrayList<Element>>();
        var liveIn = new ArrayList<Element>();
        var liveOut = new ArrayList<Element>();

        sets.add(liveIn);
        sets.add(liveOut);

        var use = new ArrayList<Element>();
        var def = new ArrayList<Element>();

        var instType = instruction.getInstType();
        Element dest;
        switch (instType){
            case ASSIGN:
                dest = ((AssignInstruction) instruction).getDest();
                def.add(dest);
        }

        sets.add(use);
        sets.add(def);
        return sets;
    }

    public void allocateRegisters(){
        buildSets();

    }

}
