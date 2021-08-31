package ch.epfl.vlsc.truffle.cal.parser.visitor;

import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtFunctionBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.LetExprNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.ProcNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.NullLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.local.InitializeArgNode;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.exception.CALParseWarning;
import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.parser.CALParser;
import ch.epfl.vlsc.truffle.cal.parser.CALParserBaseVisitor;
import ch.epfl.vlsc.truffle.cal.shared.options.OptionsCatalog;
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
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Multi-port is not yet supported");
            }
        }
        if (ctx.type() != null) {
            // TODO Add support for port type
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Port type is not yet supported");
            }
        }

        String portName = ctx.name.getText();
        ScopeEnvironment.getInstance().createReadOnlyFrameSlot(portName);

        ParserRuleContext parentCtx = ctx.getParent();
        if (parentCtx instanceof CALParser.PortDeclarationsContext) {
            // Note: Start index is currently used to account the total offset of a port declaration in an actor/network entity
            int portDeclarationIndex = portDeclarationStartIndex + ((CALParser.PortDeclarationsContext) parentCtx).portDeclaration().indexOf(ctx);

            InitializeArgNode portNode = new InitializeArgNode(ScopeEnvironment.getInstance().getCurrentScope().get(portName).getSlot(), portDeclarationIndex);
            portNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
            portNode.addStatementTag();

            return portNode;
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
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "External local variable is not yet supported");
            }
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
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Mutable variable is not yet supported");
            }
        }
        if (ctx.type() != null) {
            // TODO Add support for variable type
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Variable type is not yet supported");
            }
        }
        if (ctx.isAssignable == null && ctx.value != null) {
            // TODO Add support for non-assignable variables ('=')
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Non-assignable variable is not yet supported");
            }
        }
        if (ctx.expression().size() > 1) {
            // TODO Add support for indexing ('[]')
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Variable indexing is not yet supported");
            }
        }

        CALExpressionNode valueNode;
        if (ctx.value != null) {
            valueNode = ExpressionVisitor.getInstance().visit(ctx.value);
        } else {
            valueNode = new NullLiteralNode();
            valueNode.setSourceSection(ScopeEnvironment.getInstance().getSource().createUnavailableSection());
            valueNode.addExpressionTag();
        }

        return ScopeEnvironment.getInstance().createNewVariableWriteNode(ctx.name.getText(), valueNode, ScopeEnvironment.getInstance().createSourceSection(ctx));
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitFunctionVariableDeclaration(CALParser.FunctionVariableDeclarationContext ctx) {
        StmtBlockNode headNode;
        CALExpressionNode bodyNode;

        if (ctx.type() != null) {
            // TODO Add support for function return type
            if (CALLanguage.getCurrentContext().getEnv().getOptions().get(OptionsCatalog.WARN_SHOW_KEY)) {
                throw new CALParseWarning(ScopeEnvironment.getInstance().getSource(), ctx, "Function return type is not yet supported");
            }
        }

        ScopeEnvironment.getInstance().pushScope(true);

        if (ctx.formalParameters() != null) {
            Collection<CALStatementNode> formalParameterNodes = CollectionVisitor.getInstance().visitFormalParameters(ctx.formalParameters());
            headNode = new StmtBlockNode(formalParameterNodes.toArray(new CALStatementNode[0]));
            headNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx.formalParameters()));
            headNode.addStatementTag();
        } else {
            headNode = null;
        }

        if (ctx.localVariables != null) {
            ScopeEnvironment.getInstance().pushScope(true, false);

            Collection<CALExpressionNode> localVariableNodes = CollectionVisitor.getInstance().visitBlockVariableDeclarations(ctx.localVariables);
            StmtBlockNode letHeadNode = new StmtBlockNode(localVariableNodes.toArray(new CALStatementNode[0]));
            letHeadNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx.localVariables));
            letHeadNode.addStatementTag();

            CALExpressionNode letBodyNode = ExpressionVisitor.getInstance().visit(ctx.body);

            bodyNode = new LetExprNode(letHeadNode, letBodyNode);
            bodyNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
            bodyNode.addExpressionTag();

            ScopeEnvironment.getInstance().popScope();
        } else {
            bodyNode = ExpressionVisitor.getInstance().visit(ctx.body);
        }

        ReturnsLastBodyNode lambdaBodyNode = new ReturnsLastBodyNode(headNode, bodyNode);
        lambdaBodyNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        lambdaBodyNode.addExpressionTag();

        CALRootNode lambdaBodyRootNode = new CALRootNode(
                ScopeEnvironment.getInstance().getLanguage(),
                ScopeEnvironment.getInstance().getCurrentScope().getFrame(),
                lambdaBodyNode,
                ScopeEnvironment.getInstance().createSourceSection(ctx),
                ScopeEnvironment.generateLambdaName()
        );
        // TODO Add RootTag / CallTag for lambdaBodyRootNode

        LambdaNode valueNode = new LambdaNode(lambdaBodyRootNode);
        valueNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        valueNode.addExpressionTag();

        ScopeEnvironment.getInstance().popScope();

        return ScopeEnvironment.getInstance().createNewVariableWriteNode(ctx.name.getText(), valueNode, ScopeEnvironment.getInstance().createSourceSection(ctx));
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALExpressionNode visitProcedureVariableDeclaration(CALParser.ProcedureVariableDeclarationContext ctx) {
        // FIXME: Copied over as it is from Lambda Visitor
        StmtBlockNode headNode;
        CALExpressionNode bodyNode;

        ScopeEnvironment.getInstance().pushScope(false);

        if (ctx.formalParameters() != null) {
            Collection<CALStatementNode> formalParameterNodes = CollectionVisitor.getInstance().visitFormalParameters(ctx.formalParameters());
            headNode = new StmtBlockNode(formalParameterNodes.toArray(new CALStatementNode[0]));
            headNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx.formalParameters()));
            headNode.addStatementTag();
        } else {
            headNode = new StmtBlockNode(new CALStatementNode[0]);
        }

        if (ctx.localVariables != null) {
            ScopeEnvironment.getInstance().pushScope(true, false);

            Collection<CALExpressionNode> localVariableNodes = CollectionVisitor.getInstance().visitBlockVariableDeclarations(ctx.localVariables);
            StmtBlockNode letHeadNode = new StmtBlockNode(localVariableNodes.toArray(new CALStatementNode[0]));
            letHeadNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx.localVariables));
            letHeadNode.addStatementTag();

            CALExpressionNode letBodyNode = new StmtFunctionBodyNode(StatementVisitor.getInstance().visit(ctx.statements()));

            bodyNode = new LetExprNode(letHeadNode, letBodyNode);
            bodyNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
            bodyNode.addExpressionTag();

            ScopeEnvironment.getInstance().popScope();
        } else {
            bodyNode = new StmtFunctionBodyNode(StatementVisitor.getInstance().visit(ctx.statements()));
        }

        ReturnsLastBodyNode procBodyNode = new ReturnsLastBodyNode(headNode, bodyNode);
        procBodyNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        procBodyNode.addStatementTag();

        CALRootNode procBodyRootNode = new CALRootNode(
                ScopeEnvironment.getInstance().getLanguage(),
                ScopeEnvironment.getInstance().getCurrentScope().getFrame(),
                procBodyNode,
                ScopeEnvironment.getInstance().createSourceSection(ctx),
                ScopeEnvironment.generateLambdaName()
        );
        // TODO Add RootTag / CallTag for procBodyRootNode

        LambdaNode valueNode = new LambdaNode(procBodyRootNode);
        valueNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
        valueNode.addStatementTag();

        ScopeEnvironment.getInstance().popScope();

        return ScopeEnvironment.getInstance().createNewVariableWriteNode(ctx.name.getText(), valueNode, ScopeEnvironment.getInstance().createSourceSection(ctx));
    }

    /**
     * {@inheritDoc}
     */
    @Override public CALStatementNode visitFormalParameter(CALParser.FormalParameterContext ctx) {
        String variableName = ctx.explicitVariableDeclaration().name.getText();
        ScopeEnvironment.getInstance().createFrameSlot(variableName);

        ParserRuleContext parentCtx = ctx.getParent();
        if (parentCtx instanceof CALParser.FormalParametersContext) {
            int formalParameterIndex = ((CALParser.FormalParametersContext) parentCtx).formalParameter().indexOf(ctx);

            InitializeArgNode formalParameterNode = new InitializeArgNode(ScopeEnvironment.getInstance().getCurrentScope().get(variableName).getSlot(), formalParameterIndex);
            formalParameterNode.setSourceSection(ScopeEnvironment.getInstance().createSourceSection(ctx));
            formalParameterNode.addStatementTag();

            return formalParameterNode;
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