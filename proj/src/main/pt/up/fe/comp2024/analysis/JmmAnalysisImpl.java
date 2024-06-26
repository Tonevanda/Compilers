package pt.up.fe.comp2024.analysis;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.analysis.passes.UndeclaredVariable;
import pt.up.fe.comp2024.analysis.passes.DuplicatedMisc;
import pt.up.fe.comp2024.analysis.passes.UndeclaredMethod;
import pt.up.fe.comp2024.analysis.passes.IncorrectStatic;
import pt.up.fe.comp2024.analysis.passes.IncorrectFieldCall;
import pt.up.fe.comp2024.analysis.passes.KeywordUsage;
import pt.up.fe.comp2024.analysis.passes.IndexingNotArray;
import pt.up.fe.comp2024.analysis.passes.ArrayIndexNotInt;
import pt.up.fe.comp2024.analysis.passes.WrongIfConditionType;
import pt.up.fe.comp2024.analysis.passes.ExtendsNotImported;
import pt.up.fe.comp2024.analysis.passes.WrongWhileConditionType;
import pt.up.fe.comp2024.analysis.passes.DifferentTypeOperands;
import pt.up.fe.comp2024.analysis.passes.IncompatibleReturn;
import pt.up.fe.comp2024.analysis.passes.IncompatibleAssignment;
import pt.up.fe.comp2024.analysis.passes.IncompatibleArguments;
import pt.up.fe.comp2024.analysis.passes.IncompatibleArrayInit;
import pt.up.fe.comp2024.analysis.passes.IncorrectVarargs;
import pt.up.fe.comp2024.analysis.passes.IncorrectMainDeclaration;
import pt.up.fe.comp2024.symboltable.JmmSymbolTableBuilder;

import java.util.ArrayList;
import java.util.List;

public class JmmAnalysisImpl implements JmmAnalysis {


    private final List<AnalysisPass> analysisPasses;

    public JmmAnalysisImpl() {

        this.analysisPasses = List.of(
                new IncorrectMainDeclaration(),
                new ExtendsNotImported(),
                new UndeclaredVariable(),
                new DuplicatedMisc(),
                new UndeclaredMethod(),
                new KeywordUsage(),
                new IncorrectFieldCall(),
                new IncorrectStatic(),
                new DifferentTypeOperands(),
                new IndexingNotArray(),
                new ArrayIndexNotInt(),
                new WrongIfConditionType(),
                new WrongWhileConditionType(),
                new IncompatibleArrayInit(),
                new IncompatibleAssignment(),
                new IncorrectVarargs(),
                new IncompatibleArguments(),
                new IncompatibleReturn()
        );

    }

    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {

        JmmNode rootNode = parserResult.getRootNode();

        SymbolTable table = JmmSymbolTableBuilder.build(rootNode);

        List<Report> reports = new ArrayList<>();

        // Visit all nodes in the AST
        for (var analysisPass : analysisPasses) {
            try {
                var passReports = analysisPass.analyze(rootNode, table);
                reports.addAll(passReports);
                if(hasErrors(reports)){
                    break;
                }
            } catch (Exception e) {
                reports.add(Report.newError(Stage.SEMANTIC,
                        -1,
                        -1,
                        "Problem while executing analysis pass '" + analysisPass.getClass() + "'",
                        e)
                );
            }

        }

        return new JmmSemanticsResult(parserResult, table, reports);
    }

    // Checks if there are any errors in the reports
    private boolean hasErrors(List<Report> reports) {
        return reports.stream().anyMatch(report -> report.getStage() == Stage.SEMANTIC);
    }
}
