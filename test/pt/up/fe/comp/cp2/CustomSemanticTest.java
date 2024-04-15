package pt.up.fe.comp.cp2;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.specs.util.SpecsIo;

public class CustomSemanticTest {

    @Test
    public void VarDeclaredLocal() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/VarDeclaredLocal.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void VarDeclaredMethod() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/VarDeclaredMethod.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void VarDeclaredClassField() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/VarDeclaredClassField.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void VarDeclaredImport() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/VarDeclaredImport.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void VarDeclaredFail() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/VarDeclaredFail.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void BinOperationIntValid() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/BinOperationIntValid.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void BinOperationIntBool() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/BinOperationIntBool.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void BinOperationBoolInt() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/BinOperationBoolInt.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void BinOperationBoolValid() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/BinOperationBoolValid.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void ArithmeticArrayOperation() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/ArithmeticArrayOperation.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void ArrayAccess() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/ArrayAccess.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void ArrayAccessOnBool() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/ArrayAccessOnBool.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void ArrayAccessWithBool() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/ArrayAccessWithBool.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void AssignIntToBool() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/AssignIntToBool.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void Factorial() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/Factorial.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void ThisInStatic() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/ThisInStatic.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void ValidThis() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/ValidThis.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void intEqualsImport() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/intEqualsImport.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void ArrayIndexInvalid() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/ArrayIndexInvalid.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void miscDuplicatedFieldInvalid() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/miscDuplicatedFieldInvalid.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void miscDuplicatedMethodInvalid() {
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/miscDuplicatedMethodInvalid.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void miscDuplicatedImportClassInvalid(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/miscDuplicatedImportClassInvalid.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void miscDuplicatedImportInvalid(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/miscDuplicatedImportInvalid.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void miscDuplicatedLocalInvalid(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/miscDuplicatedLocalInvalid.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void miscDuplicatedParamInvalid(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/miscDuplicatedParamInvalid.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void miscLengthAsNameOk(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/miscLengthAsNameOk.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void varargsInFieldInvalid(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/varargsInFieldInvalid.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void varargsInLocalInvalid(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/varargsInLocalInvalid.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void varargsInReturnInvalid(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/varargsInReturnInvalid.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void arrayInitAccessOk(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/arrayInitAccessOk.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void ArrayInitLengthOk(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/ArrayInitLengthOk.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void arrayInitOnCall1Ok(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/arrayInitOnCall1Ok.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void arrayInitOnCall2Ok(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/arrayInitOnCall2Ok.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void arrayInitOnCall3Ok(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/arrayInitOnCall3Ok.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void arrayInitReturnOk(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/arrayInitReturnOk.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void fieldInStaticInvalid(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/fieldInStaticInvalid.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

    @Test
    public void inheritedMethodCallSimpleOk(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/inheritedMethodCallSimpleOk.jmm"));
        TestUtils.noErrors(result);
    }

    @Test
    public void miscFieldAccessInvalid(){
        var result = TestUtils.analyse(SpecsIo.getResource("pt/up/fe/comp/cp2/customsemantic/miscFieldAccessInvalid.jmm"));
        System.out.println(result.getReports());
        TestUtils.mustFail(result);
    }

}
