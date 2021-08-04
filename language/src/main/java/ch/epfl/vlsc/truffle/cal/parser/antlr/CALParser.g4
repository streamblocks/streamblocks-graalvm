/*
 * ----------------------------------------------------------------------------
 *  ____  _                            ____  _            _
 * / ___|| |_ _ __ ___  __ _ _ __ ___ | __ )| | ___   ___| | _____
 * \___ \| __| '__/ _ \/ _` | '_ ` _ \|  _ \| |/ _ \ / __| |/ / __|
 *  ___) | |_| | |  __/ (_| | | | | | | |_) | | (_) | (__|   <\__ \
 * |____/ \__|_|  \___|\__,_|_| |_| |_|____/|_|\___/ \___|_|\_\___/
 * ----------------------------------------------------------------------------
 * Copyright (c) 2020, Streamgenomics sarl
 * All rights reserved.
 * ----------------------------------------------------------------------------
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   * Neither the name of the Streamgenomics sarl nor the names of its
 *     contributors may be used to endorse or promote products derived from this
 *     software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * CAL Actor language parser grammar for ANTLR 4.7 (extended with NL network language).
 * Grammar derived from CAL Language Report (referred as CLR) and NL Language Report (reffered as NLR).
 *
 * CAL language specification is also extended with concepts:
 *  - Namespace, package, unit
 *  - Annotation
 *  - Type definition
 *  - Operators
 *  - Elsif statement
 *  - Case statement
 *  - Global variable availability
 *  - Documentation comment
 */

parser grammar CALParser;

options { tokenVocab=CALLexer; }

@parser::header
{
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
}

@parser::members
{
public CALNodeFactory factory;
private Source source;

/*private static final class BailoutErrorListener extends BaseErrorListener {
    private final Source source;
    BailoutErrorListener(Source source) {
        this.source = source;
    }
    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
        throwParseError(source, line, charPositionInLine, (Token) offendingSymbol, msg);
    }
}

public void SemErr(Token token, String message) {
    assert token != null;
    throwParseError(source, token.getLine(), token.getCharPositionInLine(), token, message);
}

private static void throwParseError(Source source, int line, int charPositionInLine, Token token, String message) {
    int col = charPositionInLine + 1;
    String location = "-- line " + line + " col " + col + ": ";
    int length = token == null ? 1 : Math.max(token.getStopIndex() - token.getStartIndex(), 0);
    throw new CALParseError(source, line, col, length, String.format("Error(s) parsing script:%n" + location + message));
}*/

public static Map<String, RootCallTarget> parseCAL(CALLanguage language, Source source) {
    CALLexer lexer = new CALLexer(CharStreams.fromString(source.getCharacters().toString()));
    CALParser parser = new CALParser(new CommonTokenStream(lexer));
    lexer.removeErrorListeners();
    parser.removeErrorListeners();
    //BailoutErrorListener listener = new BailoutErrorListener(source);
    //lexer.addErrorListener(listener);
    //parser.addErrorListener(listener);
    parser.factory = new CALNodeFactory(language, source);
    parser.source = source;
    return parser.compilationUnit().result;
}
}

// ----------------------------------------------------------------------------
// -- Compilation Unit (Start Rule)
// ----------------------------------------------------------------------------

compilationUnit returns [Map<String, RootCallTarget> result]
    :
        namespaceDeclaration EOF
        { $result = $namespaceDeclaration.result; }
        |
        unitDeclaration EOF
    ;

// ----------------------------------------------------------------------------
// -- Namespace Declaration (CAL Specification Extension)
// ----------------------------------------------------------------------------

namespaceDeclaration returns [Map<String, RootCallTarget> result]
    :
        body=namespaceBody
        { $result = factory.createNamespace(null, $body.result); }
        |
        DOC_COMMENT*
        'namespace' name=qualifiedID ':'
        body=namespaceBody
        ('end' | 'endnamespace')
        { $result = factory.createNamespace($name.result, $body.result); }
        |
        DOC_COMMENT*
        'package'
        qualifiedID
        ';'
        namespaceBody

    ;

namespaceBody returns [Map<String, RootCallTarget> result] locals [List<CALRootNode> entities]
    @init { $entities = new ArrayList<>(); }
    :
        (
            bodyImport=importDeclaration { factory.addNamespaceBodyImport($bodyImport.result); }
        )*
        (
            typeDefinition
            |
            globalVariableDeclaration
            |
            actorDeclaration { $entities.add($actorDeclaration.result); }
            |
            networkDeclaration
        )*
        { $result = factory.createNamespaceBody($entities); }
    ;

// ----------------------------------------------------------------------------
// -- Qualified ID (CLR §3.1)
// ----------------------------------------------------------------------------

qualifiedID returns [List<Token> result]
    @init { $result = new ArrayList<>(); }
    :
        part=ID { $result.add($part); }
        (
            '.' part=ID { $result.add($part); }
        )*
    ;

// ----------------------------------------------------------------------------
// -- Annotation (CAL Specification Extension)
// ----------------------------------------------------------------------------

annotation
    :
        '@' qualifiedID
        (
            '('
            (
                annotationParameter
                (
                    ',' annotationParameter
                )*
            )?
            ')'
        )?
    ;

annotationParameter
    :   ID '=' expression
    |   expression
    ;

// ----------------------------------------------------------------------------
// -- Unit Declaration (CAL Specification Extension)
// ----------------------------------------------------------------------------

unitDeclaration
    :   'unit' ID ':' (globalVariableDeclaration)* ('end' | 'endunit')
    ;

// ----------------------------------------------------------------------------
// -- Import Declaration (CLR §3.1)
// ----------------------------------------------------------------------------

importDeclaration returns [Pair<String, String> result]
    :
        singleImport
        { $result = $singleImport.result; }
        |
        groupImport
    ;

singleImport returns [Pair<String, String> result] locals [Token localName]
    :
        'import'
        (
            importKind
        )?
        globalName=qualifiedID
        (
            '=' ID { $localName = $ID; }
        )?
        ';'
        { $result = factory.createSingleImport($globalName.result, $localName); }
    ;

groupImport
    :   'import' 'all' importKind? qualifiedID ';'
    ;

importKind
    :
        kind='var'
        |
        kind='type'
        |
        kind='entity'
    ;

// ----------------------------------------------------------------------------
// -- Network Declaration (NLR §2)
// ----------------------------------------------------------------------------

networkDeclaration
    :
        DOC_COMMENT*
        annotation*
        'network' qualifiedID '(' formalParameters ')'
        (
            portDeclarations
        )?
        '==>'
        (
            portDeclarations
        )?
        ':'
        (
            'var'
            (
                localVariableDeclaration
            )*
        )?
        (
            'entities'
            (
                entityDeclaration
            )*
        )?
        (
            'structure'
            (
                structureStatement
            )*
        )?
        ('end' | 'endnetwork')
    ;

// ----------------------------------------------------------------------------
// -- Entities (NLR §3)
// ----------------------------------------------------------------------------

entityDeclaration
    :  ID '=' entityExpression ';'
    ;

entityExpressions
    :   entityExpression (',' entityExpression)*
    ;

entityExpression
    :   entityInstanceExpression
    |   entityIfExpression
    |   entityListExpression
    ;

entityInstanceExpression
    :   ID '(' entityParameters? ')' attributeSection?
    ;

entityIfExpression
    :   'if' expression 'then' entityExpression 'else' entityExpression ('end' | 'endif')
    ;

entityListExpression
    :   '[' entityExpressions (':' generators)? ']'
    ;

entityParameters
    :   entityParameter (',' entityParameter)*
    ;

entityParameter
    :   ID '=' expression
    ;

attributeSection
    :   '{' attributeDeclaration* '}'
    ;

attributeDeclaration
    :   ID '=' expression ';'
    |   ID ':' type ';'
    ;

// ----------------------------------------------------------------------------
// -- Structures (NLR §4)
// ----------------------------------------------------------------------------

structureStatement
    :
    annotation*
    (
        structureConnectorStatement
        |
        structureForeachStatement
        |
        structureIfStatement
    )
    ;

structureConnectorStatement
    :   connector '-->' connector attributeSection? ';'
    ;

structureForeachStatement
    :   foreachGenerators 'do' structureStatement* ('end' | 'endforeach')
    ;

structureIfStatement
    :   'if' expression 'then' structureStatement* (structureElseIfStatement | 'else' structureStatement*)? ('end' | 'endif')
    ;

structureElseIfStatement
    :   'elsif' expression 'then' expression (structureElseIfStatement | 'else' expression)?
    ;

connector
    :   (entityReference '.')? portReference
    ;

entityReference
    :   ID ('[' expression ']')*
    ;

portReference
    :   ID ('[' expression ']')*
    ;

// ----------------------------------------------------------------------------
// -- Actor Declaration (CLR §3)
// ----------------------------------------------------------------------------

actorDeclaration returns [ActorNode result] locals [List<CALExpressionNode> localVariables, List<ActionNode> initializers, List<ActionNode> actions]
    @init {
        factory.createActorScope();
        $localVariables = new ArrayList<>();
        $initializers = new ArrayList<>();
        $actions = new ArrayList<>();
    }
    :
        DOC_COMMENT*
        annotation*
        (
            'external'
        )?
        'actor' name=ID '(' formalParameters ')'
        (
            portDeclarations
        )?
        '==>'
        (
            portDeclarations
        )?
        (
            timeCause
        )?
        (
            ':'
            (
                localVariableDeclaration { $localVariables.add($localVariableDeclaration.result); }
                |
                actionDefinition { $actions.add($actionDefinition.result); }
                |
                initializationActionDefinition { $initializers.add($initializationActionDefinition.result); }
                |
                priorityOrder
                |
                actionSchedule
                |
                processDescription
            )*
            ('end' | 'endactor')
            |
            ';'
        )
        { $result = factory.createActor($name, $localVariables, $initializers, $actions); }
    ;

portDeclarations
    :
        portDeclaration
        (
            ',' portDeclaration
        )*
    ;

portDeclaration
    :
        annotation*
        (
            'multi'
        )?
        (
            type
        )?
        ID
    ;

timeCause
    :
        'time' type
    ;

processDescription
    :   ('repeat' | 'do')
        statements
        'end'
    ;

// ----------------------------------------------------------------------------
// -- Action (CLR §8)
// ----------------------------------------------------------------------------

actionDefinition returns [ActionNode result] locals [List<CALExpressionNode> guards, List<CALExpressionNode> localVariables, StmtBlockNode body]
    @init { factory.createActionScope(); }
    :
        DOC_COMMENT*
        (
            annotation
        )*
        (
            actionTag
            ':'
        )?
        'action' inputPatterns '==>' outputExpressions
        (
            'guard' expressions { $guards = $expressions.result; }
        )?
        (
            'var' blockVariableDeclarations { $localVariables = $blockVariableDeclarations.result; }
        )?
        (
            'delay' expression
        )?
        (
            'do' statements { $body = $statements.result; }
        )?
        ('end' | 'endaction')
        { $result = factory.createAction($guards, $localVariables, $body); }
    ;

// ----------------------------------------------------------------------------
// -- Input Patterns (CLR §8.1, but extended)
// ----------------------------------------------------------------------------

inputPatterns
    :   (inputPattern (',' inputPattern)*)?
    ;

inputPattern
    :   (ID ':')?
        '[' patterns? ']'
        ('repeat' expression)?
        channelSelector?
    ;

channelSelector
    :   'at' expression
    |   'at*' expression
    |   'at*'? ANY
    |   'at*'? ALL
    ;

patterns
    :   pattern (',' pattern)*
    ;

pattern
    :   variable
    |   variable '(' subPatterns? ')'
    ;

subPatterns
    :   subPattern (',' subPattern)*
    ;

subPattern
    :   (ID ':')? ('_' | patternExpression | pattern)
    ;

patternExpression
    :   literalExpression | '(' expression ')'
    ;

// ----------------------------------------------------------------------------
// -- Output Expressions (CLR §8.2)
// ----------------------------------------------------------------------------

outputExpressions
    :   (outputExpression (',' outputExpression)*)?
    ;

outputExpression
    :   (ID ':')?
        '[' expressions ']'
        ('repeat' expression)?
        channelSelector?
    ;

// ----------------------------------------------------------------------------
// -- Initialization Action (CLR §8.5)
// ----------------------------------------------------------------------------

initializationActionDefinition returns [ActionNode result] locals [List<CALExpressionNode> guards, List<CALExpressionNode> localVariables, StmtBlockNode body]
    @init { factory.createActionScope(); }
    :
        DOC_COMMENT*
        annotation*
        (
            actionTag
            ':'
        )?
        'initialize'
        '==>' outputExpressions
        (
            'guard' expressions { $guards = $expressions.result; }
        )?
        (
            'var' blockVariableDeclarations { $localVariables = $blockVariableDeclarations.result; }
        )?
        (
            'delay' expression
        )?
        (
            'do' statements { $body = $statements.result; }
        )?
        ('end' | 'endinitialize')
        { $result = factory.createAction($guards, $localVariables, $body); }
    ;

// ----------------------------------------------------------------------------
// -- Action Tags (CLR §9.1)
// ----------------------------------------------------------------------------

actionTags
    :   actionTag (',' actionTag)*
    ;

actionTag
    :   qualifiedID
    ;

// ----------------------------------------------------------------------------
// -- Action Schedules (CLR §9.2)
// ----------------------------------------------------------------------------

actionSchedule
    : scheduleFSM
    | scheduleRegExp
    ;

scheduleFSM                                       // FSM schedules (CLR §9.2.1)
    :   'schedule' 'fsm'? ID ':'
        (stateTransition ';')*
        ('end' | 'endschedule')
    ;

stateTransition
    :   ID '(' actionTags ')' '-->' ID ('|' '(' actionTags ')' '-->' ID)*
    ;

scheduleRegExp                                 // RegExp schedules (CLR §9.2.2)
    :   'schedule' 'regexp' regExp ('end' | 'endschedule')
    ;

regExp
    :   actionTag
    |   '(' regExp ')'
    |   '[' regExp ']'
    |   regExp '*'
    |   regExp regExp
    |   regExp '|' regExp
    ;

// ----------------------------------------------------------------------------
// -- Priorities (CLR §9.3)
// ----------------------------------------------------------------------------

priorityOrder
    :   'priority' (priorityInequality ';')* ('end' | 'endpriority')
    ;

priorityInequality
    :   actionTag '>' actionTag ('>' actionTag)*
    ;

// ----------------------------------------------------------------------------
// -- Variable Declrations (CLR + CAL Specification Extnsion)
// ----------------------------------------------------------------------------

availability
    :   'public'
    |   'private'
    |   'local'
    ;

globalVariableDeclaration
    :   DOC_COMMENT*
        annotation*
        availability?
        'external'?
        (
            explicitVariableDeclaration ';'
            |
            functionVariableDeclaration
            |
            procedureVariableDeclaration
        )
    ;

localVariableDeclaration returns [CALExpressionNode result]
    :   DOC_COMMENT*
        annotation*
        (
            'external'
        )?
        (
            explicitVariableDeclaration ';' { $result = $explicitVariableDeclaration.result; }
            |
            functionVariableDeclaration { $result = $functionVariableDeclaration.result; }
            |
            procedureVariableDeclaration
        )
    ;

blockVariableDeclarations returns [List<CALExpressionNode> result]
    @init { $result = new ArrayList<>(); }
    :
        blockVariable=blockVariableDeclaration { $result.add($blockVariable.result); }
        (
            ',' blockVariable=blockVariableDeclaration { $result.add($blockVariable.result); }
        )*
    ;

blockVariableDeclaration returns [CALExpressionNode result]
    :
        annotation*
        (
            explicitVariableDeclaration { $result = $explicitVariableDeclaration.result; }
            |
            functionVariableDeclaration { $result = $functionVariableDeclaration.result; }
            |
            procedureVariableDeclaration
        )
    ;

// Explicit variable declaration (CLR §5.1.1)
explicitVariableDeclaration returns [CALExpressionNode result] locals [CALExpressionNode value]
    :
        (
            'mutable'
        )?
        (
            type
        )?
        name=ID
        (
            '['
            expression
            ']'
        )*
        (
            ('=' | ':=')
            expression { $value = $expression.result; }
        )?
        { $result = factory.createExplicitVariable($name, $value); }
    ;

// Function & procedure declarations (CLR §6.9.3)
functionVariableDeclaration returns [CALExpressionNode result] locals [List<CALExpressionNode> localVariables, CALExpressionNode body]
    @init { factory.createFunctionVariableScope(); }
    :
        'function' name=ID '(' formalParameters ')'
        (
            '-->' type
        )?
        (
            (
                { factory.createLetExpressionScope(); }
                'var' blockVariableDeclarations { $localVariables = $blockVariableDeclarations.result; }
            )?
            ':' expression { $body = $expression.result; }
        )?
        ('end' | 'endfunction')
        { $result = factory.createFunctionVariable($name, $formalParameters.result, $localVariables, $body); }

    ;

procedureVariableDeclaration
    :
        'procedure' ID '(' formalParameters ')'
        (
            (
                'var' blockVariableDeclarations
            )?
            ('begin' | 'do')
            statements
        )?
        ('end' | 'endprocedure')
    ;

// ----------------------------------------------------------------------------
// -- Formal Parameters (CLR §6.9.1, but extended)
// ----------------------------------------------------------------------------

formalParameters returns [List<InitializeArgNode> result] locals [int position]
    @init {
        $result = new ArrayList<>();
        $position = 0;
    }
    :
        (
            formalParameter[$position] { $result.add($formalParameter.result); $position++; }
            (
                ',' formalParameter[$position] { $result.add($formalParameter.result); $position++; }
            )*
        )?
    ;

formalParameter [int index] returns [InitializeArgNode result]
    :
        explicitVariableDeclaration
        { $result = factory.createFormalParameter($explicitVariableDeclaration.result, $index); }
    ;

// ----------------------------------------------------------------------------
// -- Type definitions (CAL Specification Extension)
// ----------------------------------------------------------------------------

typeDefinition // AlgebraicType
    :   'type' ID ('(' formalParameters ')')? ':'
        (tuple | (taggedTuple ('|' taggedTuple )*)) // (ProductTypeDecl | SumTypeDecl)
        ('end' | 'endtype');

taggedTuple // VariantDecl
    :   ID tuple
    ;

tuple // ProductTypeDecl
    :   ('(' (explicitVariableDeclaration (',' explicitVariableDeclaration)*)? ')')?
    ;

// ----------------------------------------------------------------------------
// -- Type formats (CLR §4.2)
// ----------------------------------------------------------------------------

types
    :   type (',' type)*
    ;

type
    :   ID
    |   'type'
    |   ID '[' typeParameters ']'
    |   ID '(' typeAttributes? ')' // NominalType / Type (Orrc)
    |   '[' types? '-->' type? ']' // LambdaType
    ;

typeParameters
    :   typeParameter (',' typeParameter)*
    ;

typeParameter
    :   ID ('<' type)?
    ;

typeAttributes
    :   typeAttribute (',' typeAttribute)*
    ;

typeAttribute
    :   (ID | 'type') ':' type
    |   ID '=' expression
    ;

// ----------------------------------------------------------------------------
// -- Generators (CLR §6.10.2, §7.6, §7.7)
// ----------------------------------------------------------------------------

generators returns [List<Pair<List<Token>, List<CALExpressionNode>>> result]
    @init { $result = new ArrayList<>(); }
    :
        generator { $result.add($generator.result); }
        (
            ',' generator { $result.add($generator.result); }
        )*
    ;

generator returns [Pair<List<Token>, List<CALExpressionNode>> result]
    :
        'for' generatorBody
        { $result = $generatorBody.result; }
    ;

foreachGenerators returns [List<Pair<List<Token>, List<CALExpressionNode>>> result]
    @init { $result = new ArrayList<>(); }
    :   foreachGenerator { $result.add($foreachGenerator.result); }
        (
            ',' foreachGenerator { $result.add($foreachGenerator.result); }
        )*
    ;

foreachGenerator returns [Pair<List<Token>, List<CALExpressionNode>> result]
    :
        'foreach' generatorBody
        { $result = $generatorBody.result; }
    ;

chooseGenerators returns [List<Pair<List<Token>, List<CALExpressionNode>>> result]
    @init { $result = new ArrayList<>(); }
    :
        chooseGenerator { $result.add($chooseGenerator.result); }
        (
            ',' chooseGenerator { $result.add($chooseGenerator.result); }
        )*
    ;

chooseGenerator returns [Pair<List<Token>, List<CALExpressionNode>> result]
    :
        'choose' generatorBody
        { $result = $generatorBody.result; }
    ;

generatorBody returns [Pair<List<Token>, List<CALExpressionNode>> result] locals [List<Token> variables]
    @init { $variables = new ArrayList<>(); }
    :
        (
            type
        )?
        ID { $variables.add($ID); }
        (
            ',' ID { $variables.add($ID); }
        )?
        'in' expressions
        { $result = factory.createGeneratorBody($variables, $expressions.result); }
    ;

// ----------------------------------------------------------------------------
// -- Expressions (CLR §6, but extended)
// ----------------------------------------------------------------------------

expressions returns [List<CALExpressionNode> result]
    @init { $result = new ArrayList<>(); }
    :
        expression { $result.add($expression.result); }
        (
            ',' expression { $result.add($expression.result); }
        )*
    ;

// Operation Precedence (CLR §Appendix C.1)
expression returns [CALExpressionNode result]
    :
        <assoc=right> operand1=expression operator='^' operand2=expression
        { $result = factory.createBinaryOperationExpression($operand1.result, $operator, $operand2.result); }
        |
        composite=expression '[' indices=expressions ']'
        { $result = factory.createIndexerExpression($composite.result, $indices.result); }
        |
        composite=expression '.' field
        |
        operator='-' operand=expression
        { $result = factory.createUnaryOperationExpression($operator, $operand.result); }
        |
        operator='rng' operand=expression
        { $result = factory.createUnaryOperationExpression($operator, $operand.result); }
        |
        operator='dom' operand=expression
        { $result = factory.createUnaryOperationExpression($operator, $operand.result); }
        |
        operator='#' operand=expression
        { $result = factory.createUnaryOperationExpression($operator, $operand.result); }
        |
        operator='not' operand=expression
        { $result = factory.createUnaryOperationExpression($operator, $operand.result); }
        |
        operator='!' operand=expression
        { $result = factory.createUnaryOperationExpression($operator, $operand.result); }
        |
        operand1=expression operator='..' operand2=expression
        { $result = factory.createBinaryOperationExpression($operand1.result, $operator, $operand2.result); }
        |
        operand1=expression operator=('*' | '/' | '%' | 'div' | 'mod') operand2=expression
        { $result = factory.createBinaryOperationExpression($operand1.result, $operator, $operand2.result); }
        |
        operand1=expression operator=('+' | '-') operand2=expression
        { $result = factory.createBinaryOperationExpression($operand1.result, $operator, $operand2.result); }
        |
        operand1=expression operator=('<<' | '>>') operand2=expression
        { $result = factory.createBinaryOperationExpression($operand1.result, $operator, $operand2.result); }
        |
        operand1=expression operator=('<' | '<=' | '>' | '>=') operand2=expression
        { $result = factory.createBinaryOperationExpression($operand1.result, $operator, $operand2.result); }
        |
        operand1=expression operator=('=' | '==' | '!=') operand2=expression
        { $result = factory.createBinaryOperationExpression($operand1.result, $operator, $operand2.result); }
        |
        operand1=expression operator='&' operand2=expression
        { $result = factory.createBinaryOperationExpression($operand1.result, $operator, $operand2.result); }
        |
        operand1=expression operator='|' operand2=expression
        { $result = factory.createBinaryOperationExpression($operand1.result, $operator, $operand2.result); }
        |
        operand1=expression operator='and' operand2=expression
        { $result = factory.createBinaryOperationExpression($operand1.result, $operator, $operand2.result); }
        |
        operand1=expression operator='or' operand2=expression
        { $result = factory.createBinaryOperationExpression($operand1.result, $operator, $operand2.result); }
        |
        literalExpression
        { $result = $literalExpression.result; }
        |
        variableExpression
        { $result = $variableExpression.result; }
        |
        symbolReferenceExpression
        |
        '(' expression ')'
        { $result = $expression.result; }
        |
        ifExpression
        { $result = $ifExpression.result; }
        |
        letExpression
        { $result = $letExpression.result; }
        |
        lambdaExpression
        { $result = $lambdaExpression.result; }
        |
        procExpression
        |
        setComprehension
        |
        listComprehension
        { $result = $listComprehension.result; }
        |
        mapComprehension
        |
        typeAssertionExpr
        |
        caseExpression
        |
        callExpression
        { $result = $callExpression.result; }
    ;

// Literals (CLR §6.1)
literalExpression returns [CALExpressionNode result]
    :
        IntegerLiteral
        { $result = factory.createIntegerLiteralExpression($IntegerLiteral); }
        |
        FloatingPointLiteral
        { $result = factory.createFloatLiteralExpression($FloatingPointLiteral); }
        |
        BooleanLiteral
        { $result = factory.createBooleanLiteralExpression($BooleanLiteral); }
        |
        CharacterLiteral
        { $result = factory.createCharLiteralExpression($CharacterLiteral); }
        |
        StringLiteral
        { $result = factory.createStringLiteralExpression($StringLiteral); }
        |
        NullLiteral
        { $result = factory.createNullLiteralExpression($NullLiteral); }
    ;

// Variable reference (CLR §6.2)
variableExpression returns [CALExpressionNode result]
    :
        (
            'old'
        )?
        variable
        { $result = factory.createVariableExpression($variable.result); }
    ;

// Symbol Reference (CAL Specification Extension)
symbolReferenceExpression
    :
        variable '::' ID
        (
            '('
            (
                expressions
            )?
            ')'
        )?
    ;

// Conditionals (CLR §6.7)
ifExpression returns [ExprIfNode result] locals [CALExpressionNode elseExpression]
    :
        'if' condition=expression 'then' thenExpression=expression
        (
            elseIfExpression { $elseExpression = $elseIfExpression.result; }
            |
            'else' expression { $elseExpression = $expression.result; }
        )
        { $result = factory.createConditionalExpression($condition.result, $thenExpression.result, $elseExpression); }
    ;

// Elsif (CAL Specification Extension)
elseIfExpression returns [ExprIfNode result] locals [CALExpressionNode elseExpression]
    :
        'elsif' condition=expression 'then' thenExpression=expression
        (
            elseIfExpression { $elseExpression = $elseIfExpression.result; }
            |
            'else' expression { $elseExpression = $expression.result; }
        )
        { $result = factory.createConditionalExpression($condition.result, $thenExpression.result, $elseExpression); }
    ;

// Local Scope (CLR §6.8)
letExpression returns [LetExprNode result]
    @init { factory.createLetExpressionScope(); }
    :
        'let' localVariables=blockVariableDeclarations ':' body=expression ('end' | 'endlet')
        { $result = factory.createLetExpression($localVariables.result, $body.result); }
    ;

// Function closure (CLR §6.9.1)
lambdaExpression returns [LambdaNode result] locals [List<CALExpressionNode> localVariables]
    @init { factory.createLambdaExpressionScope(); }
    :
        (
            'const'
        )?
        'lambda' '(' formalParameters ')'
        (
            '-->' type
        )?
        (
            { factory.createLetExpressionScope(); }
            'var' blockVariableDeclarations { $localVariables = $blockVariableDeclarations.result; }
        )?
        ':'
        body=expression
        ('end' | 'endlambda')
        { $result = factory.createLambdaExpression($formalParameters.result, $localVariables, $body.result); }
    ;

// Procedure closure (CLR §6.9.2)
procExpression
    :
        'proc' '(' formalParameters ')'
        (
            'var' blockVariableDeclarations
        )?
        ('do' | 'begin')
        statements
        ('end' | 'endproc')
    ;

// Comprehensions w/ generators (CLR §6.10.2)
setComprehension
    :   '{' (expressions (':' generators)?)? '}'
    ;

listComprehension returns [CALExpressionNode result] locals [List<Pair<List<Token>, List<CALExpressionNode>>> gens]
    :
        '['
        (
            expressions
            (
                ':' generators { $gens = $generators.result; }
            )?
        )?
        ']'
        { $result = factory.createComprehension($expressions.result, $gens); }
    ;

mapComprehension
    :   'map' '{' (mappings (':' generators)?)? '}'
    ;

mappings
    :   mapping (',' mapping)*
    ;

mapping
    :   expression '->' expression
    ;

// Type Assertion (CLR §6.11)
typeAssertionExpr
    :
        '('
        expression
        ':'
        type
        ')'
    ;

// Case (CAL Specification Extension)
caseExpression
    :
        'case'
        expression
        'of'
        alternativeExpression+
        ('end' | 'endcase')
    ;

alternativeExpression
    :
        pattern
        (
            'guard'
            expressions
        )?
        ':'
        expression
        'end'
    ;

// Function / Procedure Call (CAL Specification Extension)
callExpression returns [CALInvokeNode result] locals [List<CALExpressionNode> arguments]
    :
        function=variableExpression
        '('
            (
                expressions { $arguments = $expressions.result; }
            )?
        ')'
        { $result = factory.createCallExpression($function.result, $arguments); }
    ;

// ----------------------------------------------------------------------------
// -- LValue (CLR §7.1, as part of assignment definition)
// ----------------------------------------------------------------------------

lvalues returns [List<Pair<Token, List<CALExpressionNode>>> result]
    @init { $result = new ArrayList<>(); }
    :
        lvalue { $result.add($lvalue.result); }
        (
            ',' lvalue { $result.add($lvalue.result); }
        )*
    ;

lvalue returns [Pair<Token, List<CALExpressionNode>> result] locals [List<CALExpressionNode> indices]
    @init { $indices = new ArrayList<>(); }
    :
        variable
        (
            '.' field
            |
            '[' expression ']' { $indices.add($expression.result); }
        )*
        { $result = factory.createLvalue($variable.result, $indices); }
    ;

variable returns [Token result]
    :
        ID
        { $result = $ID; }
    ;

field returns [Token result]
    :
        ID
        { $result = $ID; }
    ;

// ----------------------------------------------------------------------------
// -- Statements (CLR §7, but extended)
// ----------------------------------------------------------------------------

statements returns [StmtBlockNode result] locals [List<CALStatementNode> stmts]
    @init { $stmts = new ArrayList<>(); }
    :
        (
            statement { $stmts.add($statement.result); }
        )*
        { $result = factory.createStatements($stmts); }
    ;

statement returns [CALStatementNode result]
    :
        annotation*
        (
            assignmentStatement
            { $result = $assignmentStatement.result; }
            |
            callStatement
            { $result = $callStatement.result; }
            |
            blockStatement
            |
            ifStatement
            { $result = $ifStatement.result; }
            |
            whileStatement
            |
            foreachStatement
            { $result = $foreachStatement.result; }
            |
            chooseStatement
            |
            caseStatement
            |
            readStatement
            |
            writeStatement
            |
            actionSelectionStatement
        )
    ;

// Assignment (CLR §7.1)
assignmentStatement returns [CALStatementNode result]
    :
        lvalue ':=' value=expression ';'
        { $result = factory.createAssignmentStatement($lvalue.result, $value.result); }
    ;

// Procedure call (CLR §7.2)
callStatement returns [CALInvokeNode result] locals [List<CALExpressionNode> arguments]
    :
        function=variableExpression
        '('
        (
            expressions { $arguments = $expressions.result; }
        )?
        ')' ';'
        { $result = factory.createCallStatement($function.result, $arguments); }
    ;

// Statement block (CLR §7.3)
blockStatement
    :
        'begin'
        (
            'var'
            blockVariableDeclarations
            'do'
        )?
        statements
        'end'
    ;

// If (CLR §7.4)
ifStatement returns [StmtIfNode result] locals [CALStatementNode elseStatements]
    :
        'if' condition=expression 'then' thenStatements=statements
        (
            elseIfStatement { $elseStatements = $elseIfStatement.result; }
            |
            'else' statements { $elseStatements = $statements.result; }
        )?
        ('end' | 'endif')
        { $result = factory.createConditionalStatement($condition.result, $thenStatements.result, $elseStatements); }
    ;

// Elsif (CAL Specification Extension)
elseIfStatement returns [StmtIfNode result] locals [CALStatementNode elseStatements]
    :
        'elsif' condition=expression 'then' thenStatements=statements
        (
            elseIfStatement { $elseStatements = $elseIfStatement.result; }
            |
            'else' statements { $elseStatements = $statements.result; }
        )?
        ('end' | 'endif')
        { $result = factory.createConditionalStatement($condition.result, $thenStatements.result, $elseStatements); }
    ;

// While (CLR §7.5)
whileStatement
    :   'while' expression
        (
            'var' blockVariableDeclarations
        )?
        'do'
        statements
        ('end' | 'endwhile')
    ;

// Foreach (CLR §7.6)
foreachStatement returns [ForeacheNode result] locals [List<List<CALExpressionNode>> variables, List<CALExpressionNode> collections, List<List<CALExpressionNode>> filters, List<CALExpressionNode> localVariables]
    :
        foreachGenerators
        { $variables = factory.createForeachStatementGeneratorVariables($foreachGenerators.result); }
        { $collections = factory.getForeachStatementGeneratorCollections($foreachGenerators.result); }
        { $filters = factory.getForeachStatementGeneratorFilters($foreachGenerators.result); }
        (
            'var' blockVariableDeclarations { $localVariables = $blockVariableDeclarations.result; }
        )?
        'do'
        body=statements
        ('end' | 'endforeach')
        { $result = factory.createForeachStatement($variables, $collections, $filters, $localVariables, $body.result); }
    ;


// Choose (CLR §7.7)
chooseStatement
    :
        chooseGenerators
        (
            'var' blockVariableDeclarations
        )?
        'do'
        body=statements
        ('end' | 'endchoose')
    ;


// Case (CAL Specification Extension)
caseStatement
    :   'case' expression 'of' alternativeStatement+ ('end' | 'endcase')
    ;

alternativeStatement
    :   pattern ('guard' expressions )? 'do' statements 'end'
    ;

// Read (CAL Specification Extension)
readStatement
    :   ID '-->' lvalues ('repeat' expression)? ';'
    ;

// Write (CAL Specification Extension)
writeStatement
    :   ID '<--' expressions ('repeat' expression)? ';'
    ;

// Not part of the official specification
actionSelectionStatement
    :   qualifiedID ';'
    ;