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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import se.lth.cs.tycho.ir.ValueParameter;
import se.lth.cs.tycho.ir.decl.GlobalEntityDecl;
import se.lth.cs.tycho.ir.decl.LocalVarDecl;
import se.lth.cs.tycho.ir.decl.VarDecl;
import se.lth.cs.tycho.ir.entity.PortDecl;
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

    public NetworkTransformer(NlNetwork network, QID name, TransformContext context) {
    	super(context);
    	// We want a clean frame
    	// TODO support global variables
    	context.clearLexicalScopeAndFrame();
        //super(language, source, new LexicalScopeRW(null), new FrameDescriptor(), depth, context);
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
        List<CALStatementNode> headStatements = new ArrayList<>(network.getVarDecls().size()
                + network.getStructure().size() + network.getValueParameters().size() + network.getEntities().size() + network.getInputPorts().size() + network.getOutputPorts().size());
        int i = 0;
        // merge with actor if possible
        // TODO we are making assumptions about the number of arguments
        // and that EVERY argument and port is effectively passed

        // WARNING keep as the first declaration as it needs to match the arguments
        // position
        // Prepend arguments so they are specialized the same way as in the body
        for (VarDecl varDecl : network.getValueParameters()) {
            headStatements.add(transformArgument(varDecl, i));
            i++;
        }

        for (PortDecl in : network.getInputPorts()) {
            // Input ports are passed as arguments
            headStatements.add(transformPortDecl(in, i));
            i++;
        }
        for (PortDecl out : network.getOutputPorts()) {
            // Input ports are passed as arguments
            headStatements.add(transformPortDecl(out, i));
            i++;
        }
        for (LocalVarDecl varDecl : network.getVarDecls()) {
            headStatements.add(transformVarDecl(varDecl));
            i++;
        }
        // List all the actors instansiated
        Map<String, ActorArguments> actors = new LinkedHashMap<>();
        for (InstanceDecl instanceDecl : network.getEntities()) {
            String instanceName = instanceDecl.getInstanceName();
            // FIXME handle arguments
            if (instanceDecl.getEntityExpr() instanceof EntityInstanceExpr) {
                EntityInstanceExpr entity = (EntityInstanceExpr) instanceDecl.getEntityExpr();
                if (entity.getEntityName() instanceof EntityReferenceLocal) {
                    String actorName = ((EntityReferenceLocal) entity.getEntityName()).getName();
                    // FIXME here we assume that the parameters are passed in the same order as in the declaration
                    // it however does not need to be the case, so this has to be changed
                    List<CALExpressionNode> arguments = new ArrayList<>(entity.getValueParameters().size());
                    for (ValueParameter parameter: entity.getValueParameters())
                        arguments.add(transformExpr(parameter.getValue()));
                    actors.put(instanceName, new ActorArguments(actorName, arguments.toArray(new CALExpressionNode[arguments.size()])));
                } else {
                    throw new UnsupportedOperationException("Unknown entity reference in network");
                }

            } else {
                throw new UnsupportedOperationException("Unknown entity in network");
            }

        }
        int j = 0;
        // create the fifos and add them to the actors
        for (StructureStatement struct : network.getStructure()) {
            if (struct instanceof StructureConnectionStmt) {
                StructureConnectionStmt connection = (StructureConnectionStmt) struct;
                // create a FIFO
                // TODO handle mulitple ports
                String source = connection.getSrc().getEntityName();
                String dest = connection.getDst().getEntityName();
                // If we have a FIFO connections two entities instantiated here 
                // we have to create a new FIFO
                if (source != null && dest != null) {
                    CALExpressionNode assignment = createAssignment("fifo-" + j, new CALCreateFIFONode());
                    headStatements.add(assignment);
                    actors.get(source).outputs.add(getReadNode("fifo-" + j));
                    actors.get(dest).inputs.add(getReadNode("fifo-" + j));
                    j++;
                    i++;
                }
                // Otherwise we use the FIFO given in arguments
                else {
                    String fifoName;
                    if (source == null)
                        fifoName = connection.getSrc().getPortName();
                    else if (dest == null)
                        fifoName = connection.getDst().getPortName();
                    else
                        throw new TransformException("unsupported network local fifo", context.getSource(), connection);
                    if (source != null)
                        actors.get(source).outputs.add(getReadNode(fifoName));
                    if (dest != null)
                        actors.get(dest).inputs.add(getReadNode(fifoName));
                }

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
            headStatements.add(createAssignment(entry.getKey(), call));
            i++;
        }

        context.setDepth(context.getDepth()+1);
        // Run the actors
        List<CALExpressionNode> bodyStatements = new LinkedList<>();
        for (String instanceName : actors.keySet()) {
            CALExpressionNode entityNode = getReadNode(instanceName);
            bodyStatements.add(new CALInvokeNode(entityNode, new CALExpressionNode[0]));
        }
        CALExpressionNode body = new NetworkBodyNode(
                bodyStatements.toArray(new CALExpressionNode[bodyStatements.size()]));

        StmtBlockNode head = new StmtBlockNode(headStatements.toArray(new CALStatementNode[headStatements.size()]));
        SourceSection networkSrc = context.getSource().createUnavailableSection();// .createSection(network.getFromLineNumber(),
                                                                     // network.getFromColumnNumber(),
        // network.getToLineNumber());
        // FIXME wrap in a stmt block node so that library is adopted, don't know why
        CALRootNode toyRoot = new CALRootNode(context.getLanguage(), context.getFrameDescriptor(), body,
        		context.getSource().createUnavailableSection(), name.toString());
        context.setDepth(context.getDepth()-1);
        return new NetworkNode(context.getLanguage(), context.getFrameDescriptor(), head, toyRoot, networkSrc, name.toString());
    }

}
