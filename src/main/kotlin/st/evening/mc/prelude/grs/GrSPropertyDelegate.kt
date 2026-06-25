package st.evening.mc.prelude.grs

import org.apache.groovy.ast.tools.ClassNodeUtils
import org.apache.groovy.util.BeanUtils
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.EmptyExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.tools.GeneralUtils
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformationClass
import st.evening.mc.prelude.api.util.data.UnboundGenericProperty
import java.lang.reflect.Modifier

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FIELD)
@GroovyASTTransformationClass(classes = [GrSPropertyDelegateTransformation::class])
annotation class GrSPropertyDelegate

@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class GrSPropertyDelegateTransformation : AbstractASTTransformation() { // based on LazyASTTransformation
    companion object {
        private const val NOT_PUBLIC_PROTECTED: Int = (Modifier.PUBLIC or Modifier.PROTECTED).inv()
        private const val PRIVATE_SYNTHETIC: Int = Modifier.PRIVATE or 0x1000
        private const val PUBLIC_STATIC: Int = Modifier.PUBLIC or Modifier.STATIC

        private val UNBOUND_GENERIC_PROPERTY: ClassNode =
            ClassHelper.makeWithoutCaching(UnboundGenericProperty::class.java, false)

        private fun modifyBackingField(mask: Int): Int = (mask and NOT_PUBLIC_PROTECTED) or PRIVATE_SYNTHETIC
    }

    override fun visit(nodes: Array<ASTNode>, source: SourceUnit) {
        val field = nodes[1]
        if (field !is FieldNode) {
            addError("@GrSPropertyDelegate only applies to fields!", field)
            return
        }
        val delegateExpr = field.initialValueExpression
        if (delegateExpr == null || delegateExpr is EmptyExpression) {
            addError("Delegated property must have a delegate initializer!", field)
            return
        }

        // repurpose the field to store the delegate
        val propName = field.name
        val propType = field.type
        val backingFieldName = "$$propName"
        field.rename(backingFieldName)
        field.type = UNBOUND_GENERIC_PROPERTY
        field.modifiers = modifyBackingField(field.modifiers)

        // ensure the backing field isn't exposed as a property
        val enclosingClass = field.declaringClass
        enclosingClass.properties.removeAll { it.name == backingFieldName }

        // generate the new getter
        val body = BlockStatement()
        body.addStatement(GeneralUtils.stmt(GeneralUtils.callX(GeneralUtils.varX(field), "getPropertyValue")))
        ClassNodeUtils.addGeneratedMethod(
            enclosingClass,
            "get${BeanUtils.capitalize(propName)}",
            if (field.isStatic) PUBLIC_STATIC else Modifier.PUBLIC,
            propType,
            Parameter.EMPTY_ARRAY,
            ClassNode.EMPTY_ARRAY,
            body
        )
    }
}

