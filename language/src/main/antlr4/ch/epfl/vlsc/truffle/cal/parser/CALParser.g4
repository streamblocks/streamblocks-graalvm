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
import java.util.Map;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.RootCallTarget;

import ch.epfl.vlsc.truffle.cal.CALLanguage;

import ch.epfl.vlsc.truffle.cal.parser.scope.ScopeEnvironment;
import ch.epfl.vlsc.truffle.cal.parser.exception.ErrorListener;
import ch.epfl.vlsc.truffle.cal.parser.visitor.*;

import ch.epfl.vlsc.truffle.cal.nodes.*;
import ch.epfl.vlsc.truffle.cal.nodes.expression.*;
import ch.epfl.vlsc.truffle.cal.nodes.contorlflow.*;
import ch.epfl.vlsc.truffle.cal.nodes.local.*;
import ch.epfl.vlsc.truffle.cal.nodes.fifo.*;
import ch.epfl.vlsc.truffle.cal.nodes.util.QualifiedID;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
}

@parser::members
{
    public static Pair<Map<List<String>, List<QualifiedID>>, CompilationUnitContext> getNamespaceEntitiesAndParser(Source source) {
        CALLexer lexer = new CALLexer(CharStreams.fromString(source.getCharacters().toString()));
        CALParser parser = new CALParser(new CommonTokenStream(lexer));

        lexer.removeErrorListeners();
        parser.removeErrorListeners();

        ErrorListener listener = new ErrorListener(source);
        lexer.addErrorListener(listener);
        parser.addErrorListener(listener);

        CompilationUnitContext compilationUnitContext = parser.compilationUnit();
        if (compilationUnitContext instanceof NamespaceCompilationUnitContext)
            return new ImmutablePair<>(
                    NamespaceEntitiesMapVisitor.getInstance().visitNamespaceCompilationUnit((NamespaceCompilationUnitContext) compilationUnitContext),
                    compilationUnitContext
            );
        else
            return new ImmutablePair(Map.of(), compilationUnitContext);
    }

    public static Map<String, RootCallTarget> parseCAL(CALLanguage language, CompilationUnitContext compilationUnitContext, Source source, Map<List<String>, List<QualifiedID>> namespaceEntities) {
        ScopeEnvironment.createInstance(language, source);
        CompilationUnitVisitor.getInstance().setNamespaceEntitiesMap(namespaceEntities);
        return (Map<String, RootCallTarget>) CompilationUnitVisitor.getInstance().visit(compilationUnitContext);
    }
}

// ----------------------------------------------------------------------------
// -- Compilation Unit (Start Rule)
// ----------------------------------------------------------------------------

compilationUnit:
    namespaceDeclaration EOF # NamespaceCompilationUnit
    |
    unitDeclaration EOF # UnitCompilationUnit
;

// ----------------------------------------------------------------------------
// -- Namespace Declaration (CAL Specification Extension)
// ----------------------------------------------------------------------------

namespaceDeclaration:
    namedNamespaceDeclaration
    |
    packageNamespaceDeclaration
    |
    unnamedNamespaceDeclaration
;

packageNamespaceDeclaration:
    DOC_COMMENT*
    'package' name=qualifiedID ';'
    namespaceBody
;

namedNamespaceDeclaration:
    DOC_COMMENT*
    'namespace' name=qualifiedID ':'
    body=namespaceBody
    ('end' | 'endnamespace')
;

unnamedNamespaceDeclaration:
    body=namespaceBody
;

namespaceBody:
    importDeclaration*
    (typeDefinition | globalVariableDeclaration | actorDeclaration | networkDeclaration)*
;

// ----------------------------------------------------------------------------
// -- Qualified ID (CLR §3.1)
// ----------------------------------------------------------------------------

qualifiedID:
    ID ('.' ID)*
;

// ----------------------------------------------------------------------------
// -- Annotation (CAL Specification Extension)
// ----------------------------------------------------------------------------

annotation:
    '@' name=qualifiedID ('(' (annotationParameter (',' annotationParameter)*)? ')')?
;

annotationParameter:
    name=ID '=' expression # NamedAnnotationParameter
    |
    expression # UnnamedAnnotationParameter
;

// ----------------------------------------------------------------------------
// -- Unit Declaration (CAL Specification Extension)
// ----------------------------------------------------------------------------

unitDeclaration:
    'unit' name=ID ':' globalVariableDeclaration* ('end' | 'endunit')
;

// ----------------------------------------------------------------------------
// -- Import Declaration (CLR §3.1)
// ----------------------------------------------------------------------------

importDeclaration:
    singleImport # SingleImportDeclaration
    |
    groupImport # GroupImportDeclaration
;

singleImport:
    'import' kind=importKind? globalName=qualifiedID ('=' localName=ID)? ';'
;

groupImport:
    'import' 'all' kind=importKind? globalName=qualifiedID ';'
;

importKind:
    kind='var'
    |
    kind='type'
    |
    kind='entity'
;

// ----------------------------------------------------------------------------
// -- Network Declaration (NLR §2)
// ----------------------------------------------------------------------------

networkDeclaration:
    DOC_COMMENT*
    annotation*
    'network' name=qualifiedID '(' formalParameters? ')' inputPorts=portDeclarations? '==>' outputPorts=portDeclarations? ':'
    ('var' localVariableDeclaration*)?
    ('entities' entityDeclaration*)?
    ('structure' structureStatement*)?
    ('end' | 'endnetwork')
;

// ----------------------------------------------------------------------------
// -- Entities (NLR §3)
// ----------------------------------------------------------------------------

entityDeclaration:
    name=ID '=' entityExpression ';'
;

entityExpressions:
    entityExpression (',' entityExpression)*
;

entityExpression:
    entityInstanceExpression
    |
    entityIfExpression
    |
    entityListExpression
;

entityInstanceExpression:
    actor=ID '(' entityParameters? ')' attributeSection?
;

entityIfExpression:
    'if' expression 'then'
    entityExpression
    'else'
    entityExpression
    ('end' | 'endif')
;

entityListExpression:
    '[' entityExpressions (':' generators)? ']'
;

entityParameters:
    entityParameter (',' entityParameter)*
;

entityParameter:
    name=ID '=' value=expression
;

attributeSection:
    '{' attributeDeclaration* '}'
;

attributeDeclaration:
    ID '=' expression ';'
    |
    ID ':' type ';'
;

// ----------------------------------------------------------------------------
// -- Structures (NLR §4)
// ----------------------------------------------------------------------------

structureStatement:
    annotation*
    (structureConnectorStatement | structureForeachStatement | structureIfStatement)
;

structureConnectorStatement:
    connector '-->' connector attributeSection? ';'
;

structureForeachStatement:
    foreachGenerators 'do'
    structureStatement*
    ('end' | 'endforeach')
;

structureIfStatement:
    'if' expression 'then'
    structureStatement*
    (structureElseIfStatement | 'else' structureStatement*)?
    ('end' | 'endif')
;

structureElseIfStatement:
    'elsif' expression 'then'
    expression
    (structureElseIfStatement | 'else' expression)?
;

connector:
    (entityReference '.')? portReference
;

entityReference:
    name=ID ('[' expression ']')*
;

portReference:
    name=ID ('[' expression ']')*
;

// ----------------------------------------------------------------------------
// -- Actor Declaration (CLR §3)
// ----------------------------------------------------------------------------

actorDeclaration:
    DOC_COMMENT*
    annotation*
    isExternal='external'? 'actor' name=ID '(' formalParameters? ')' inputPorts=portDeclarations? '==>' outputPorts=portDeclarations? ('time' time=type)?
    (
        ':'
        (localVariableDeclaration | actionDefinition | initializationActionDefinition | priorityOrder | actionSchedule | processDescription)*
        ('end' | 'endactor')
        |
        ';'
    )
;

portDeclarations:
    portDeclaration (',' portDeclaration)*
;

portDeclaration:
    annotation*
    isMulti='multi'? type? name=ID
;

processDescription:
    ('repeat' | 'do')
    statements
    'end'
;

// ----------------------------------------------------------------------------
// -- Action (CLR §8)
// ----------------------------------------------------------------------------

actionDefinition:
    DOC_COMMENT*
    annotation*
    (actionTag ':')? 'action' inputPatterns? '==>' outputExpressions? ('guard' guards=expressions)?
    ('var' localVariables=blockVariableDeclarations)?
    ('delay' delay=expression)?
    ('do' body=statements)?
    ('end' | 'endaction')
;

// ----------------------------------------------------------------------------
// -- Input Patterns (CLR §8.1, but extended)
// ----------------------------------------------------------------------------

inputPatterns:
    inputPattern (',' inputPattern)*
;

inputPattern:
    (port=ID ':')? '[' patterns? ']' ('repeat' repeat=expression)? channelSelector?
;

channelSelector:
    'at' expression
    |
    'at*' expression
    |
    'at*'? 'any'
    |
    'at*'? 'all'
;

patterns:
    pattern (',' pattern)*
;

pattern:
    variable # SimplePattern
    |
    variable '(' subPatterns? ')' # ComplexPattern
;

subPatterns:
    subPattern (',' subPattern)*
;

subPattern:
    (ID ':')? ('_' | patternExpression | pattern)
;

patternExpression:
    literalExpression
    |
    '(' expression ')'
;

// ----------------------------------------------------------------------------
// -- Output Expressions (CLR §8.2)
// ----------------------------------------------------------------------------

outputExpressions:
    outputExpression (',' outputExpression)*
;

outputExpression:
    (port=ID ':')? '[' expressions ']' ('repeat' repeat=expression)? channelSelector?
;

// ----------------------------------------------------------------------------
// -- Initialization Action (CLR §8.5)
// ----------------------------------------------------------------------------

initializationActionDefinition:
    DOC_COMMENT*
    annotation*
    (actionTag ':')? 'initialize' '==>' outputExpressions? ('guard' guards=expressions)?
    ('var' localVariables=blockVariableDeclarations)?
    ('delay' delay=expression)?
    ('do' body=statements)?
    ('end' | 'endinitialize')
;

// ----------------------------------------------------------------------------
// -- Action Tags (CLR §9.1)
// ----------------------------------------------------------------------------

actionTags:
    actionTag (',' actionTag)*
;

actionTag:
    qualifiedID
;

// ----------------------------------------------------------------------------
// -- Action Schedules (CLR §9.2)
// ----------------------------------------------------------------------------

actionSchedule:
    scheduleFSM
    |
    scheduleRegExp
;

// FSM schedules (CLR §9.2.1)
scheduleFSM:
    'schedule' 'fsm'? name=ID ':'
    (stateTransition ';')*
    ('end' | 'endschedule')
;

stateTransition:
    source=ID transitionTargetList
;

transitionTargetList:
    transitionTarget ('|' transitionTarget)*
;

transitionTarget:
    '(' actionTags ')' '-->' target=ID
;

// RegExp schedules (CLR §9.2.2)
scheduleRegExp:
    'schedule' 'regexp' regExp ('end' | 'endschedule')
;

regExp:
    actionTag
    |
    regExpGroup
    |
    regExpOptional
    |
    regExp '*'
    |
    regExp regExp
    |
    regExp '|' regExp
;

regExpGroup:
    '(' regExp ')'
;

regExpOptional:
    '[' regExp ']'
;

// ----------------------------------------------------------------------------
// -- Priorities (CLR §9.3)
// ----------------------------------------------------------------------------

priorityOrder:
    'priority'
    (priorityInequality ';')*
    ('end' | 'endpriority')
;

priorityInequality:
    actionTag '>' actionTag ('>' actionTag)*
;

// ----------------------------------------------------------------------------
// -- Variable Declrations (CLR + CAL Specification Extnsion)
// ----------------------------------------------------------------------------

availability:
    'public'
    |
    'private'
    |
    'local'
;

globalVariableDeclaration:
    DOC_COMMENT*
    annotation*
    availability? isExternal='external'? (explicitVariableDeclaration ';' | functionVariableDeclaration | procedureVariableDeclaration)
;

localVariableDeclaration:
    DOC_COMMENT*
    annotation*
    isExternal='external'? (explicitVariableDeclaration ';' | functionVariableDeclaration | procedureVariableDeclaration)
;

blockVariableDeclarations:
    blockVariableDeclaration (',' blockVariableDeclaration)*
;

blockVariableDeclaration:
    annotation*
    (explicitVariableDeclaration | functionVariableDeclaration | procedureVariableDeclaration)
;

// Explicit variable declaration (CLR §5.1.1)
explicitVariableDeclaration:
    isMutable='mutable'? type? name=ID ('[' expression ']')* (('=' | isAssignable=':=') value=expression)?
;

// Function & procedure declarations (CLR §6.9.3)
functionVariableDeclaration:
    'function' name=ID '(' formalParameters? ')' ('-->' type)?
    (
        ('var' localVariables=blockVariableDeclarations)? ':'
        body=expression
    )?
    ('end' | 'endfunction')
;

procedureVariableDeclaration:
    'procedure' name=ID '(' formalParameters? ')'
    (
        ('var' localVariables=blockVariableDeclarations)?
        ('begin' | 'do')
        statements
    )?
    ('end' | 'endprocedure')
;

// ----------------------------------------------------------------------------
// -- Formal Parameters (CLR §6.9.1, but extended)
// ----------------------------------------------------------------------------

formalParameters:
    formalParameter (',' formalParameter)*
;

formalParameter:
    explicitVariableDeclaration
;

// ----------------------------------------------------------------------------
// -- Type definitions (CAL Specification Extension)
// ----------------------------------------------------------------------------

// AlgebraicType (in Tycho)
typeDefinition:
    'type' ID ('(' formalParameters? ')')? ':'
    (tuple | (taggedTuple ('|' taggedTuple )*)) // ProductTypeDecl | SumTypeDecl (in Tycho)
    ('end' | 'endtype')
;

// VariantDecl (in Tycho)
taggedTuple:
    ID tuple
;

// ProductTypeDecl (in Tycho)
tuple:
    ('(' (explicitVariableDeclaration (',' explicitVariableDeclaration)*)? ')')?
;

// ----------------------------------------------------------------------------
// -- Type formats (CLR §4.2)
// ----------------------------------------------------------------------------

types:
    type (',' type)*
;

type:
    ID
    |
    'type'
    |
    ID '[' typeParameters ']'
    |
    ID '(' typeAttributes? ')' // NominalType (in Tycho) / Type (in Orrc)
    |
    '[' types? '-->' type? ']' // LambdaType (in Tycho)
;

typeParameters:
    typeParameter (',' typeParameter)*
;

typeParameter:
    ID ('<' type)?
;

typeAttributes:
    typeAttribute (',' typeAttribute)*
;

typeAttribute:
    (ID | 'type') ':' type
    |
    ID '=' expression
;

// ----------------------------------------------------------------------------
// -- Generators (CLR §6.10.2, §7.6, §7.7)
// ----------------------------------------------------------------------------

generators:
    generator (',' generator)*
;

generator:
    'for' generatorBody
;

foreachGenerators:
    foreachGenerator (',' foreachGenerator)*
;

foreachGenerator:
    'foreach' generatorBody
;

chooseGenerators:
    chooseGenerator (',' chooseGenerator)*
;

chooseGenerator:
    'choose' generatorBody
;

generatorBody:
    type? variables+=ID (',' variables+=ID)? 'in' generatorExpressions
;

// ----------------------------------------------------------------------------
// -- Expressions (CLR §6, but extended)
// ----------------------------------------------------------------------------

generatorExpressions:
    collection=expression (',' filters+=expression)*
;

expressions:
    expression (',' expression)*
;

// Operation Precedence (CLR §Appendix C.1)
expression:
    <assoc=right> operand1=expression operator='^' operand2=expression # BinaryOperationExpression
    |
    composite=expression '[' indices=expressions ']' # IndexerExpression
    |
    composite=expression '.' field # FieldSelectorExpression
    |
    operator='-' operand=expression # UnaryOperationExpression
    |
    operator='rng' operand=expression # UnaryOperationExpression
    |
    operator='dom' operand=expression # UnaryOperationExpression
    |
    operator='#' operand=expression # UnaryOperationExpression
    |
    operator='not' operand=expression # UnaryOperationExpression
    |
    operator='!' operand=expression # UnaryOperationExpression
    |
    operand1=expression operator='..' operand2=expression # BinaryOperationExpression
    |
    operand1=expression operator=('*' | '/' | '%' | 'div' | 'mod') operand2=expression # BinaryOperationExpression
    |
    operand1=expression operator=('+' | '-') operand2=expression # BinaryOperationExpression
    |
    operand1=expression operator=('<<' | '>>') operand2=expression # BinaryOperationExpression
    |
    operand1=expression operator=('<' | '<=' | '>' | '>=') operand2=expression # BinaryOperationExpression
    |
    operand1=expression operator=('=' | '==' | '!=') operand2=expression # BinaryOperationExpression
    |
    operand1=expression operator='&' operand2=expression # BinaryOperationExpression
    |
    operand1=expression operator='|' operand2=expression # BinaryOperationExpression
    |
    operand1=expression operator='and' operand2=expression # BinaryOperationExpression
    |
    operand1=expression operator='or' operand2=expression # BinaryOperationExpression
    |
    literalExpression # LiteralExprExpression
    |
    variableExpression # VariableExprExpression
    |
    symbolReferenceExpression # SymbolReferenceExprExpression
    |
    '(' expression ')' # ExprExpression
    |
    ifExpression # IfExprExpression
    |
    letExpression # LetExprExpression
    |
    lambdaExpression # LambdaExprExpression
    |
    procExpression # ProcExprExpression
    |
    setComprehension # SetComprehensionExprExpression
    |
    listComprehension # ListComprehensionExprExpression
    |
    mapComprehension # MapComprehensionExprExpression
    |
    typeAssertionExpression # TypeAssertionExprExpression
    |
    caseExpression # CaseExprExpression
    |
    callExpression # CallExprExpression
;

// Literals (CLR §6.1)
literalExpression:
    IntegerLiteral # IntegerLiteralExpression
    |
    FloatingPointLiteral # FloatingPointLiteralExpression
    |
    BooleanLiteral # BooleanLiteralExpression
    |
    CharacterLiteral # CharacterLiteralExpression
    |
    StringLiteral # StringLiteralExpression
    |
    NullLiteral # NullLiteralExpression
;

// Variable reference (CLR §6.2)
variableExpression:
    isOld='old'? variable
;

// Symbol Reference (CAL Specification Extension)
symbolReferenceExpression:
    variable '::' ID ('(' expressions? ')')?
;

// Conditionals (CLR §6.7)
ifExpression:
    'if' condition=expression 'then'
    then=expression
    (elseIf=elseIfExpression | 'else' elze=expression)
    ('end' | 'endif')
;

// Elsif (CAL Specification Extension)
elseIfExpression:
    'elsif' condition=expression 'then'
    then=expression
    (elseIf=elseIfExpression | 'else' elze=expression)
    ('end' | 'endif')
;

// Local Scope (CLR §6.8)
letExpression:
    'let' localVariables=blockVariableDeclarations ':' body=expression ('end' | 'endlet')
;

// Function closure (CLR §6.9.1)
lambdaExpression:
    isConst='const'? 'lambda' '(' formalParameters? ')' ('-->' type)?
    ('var' localVariables=blockVariableDeclarations)? ':'
    body=expression
    ('end' | 'endlambda')
;

// Procedure closure (CLR §6.9.2)
procExpression:
    'proc' '(' formalParameters? ')'
    ('var' localVariables=blockVariableDeclarations)?
    ('do' | 'begin')
    statements
    ('end' | 'endproc')
;

// Comprehensions w/ generators (CLR §6.10.2)
setComprehension:
    '{' (expressions (':' generators)?)? '}'
;

listComprehension:
    '[' (computations=expressions (':' generators)?  ('|' tail=expression )?)? ']'
;

mapComprehension:
    'map' '{' (mappings (':' generators)?)? '}'
;

mappings:
    mapping (',' mapping)*
;

mapping:
    expression '->' expression
;

// Type Assertion (CLR §6.11)
typeAssertionExpression:
    '(' expression ':' type ')'
;

// Case (CAL Specification Extension)
caseExpression:
    'case' expression 'of'
    alternativeExpression+
    ('end' | 'endcase')
;

alternativeExpression:
    pattern ('guard' expressions)? ':'
    expression
    'end'
;

// Function / Procedure Call (CAL Specification Extension)
callExpression:
    function=variableExpression '(' arguments=expressions? ')'
;

// ----------------------------------------------------------------------------
// -- LValue (CLR §7.1, as part of assignment definition)
// ----------------------------------------------------------------------------

lvalues:
    lvalue (',' lvalue)*
;

lvalue:
    variable ('.' field | '[' expression ']')*
;

variable:
    name=ID
;

field:
    name=ID
;

// ----------------------------------------------------------------------------
// -- Statements (CLR §7, but extended)
// ----------------------------------------------------------------------------

statements:
    statement*
;

statement:
        annotation*
        (
            assignmentStatement
            |
            callStatement
            |
            blockStatement
            |
            ifStatement
            |
            whileStatement
            |
            foreachStatement
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
assignmentStatement:
    lvalue ':=' value=expression ';'
;

// Procedure call (CLR §7.2)
callStatement:
    function=variableExpression '(' arguments=expressions? ')' ';'
;

// Statement block (CLR §7.3)
blockStatement:
    'begin'
    ('var' localVariables=blockVariableDeclarations 'do')?
    statements
    'end'
;

// If (CLR §7.4)
ifStatement:
    'if' condition=expression 'then'
    then=statements
    (elseIf=elseIfStatement | 'else' elze=statements)?
    ('end' | 'endif')
;

// Elsif (CAL Specification Extension)
elseIfStatement:
    'elsif' condition=expression 'then'
    then=statements
    (elseIf=elseIfStatement | 'else' elze=statements)?
    ('end' | 'endif')
;

// While (CLR §7.5)
whileStatement:
    'while' expression
    ('var' blockVariableDeclarations)?
    'do'
    statements
    ('end' | 'endwhile')
;

// Foreach (CLR §7.6)
foreachStatement:
    foreachGenerators
    ('var' localVariables=blockVariableDeclarations)?
    'do'
    body=statements
    ('end' | 'endforeach')
;

// Choose (CLR §7.7)
chooseStatement:
    chooseGenerators
    ('var' localVariables=blockVariableDeclarations)?
    'do'
    body=statements
    ('end' | 'endchoose')
;

// Case (CAL Specification Extension)
caseStatement:
    'case' expression 'of'
    alternativeStatement+
    ('end' | 'endcase')
;

alternativeStatement:
    pattern ('guard' expressions )? 'do' statements 'end'
;

// Read (CAL Specification Extension)
readStatement:
    ID '-->' lvalues ('repeat' expression)? ';'
;

// Write (CAL Specification Extension)
writeStatement:
    ID '<--' expressions ('repeat' expression)? ';'
;

// Not part of the official specification
actionSelectionStatement:
    qualifiedID ';'
;