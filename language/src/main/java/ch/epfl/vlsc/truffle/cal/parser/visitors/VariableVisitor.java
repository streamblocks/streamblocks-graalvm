package ch.epfl.vlsc.truffle.cal.parser.visitors;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.LetExprNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteLocalVariableNodeGen;
import ch.epfl.vlsc.truffle.cal.nodes.local.InitializeArgNode;
import ch.epfl.vlsc.truffle.cal.parser.DepthFrameSlot;
import ch.epfl.vlsc.truffle.cal.parser.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.gen.CALParserBaseVisitor;

/**
 * Singleton class that provides an implementation for a variable sub-tree.
 */
public class VariableVisitor extends CALParserBaseVisitor<CALStatementNode> {

    private static VariableVisitor instance;

    private VariableVisitor() {}

    public static VariableVisitor getInstance() {
        if (instance == null) {
            instance = new VariableVisitor();
        }

        return instance;
    }



    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitAvailability(CALParser.AvailabilityContext ctx) {
        // TODO First resolve {@link visitGlobalVariableDeclaration} dependencies

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitGlobalVariableDeclaration(CALParser.GlobalVariableDeclarationContext ctx) {
        // TODO Add support for global variables

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitLocalVariableDeclaration(CALParser.LocalVariableDeclarationContext ctx) {
        // TODO Add support for external variables ('external')

        if (ctx.explicitVariableDeclaration() != null) {
            return visitExplicitVariableDeclaration(ctx.explicitVariableDeclaration());
        } else if (ctx.functionVariableDeclaration() != null) {
            return visitFunctionVariableDeclaration(ctx.functionVariableDeclaration());
        } else {
            return visitProcedureVariableDeclaration(ctx.procedureVariableDeclaration());
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
        } else {
            return visitProcedureVariableDeclaration(ctx.procedureVariableDeclaration());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitExplicitVariableDeclaration(CALParser.ExplicitVariableDeclarationContext ctx) {
        // TODO Add support for variable type
        // TODO Add support for constant variables ('=')
        // TODO Add support for mutable variables ('mutable')
        // TODO Add support for indexing ('[]')

        CALExpressionNode valueNode = null;
        if (ctx.value != null) {
            valueNode = ExpressionVisitor.getInstance().visit(ctx.value);
        }

        return ScopeEnvironment.getInstance().createWriteNode(ctx.name.getText(), valueNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitFunctionVariableDeclaration(CALParser.FunctionVariableDeclarationContext ctx) {
        // TODO Add support for constant functions
        // TODO Add support for function return type

        ScopeEnvironment.getInstance().pushScope(true);

        StmtBlockNode head;
        CALExpressionNode body;

        ScopeEnvironment.getInstance().pushScope(true);

        if (ctx.formalParameters() != null) {
            head = new StmtBlockNode(CollectionVisitor.getInstance().visitFormalParameters(ctx.formalParameters()).toArray(new CALStatementNode[0]));
        } else {
            head = null;
        }

        if (ctx.localVariables != null) {
            ScopeEnvironment.getInstance().pushScope(true, false);

            StmtBlockNode letHead = new StmtBlockNode(CollectionVisitor.getInstance().visitBlockVariableDeclarations(ctx.localVariables).toArray(new CALStatementNode[0]));
            body = new LetExprNode(letHead, ExpressionVisitor.getInstance().visit(ctx.body));

            ScopeEnvironment.getInstance().popScope();
        } else {
            body = ExpressionVisitor.getInstance().visit(ctx.body);
        }

        ReturnsLastBodyNode bodyNode = new ReturnsLastBodyNode(head, body);
        CALRootNode bodyRootNode = new CALRootNode(ScopeEnvironment.getInstance().getLanguage(), ScopeEnvironment.getInstance().getCurrentScope().getFrame(), bodyNode, null, ctx.name.getText());
        LambdaNode valueNode = new LambdaNode(bodyRootNode);

        ScopeEnvironment.getInstance().popScope();

        return ScopeEnvironment.getInstance().createWriteNode(ctx.name.getText(), valueNode);
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitProcedureVariableDeclaration(CALParser.ProcedureVariableDeclarationContext ctx) {
        // TODO Add support for procedure closure

        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitFormalParameter(CALParser.FormalParameterContext ctx) {
        CALExpressionNode explicitVariable = visitExplicitVariableDeclaration(ctx.explicitVariableDeclaration());
        if (explicitVariable instanceof CALWriteLocalVariableNodeGen) {
            String variableName = ctx.explicitVariableDeclaration().name.getText();

            DepthFrameSlot existingSlot = ScopeEnvironment.getInstance().getCurrentScope().get(variableName);
            DepthFrameSlot readOnlySlot = new DepthFrameSlot(existingSlot);

            ScopeEnvironment.getInstance().getCurrentScope().put(variableName, readOnlySlot);

            return new InitializeArgNode(ScopeEnvironment.getInstance().getCurrentScope().get(variableName).getSlot(), ctx.position);
        }

        return null;
    }

}