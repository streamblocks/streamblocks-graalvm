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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.graalvm.collections.Pair;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.RootCallTarget;
import ch.epfl.vlsc.truffle.cal.CALLanguage;
//import ch.epfl.vlsc.truffle.cal.parser.CALParseError;
import ch.epfl.vlsc.truffle.cal.parser.CALNodeFactory;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.expression.*;
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
        { factory.initializeCompilationUnit(); }
        (
            namespaceDeclaration { factory.setCompilationUnitEntities($namespaceDeclaration.result); }
            EOF
            |
            unitDeclaration
            EOF
        )
        { $result = factory.finalizeCompilationUnit(); }
    ;

// ----------------------------------------------------------------------------
// -- Namespace Declaration (CAL Specification Extension)
// ----------------------------------------------------------------------------

namespaceDeclaration returns [Map<String, RootCallTarget> result]
    :
        { factory.initializeNamespace(); }
        (
            namespaceBody { factory.setNamespaceEntities($namespaceBody.result); }
            |
            DOC_COMMENT*
            'namespace'
            qualifiedID { factory.setNamespaceName($qualifiedID.result); }
            ':'
            namespaceBody { factory.setNamespaceEntities($namespaceBody.result); }
            ('end' | 'endnamespace')
            |
            DOC_COMMENT*
            'package'
            qualifiedID
            ';'
            namespaceBody
        )
        { $result = factory.finalizeNamespace(); }
    ;

namespaceBody returns [Map<String, RootCallTarget> result]
    :
        { factory.initializeNamespaceBody(); }
        (
            importDeclaration { factory.addNamespaceBodyImport($importDeclaration.result); }
        )*
        (
            typeDefinition
            |
            globalVariableDeclaration
            |
            actorDeclaration { factory.addNamespaceBodyEntity($actorDeclaration.result); }
            |
            networkDeclaration
        )*
        { $result = factory.finalizeNamespaceBody(); }
    ;

// ----------------------------------------------------------------------------
// -- Qualified ID (CLR §3.1)
// ----------------------------------------------------------------------------

qualifiedID returns [List<Token> result]
    :
        { factory.initializeQualifiedId(); }
        ID { factory.addQualifiedIdPart($ID); }
        (
            '.'
            ID { factory.addQualifiedIdPart($ID); }
        )*
        { $result = factory.finalizeQualifiedId(); }
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
        { factory.initializeImport(); }
        (
            singleImport { factory.setImportAsSingle($singleImport.result); }
            |
            groupImport
        )
        { $result = factory.finalizeImport(); }
    ;

singleImport returns [Pair<String, String> result]
    :
        { factory.initializeSingleImport(); }
        'import'
        (
            importKind
        )?
        qualifiedID { factory.setSingleImportGlobalName($qualifiedID.result); }
        (
            '='
            ID { factory.setSingleImportLocalName($ID); }
        )?
        ';'
        { $result = factory.finalizeSingleImport(); }
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

actorDeclaration returns [ActorNode result ]
    :
        { factory.initializeActor(); }
        DOC_COMMENT*
        (
            annotation
        )*
        (
            'external'
        )?
        'actor'
        ID { factory.setActorName($ID); }
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
                actionDefinition { factory.addActorAction($actionDefinition.result); }
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
        { $result = factory.finalizeActor(); }
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
    :
        { factory.initializeAction(); }
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
            blockVariableDeclarations { factory.setActionLocalVariables($blockVariableDeclarations.result); }
        )?
        //('delay' expression)?
        (
            'do'
            statements { factory.setActionBodyStatements($statements.result); }
        )?
        ('end' | 'endaction')
        { $result = factory.finalizeAction(); }
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
    :
        { factory.initializeBlockVariables(); }
        blockVariableDeclaration { factory.addBlockVariable($blockVariableDeclaration.result); }
        (
            ','
            blockVariableDeclaration { factory.addBlockVariable($blockVariableDeclaration.result); }
        )*
        { $result = factory.finalizeBlockVariables(); }
    ;

blockVariableDeclaration returns [CALExpressionNode result]
    :
        { factory.initializeBlockVariable(); }
        annotation*
        (
            explicitVariableDeclaration { factory.setExplicitBlockVariable($explicitVariableDeclaration.result); }
            |
            functionVariableDeclaration
            |
            procedureVariableDeclaration
        )
        { $result = factory.finalizeBlockVariable(); }
    ;

// Explicit variable declaration (CLR §5.1.1)
explicitVariableDeclaration returns [CALExpressionNode result]
    :
        { factory.initializeExplicitVariable(); }
        /*'mutable'?*/
        type?
        ID { factory.setExplicitVariableName($ID); }
        (
            '['
            expression
            ']'
        )*
        (
            ('=' | ':=')
            expression { factory.setExplicitVariableExpression($expression.result); }
        )?
        { $result = factory.finalizeExplicitVariable(); }
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

formalParameters
    :   (formalParameter (',' formalParameter)*)?
    ;

formalParameter
    :   explicitVariableDeclaration
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
    :
        { factory.initializeExpressions(); }
        expression { factory.addExpression($expression.result); }
        (
            ','
            expression { factory.addExpression($expression.result); }
        )*
        { $result = factory.finalizeExpressions(); }
    ;

// Operation Precedence (CLR §Appendix C.1)
expression returns [CALExpressionNode result]
    @init { factory.initializeExpression(); }
    :
        <assoc=right> operand1=expression operator='^' operand2=expression { factory.setBinaryOperationExpression($operand1.result, $operator, $operand2.result); $result = factory.finalizeExpression(); }
        |
        expression '[' expressions ']'
        |
        expression '.' field
        |
        operator='-' operand=expression { factory.setUnaryOperationExpression($operator, $operand.result); $result = factory.finalizeExpression(); }
        |
        operator='rng' operand=expression { factory.setUnaryOperationExpression($operator, $operand.result); $result = factory.finalizeExpression(); }
        |
        operator='dom' operand=expression { factory.setUnaryOperationExpression($operator, $operand.result); $result = factory.finalizeExpression(); }
        |
        operator='#' operand=expression { factory.setUnaryOperationExpression($operator, $operand.result); $result = factory.finalizeExpression(); }
        |
        operator='not' operand=expression { factory.setUnaryOperationExpression($operator, $operand.result); $result = factory.finalizeExpression(); }
        |
        operator='!' operand=expression { factory.setUnaryOperationExpression($operator, $operand.result); $result = factory.finalizeExpression(); }
        |
        operand1=expression operator='..' operand2=expression { factory.setBinaryOperationExpression($operand1.result, $operator, $operand2.result); $result = factory.finalizeExpression(); }
        |
        operand1=expression operator=('*' | '/' | '%' | 'div' | 'mod') operand2=expression { factory.setBinaryOperationExpression($operand1.result, $operator, $operand2.result); $result = factory.finalizeExpression(); }
        |
        operand1=expression operator=('+' | '-') operand2=expression { factory.setBinaryOperationExpression($operand1.result, $operator, $operand2.result); $result = factory.finalizeExpression(); }
        |
        operand1=expression operator=('<<' | '>>') operand2=expression { factory.setBinaryOperationExpression($operand1.result, $operator, $operand2.result); $result = factory.finalizeExpression(); }
        |
        operand1=expression operator=('<' | '<=' | '>' | '>=') operand2=expression { factory.setBinaryOperationExpression($operand1.result, $operator, $operand2.result); $result = factory.finalizeExpression(); }
        |
        operand1=expression operator=('=' | '==' | '!=') operand2=expression { factory.setBinaryOperationExpression($operand1.result, $operator, $operand2.result); $result = factory.finalizeExpression(); }
        |
        operand1=expression operator='&' operand2=expression { factory.setBinaryOperationExpression($operand1.result, $operator, $operand2.result); $result = factory.finalizeExpression(); }
        |
        operand1=expression operator='|' operand2=expression { factory.setBinaryOperationExpression($operand1.result, $operator, $operand2.result); $result = factory.finalizeExpression(); }
        |
        operand1=expression operator='and' operand2=expression { factory.setBinaryOperationExpression($operand1.result, $operator, $operand2.result); $result = factory.finalizeExpression(); }
        |
        operand1=expression operator='or' operand2=expression { factory.setBinaryOperationExpression($operand1.result, $operator, $operand2.result); $result = factory.finalizeExpression(); }
        |
        literalExpression { factory.setLiteralExpression($literalExpression.result); $result = factory.finalizeExpression(); }
        |
        variableExpression { factory.setVariableExpression($variableExpression.result); $result = factory.finalizeExpression(); }
        |
        symbolReferenceExpression
        |
        '(' expression ')'
        |
        ifExpression
        |
        letExpression
        |
        lambdaExpression
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
        callExpression { factory.setCallExpression($callExpression.result); $result = factory.finalizeExpression(); }
    ;

// Literals (CLR §6.1)
literalExpression returns [CALExpressionNode result]
    :
        { factory.initializeLiteralExpression(); }
        (
            IntegerLiteral { factory.setIntegerLiteralExpression($IntegerLiteral); }
            |
            FloatingPointLiteral { factory.setFloatLiteralExpression($FloatingPointLiteral); }
            |
            BooleanLiteral { factory.setBooleanLiteralExpression($BooleanLiteral); }
            |
            CharacterLiteral { factory.setCharLiteralExpression($CharacterLiteral); }
            |
            StringLiteral { factory.setStringLiteralExpression($StringLiteral); }
            |
            NullLiteral { factory.setNullLiteralExpression($NullLiteral); }
        )
        { $result = factory.finalizeLiteralExpression(); }
    ;

// Variable reference (CLR §6.2)
variableExpression returns [CALExpressionNode result]
    :
        { factory.initializeVariableExpression(); }
        (
            'old'
        )?
        variable { factory.setVariableExpressionVariable($variable.result); }
        { $result = factory.finalizeVariableExpression(); }
    ;

symbolReferenceExpression     // Symbol Reference (CAL Specification Extension)
    :   variable '::' ID ('(' expressions? ')')?
    ;

ifExpression                                         // Conditionals (CLR §6.7)
    :   'if' expression 'then' expression (elseIfExpression | 'else' expression) ('end' | 'endif')
    ;

elseIfExpression                         // Elsif (CAL Specification Extension)
    :   'elsif' expression 'then' expression (elseIfExpression | 'else' expression)
    ;

letExpression                                         // Local Scope (CLR §6.8)
    :   'let' blockVariableDeclarations ':' expression ('end' | 'endlet')
    ;

lambdaExpression                               // Function closure (CLR §6.9.1)
    :   'const'? 'lambda' '(' formalParameters ')' ('-->' type)?
        ('var' blockVariableDeclarations)? ':' expression
        ('end' | 'endlambda')
    ;

procExpression                                // Procedure closure (CLR §6.9.2)
    :   'proc' '(' formalParameters ')' ('var' blockVariableDeclarations)? ('do' | 'begin') statements ('end' | 'endproc')
    ;

setComprehension                  // Comprehensions w/ generators (CLR §6.10.2)
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

typeAssertionExpr                                 // Type Assertion (CLR §6.11)
    :   '(' expression ':' type ')'
    ;

caseExpression                            // Case (CAL Specification Extension)
    :   'case' expression 'of' alternativeExpression+ ('end' | 'endcase')
    ;

alternativeExpression
    :   pattern ('guard' expressions)? ':' expression 'end'
    ;

// Function / Procedure Call (CAL Specification Extension)
callExpression returns [CALInvokeNode result]
    :
        { factory.initializeCallExpression(); }
        variableExpression { factory.setCallExpressionFunction($variableExpression.result); }
        '('
            (
                expressions { factory.setCallExpressionArguments($expressions.result); }
            )?
        ')'
        { $result = factory.finalizeCallExpression(); }
    ;

// ----------------------------------------------------------------------------
// -- LValue (CLR §7.1, as part of assignment definition)
// ----------------------------------------------------------------------------

lvalues returns [List<Token> result]
    :
        { factory.initializeLvalues(); }
        lvalue { factory.addLvalue($lvalue.result); }
        (
            ','
            lvalue { factory.addLvalue($lvalue.result); }
        )*
        { $result = factory.finalizeLvalues(); }
    ;

lvalue returns [Token result]
    :
        { factory.initializeLvalue(); }
        variable { factory.setLvalueVariable($variable.result); }
        (
            '.'
            field
            |
            '['
            expression
            ']'
        )*
        { $result = factory.finalizeLvalue(); }
    ;

variable returns [Token result]
    :
        { factory.initializeVariable(); }
        ID { factory.setVariable($ID); }
        { $result = factory.finalizeVariable(); }
    ;

field
    :   ID
    ;

// ----------------------------------------------------------------------------
// -- Statements (CLR §7, but extended)
// ----------------------------------------------------------------------------

statements returns [List<CALStatementNode> result]
    :
        { factory.initializeStatements(); }
        (
            statement { factory.addStatement($statement.result); }
        )*
        { $result = factory.finalizeStatements(); }
    ;

statement returns [CALStatementNode result]
    :
        { factory.initializeStatement(); }
        annotation*
        (
            assignmentStatement { factory.setAssignmentStatement($assignmentStatement.result); }
            |
            callStatement { factory.setCallStatement($callStatement.result); }
            |
            blockStatement
            |
            ifStatement
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
        { $result = factory.finalizeStatement(); }
    ;

// Assignment (CLR §7.1)
assignmentStatement returns [CALExpressionNode result]
    :
        { factory.initializeAssignmentStatement(); }
        lvalue { factory.setAssignmentStatementLvalue($lvalue.result); }
        ':='
        expression { factory.setAssignmentStatementExpression($expression.result); }
        ';'
        { $result = factory.finalizeAssignmentStatement(); }
    ;

// Procedure call (CLR §7.2)
callStatement returns [CALInvokeNode result]
    :
        { factory.initializeCallStatement(); }
        variableExpression { factory.setCallStatementFunction($variableExpression.result); }
        '('
        (
            expressions { factory.setCallStatementArguments($expressions.result); }
        )?
        ')'
        ';'
        { $result = factory.finalizeCallStatement(); }
    ;

blockStatement                                    // Statement block (CLR §7.3)
    :   'begin' ('var' blockVariableDeclarations 'do')? statements 'end'
    ;

ifStatement                                                    // If (CLR §7.4)
    :   'if' expression 'then' statements (elseIfStatement | 'else' statements)? ('end' | 'endif')
    ;

elseIfStatement                          // Elsif (CAL Specification Extension)
    :   'elsif' expression 'then' statements (elseIfStatement | 'else' statements)?
    ;

whileStatement                                              // While (CLR §7.5)
    :   'while' expression ('var' blockVariableDeclarations)? 'do' statements ('end' | 'endwhile')
    ;

foreachStatement                                          // Foreach (CLR §7.6)
    :   foreachGenerators ('var' blockVariableDeclarations)? 'do' statements ('end' | 'endforeach')
    ;

/*
chooseStatement                                            // Choose (CLR §7.7)
    :   chooseGenerators ('var' blockVariableDeclarations)? 'do' statements ('end' | 'endchoose')
    ;
*/

caseStatement                             // Case (CAL Specification Extension)
    :   'case' expression 'of' alternativeStatement+ ('end' | 'endcase')
    ;

alternativeStatement
    :   pattern ('guard' expressions )? 'do' statements 'end'
    ;

readStatement                             // Read (CAL Specification Extension)
    :   ID '-->' lvalues ('repeat' expression)? ';'
    ;

writeStatement                           // Write (CAL Specification Extension)
    :   ID '<--' expressions ('repeat' expression)? ';'
    ;

actionSelectionStatement              // Not part of the official specification
    :   qualifiedID ';'
    ;