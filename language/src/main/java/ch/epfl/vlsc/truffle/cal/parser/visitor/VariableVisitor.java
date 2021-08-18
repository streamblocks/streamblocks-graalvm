package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.LetExprNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.NullLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.InitializeArgNode;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParserBaseVisitor;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Collection;

/**
 * Singleton class that provides an implementation for a variable sub-tree.
 */
public class VariableVisitor extends CALParserBaseVisitor<CALStatementNode> {

    private static VariableVisitor instance;

    private static int portDeclarationStartIndex;

    private VariableVisitor() {}

    public static VariableVisitor getInstance() {
        if (instance == null) {
            instance = new VariableVisitor();
        }

        return instance;
    }

    public static void setPortDeclarationStartIndex(int startIndex) {
        // Note: Must be set before visiting each group of port declarations
        portDeclarationStartIndex = startIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override public InitializeArgNode visitPortDeclaration(CALParser.PortDeclarationContext ctx) {
        if (ctx.isMulti != null) {
            // TODO Add support for multi-ports ('multi')
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Multi-port is not yet supported");
        }
        if (ctx.type() != null) {
            // TODO Add support for port type
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Port type is not yet supported");
        }

        String portName = ctx.name.getText();
        ScopeEnvironment.getInstance().createReadOnlyFrameSlot(portName);

        ParserRuleContext parentCtx = ctx.getParent();
        if (parentCtx instanceof CALParser.PortDeclarationsContext) {
            // Note: Start index is currently used to account the total offset of a port declaration in an actor/network entity
            int portDeclarationIndex = portDeclarationStartIndex + ((CALParser.PortDeclarationsContext) parentCtx).portDeclaration().indexOf(ctx);
            return new InitializeArgNode(ScopeEnvironment.getInstance().getCurrentScope().get(portName).getSlot(), portDeclarationIndex);
        } else {
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Port position cannot be determined"); // Note: Unreachable case
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitAvailability(CALParser.AvailabilityContext ctx) {
        // TODO First resolve #visitGlobalVariableDeclaration
        // Note: Unreachable for now
        return super.visitAvailability(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitGlobalVariableDeclaration(CALParser.GlobalVariableDeclarationContext ctx) {
        // TODO First resolve CompilationUnitVisitor#visitNamespaceBody
        // Note: Unreachable for now
        return super.visitGlobalVariableDeclaration(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitLocalVariableDeclaration(CALParser.LocalVariableDeclarationContext ctx) {
        if (ctx.isExternal != null) {
            // TODO Add support for external variables ('external')
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "External local variable is not yet supported");
        }

        if (ctx.explicitVariableDeclaration() != null) {
            return visitExplicitVariableDeclaration(ctx.explicitVariableDeclaration());
        } else if (ctx.functionVariableDeclaration() != null) {
            return visitFunctionVariableDeclaration(ctx.functionVariableDeclaration());
        } else if (ctx.procedureVariableDeclaration() != null) {
            return visitProcedureVariableDeclaration(ctx.procedureVariableDeclaration());
        } else {
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Unrecognized local variable type"); // Note: Unreachable case
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitBlockVariableDeclaration(CALParser.BlockVariableDeclarationContext ctx) {
        if (ctx.explicitVariableDeclaration() != null) {
            return visitExplicitVariableDeclaration(ctx.explicitVariableDeclaration());
        } else if (ctx.functionVariableDeclaration() != null) {
            return visitFunctionVariableDeclaration(ctx.functionVariableDeclaration());
        } else if (ctx.procedureVariableDeclaration() != null) {
            return visitProcedureVariableDeclaration(ctx.procedureVariableDeclaration());
        } else {
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Unrecognized block variable type"); // Note: Unreachable case
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitExplicitVariableDeclaration(CALParser.ExplicitVariableDeclarationContext ctx) {
        if (ctx.isMutable != null) {
            // TODO Add support for mutable variables ('mutable')
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Mutable variable is not yet supported");
        }
        if (ctx.type() != null) {
            // TODO Add support for variable type
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Variable type is not yet supported");
        }
        if (ctx.isAssignable == null && ctx.value != null) {
            // TODO Add support for non-assignable variables ('=')
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Non-assignable variable is not yet supported");
        }
        if (ctx.expression().size() > 1) {
            // TODO Add support for indexing ('[]')
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Variable indexing is not yet supported");
        }

        CALExpressionNode valueNode;
        if (ctx.value != null) {
            valueNode = ExpressionVisitor.getInstance().visit(ctx.value);
        } else {
            valueNode = new NullLiteralNode();
        }

        return ScopeEnvironment.getInstance().createWriteNode(ctx.name.getText(), valueNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitFunctionVariableDeclaration(CALParser.FunctionVariableDeclarationContext ctx) {
        StmtBlockNode headNode;
        CALExpressionNode bodyNode;

        if (ctx.type() != null) {
            // TODO Add support for function return type
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Function return type is not yet supported");
        }

        ScopeEnvironment.getInstance().pushScope(true);

        if (ctx.formalParameters() != null) {
            Collection<CALStatementNode> formalParameterNodes = CollectionVisitor.getInstance().visitFormalParameters(ctx.formalParameters());
            headNode = new StmtBlockNode(formalParameterNodes.toArray(new CALStatementNode[0]));
        } else {
            headNode = null;
        }

        if (ctx.localVariables != null) {
            ScopeEnvironment.getInstance().pushScope(true, false);

            Collection<CALExpressionNode> localVariableNodes = CollectionVisitor.getInstance().visitBlockVariableDeclarations(ctx.localVariables);
            StmtBlockNode letHeadNode = new StmtBlockNode(localVariableNodes.toArray(new CALStatementNode[0]));
            CALExpressionNode letBodyNode = ExpressionVisitor.getInstance().visit(ctx.body);

            bodyNode = new LetExprNode(letHeadNode, letBodyNode);

            ScopeEnvironment.getInstance().popScope();
        } else {
            bodyNode = ExpressionVisitor.getInstance().visit(ctx.body);
        }

        ReturnsLastBodyNode lambdaBodyNode = new ReturnsLastBodyNode(headNode, bodyNode);
        CALRootNode lambdaBodyRootNode = new CALRootNode(
                ScopeEnvironment.getInstance().getLanguage(),
                ScopeEnvironment.getInstance().getCurrentScope().getFrame(),
                lambdaBodyNode,
                null,
                ScopeEnvironment.generateLambdaName()
        );
        LambdaNode valueNode = new LambdaNode(lambdaBodyRootNode);

        ScopeEnvironment.getInstance().popScope();

        return ScopeEnvironment.getInstance().createWriteNode(ctx.name.getText(), valueNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitProcedureVariableDeclaration(CALParser.ProcedureVariableDeclarationContext ctx) {
        // TODO First resolve ExpressionVisitor#visitProcExpression  => use named proc closure as a procedure variable
        throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Procedure declaration is not yet supported");
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitFormalParameter(CALParser.FormalParameterContext ctx) {
        String variableName = ctx.explicitVariableDeclaration().name.getText();
        ScopeEnvironment.getInstance().createFrameSlot(variableName);

        ParserRuleContext parentCtx = ctx.getParent();
        if (parentCtx instanceof CALParser.FormalParametersContext) {
            return new InitializeArgNode(ScopeEnvironment.getInstance().getCurrentScope().get(variableName).getSlot(), ((CALParser.FormalParametersContext) parentCtx).formalParameter().indexOf(ctx));
        } else {
            throw new CALParseError(ScopeEnvironment.getInstance().getSource(), ctx, "Formal parameter position cannot be determined"); // Note: Unreachable case
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitLvalue(CALParser.LvalueContext ctx) {
        // Note: Unreachable for now, only children directly accessed in StatementVisitor#visitAssignmentStatement
        return super.visitLvalue(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitVariable(CALParser.VariableContext ctx) {
        // Note: Unreachable for now, only token is directly accessed
        return super.visitVariable(ctx);
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitField(CALParser.FieldContext ctx) {
        // Note: Unreachable for now, only token is directly accessed
        return super.visitField(ctx);
    }

}