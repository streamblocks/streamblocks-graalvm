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

parser grammar CalParser;

options { tokenVocab=CalLexer; }

// ----------------------------------------------------------------------------
// -- Compilation Unit (Start Rule)
// ----------------------------------------------------------------------------

compilationUnit
    :   namespaceBody EOF
    |   namespaceDeclaration EOF
    |   unitDeclaration EOF
    ;

// ----------------------------------------------------------------------------
// -- Namespace Declaration (CAL Specification Extension)
// ----------------------------------------------------------------------------

namespaceDeclaration
    :   DOC_COMMENT* NAMESPACE qualifiedID COLON namespaceBody (END | ENDNAMESPACE)
    |   DOC_COMMENT* PACKAGE qualifiedID SEMICOLON namespaceBody
    ;

namespaceBody
    :   annotation*
        importDeclaration*
        (
            typeDefinition
            |
            globalVariableDeclaration
            |
            actorDeclaration
            |
            networkDeclaration
        )*
    ;

// ----------------------------------------------------------------------------
// -- Qualified ID (CLR §3.1)
// ----------------------------------------------------------------------------

qualifiedID
    :   ID (DOT ID)*
    ;

// ----------------------------------------------------------------------------
// -- Annotation (CAL Specification Extension)
// ----------------------------------------------------------------------------

annotation
    :   AT_SIGN qualifiedID (LPAREN (annotationParameter (COMMA annotationParameter)* )? RPAREN)?
    ;

annotationParameter
    :   ID EQ expression
    |   expression
    ;

// ----------------------------------------------------------------------------
// -- Unit Declaration (CAL Specification Extension)
// ----------------------------------------------------------------------------

unitDeclaration
    :   UNIT ID COLON (globalVariableDeclaration)* (END | ENDUNIT)
    ;

// ----------------------------------------------------------------------------
// -- Import Declaration (CLR §3.1)
// ----------------------------------------------------------------------------

importDeclaration
    :   (singleImport | groupImport) SEMICOLON
    ;

singleImport
    :   IMPORT importKind? qualifiedID (EQ ID)?
    ;

groupImport
    :   IMPORT importKind? ALL qualifiedID
    ;

importKind
    :   VAR
    |   TYPE
    |   ENTITY
    ;

// ----------------------------------------------------------------------------
// -- Network Declaration (NLR §2)
// ----------------------------------------------------------------------------

networkDeclaration
    :   DOC_COMMENT*
        NETWORK qualifiedID
        LPAREN formalParameters RPAREN
        ioSignature
        COLON
        importDeclaration*
        variableDeclarationSection?
        //networkDeclaration*
        entitySection?
        structureSection?
        (END | ENDNETWORK)
    ;

variableDeclarationSection
    :   VAR localVariableDeclaration*
    ;

// ----------------------------------------------------------------------------
// -- Entities (NLR §3)
// ----------------------------------------------------------------------------

entitySection
    :   ENTITIES entityDeclaration*
    ;

entityDeclaration
    :   /* EntityType? */ ID EQ entityExpression SEMICOLON
    ;

entityExpression
    :   entityInstanceExpression
    |   entityIfExpression
    |   entityListExpression
    ;

entityInstanceExpression
    :   ID LPAREN entityParameters? RPAREN attributeSection?
    ;

entityIfExpression
    :   IF expression THEN entityExpression ELSE entityExpression (END | ENDIF)
    ;

entityListExpression
    :   LSQUARE entityExpressions (COLON generators)? RSQUARE
    ;

entityExpressions
    :   entityExpression (COMMA entityExpression)*
    ;

entityParameters
    :   entityParameter (COMMA entityParameter)*
    ;

entityParameter
    :   ID EQ expression
    ;

attributeSection
    :   LCURLY attributeDeclaration* RCURLY
    ;

attributeDeclaration
    :   ID EQ expression SEMICOLON
    |   ID COLON type SEMICOLON
    ;

// ----------------------------------------------------------------------------
// -- Structures (NLR §4)
// ----------------------------------------------------------------------------

structureSection
    :   STRUCTURE structureStatement*
    ;

structureStatement
    :   structureConnectorStatement
    |   structureForeachStatement
    |   structureIfStatement
    ;

structureConnectorStatement
    :   connector LONG_SINGLE_ARROW_RIGHT connector attributeSection? SEMICOLON
    ;

structureForeachStatement
    :   FOREACH generator (COMMA FOREACH generator)* DO structureStatement* (END | ENDFOREACH)
    ;

structureIfStatement
    :   IF expression THEN structureStatement* (ELSE structureStatement*)? (END | ENDIF)
    ;

connector
    :   portReference
    |   entityReference DOT portReference
    ;

entityReference
    :   ID (LSQUARE expression RSQUARE)*
    ;

portReference
    :   ID (LSQUARE expression RSQUARE)*
    ;

// ----------------------------------------------------------------------------
// -- Actor Declaration (CLR §3)
// ----------------------------------------------------------------------------

actorDeclaration
    :   DOC_COMMENT*
        annotation*
        EXTERNAL?
        ACTOR ID
        //(LSQUARE typePars RSQUARE)?
        LPAREN formalParameters RPAREN
        ioSignature
        timeCause?
        (
            COLON
            (
                annotation*
                (
                    localVariableDeclaration
                    |
                    actionDefinition
                    |
                    initializeActionDefinition
                    |
                    priorityOrder
                    |
                    actionSchedule
                    |
                    processDescription
                )
            )*
            (END | ENDACTOR)
            |
            SEMICOLON
        )
    ;

ioSignature
    :   portDeclarations? LONG_DOUBLE_ARROW_RIGHT portDeclarations?
    ;

portDeclarations
    : portDeclaration (COMMA portDeclaration)*
    ;

portDeclaration
    :   annotation* MULTI? type? ID
    ;

timeCause
    : TIME type
    ;

processDescription
    :   (REPEAT | DO)
        statements
        END
    ;

// ----------------------------------------------------------------------------
// -- Action (CLR §8)
// ----------------------------------------------------------------------------

actionDefinition
    :   DOC_COMMENT*
        (actionTag COLON)?
        ACTION
        inputPatterns LONG_DOUBLE_ARROW_RIGHT outputExpressions
        actionGuards?
        actionVariableDeclarations?
        actionDelay?
        actionBody?
        (END | ENDACTION)
    ;

actionVariableDeclarations
    :   VAR blockVariableDeclarations
    ;

actionGuards
    :   GUARD expressions
    ;

actionDelay
    :   DELAY expression
    ;

actionBody
    :   DO statements
    ;

// ----------------------------------------------------------------------------
// -- Input Patterns (CLR §8.1, but extended)
// ----------------------------------------------------------------------------

inputPatterns
    :   (inputPattern (COMMA inputPattern)*)?
    ;

inputPattern
    :   (ID COLON)?
        LSQUARE patterns? RSQUARE
        (REPEAT expression)?
        channelSelector?
    ;

channelSelector
    :   AT expression
    |   AT_STAR expression
    |   AT_STAR? ANY
    |   AT_STAR? ALL
    ;

patterns
    :   pattern (COMMA pattern)*
    ;

pattern
    :   variable
    |   variable LPAREN subPatterns? RPAREN
    ;

subPatterns
    :   subPattern (COMMA subPattern)*
    ;

subPattern
    :   (ID COLON)? (DONT_CARE | patternExpression | pattern)
    ;

patternExpression
    :   literalExpression | LPAREN expression RPAREN
    ;

// ----------------------------------------------------------------------------
// -- Output Expressions (CLR §8.2)
// ----------------------------------------------------------------------------

outputExpressions
    :   (outputExpression (COMMA outputExpression)*)?
    ;

outputExpression
    :   (ID COLON)?
        LSQUARE expressions RSQUARE
        (REPEAT expression)?
        channelSelector?
    ;

// ----------------------------------------------------------------------------
// -- Initialization Action (CLR §8.5)
// ----------------------------------------------------------------------------

initializeActionDefinition
    :   DOC_COMMENT*
        (actionTag COLON)?
        INITIALIZE
        inputPatterns LONG_DOUBLE_ARROW_RIGHT outputExpressions
        actionGuards?
        actionVariableDeclarations?
        actionDelay?
        actionBody?
        (END | ENDACTION)
    ;

// ----------------------------------------------------------------------------
// -- Action Tags (CLR §9.1)
// ----------------------------------------------------------------------------

actionTags
    :   actionTag
    |   actionTag COMMA actionTags
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
    :   SCHEDULE FSM? ID COLON
        stateTransitions
        (END | ENDSCHEDULE)
    ;

stateTransitions
    : (stateTransition SEMICOLON)*
    ;

stateTransition
    :   ID
        LPAREN actionTags RPAREN LONG_SINGLE_ARROW_RIGHT ID
        (VERTICAL_BAR LPAREN actionTags RPAREN LONG_SINGLE_ARROW_RIGHT ID)*
    ;

scheduleRegExp                                 // RegExp schedules (CLR §9.2.2)
    :   SCHEDULE REGEXP regExp (END | ENDSCHEDULE)
    ;

regExp
    :   actionTag
    |   LPAREN regExp RPAREN
    |   LSQUARE regExp RSQUARE
    |   regExp STAR
    |   regExp regExp
    |   regExp VERTICAL_BAR regExp
    ;

// ----------------------------------------------------------------------------
// -- Priorities (CLR §9.3)
// ----------------------------------------------------------------------------

priorityOrder
    :   PRIORITY (priorityInequality SEMICOLON)* (END | ENDPRIORITY)
    ;

priorityInequality
    :   actionTag GT actionTag (GT actionTag)*
    ;

// ----------------------------------------------------------------------------
// -- Variable Declrations (CLR + CAL Specification Extnsion)
// ----------------------------------------------------------------------------

availability
    :   PUBLIC
    |   PRIVATE
    |   LOCAL
    ;

globalVariableDeclaration
    :   availability? EXTERNAL? (explicitVariableDeclaration SEMICOLON | functionVariableDeclaration | procedureVariableDeclaration)
    ;

localVariableDeclaration
    :   DOC_COMMENT* EXTERNAL? (explicitVariableDeclaration SEMICOLON | functionVariableDeclaration | procedureVariableDeclaration)
    ;

blockVariableDeclarations
    :   (blockVariableDeclaration (COMMA blockVariableDeclaration)*)?
    ;

blockVariableDeclaration
    :   (explicitVariableDeclaration | functionVariableDeclaration | procedureVariableDeclaration)
    ;

explicitVariableDeclaration       // Explicit variable declaration (CLR §5.1.1)
    :   annotation* MUTABLE? type? ID (LSQUARE expression RSQUARE)* ((EQ | COLON_EQ) expression)?
    ;

functionVariableDeclaration   // Function & procedure declarations (CLR §6.9.3)
    :   DOC_COMMENT*
        FUNCTION ID LPAREN formalParameters RPAREN (LONG_SINGLE_ARROW_RIGHT type)?
        ((VAR blockVariableDeclarations)? COLON expression)?
        (END | ENDFUNCTION)
    ;

procedureVariableDeclaration
    :   DOC_COMMENT*
        PROCEDURE ID LPAREN formalParameters RPAREN
        ((VAR blockVariableDeclarations)? (BEGIN | DO) statements)?
        (END | ENDPROCEDURE)
    ;

// ----------------------------------------------------------------------------
// -- Formal Parameters (CLR §6.9.1, but extended)
// ----------------------------------------------------------------------------

formalParameters
    :   (formalParameter (COMMA formalParameter)*)?
    ;

formalParameter
    :   explicitVariableDeclaration
    ;

// ----------------------------------------------------------------------------
// -- Type definitions (CAL Specification Extension)
// ----------------------------------------------------------------------------

typeDefinition
    :   TYPE ID (LPAREN formalParameters RPAREN)? COLON
        (tuple | (taggedTuple (VERTICAL_BAR taggedTuple )*))
        (END | ENDTYPE);

taggedTuple
    :   ID tuple
    ;

tuple
    :   (LPAREN (explicitVariableDeclaration (COMMA explicitVariableDeclaration)*)? RPAREN)?
    ;

// ----------------------------------------------------------------------------
// -- Type formats (CLR §4.2)
// ----------------------------------------------------------------------------

types
    :   type (COMMA type)*
    ;

type
    :   ID
    |   TYPE // ??
    |   ID LSQUARE typeParameters RSQUARE
    |   ID LPAREN typeAttributes? RPAREN
    |   LSQUARE types? LONG_SINGLE_ARROW_RIGHT type? LSQUARE
    ;

typeParameters
    :   typeParameter (COMMA typeParameter)*
    ;

typeParameter
    :   ID (LT type)?
    ;

typeAttributes
    :   typeAttribute (COMMA typeAttribute)*
    ;

typeAttribute
    :   (ID | TYPE /* ?? */) COLON type
    |   ID EQ expression
    ;

// ----------------------------------------------------------------------------
// -- Expressions (CLR §6, but extended)
// ----------------------------------------------------------------------------

expressions
    :   expression (COMMA expression)*
    ;

expression                                    // Priorities (CLR §Appendix C.1)
    :   <assoc=right> expression CARET expression
    |   expression LSQUARE expressions RSQUARE
    |   expression DOT field
    |   MINUS expression
    |   RNG expression
    |   DOM expression
    |   DASH expression
    |   NOT expression
    |   BIT_NOT expression
    |   expression DOT_DOT expression
    |   expression (STAR | SLASH | MODULO | DIV | MOD) expression
    |   expression (PLUS | MINUS) expression
    |   expression (SHIFT_LEFT | SHIFT_RIGHT) expression
    |   expression (LT | LTE | GT | GTE) expression
    |   expression (EQ | EQ_EQ | NOT_EQ) expression
    |   expression BIT_AND expression
    |   expression DOUBLE_COLON expression
    |   expression AND expression
    |   expression OR expression
    |   literalExpression
    |   variableExpression
    |   symbolReferenceExpression
    |   LPAREN expression RPAREN
    |   ifExpression
    |   letExpression
    |   lambdaExpression
    |   procExpression
    |   listComprehension
    |   setComprehension
    |   mapComprehension
    |   typeAssertionExpr
    |   caseExpression
    |   callExpression
    ;

literalExpression                                        // Literals (CLR §6.1)
    :   IntegerLiteral | FloatingPointLiteral | BooleanLiteral | CharacterLiteral | StringLiteral | NullLiteral
    ;

variableExpression                             // Variable reference (CLR §6.2)
    :   OLD? variable
    ;

symbolReferenceExpression     // Symbol Reference (CAL Specification Extension)
    :   variable DOUBLE_COLON ID (LPAREN expressions? RPAREN)?
    ;

ifExpression                                         // Conditionals (CLR §6.7)
    :   IF expression THEN expression (elseIfExpression | ELSE expression) (END | ENDIF)
    ;

elseIfExpression                         // Elsif (CAL Specification Extension)
    :   ELSIF expression THEN expression (elseIfExpression | ELSE expression)
    ;

letExpression                                         // Local Scope (CLR §6.8)
    :   LET blockVariableDeclarations COLON expression (END | ENDLET)
    ;

lambdaExpression                               // Function closure (CLR §6.9.1)
    :   CONST? LAMBDA LPAREN formalParameters RPAREN (LONG_SINGLE_ARROW_RIGHT type)?
        (VAR blockVariableDeclarations)? COLON expression
        (END | ENDLAMBDA)
    ;

procExpression                                // Procedure closure (CLR §6.9.2)
    :   PROC LPAREN formalParameters RPAREN (VAR blockVariableDeclarations)? (DO | BEGIN) statements (END | ENDPROC)
    ;

setComprehension                  // Comprehensions w/ generators (CLR §6.10.2)
    :   LCURLY (expressions (COLON generators)?)? RCURLY
    ;

listComprehension
    :   LSQUARE (expressions (COLON generators)? (VERTICAL_BAR expression)?)? RSQUARE
    ;

mapComprehension
    :   MAP LCURLY (mappings (COLON generators)?)? RCURLY
    ;

mappings
    :   mapping (COMMA mapping)*
    ;

mapping
    :   expression SINGLE_ARROW_RIGHT expression
    ;

generators
    :   generator (COMMA generator)*
    ;

generator
    :   FOR? type? ID (COMMA ID)? IN expressions
    ;

typeAssertionExpr                                 // Type Assertion (CLR §6.11)
    :   LPAREN expression DOUBLE_COLON type RPAREN
    ;

caseExpression                            // Case (CAL Specification Extension)
    :   CASE expression OF alternativeExpression+ (END | ENDCASE)
    ;

alternativeExpression
    :   pattern (GUARD expressions)? COLON expression END
    ;

callExpression                            // Call (CAL Specification Extension)
    :   variable LPAREN expressions? RPAREN
    ;

// ----------------------------------------------------------------------------
// -- LValue (CLR §7.1, as part of assignment definition)
// ----------------------------------------------------------------------------

lvalues
    :   lvalue (COMMA lvalue)*
    ;

lvalue
    :   variable (DOT field | LSQUARE expression RSQUARE)*
    ;

variable
    :   ID
    ;

field
    :   ID
    ;

// ----------------------------------------------------------------------------
// -- Statements (CLR §7, but extended)
// ----------------------------------------------------------------------------

statements
    :   statement*
    ;

statement
    :   assignmentStatement
    |   callStatement
    |   blockStatement
    |   ifStatement
    |   whileStatement
    |   foreachStatement
    |   chooseStatement
    |   caseStatement
    |   readStatement
    |   writeStatement
    |   actionSelectionStatement
    ;

assignmentStatement                                    // Assignment (CLR §7.1)
    :   lvalue COLON_EQ expression SEMICOLON
    ;

callStatement                                      // Procedure call (CLR §7.2)
    :   variable LPAREN expressions? RPAREN SEMICOLON
    ;

blockStatement                                    // Statement block (CLR §7.3)
    :   BEGIN (VAR blockVariableDeclarations DO)? statements END
    ;

ifStatement                                                    // If (CLR §7.4)
    :   IF expression THEN statements (elseIfStatement | ELSE statements)? (END | ENDIF)
    ;

elseIfStatement                          // Elsif (CAL Specification Extension)
    :   ELSIF expression THEN statements (elseIfStatement | ELSE statements)?
    ;

whileStatement                                              // While (CLR §7.5)
    :   WHILE expression (VAR blockVariableDeclarations)? DO statements (END | ENDWHILE)
    ;

foreachStatement                                          // Foreach (CLR §7.6)
    :   foreachGenerators (VAR blockVariableDeclarations)? DO statements (END | ENDFOREACH)
    ;

foreachGenerators
    :   foreachGenerator (COMMA foreachGenerator)*
    ;

foreachGenerator
    :   FOREACH type? ID (COMMA ID)* IN expressions
    ;

chooseStatement                                            // Choose (CLR §7.7)
    :   chooseGenerators (VAR blockVariableDeclarations)? DO statements (END | ENDCHOOSE)
    ;

chooseGenerators
    :   chooseGenerator (COMMA chooseGenerator)*
    ;

chooseGenerator
    :   CHOOSE type? ID (COMMA ID)* IN expressions
    ;

caseStatement                             // Case (CAL Specification Extension)
    :   CASE expression OF alternativeStatement+ (END | ENDCASE)
    ;

alternativeStatement
    :   pattern (GUARD expressions )? DO statements END
    ;

readStatement                             // Read (CAL Specification Extension)
    :   ID LONG_SINGLE_ARROW_RIGHT lvalues (REPEAT expression)? SEMICOLON
    ;

writeStatement                           // Write (CAL Specification Extension)
    :   ID LONG_SINGLE_ARROW_LEFT expressions (REPEAT expression)? SEMICOLON
    ;

actionSelectionStatement              // Not part of the official specification
    :   qualifiedID SEMICOLON
    ;