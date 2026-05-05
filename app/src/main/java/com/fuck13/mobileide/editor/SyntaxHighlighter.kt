package com.fuck13.mobileide.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.fuck13.mobileide.data.model.FileType
import com.fuck13.mobileide.ui.theme.*

data class SyntaxToken(
    val type: TokenType,
    val start: Int,
    val end: Int,
    val text: String
)

enum class TokenType {
    KEYWORD, STRING, NUMBER, COMMENT, FUNCTION, 
    VARIABLE, OPERATOR, TYPE, CLASS, METHOD, 
    PROPERTY, ANNOTATION, BRACKET, PUNCTUATION, WHITESPACE, UNKNOWN
}

interface LanguageSyntax {
    val languageName: String
    val keywords: Set<String>
    val types: Set<String>
    val operators: Set<String>
    val commentLine: String
    val commentBlockStart: String?
    val commentBlockEnd: String?
    val stringDelimiters: List<Pair<String, String>>
    
    fun tokenize(code: String): List<SyntaxToken>
}

class KotlinSyntax : LanguageSyntax {
    override val languageName = "Kotlin"
    override val keywords = setOf(
        "package", "import", "class", "interface", "object", "fun", "val", "var",
        "if", "else", "when", "for", "while", "do", "try", "catch", "finally",
        "throw", "return", "break", "continue", "is", "as", "in", "out",
        "override", "abstract", "final", "open", "private", "protected", "public",
        "internal", "companion", "data", "enum", "sealed", "annotation", "inner",
        "tailrec", "operator", "infix", "inline", "noinline", "crossinline",
        "reified", "suspend", "by", "lateinit", "const", "vararg", "where",
        "true", "false", "null", "this", "super", "it", "constructor", "init"
    )
    override val types = setOf(
        "Int", "Long", "Short", "Byte", "Float", "Double", "Boolean", "Char",
        "String", "Unit", "Nothing", "Any", "List", "Set", "Map", "MutableList",
        "MutableSet", "MutableMap", "Array", "ByteArray", "IntArray", "LongArray",
        "Pair", "Triple", "Result", "Sequence", "Iterator", "Collection"
    )
    override val operators = setOf(
        "+", "-", "*", "/", "%", "=", "+=", "-=", "*=", "/=", "++", "--",
        "==", "!=", "<", ">", "<=", ">=", "&&", "||", "!", "?", "?:",
        "->", "..", "::", "..<", "in", "!in", "is", "!is", "as?"
    )
    override val commentLine = "//"
    override val commentBlockStart = "/*"
    override val commentBlockEnd = "*/"
    override val stringDelimiters = listOf(
        "\"" to "\"",
        "'" to "'",
        "\"\"\"" to "\"\"\""
    )
    
    override fun tokenize(code: String): List<SyntaxToken> {
        val tokens = mutableListOf<SyntaxToken>()
        var i = 0
        
        while (i < code.length) {
            // Skip whitespace
            if (code[i].isWhitespace()) {
                val start = i
                while (i < code.length && code[i].isWhitespace()) i++
                tokens.add(SyntaxToken(TokenType.WHITESPACE, start, i, code.substring(start, i)))
                continue
            }
            
            // Block comment
            if (code.startsWith("/*", i)) {
                val start = i
                i += 2
                while (i < code.length && !code.startsWith("*/", i)) i++
                if (i < code.length) i += 2
                tokens.add(SyntaxToken(TokenType.COMMENT, start, i, code.substring(start, i)))
                continue
            }
            
            // Line comment
            if (code.startsWith("//", i)) {
                val start = i
                while (i < code.length && code[i] != '\n') i++
                tokens.add(SyntaxToken(TokenType.COMMENT, start, i, code.substring(start, i)))
                continue
            }
            
            // Triple-quoted string
            if (code.startsWith("\"\"\"", i)) {
                val start = i
                i += 3
                while (i < code.length && !code.startsWith("\"\"\"", i)) i++
                if (i < code.length) i += 3
                tokens.add(SyntaxToken(TokenType.STRING, start, i, code.substring(start, i)))
                continue
            }
            
            // String literal
            if (code[i] == '"') {
                val start = i
                i++
                while (i < code.length && code[i] != '"') {
                    if (code[i] == '\\' && i + 1 < code.length) i++
                    i++
                }
                if (i < code.length) i++
                tokens.add(SyntaxToken(TokenType.STRING, start, i, code.substring(start, i)))
                continue
            }
            
            // Character literal
            if (code[i] == '\'') {
                val start = i
                i++
                while (i < code.length && code[i] != '\'') {
                    if (code[i] == '\\' && i + 1 < code.length) i++
                    i++
                }
                if (i < code.length) i++
                tokens.add(SyntaxToken(TokenType.STRING, start, i, code.substring(start, i)))
                continue
            }
            
            // Number
            if (code[i].isDigit() || (code[i] == '-' && i + 1 < code.length && code[i + 1].isDigit())) {
                val start = i
                if (code[i] == '-') i++
                while (i < code.length && (code[i].isDigit() || code[i] == '.' || code[i] in "eExXbBoOlLfF")) i++
                tokens.add(SyntaxToken(TokenType.NUMBER, start, i, code.substring(start, i)))
                continue
            }
            
            // Annotation
            if (code[i] == '@') {
                val start = i
                i++
                while (i < code.length && (code[i].isLetterOrDigit() || code[i] == '_')) i++
                tokens.add(SyntaxToken(TokenType.ANNOTATION, start, i, code.substring(start, i)))
                continue
            }
            
            // Identifier or keyword
            if (code[i].isLetter() || code[i] == '_') {
                val start = i
                while (i < code.length && (code[i].isLetterOrDigit() || code[i] == '_')) i++
                val text = code.substring(start, i)
                
                val tokenType = when {
                    keywords.contains(text) -> TokenType.KEYWORD
                    types.contains(text) -> TokenType.TYPE
                    else -> TokenType.VARIABLE
                }
                tokens.add(SyntaxToken(tokenType, start, i, text))
                continue
            }
            
            // Function call detection (identifier followed by '(')
            // Brackets and punctuation
            when (code[i]) {
                '(', ')', '[', ']', '{', '}' -> {
                    tokens.add(SyntaxToken(TokenType.BRACKET, i, i + 1, code[i].toString()))
                    i++
                }
                '.', ',', ':', ';', '\\' -> {
                    tokens.add(SyntaxToken(TokenType.PUNCTUATION, i, i + 1, code[i].toString()))
                    i++
                }
                in operators -> {
                    val start = i
                    var opText = code[i].toString()
                    // Try to match longer operators
                    for (len in 3 downTo 2) {
                        if (i + len <= code.length) {
                            val candidate = code.substring(i, i + len)
                            if (operators.contains(candidate)) {
                                opText = candidate
                                break
                            }
                        }
                    }
                    i += opText.length
                    tokens.add(SyntaxToken(TokenType.OPERATOR, start, i, opText))
                }
                else -> {
                    tokens.add(SyntaxToken(TokenType.UNKNOWN, i, i + 1, code[i].toString()))
                    i++
                }
            }
        }
        
        return tokens
    }
}

class JavaSyntax : LanguageSyntax {
    override val languageName = "Java"
    override val keywords = setOf(
        "package", "import", "class", "interface", "enum", "extends", "implements",
        "public", "private", "protected", "static", "final", "abstract", "native",
        "synchronized", "volatile", "transient", "strictfp", "void", "int", "long",
        "short", "byte", "float", "double", "boolean", "char", "new", "return",
        "if", "else", "switch", "case", "default", "break", "continue", "for",
        "while", "do", "try", "catch", "finally", "throw", "throws", "this",
        "super", "instanceof", "true", "false", "null", "const", "goto"
    )
    override val types = setOf(
        "String", "Integer", "Long", "Short", "Byte", "Float", "Double",
        "Boolean", "Character", "Object", "Class", "System", "Exception",
        "Runnable", "Thread", "List", "ArrayList", "Map", "HashMap", "Set", "HashSet"
    )
    override val operators = setOf(
        "+", "-", "*", "/", "%", "=", "+=", "-=", "*=", "/=", "%=", "++", "--",
        "==", "!=", "<", ">", "<=", ">=", "&&", "||", "!", "?", ":",
        "->", "::", "instanceof"
    )
    override val commentLine = "//"
    override val commentBlockStart = "/*"
    override val commentBlockEnd = "*/"
    override val stringDelimiters = listOf("\"" to "\"", "'" to "'")
    
    override fun tokenize(code: String): List<SyntaxToken> {
        // Similar to Kotlin but without Kotlin-specific syntax
        return KotlinSyntax().tokenize(code)
    }
}

class PythonSyntax : LanguageSyntax {
    override val languageName = "Python"
    override val keywords = setOf(
        "False", "None", "True", "and", "as", "assert", "async", "await",
        "break", "class", "continue", "def", "del", "elif", "else", "except",
        "finally", "for", "from", "global", "if", "import", "in", "is", "lambda",
        "nonlocal", "not", "or", "pass", "raise", "return", "try", "while",
        "with", "yield"
    )
    override val types = setOf(
        "int", "float", "str", "bool", "list", "dict", "set", "tuple",
        "bytes", "bytearray", "range", "type", "NoneType", "object",
        "Exception", "BaseException", "callable", "iter", "len", "range"
    )
    override val operators = setOf(
        "+", "-", "*", "/", "//", "%", "**", "=", "+=", "-=", "*=", "/=",
        "//=", "%=", "**=", "&=", "|=", "^=", ">>=", "<<=",
        "==", "!=", "<", ">", "<=", ">=", "and", "or", "not", "in", "is",
        "&", "|", "^", "~", "<<", ">>", "->", ":="
    )
    override val commentLine = "#"
    override val commentBlockStart = null
    override val commentBlockEnd = null
    override val stringDelimiters = listOf(
        "\"" to "\"", "'" to "'", "\"\"\"" to "\"\"\"", "'''" to "'''"
    )
    
    override fun tokenize(code: String): List<SyntaxToken> {
        val tokens = mutableListOf<SyntaxToken>()
        var i = 0
        
        while (i < code.length) {
            // Skip whitespace
            if (code[i].isWhitespace()) {
                val start = i
                while (i < code.length && code[i].isWhitespace()) i++
                tokens.add(SyntaxToken(TokenType.WHITESPACE, start, i, code.substring(start, i)))
                continue
            }
            
            // Comment
            if (code[i] == '#') {
                val start = i
                while (i < code.length && code[i] != '\n') i++
                tokens.add(SyntaxToken(TokenType.COMMENT, start, i, code.substring(start, i)))
                continue
            }
            
            // Triple-quoted strings
            if (code.startsWith("\"\"\"", i) || code.startsWith("'''", i)) {
                val quote = code.substring(i, i + 3)
                val start = i
                i += 3
                while (i < code.length - 2 && code.substring(i, i + 3) != quote) i++
                if (i < code.length) i += 3
                tokens.add(SyntaxToken(TokenType.STRING, start, i, code.substring(start, i)))
                continue
            }
            
            // Strings
            if (code[i] == '"' || code[i] == '\'') {
                val quote = code[i]
                val start = i
                i++
                while (i < code.length && code[i] != quote) {
                    if (code[i] == '\\' && i + 1 < code.length) i++
                    i++
                }
                if (i < code.length) i++
                tokens.add(SyntaxToken(TokenType.STRING, start, i, code.substring(start, i)))
                continue
            }
            
            // Number
            if (code[i].isDigit() || (code[i] == '-' && i + 1 < code.length && code[i + 1].isDigit())) {
                val start = i
                if (code[i] == '-') i++
                while (i < code.length && (code[i].isDigit() || code[i] == '.' || code[i] in "eExXbBoOjJ")) i++
                tokens.add(SyntaxToken(TokenType.NUMBER, start, i, code.substring(start, i)))
                continue
            }
            
            // Decorator
            if (code[i] == '@') {
                val start = i
                i++
                while (i < code.length && (code[i].isLetterOrDigit() || code[i] == '_')) i++
                tokens.add(SyntaxToken(TokenType.ANNOTATION, start, i, code.substring(start, i)))
                continue
            }
            
            // Identifier or keyword
            if (code[i].isLetter() || code[i] == '_') {
                val start = i
                while (i < code.length && (code[i].isLetterOrDigit() || code[i] == '_')) i++
                val text = code.substring(start, i)
                
                val tokenType = when {
                    keywords.contains(text) -> TokenType.KEYWORD
                    types.contains(text) -> TokenType.TYPE
                    else -> TokenType.VARIABLE
                }
                tokens.add(SyntaxToken(tokenType, start, i, text))
                continue
            }
            
            // Operators
            val remaining = code.substring(i)
            val matchedOp = operators.find { remaining.startsWith(it) }
            if (matchedOp != null) {
                tokens.add(SyntaxToken(TokenType.OPERATOR, i, i + matchedOp.length, matchedOp))
                i += matchedOp.length
                continue
            }
            
            // Brackets
            when (code[i]) {
                '(', ')', '[', ']', '{', '}' -> {
                    tokens.add(SyntaxToken(TokenType.BRACKET, i, i + 1, code[i].toString()))
                    i++
                }
                '.', ',', ':', ';' -> {
                    tokens.add(SyntaxToken(TokenType.PUNCTUATION, i, i + 1, code[i].toString()))
                    i++
                }
                else -> {
                    tokens.add(SyntaxToken(TokenType.UNKNOWN, i, i + 1, code[i].toString()))
                    i++
                }
            }
        }
        
        return tokens
    }
}

class JavaScriptSyntax : LanguageSyntax {
    override val languageName = "JavaScript"
    override val keywords = setOf(
        "break", "case", "catch", "continue", "debugger", "default", "delete",
        "do", "else", "finally", "for", "function", "if", "in", "instanceof",
        "new", "return", "switch", "this", "throw", "try", "typeof", "var",
        "void", "while", "with", "class", "const", "enum", "export", "extends",
        "import", "super", "implements", "interface", "let", "package", "private",
        "protected", "public", "static", "yield", "await", "async", "null",
        "true", "false", "undefined", "NaN", "Infinity"
    )
    override val types = setOf(
        "Object", "Array", "String", "Number", "Boolean", "Symbol", "Function",
        "Promise", "Map", "Set", "WeakMap", "WeakSet", "Date", "RegExp",
        "Error", "TypeError", "ReferenceError", "JSON", "Math", "console"
    )
    override val operators = setOf(
        "+", "-", "*", "/", "%", "=", "+=", "-=", "*=", "/=", "%=",
        "++", "--", "==", "===", "!=", "!==", "<", ">", "<=", ">=",
        "&&", "||", "!", "?", ":", "=>", "...", "?.", "??", "?.",
        "&", "|", "^", "~", "<<", ">>", ">>>"
    )
    override val commentLine = "//"
    override val commentBlockStart = "/*"
    override val commentBlockEnd = "*/"
    override val stringDelimiters = listOf(
        "\"" to "\"", "'" to "'", "`" to "`"
    )
    
    override fun tokenize(code: String): List<SyntaxToken> {
        // Similar structure to Kotlin tokenizer
        return KotlinSyntax().tokenize(code)
    }
}

object SyntaxHighlighter {
    private val languageSyntaxMap = mapOf<FileType, LanguageSyntax>(
        FileType.KOTLIN to KotlinSyntax(),
        FileType.JAVA to JavaSyntax(),
        FileType.PYTHON to PythonSyntax(),
        FileType.JAVASCRIPT to JavaScriptSyntax(),
        FileType.TYPESCRIPT to JavaScriptSyntax()
    )
    
    private val tokenColors = mapOf(
        TokenType.KEYWORD to SyntaxKeyword,
        TokenType.STRING to SyntaxString,
        TokenType.NUMBER to SyntaxNumber,
        TokenType.COMMENT to SyntaxComment,
        TokenType.FUNCTION to SyntaxFunction,
        TokenType.VARIABLE to SyntaxVariable,
        TokenType.OPERATOR to SyntaxOperator,
        TokenType.TYPE to SyntaxType,
        TokenType.CLASS to SyntaxClass,
        TokenType.METHOD to SyntaxMethod,
        TokenType.PROPERTY to SyntaxProperty,
        TokenType.ANNOTATION to SyntaxFunction,
        TokenType.BRACKET to SyntaxVariable,
        TokenType.PUNCTUATION to SyntaxComment,
        TokenType.UNKNOWN to Color.White
    )
    
    fun highlight(code: String, fileType: FileType): AnnotatedString {
        val syntax = languageSyntaxMap[fileType]
        
        if (syntax == null) {
            // No syntax highlighting for unsupported types
            return AnnotatedString(code)
        }
        
        val tokens = syntax.tokenize(code)
        
        return buildAnnotatedString {
            for (token in tokens) {
                val color = tokenColors[token.type] ?: Color.White
                withStyle(SpanStyle(color = color)) {
                    append(token.text)
                }
            }
        }
    }
    
    fun tokenize(code: String, fileType: FileType): List<SyntaxToken> {
        val syntax = languageSyntaxMap[fileType] ?: return emptyList()
        return syntax.tokenize(code)
    }
}
