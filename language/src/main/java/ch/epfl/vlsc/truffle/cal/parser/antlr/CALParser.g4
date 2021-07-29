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
        body=namespaceBody { factory.setNamespaceBody($body.result); }
        { $result = factory.createNamespace(); }
        |
        DOC_COMMENT*
        'namespace'
        name=qualifiedID { factory.setNamespaceName($name.result); }
        ':'
        body=namespaceBody { factory.setNamespaceBody($body.result); }
        ('end' | 'endnamespace')
        { $result = factory.createNamespace(); }
        |
        DOC_COMMENT*
        'package'
        qualifiedID
        ';'
        namespaceBody

    ;

namespaceBody returns [Map<String, RootCallTarget> result]
    @init { factory.initNamespaceBody(); }
    :
        (
            bodyImport=importDeclaration { factory.addNamespaceBodyImport($bodyImport.result); }
        )*
        (
            typeDefinition
            |
            globalVariableDeclaration
            |
            actor=actorDeclaration { factory.addNamespaceBodyEntity($actor.result); }
            |
            networkDeclaration
        )*
        { $result = factory.createNamespaceBody(); }
    ;

// ----------------------------------------------------------------------------
// -- Qualified ID (CLR §3.1)
// ----------------------------------------------------------------------------

qualifiedID returns [List<Token> result]
    @init { factory.initQualifiedId(); }
    :
        part=ID { factory.addQualifiedIdPart($part); }
        (
            '.'
            part=ID { factory.addQualifiedIdPart($part); }
        )*
        { $result = factory.createQualifiedId(); }
    ;

// ----------------------------------------------------------------------------
// -- Annotation (CAL Specification Extension)
// ----------------------------------------------------------------------------

annotation
    :   '@' qualifiedID ('(' (annotationParameter (',' annotationParameter)* )? ')')?
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

singleImport returns [Pair<String, String> result]
    :
        'import'
        (
            importKind
        )?
        globalName=qualifiedID { factory.setSingleImportGlobalName($globalName.result); }
        (
            '='
            localName=ID { factory.setSingleImportLocalName($localName); }
        )?
        ';'
        { $result = factory.createSingleImport(); }
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
        (
            annotation
        )*
        'network'
        qualifiedID
        '('
        formalParameters
        ')'
        ioSignature
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

actorDeclaration returns [ActorNode result]
    @init { factory.initActor(); }
    :
        DOC_COMMENT*
        (
            annotation
        )*
        (
            'external'
        )?
        'actor'
        name=ID { factory.setActorName($name); }
        '('
        formalParameters
        ')'
        ioSignature
        (
            timeCause
        )?
        (
            ':'
            (
                localVariableDeclaration
                |
                action=actionDefinition { factory.addActorAction($action.result); }
                |
                initializeActionDefinition
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
        { $result = factory.createActor(); }
    ;

ioSignature
    :   portDeclarations? '==>' portDeclarations?
    ;

portDeclarations
    :   portDeclaration (',' portDeclaration)*
    ;

portDeclaration
    :   annotation* /*'multi'?*/ type? ID
    ;

timeCause
    :   'time' type
    ;

processDescription
    :   ('repeat' | 'do')
        statements
        'end'
    ;

// ----------------------------------------------------------------------------
// -- Action (CLR §8)
// ----------------------------------------------------------------------------

actionDefinition returns [ActionNode result]
    @init { factory.initAction(); }
    :
        DOC_COMMENT*
        (
            annotation
        )*
        (
            actionTag
            ':'
        )?
        'action'
        inputPatterns
        '==>'
        outputExpressions
        (
            'guard'
            expressions
        )?
        (
            'var'
            localVariables=blockVariableDeclarations { factory.setActionLocalVariables($localVariables.result); }
        )?
        //('delay' expression)?
        (
            'do'
            body=statements { factory.setActionBody($body.result); }
        )?
        ('end' | 'endaction')
        { $result = factory.createAction(); }
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

initializeActionDefinition
    :   DOC_COMMENT*
        annotation*
        (actionTag ':')?
        'initialize'
        inputPatterns '==>' outputExpressions
        ('guard' expressions)?
        ('var' blockVariableDeclarations)?
        //('delay' expression)?
        ('do' statements)?
        ('end' | 'endinitialize')
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

localVariableDeclaration
    :   DOC_COMMENT*
        annotation*
        'external'?
        (
            explicitVariableDeclaration ';'
            |
            functionVariableDeclaration
            |
            procedureVariableDeclaration
        )
    ;

blockVariableDeclarations returns [List<CALExpressionNode> result]
    @init { factory.initBlockVariables(); }
    :
        blockVariable=blockVariableDeclaration { factory.addBlockVariable($blockVariable.result); }
        (
            ','
            blockVariable=blockVariableDeclaration { factory.addBlockVariable($blockVariable.result); }
        )*
        { $result = factory.createBlockVariables(); }
    ;

blockVariableDeclaration returns [CALExpressionNode result]
    :
        annotation*
        (
            explicitVariableDeclaration { $result = $explicitVariableDeclaration.result; }
            |
            functionVariableDeclaration
            |
            procedureVariableDeclaration
        )
    ;

// Explicit variable declaration (CLR §5.1.1)
explicitVariableDeclaration returns [CALExpressionNode result]
    :
        /*'mutable'?*/
        (
            type
        )?
        name=ID { factory.setExplicitVariableName($name); }
        (
            '['
            expression
            ']'
        )*
        (
            ('=' | ':=')
            value=expression { factory.setExplicitVariableValue($value.result); }
        )?
        { $result = factory.createExplicitVariable(); }
    ;

functionVariableDeclaration   // Function & procedure declarations (CLR §6.9.3)
    :   'function' ID '(' formalParameters ')' ('-->' type)?
        (('var' blockVariableDeclarations)? ':' expression)?
        ('end' | 'endfunction')
    ;

procedureVariableDeclaration
    :   'procedure' ID '(' formalParameters ')'
        (('var' blockVariableDeclarations)? ('begin' | 'do') statements)?
        ('end' | 'endprocedure')
    ;

// ----------------------------------------------------------------------------
// -- Formal Parameters (CLR §6.9.1, but extended)
// ----------------------------------------------------------------------------

formalParameters returns [List<CALExpressionNode> result]
    @init { factory.initFormalParameters(); }
    :
        (
            formalParameter { factory.addFormalParameter($formalParameter.result); }
            (
                ','
                formalParameter { factory.addFormalParameter($formalParameter.result); }
            )*
        )?
        { $result = factory.createFormalParameters(); }
    ;

formalParameter returns [CALExpressionNode result]
    :
        explicitVariable=explicitVariableDeclaration
        { $result = $explicitVariable.result; }
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

generators
    :   generator (',' generator)*
    ;

generator
    :   'for' generatorBody
    ;

foreachGenerators
    :   foreachGenerator (',' foreachGenerator)*
    ;

foreachGenerator
    :   'foreach' generatorBody
    ;

/*
chooseGenerators
    :   chooseGenerator (',' chooseGenerator)*
    ;

chooseGenerator
    :   'choose' generatorBody
    ;
*/

generatorBody
    :   type? ID (',' ID)? IN expressions
    ;

// ----------------------------------------------------------------------------
// -- Expressions (CLR §6, but extended)
// ----------------------------------------------------------------------------

expressions returns [List<CALExpressionNode> result]
    @init { factory.initExpressions(); }
    :
        expression { factory.addExpression($expression.result); }
        (
            ','
            expression { factory.addExpression($expression.result); }
        )*
        { $result = factory.createExpressions(); }
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
        listComprehension
        |
        setComprehension
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
            'old' { factory.setVariableExpressionAsOld(); }
        )?
        variable { factory.setVariableExpressionVariable($variable.result); }
        { $result = factory.createVariableExpression(); }
    ;

// Symbol Reference (CAL Specification Extension)
symbolReferenceExpression
    :
        variable
        '::'
        ID
        (
            '('
            (
                expressions
            )?
            ')'
        )?
    ;

// Conditionals (CLR §6.7)
ifExpression returns [ExprIfNode result]
    :
        'if'
        condition=expression { factory.setConditionalExpressionCondition($condition.result); }
        'then'
        thenExpr=expression { factory.setConditionalExpressionThenExpression($thenExpr.result); }
        (
            elseIfExpression { factory.setConditionalExpressionElseExpression($elseIfExpression.result); }
            |
            'else'
            elseExpr=expression { factory.setConditionalExpressionElseExpression($elseExpr.result); }
        )
        ('end' | 'endif')
        { $result = factory.createConditionalExpression(); }
    ;

// Elsif (CAL Specification Extension)
elseIfExpression returns [ExprIfNode result]
    :
        'elsif'
        condition=expression { factory.setConditionalExpressionCondition($condition.result); }
        'then'
        thenExpr=expression { factory.setConditionalExpressionThenExpression($thenExpr.result); }
        (
            elseIfExpression { factory.setConditionalExpressionElseExpression($elseIfExpression.result); }
            |
            'else'
            elseExpr=expression { factory.setConditionalExpressionElseExpression($elseExpr.result); }
        )
        { $result = factory.createConditionalExpression(); }
    ;

// Local Scope (CLR §6.8)
letExpression returns [LetExprNode result]
    @init { factory.initLetExpression(); }
    :
        'let'
        localVariables=blockVariableDeclarations { factory.setLetExpressionLocalVariables($localVariables.result); }
        ':'
        body=expression { factory.setLetExpressionBody($body.result); }
        ('end' | 'endlet')
        { $result = factory.createLetExpression(); }
    ;

// Function closure (CLR §6.9.1)
lambdaExpression returns [LambdaNode result]
    @init { factory.initLambdaExpression(); }
    :
        (
            'const'
        )?
        'lambda'
        '('
        formalParameters { factory.setLambdaExpressionFormalParameters($formalParameters.result); }
        ')'
        (
            '-->'
            type
        )?
        (
            'var'
            localVariables=blockVariableDeclarations { factory.setLambdaExpressionLocalVariables($localVariables.result); }
        )?
        ':'
        body=expression { factory.setLambdaExpressionBody($body.result); }
        ('end' | 'endlambda')
        { $result = factory.createLambdaExpression(); }
    ;

// Procedure closure (CLR §6.9.2)
procExpression
    :
        'proc'
        '('
        formalParameters
        ')'
        (
            'var'
            blockVariableDeclarations
        )?
         ('do' | 'begin')
         statements
         ('end' | 'endproc')
    ;

// Comprehensions w/ generators (CLR §6.10.2)
setComprehension
    :   '{' (expressions (':' generators)?)? '}'
    ;

listComprehension
    :   '[' (expressions (':' generators)?)? ']'
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
callExpression returns [CALInvokeNode result]
    :
        function=variableExpression { factory.setCallExpressionFunction($function.result); }
        '('
            (
                arguments=expressions { factory.setCallExpressionArguments($arguments.result); }
            )?
        ')'
        { $result = factory.createCallExpression(); }
    ;

// ----------------------------------------------------------------------------
// -- LValue (CLR §7.1, as part of assignment definition)
// ----------------------------------------------------------------------------

lvalues returns [List<Token> result]
    @init { factory.initLvalues(); }
    :
        lvalue { factory.addLvalue($lvalue.result); }
        (
            ','
            lvalue { factory.addLvalue($lvalue.result); }
        )*
        { $result = factory.createLvalues(); }
    ;

lvalue returns [Token result]
    :
        variable { factory.setLvalueVariable($variable.result); }
        (
            '.'
            field
            |
            '['
            expression
            ']'
        )*
        { $result = factory.createLvalue(); }
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

statements returns [List<CALStatementNode> result]
    @init { factory.initStatements(); }
    :
        (
            statement { factory.addStatement($statement.result); }
        )*
        { $result = factory.createStatements(); }
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
            |/*
            chooseStatement
            |*/
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
assignmentStatement returns [CALExpressionNode result]
    :
        var=lvalue { factory.setAssignmentStatementVariable($var.result); }
        ':='
        value=expression { factory.setAssignmentStatementValue($value.result); }
        ';'
        { $result = factory.createAssignmentStatement(); }
    ;

// Procedure call (CLR §7.2)
callStatement returns [CALInvokeNode result]
    :
        function=variableExpression { factory.setCallStatementFunction($function.result); }
        '('
        (
            arguments=expressions { factory.setCallStatementArguments($arguments.result); }
        )?
        ')'
        ';'
        { $result = factory.createCallStatement(); }
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
ifStatement returns [StmtIfNode result]
    :
        'if'
        condition=expression { factory.setConditionalStatementCondition($condition.result); }
        'then'
        thenStmts=statements { factory.setConditionalStatementThenStatements($thenStmts.result); }
        (
            elseIfStatement { factory.setConditionalStatementElseStatements(Arrays.asList($elseIfStatement.result)); }
            |
            'else'
            elseStmts=statements { factory.setConditionalStatementElseStatements($elseStmts.result); }
        )?
        ('end' | 'endif')
        { $result = factory.createConditionalStatement(); }
    ;

// Elsif (CAL Specification Extension)
elseIfStatement returns [StmtIfNode result]
    :
        'elsif'
        condition=expression { factory.setConditionalStatementCondition($condition.result); }
        'then'
        thenStmts=statements { factory.setConditionalStatementThenStatements($thenStmts.result); }
        (
            elseIfStatement { factory.setConditionalStatementElseStatements(Arrays.asList($elseIfStatement.result)); }
            |
            'else'
            elseStmts=statements { factory.setConditionalStatementElseStatements($elseStmts.result); }
        )?
        { $result = factory.createConditionalStatement(); }
    ;

// While (CLR §7.5)
whileStatement
    :   'while' expression ('var' blockVariableDeclarations)? 'do' statements ('end' | 'endwhile')
    ;

// Foreach (CLR §7.6)
foreachStatement
    :   foreachGenerators ('var' blockVariableDeclarations)? 'do' statements ('end' | 'endforeach')
    ;

/*
// Choose (CLR §7.7)
chooseStatement
    :   chooseGenerators ('var' blockVariableDeclarations)? 'do' statements ('end' | 'endchoose')
    ;
*/

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