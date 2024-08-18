package sspeiser.gMockGen


import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement


internal class SimpleAnnotator : Annotator {
    override fun annotate( element: PsiElement,  holder: AnnotationHolder) {
        if (ToHiglight.elements.contains(element)) {
            holder.newAnnotation(HighlightSeverity.ERROR, "Hello")
                .range(element.textRange)
                .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                .create()
        }
    }

}