package sspeiser.gMockGen

import com.jetbrains.cidr.lang.navigation.OCSymbolNavigationItem
import com.jetbrains.cidr.lang.psi.OCFile
import com.jetbrains.cidr.lang.symbols.OCResolveContext
import com.jetbrains.cidr.lang.symbols.OCSymbol
import com.jetbrains.cidr.lang.symbols.OCVisibility
import com.jetbrains.cidr.lang.symbols.cpp.OCFunctionSymbol
import com.jetbrains.cidr.lang.symbols.cpp.OCStructSymbol
import sspeiser.gMockGen.FunctionProcessor.isVirtual


object FunctionProcessor : com.intellij.util.Processor<OCFunctionSymbol> {
    lateinit var console: OutputConsole
    var functions: List<OCFunctionSymbol> = emptyList()
    var isVirtual: Boolean = true
    override fun process(p0: OCFunctionSymbol?): Boolean {
        if (p0 != null) {
            if(p0.visibility == OCVisibility.PRIVATE && p0.isPureVirtual == false){
                console.info("Function " + p0.name + " with signature " + p0.signatureWithoutParamNames +  " is not marked public or protected, but is already implemented. This function can not be mocked.")
                return true;
            } else if (p0.visibility == OCVisibility.PRIVATE && p0.isPureVirtual == true){
                console.error("Function " + p0.name + " with signature " + p0.signatureWithoutParamNames +  " and not public or protected, but is abstract. Teh whole class can not be mocked.")
                isVirtual = false
                return false;
            }
            else if(p0.isVirtual == false){
                console.error("Function " + p0.name + " with signature " + p0.signatureWithoutParamNames +  " is not marked virtual.")
                isVirtual = false
                return false;
            }
            functions = functions.plus(p0)
            return true
        }
        return false
    }
}

object ParentProcessor : OCStructSymbol.BaseClassProcessor {
    lateinit var console: OutputConsole
    var parents: List<OCStructSymbol> = emptyList()
    var isValid: Boolean = true;
    override fun process(p0: OCSymbol?, p1: OCVisibility?): Boolean {
        if (p0 != null && p1 != null) {
            if (p0 is OCStructSymbol) {
                parents = parents.plus(p0)
                return true
            }
        }
        return false
    }
}


class CppClass {
    val declaration: OCStructSymbol
    val containingFile: OCFile
    val console: OutputConsole
    val parents: List<OCStructSymbol>
    val functions_to_implement: Set<OCFunctionSymbol>
    val isVirtual : Boolean
    constructor(declaration: OCStructSymbol, containingFile: OCFile, console: OutputConsole) {
        this.declaration = declaration
        this.containingFile = containingFile
        this.console = console
        ParentProcessor.console = console
        val resolveContext =
            OCResolveContext.forPsi(OCSymbolNavigationItem(declaration, containingFile.project).targetElement)
        declaration.processAllBaseClasses(ParentProcessor, resolveContext)

        FunctionProcessor.console = console
        declaration.processFunctions(null, FunctionProcessor)

        declaration.nameNoResolve
        for (parent in ParentProcessor.parents) {
            parent.processFunctions(null, FunctionProcessor);
        }
        this.functions_to_implement = FunctionProcessor.functions.toSet()
        this.isVirtual = FunctionProcessor.isVirtual
        FunctionProcessor.functions = emptyList()
        this.parents = ParentProcessor.parents
        ParentProcessor.parents = emptyList()

    }

}