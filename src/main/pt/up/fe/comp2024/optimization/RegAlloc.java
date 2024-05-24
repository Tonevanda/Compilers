package pt.up.fe.comp2024.optimization;

import org.antlr.v4.codegen.model.SrcOp;
import org.antlr.v4.runtime.misc.Pair;
import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2024.ast.Kind;

import java.util.*;
import java.util.stream.IntStream;

public class RegAlloc {

    ClassUnit cfg;
    int maxRegisters;

    // Each instruction has a list of sets of variables, with each set being a list of strings
    // First set is liveIn, second set is liveOut
    HashMap<Method, HashMap<Node, Pair<Set<String>, Set<String>>>> sets;
    HashMap<Method, HashMap<String, Set<String>>> graph;
    HashMap<Method, HashMap<String, Integer>> color;

    public RegAlloc(ClassUnit cfg, int maxRegisters){
        this.cfg = cfg;
        this.maxRegisters = maxRegisters;
        this.sets = new HashMap<>();
        this.graph = new HashMap<>();
        this.color = new HashMap<>();
    }

    private void initSets() {
        for (var method : cfg.getMethods()) {
            this.sets.put(method, new HashMap<>());
            for (Node instr : method.getInstructions())  {
                this.sets.get(method).put(instr, new Pair<>(new HashSet<>(), new HashSet<>()));
            }
        }
    }

    private void buildSets(){
        initSets();
        for (var method : cfg.getMethods()) {
            boolean stay = true;
            while (stay) {
                stay = false;
                for (Node instr : method.getInstructions())  {
                    var oldLiveIn = new HashSet<>(this.sets.get(method).get(instr).a);
                    var oldLiveOut = new HashSet<>(this.sets.get(method).get(instr).b);
                    Set<String> liveIn;
                    Set<String> liveOut = this.sets.get(method).get(instr).b;
                    // LIVEin(n) = use[n] U (LIVEout(n) - def[n])
                    liveIn = use(instr);
                    liveOut.removeAll(def(instr));
                    liveIn.addAll(liveOut);
                    liveOut.clear();
                    // LIVEout(n) = U LIVEin(s)
                    for (Node succ :instr.getSuccessors())
                        if (succ.getNodeType()!=NodeType.END) liveOut.addAll(this.sets.get(method).get(succ).a);
                    // update this.sets
                    Pair<Set<String>, Set<String>> newPair = new Pair(liveIn, liveOut);
                    this.sets.get(method).put(instr, newPair);
                    // if something changed, cant leave will be true
                    stay = stay || (!oldLiveIn.equals(liveIn) || !oldLiveOut.equals(liveOut));
                }
            }
        }
    }

    private Set<String> use(Node inst) {
        var instType = inst.toInstruction().getInstType();
        Set<String> use = new HashSet<>();
        switch (instType){
            case ASSIGN:
                AssignInstruction assInst = (AssignInstruction) inst;
                switch (assInst.getRhs().getInstType()) {
                    case NOPER:
                        SingleOpInstruction single_rhs = (SingleOpInstruction) assInst.getRhs();
                        if (!single_rhs.getSingleOperand().isLiteral()) {
                            Operand op = (Operand) single_rhs.getSingleOperand();
                            use.add(op.getName());
                        }
                        break;
                    case BINARYOPER:
                        BinaryOpInstruction binary_rhs = (BinaryOpInstruction) assInst.getRhs();
                        Operand op;
                        if (!binary_rhs.getLeftOperand().isLiteral()) {
                            op = (Operand) binary_rhs.getLeftOperand();
                            use.add(op.getName());
                        }
                        if (!binary_rhs.getRightOperand().isLiteral()) {
                            op = (Operand) binary_rhs.getRightOperand();
                            use.add(op.getName());
                        }
                        break;
                }
                break;
            case RETURN:
                Operand op;
                ReturnInstruction retInst = (ReturnInstruction) inst;
                if (retInst.hasReturnValue()){
                    Element e = retInst.getOperand();
                    if (!e.isLiteral()) {
                        op = (Operand) e;
                        use.add(op.getName());
                    }
                }
                break;
        }
        return use;
    }

    private Set<String> def(Node inst) {
        var instType = inst.toInstruction().getInstType();
        Set<String> def = new HashSet<>();
        Operand op;
        switch (instType) {
            case ASSIGN:
                AssignInstruction assInst = (AssignInstruction) inst;
                op = (Operand) assInst.getDest();
                def.add(op.getName());
                break;
        }
        return def;
    }

    private void buildGraph() {
        for (var method : cfg.getMethods()) {
            this.graph.put(method, new HashMap<>());
            for (var var : method.getVarTable().keySet()) {
                if (method.getVarTable().get(var).getScope() == VarScope.LOCAL) {
                    this.graph.get(method).put(var, new HashSet<>());
                }
            }
        }
    }

    public void buildEdges() {
        for (var method : cfg.getMethods()) {
            for (var inst : this.sets.get(method).keySet()) {
                var liveIn = new HashSet<>(this.sets.get(method).get(inst).a);
                var liveOut = new HashSet<>(this.sets.get(method).get(inst).b);
                liveOut.addAll(def(inst));
                for (String a : liveIn) {
                    for (String b : liveIn) {
                        if (!a.equals(b)) {
                            this.graph.get(method).get(a).add(b);
                            this.graph.get(method).get(b).add(a);
                        }
                    }
                }
                for (String a : liveOut) {
                    for (String b : liveOut) {
                        if (!a.equals(b)) {
                            this.graph.get(method).get(a).add(b);
                            this.graph.get(method).get(b).add(a);
                        }
                    }
                }
            }
        }
    }

    public boolean colorGraph() {
        for (var method : this.cfg.getMethods()) {
            this.color.put(method, new HashMap<>());
            Stack stack = new Stack();
            while (!this.graph.get(method).isEmpty()) {
                boolean foundOne = false;
                List keys = new ArrayList(this.graph.get(method).keySet());
                for (var var : keys) {
                    if (this.maxRegisters==0 || this.graph.get(method).get(var).size()<(this.maxRegisters-method.getParams().size())) {
                        foundOne = true;
                        //add to stack
                        stack.add(new Pair<>(var, this.graph.get(method).get(var)));
                        this.graph.get(method).remove(var);
                    }
                }
                if (!foundOne) return false;
            }
            // register alloc
            int register = 1 + method.getParams().size();
            while (!stack.empty()) {
                Pair<String, Set<String>> p = (Pair) stack.pop();
                List<Integer> minNeighbour = new ArrayList<>();
                // put node back
                this.graph.get(method).put(p.a, p.b);
                for (String otherNode : p.b) {
                    minNeighbour.add(this.color.get(method).get(otherNode));
                }
                // update
                Integer color = null;
                for (int i = register; i<=((this.maxRegisters==0)?Integer.MAX_VALUE:this.maxRegisters+register); i++) {
                    if (minNeighbour.contains(i)) continue;
                    color = i;
                    break;
                }
                if (color==null) return false;
                method.getVarTable().get(p.a).setVirtualReg(color);
                this.color.get(method).put(p.a,color);
            }
        }
        return true;
    }

    public boolean allocateRegisters(){
        buildSets();
        buildGraph();
        buildEdges();
        return colorGraph();
    }

}
