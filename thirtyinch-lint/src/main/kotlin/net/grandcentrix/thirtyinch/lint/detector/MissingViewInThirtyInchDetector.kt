package net.grandcentrix.thirtyinch.lint.detector

import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiType
import net.grandcentrix.thirtyinch.lint.TiIssue.MissingView
import org.jetbrains.uast.UClass

private const val TI_VIEW_FQ = "net.grandcentrix.thirtyinch.TiView"
private const val PROVIDE_VIEW_METHOD = "provideView"
private val TI_CLASS_NAMES = listOf(
        "net.grandcentrix.thirtyinch.TiActivity",
        "net.grandcentrix.thirtyinch.TiFragment"
)

class MissingViewInThirtyInchDetector : BaseMissingViewDetector() {
    companion object {
        val ISSUE = MissingView.asLintIssue(
                MissingViewInThirtyInchDetector::class.java,
                "When using ThirtyInch, a class extending TiActivity or TiFragment " +
                        "has to implement the TiView interface associated with it in its signature, " +
                        "or implement `provideView()` instead to override this default behaviour."
        )
    }

    override fun applicableSuperClasses() = TI_CLASS_NAMES

    override val issue: Issue = ISSUE

    override fun findViewInterface(context: JavaContext, declaration: UClass): PsiType? {
        for (extendedType in declaration.extendsListTypes) {
            extendedType.resolveGenerics().element?.let { resolvedType ->
                return tryFindViewInterface(extendedType, resolvedType)
            }
        }
        return null
    }

    private fun tryFindViewInterface(extendedType: PsiClassType, resolvedType: PsiClass): PsiType? {
        // Expect <P extends TiPresenter, V extends TiView> signature in the extended Ti class
        val parameters = extendedType.parameters
        val parameterTypes = resolvedType.typeParameters
        if (parameters.size != 2 || parameterTypes.size != 2) {
            return null
        }

        // Check that the second type parameter is actually a TiView
        val parameterType = parameterTypes[1]
        val parameter = parameters[1]
        return parameterType.extendsListTypes
                .map { it.resolveGenerics().element }
                .filter { TI_VIEW_FQ == it?.qualifiedName }
                .map { parameter }
                .firstOrNull()
    }

    override fun allowMissingViewInterface(context: JavaContext, declaration: UClass,
            viewInterface: PsiType): Boolean {
        // Interface not implemented; check if provideView() is overridden instead
        return declaration.findMethodsByName(PROVIDE_VIEW_METHOD, true)
                .any { viewInterface == it.returnType }
    }
}