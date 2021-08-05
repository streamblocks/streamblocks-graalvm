// Generated from language/src/main/java/ch/epfl/vlsc/truffle/cal/parser/antlr/CALParser.g4 by ANTLR 4.7.1
package ch.epfl.vlsc.truffle.cal.parser.antlr;

import java.util.*;

import org.graalvm.collections.Pair;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.RootCallTarget;
import ch.epfl.vlsc.truffle.cal.CALLanguage;
import ch.epfl.vlsc.truffle.cal.parser.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.CALNodeFactory;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.expression.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.*;
import ch.epfl.vlsc.truffle.cal.nodes.local.*;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.*;

import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link CALParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface CALParserVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link CALParser#compilationUnit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCompilationUnit(CALParser.CompilationUnitContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#namespaceDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespaceDeclaration(CALParser.NamespaceDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#namespaceBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNamespaceBody(CALParser.NamespaceBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#qualifiedID}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitQualifiedID(CALParser.QualifiedIDContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#annotation}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotation(CALParser.AnnotationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#annotationParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAnnotationParameter(CALParser.AnnotationParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#unitDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnitDeclaration(CALParser.UnitDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#importDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportDeclaration(CALParser.ImportDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#singleImport}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSingleImport(CALParser.SingleImportContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#groupImport}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroupImport(CALParser.GroupImportContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#importKind}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitImportKind(CALParser.ImportKindContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#networkDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNetworkDeclaration(CALParser.NetworkDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#entityDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntityDeclaration(CALParser.EntityDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#entityExpressions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntityExpressions(CALParser.EntityExpressionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#entityExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntityExpression(CALParser.EntityExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#entityInstanceExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntityInstanceExpression(CALParser.EntityInstanceExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#entityIfExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntityIfExpression(CALParser.EntityIfExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#entityListExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntityListExpression(CALParser.EntityListExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#entityParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntityParameters(CALParser.EntityParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#entityParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntityParameter(CALParser.EntityParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#attributeSection}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttributeSection(CALParser.AttributeSectionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#attributeDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAttributeDeclaration(CALParser.AttributeDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#structureStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructureStatement(CALParser.StructureStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#structureConnectorStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructureConnectorStatement(CALParser.StructureConnectorStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#structureForeachStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructureForeachStatement(CALParser.StructureForeachStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#structureIfStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructureIfStatement(CALParser.StructureIfStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#structureElseIfStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStructureElseIfStatement(CALParser.StructureElseIfStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#connector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitConnector(CALParser.ConnectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#entityReference}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitEntityReference(CALParser.EntityReferenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#portReference}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPortReference(CALParser.PortReferenceContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#actorDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitActorDeclaration(CALParser.ActorDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#portDeclarations}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPortDeclarations(CALParser.PortDeclarationsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#portDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPortDeclaration(CALParser.PortDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#timeCause}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTimeCause(CALParser.TimeCauseContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#processDescription}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcessDescription(CALParser.ProcessDescriptionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#actionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitActionDefinition(CALParser.ActionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#inputPatterns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInputPatterns(CALParser.InputPatternsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#inputPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInputPattern(CALParser.InputPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#channelSelector}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChannelSelector(CALParser.ChannelSelectorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#patterns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPatterns(CALParser.PatternsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#pattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPattern(CALParser.PatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#subPatterns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubPatterns(CALParser.SubPatternsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#subPattern}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSubPattern(CALParser.SubPatternContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#patternExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPatternExpression(CALParser.PatternExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#outputExpressions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOutputExpressions(CALParser.OutputExpressionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#outputExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOutputExpression(CALParser.OutputExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#initializationActionDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInitializationActionDefinition(CALParser.InitializationActionDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#actionTags}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitActionTags(CALParser.ActionTagsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#actionTag}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitActionTag(CALParser.ActionTagContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#actionSchedule}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitActionSchedule(CALParser.ActionScheduleContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#scheduleFSM}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScheduleFSM(CALParser.ScheduleFSMContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#stateTransition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStateTransition(CALParser.StateTransitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#scheduleRegExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitScheduleRegExp(CALParser.ScheduleRegExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#regExp}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRegExp(CALParser.RegExpContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#priorityOrder}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPriorityOrder(CALParser.PriorityOrderContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#priorityInequality}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPriorityInequality(CALParser.PriorityInequalityContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#availability}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAvailability(CALParser.AvailabilityContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#globalVariableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGlobalVariableDeclaration(CALParser.GlobalVariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#localVariableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLocalVariableDeclaration(CALParser.LocalVariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#blockVariableDeclarations}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockVariableDeclarations(CALParser.BlockVariableDeclarationsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#blockVariableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockVariableDeclaration(CALParser.BlockVariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#explicitVariableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExplicitVariableDeclaration(CALParser.ExplicitVariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#functionVariableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunctionVariableDeclaration(CALParser.FunctionVariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#procedureVariableDeclaration}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcedureVariableDeclaration(CALParser.ProcedureVariableDeclarationContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#formalParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParameters(CALParser.FormalParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#formalParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormalParameter(CALParser.FormalParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#typeDefinition}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeDefinition(CALParser.TypeDefinitionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#taggedTuple}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTaggedTuple(CALParser.TaggedTupleContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#tuple}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTuple(CALParser.TupleContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#types}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypes(CALParser.TypesContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(CALParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#typeParameters}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameters(CALParser.TypeParametersContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#typeParameter}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParameter(CALParser.TypeParameterContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#typeAttributes}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeAttributes(CALParser.TypeAttributesContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#typeAttribute}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeAttribute(CALParser.TypeAttributeContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#generators}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenerators(CALParser.GeneratorsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#generator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGenerator(CALParser.GeneratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#foreachGenerators}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeachGenerators(CALParser.ForeachGeneratorsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#foreachGenerator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeachGenerator(CALParser.ForeachGeneratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#chooseGenerators}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChooseGenerators(CALParser.ChooseGeneratorsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#chooseGenerator}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChooseGenerator(CALParser.ChooseGeneratorContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#generatorBody}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGeneratorBody(CALParser.GeneratorBodyContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#expressions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpressions(CALParser.ExpressionsContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ListComprehensionExprExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListComprehensionExprExpression(CALParser.ListComprehensionExprExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ExprExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprExpression(CALParser.ExprExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FieldSelectorExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFieldSelectorExpression(CALParser.FieldSelectorExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code VariableExprExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableExprExpression(CALParser.VariableExprExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code MapComprehensionExprExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapComprehensionExprExpression(CALParser.MapComprehensionExprExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code UnaryOperationExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnaryOperationExpression(CALParser.UnaryOperationExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code ProcExprExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcExprExpression(CALParser.ProcExprExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SymbolReferenceExprExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSymbolReferenceExprExpression(CALParser.SymbolReferenceExprExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LiteralExprExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLiteralExprExpression(CALParser.LiteralExprExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IndexerExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIndexerExpression(CALParser.IndexerExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code SetComprehensionExprExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetComprehensionExprExpression(CALParser.SetComprehensionExprExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CaseExprExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCaseExprExpression(CALParser.CaseExprExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BinaryOperationExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBinaryOperationExpression(CALParser.BinaryOperationExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LambdaExprExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaExprExpression(CALParser.LambdaExprExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code TypeAssertionExprExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeAssertionExprExpression(CALParser.TypeAssertionExprExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IfExprExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfExprExpression(CALParser.IfExprExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code LetExprExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLetExprExpression(CALParser.LetExprExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CallExprExpression}
	 * labeled alternative in {@link CALParser#expression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallExprExpression(CALParser.CallExprExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code IntegerLiteralExpression}
	 * labeled alternative in {@link CALParser#literalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIntegerLiteralExpression(CALParser.IntegerLiteralExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code FloatingPointLiteralExpression}
	 * labeled alternative in {@link CALParser#literalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFloatingPointLiteralExpression(CALParser.FloatingPointLiteralExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code BooleanLiteralExpression}
	 * labeled alternative in {@link CALParser#literalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBooleanLiteralExpression(CALParser.BooleanLiteralExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code CharacterLiteralExpression}
	 * labeled alternative in {@link CALParser#literalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCharacterLiteralExpression(CALParser.CharacterLiteralExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code StringLiteralExpression}
	 * labeled alternative in {@link CALParser#literalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStringLiteralExpression(CALParser.StringLiteralExpressionContext ctx);
	/**
	 * Visit a parse tree produced by the {@code NullLiteralExpression}
	 * labeled alternative in {@link CALParser#literalExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNullLiteralExpression(CALParser.NullLiteralExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#variableExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariableExpression(CALParser.VariableExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#symbolReferenceExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSymbolReferenceExpression(CALParser.SymbolReferenceExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#ifExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfExpression(CALParser.IfExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#elseIfExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElseIfExpression(CALParser.ElseIfExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#letExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLetExpression(CALParser.LetExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#lambdaExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLambdaExpression(CALParser.LambdaExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#procExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitProcExpression(CALParser.ProcExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#setComprehension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetComprehension(CALParser.SetComprehensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#listComprehension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitListComprehension(CALParser.ListComprehensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#mapComprehension}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapComprehension(CALParser.MapComprehensionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#mappings}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMappings(CALParser.MappingsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#mapping}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMapping(CALParser.MappingContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#typeAssertionExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeAssertionExpression(CALParser.TypeAssertionExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#caseExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCaseExpression(CALParser.CaseExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#alternativeExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlternativeExpression(CALParser.AlternativeExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#callExpression}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallExpression(CALParser.CallExpressionContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#lvalues}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLvalues(CALParser.LvaluesContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#lvalue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLvalue(CALParser.LvalueContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable(CALParser.VariableContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#field}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitField(CALParser.FieldContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#statements}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatements(CALParser.StatementsContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#statement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStatement(CALParser.StatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#assignmentStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAssignmentStatement(CALParser.AssignmentStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#callStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCallStatement(CALParser.CallStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#blockStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitBlockStatement(CALParser.BlockStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#ifStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitIfStatement(CALParser.IfStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#elseIfStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitElseIfStatement(CALParser.ElseIfStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#whileStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWhileStatement(CALParser.WhileStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#foreachStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitForeachStatement(CALParser.ForeachStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#chooseStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitChooseStatement(CALParser.ChooseStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#caseStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCaseStatement(CALParser.CaseStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#alternativeStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAlternativeStatement(CALParser.AlternativeStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#readStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitReadStatement(CALParser.ReadStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#writeStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitWriteStatement(CALParser.WriteStatementContext ctx);
	/**
	 * Visit a parse tree produced by {@link CALParser#actionSelectionStatement}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitActionSelectionStatement(CALParser.ActionSelectionStatementContext ctx);
}