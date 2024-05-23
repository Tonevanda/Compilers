package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp2024.CompilerConfig;

import java.util.Collections;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {

        // Get the register allocation value from the config
        int maxRegisters = CompilerConfig.getRegisterAllocation(ollirResult.getConfig());

        // If it's -1, return the result without optimizing
        if (maxRegisters == -1) return ollirResult;

        // Otherwise, optimize the result
        ollirResult.getOllirClass().buildCFGs();
        var CFG = ollirResult.getOllirClass();

        var regAlloc = new RegAlloc(CFG, maxRegisters);
        regAlloc.allocateRegisters();

        return ollirResult;
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {

        var visitor = new ASTVisitor(semanticsResult.getSymbolTable());
        visitor.visit(semanticsResult.getRootNode());
        if (CompilerConfig.getOptimize(semanticsResult.getConfig())) {
            var constPropagationVisitor = new ASTConstPropagationVisitor();
            constPropagationVisitor.visit(semanticsResult.getRootNode());

            var constFoldingVisitor = new ASTConstFoldingVisitor();
            constFoldingVisitor.visit(semanticsResult.getRootNode());
        }
        return semanticsResult;
    }
}
