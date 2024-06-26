package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
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
        int ogMaxReg = maxRegisters;
        boolean success;
        do {
            ollirResult.getOllirClass().buildCFGs();
            var CFG = ollirResult.getOllirClass();

            var regAlloc = new RegAlloc(CFG, maxRegisters);
            success = regAlloc.allocateRegisters();

            maxRegisters++;
        } while (!success);
        maxRegisters--;

        //If we had to increment the max register
        if (maxRegisters!=ogMaxReg) {
            var message = String.format("%s register(s) is not enough. Cannot allocate with less than %s", ogMaxReg, maxRegisters);
            Report error = Report.newError(
                    Stage.OPTIMIZATION,
                    0,
                    0,
                    message,
                    null);
            ollirResult.getReports().add(error);
        }
        return ollirResult;
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {

        var visitor = new ASTVisitor(semanticsResult.getSymbolTable());
        visitor.visit(semanticsResult.getRootNode());

        if (CompilerConfig.getOptimize(semanticsResult.getConfig())) {
            while (true) {
                var constPropagationVisitor = new ASTConstPropagationVisitor();
                constPropagationVisitor.visit(semanticsResult.getRootNode());

                var constFoldingVisitor = new ASTConstFoldingVisitor();
                constFoldingVisitor.visit(semanticsResult.getRootNode());

                // If none of the visitors made changes we end the loop
                if (!constPropagationVisitor.madeChanges() && !constFoldingVisitor.madeChanges()) break;
            }
        }
        return semanticsResult;
    }
}
