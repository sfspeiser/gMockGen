package sspeiser.gMockGen

import com.intellij.formatting.FormatterEx
import com.intellij.formatting.FormattingMode
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cidr.lang.formatting.OCPsiBasedFormattingModel
import com.jetbrains.cidr.lang.navigation.OCSymbolNavigationItem
import com.jetbrains.cidr.lang.psi.OCFile
import com.jetbrains.cidr.lang.psi.OCStruct
import com.jetbrains.cidr.lang.symbols.OCResolveContext
import io.ktor.util.*


class GenAction : AnAction() {
    lateinit var console: OutputConsole;
    lateinit var oc_file: OCFile;
    lateinit var includes: List<OCFile>
    override fun actionPerformed(e: AnActionEvent) {

        val currentProject: Project? = e.getProject();
        console = OutputConsole.get(currentProject!!);
        console.clear()

        // If an element is selected in the editor, add info about it.
        val psi_file = e.getData(CommonDataKeys.PSI_FILE);

        if (psi_file == null) {
            console.error("Could not find file")
            return
        }
        if (psi_file !is OCFile) {
            console.error("Not processing a C++ file")
            return
        }
        oc_file = psi_file;


        val caret = e.getData(CommonDataKeys.CARET);
        if (caret == null) {
            console.error("Could not find crate")
            return
        }


        val element: PsiElement? = psi_file.findElementAt(caret.offset)
        if (element == null) {
            console.error("Could not find an Psi Element at the crate location")
            return
        }

        val class_element = PsiTreeUtil.getParentOfType(element, OCStruct::class.java)

        if (class_element == null) {
            console.error("Could not find an Class Element at the crate location")
            return
        }
        val class_symbol = class_element.symbol;

        if (class_symbol == null) {
            console.error("Could not find an Class Element at the crate location")
            return
        }

        val class_to_mock = CppClass(class_symbol, oc_file, console)
        if (!class_to_mock.isVirtual) {
            console.error("Trying to mock a class which isn't pure virtual.")
            return
        }

        val resolveContext =
            OCResolveContext.forPsi(OCSymbolNavigationItem(class_symbol, currentProject).targetElement)
        var mock_string: String = "";
        val original_root_file_path = oc_file.virtualFile.path.replace(currentProject.basePath + "/", "")
        val include_guard =
            (original_root_file_path.replace("/", "_").replace(class_symbol.name, "mock_" + class_symbol.name)
                .replace(".h", "_H_")).toUpperCasePreservingASCIIRules()
        mock_string += "#ifndef " + include_guard + "\n"
        mock_string += "#define " + include_guard + "\n"

        mock_string += "#include <gmock/gmock.h>\n"
        mock_string += "#include \"" + original_root_file_path + "\"\n\n"
        mock_string += "namespace " + class_symbol.getResolvedQualifiedName(resolveContext).toString()
            .replace("::" + class_symbol.name, "") + "{\n"
        mock_string += "class Mock" + class_symbol.name + " final : public ::" + class_symbol.getResolvedQualifiedName(
            resolveContext
        ) + " {\n"

        for (func in class_to_mock.functions_to_implement) {
            mock_string += "   MOCK_METHOD(" + func.returnType.name + ", " + func.name + ", ("
            for (param in func.getParameterSymbols()) {
                mock_string += param.getEffectiveResolvedType(resolveContext).name + ", "
            }
            if (func.parameterSymbols.size > 0) {
                mock_string = mock_string.substring(0, mock_string.length - 2)

            }
            mock_string += ") , ("
            if (func.isConst) {
                mock_string += "const, "
            }
            if (func.type.getExceptionSpecification().isNoexcept) {
                mock_string += "noexcept, "
            }

            mock_string += "override));\n"
        }
        mock_string += " };\n}\n"
        mock_string += "#endif\n"


        val new_file = PsiFileFactory.getInstance(currentProject).createFileFromText(
            "mock_" + oc_file.name,
            oc_file.language,
            mock_string
        )
        val vfile = new_file.virtualFile!!

        FileEditorManager.getInstance(currentProject)
            .openTextEditor(OpenFileDescriptor(currentProject, vfile), true);

        WriteCommandAction.runWriteCommandAction(
            currentProject
        ) {
            oc_file.containingDirectory.add(new_file)
        }
    }
}