package ch.epfl.vlsc.truffle.cal.ast;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import se.lth.cs.tycho.ir.entity.nl.EntityInstanceExpr;
import se.lth.cs.tycho.ir.entity.nl.EntityReferenceLocal;
import se.lth.cs.tycho.ir.entity.nl.InstanceDecl;
import se.lth.cs.tycho.ir.entity.nl.NlNetwork;
import se.lth.cs.tycho.ir.entity.nl.StructureConnectionStmt;
import se.lth.cs.tycho.ir.entity.nl.StructureStatement;
import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.nodes.ActionNode;
import ch.epfl.vlsc.truffle.cal.nodes.NetworkNode;
import ch.epfl.vlsc.truffle.cal.nodes.ReturnsLastBodyNode;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.nodes.RootNode;

import ch.epfl.vlsc.truffle.cal.nodes.CALExpressionNode;
import ch.epfl.vlsc.truffle.cal.runtime.CALArguments;
import ch.epfl.vlsc.truffle.cal.nodes.CALRootNode;
import ch.epfl.vlsc.truffle.cal.nodes.CALStatementNode;
import ch.epfl.vlsc.truffle.cal.nodes.NetworkBodyNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.StringLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.CALCreateFIFONode;
import ch.epfl.vlsc.truffle.cal.nodes.local.CALWriteFrameSlotNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.CALInvokeNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.ActorLiteralNode;
import ch.epfl.vlsc.truffle.cal.nodes.expression.literals.FunctionLiteralNode;
import se.lth.cs.tycho.attribute.EntityDeclarations;
import se.lth.cs.tycho.ir.NamespaceDecl;
import se.lth.cs.tycho.ir.QID;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.decl.LocalVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.cal.Action;
import se.lth.cs.tycho.ir.expr.ExprLiteral;
import se.lth.cs.tycho.ir.expr.ExprVariable;
import se.lth.cs.tycho.ir.expr.Expression;
import se.lth.cs.tycho.ir.stmt.Statement;
import se.lth.cs.tycho.ir.stmt.StmtAssignment;
import se.lth.cs.tycho.ir.stmt.StmtCall;
import se.lth.cs.tycho.ir.stmt.lvalue.LValueVariable;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.StmtBlockNode;

public class NetworkTransformer extends ScopedTransformer<NetworkNode> {

    NlNetwork network;
    QID name;

    public NetworkTransformer(CALLanguage language, Source source, NlNetwork network, QID name, int depth, TransformContext context) {
        super(language, source, new LexicalScopeRW(null), new FrameDescriptor(), depth, context);
        this.network = network;
        this.name = name;
    }

    // Tranform a NlNetwork to a usable network for us.
    // 1. Tranform entity declarations
    // 1.1 Create variable declarations
    // 2. Tranform structure
    // 2.1 FIFO creation nodes
    // 2.2 FIFO linking nodes
    public NetworkNode transform() {
        CALExpressionNode[] headStatements = new CALExpressionNode[network.getVarDecls().size()
                + network.getStructure().size() + network.getEntities().size()];
        int i = 0;
        for (LocalVarDecl varDecl : network.getVarDecls()) {
            headStatements[i] = transformVarDecl(varDecl);
            i++;
        }
        // List all the actors instansiated
        Map<String, ActorArguments> actors = new HashMap<>();
        for (InstanceDecl instanceDecl : network.getEntities()) {
            String instanceName = instanceDecl.getInstanceName();
            // FIXME handle arguments
            CALExpressionNode[] arguments = new CALExpressionNode[0];
            if (instanceDecl.getEntityExpr() instanceof EntityInstanceExpr) {
                EntityInstanceExpr entity = (EntityInstanceExpr) instanceDecl.getEntityExpr();
                if (entity.getEntityName() instanceof EntityReferenceLocal) {
                    String actorName = ((EntityReferenceLocal) entity.getEntityName()).getName();
                    // FIXME
                    actors.put(instanceName, new ActorArguments(actorName, arguments));
                } else {
                    throw new UnsupportedOperationException("Unknown entity referencei in network");
                }

            } else {
                throw new UnsupportedOperationException("Unknown entity in network");
            }

        }
        // create the fifos and add them to the actors
        for (StructureStatement struct : network.getStructure()) {
            if (struct instanceof StructureConnectionStmt) {
                StructureConnectionStmt connection = (StructureConnectionStmt) struct;
                // create a FIFO
                // FIXME
                CALExpressionNode assignment = createAssignment("fifo-1", new CALCreateFIFONode());
                // TODO handle mulitple ports
                String source = connection.getSrc().getEntityName();
                String dest = connection.getDst().getEntityName();

                actors.get(source).outputs.add(getReadNode("fifo-1"));
                actors.get(dest).inputs.add(getReadNode("fifo-1"));
                headStatements[i] = assignment;
                i++;
            } else {
                throw new UnsupportedOperationException("Unsupported Structure found");
            }
        }

        // Instansiation nodes for all actors
        for (Entry<String, ActorArguments> entry : actors.entrySet()) {
            ActorArguments args = entry.getValue();
            CALExpressionNode actor = new ActorLiteralNode(context.getEntityQID(args.actorName).toString());
            CALExpressionNode[] arguments = new CALExpressionNode[args.arguments.length + args.inputs.size()
                    + args.outputs.size()];
            System.arraycopy(args.arguments, 0, arguments, 0, args.arguments.length);
            System.arraycopy(args.inputs.toArray(), 0, arguments, args.arguments.length, args.inputs.size());
            System.arraycopy(args.outputs.toArray(), 0, arguments, args.arguments.length + args.inputs.size(),
                    args.outputs.size());

            CALExpressionNode call = new CALInvokeNode(actor, arguments);
            headStatements[i] = createAssignment(entry.getKey(), call);
            i++;
        }

        depth++;
        // Run the actors
        List<CALExpressionNode> bodyStatements = new LinkedList<>();
        for (String instanceName : actors.keySet()) {
            CALExpressionNode entityNode = getReadNode(instanceName);
            bodyStatements.add(new CALInvokeNode(entityNode, new CALExpressionNode[0]));
        }
        CALStatementNode body = new NetworkBodyNode(
                bodyStatements.toArray(new CALExpressionNode[bodyStatements.size()]));

        StmtBlockNode head = new StmtBlockNode(headStatements);
        SourceSection networkSrc = source.createUnavailableSection();// .createSection(network.getFromLineNumber(),
                                                                     // network.getFromColumnNumber(),
        // network.getToLineNumber());
        // FIXME wrap in a stmt block node so that library is adopted, don't know why
        CALRootNode toyRoot = new CALRootNode(language, frameDescriptor,
                new ReturnsLastBodyNode(new StmtBlockNode(new CALStatementNode[] { body })),
                source.createUnavailableSection(), name.toString());
        depth--;
        return new NetworkNode(language, frameDescriptor, head, toyRoot, networkSrc, name.toString());
    }

}
