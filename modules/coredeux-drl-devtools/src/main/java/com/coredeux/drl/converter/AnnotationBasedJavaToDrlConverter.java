package com.coredeux.drl.converter;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AnnotationBasedJavaToDrlConverter implements JavaToDrlConverter {

    private static final String IMPORT_JAVA_LANG = "import java.lang.*;";
    private static final String ANNOTATION_PACKAGE = "com.coredeux.drl.converter.annotations";

    @Override
    public ConvertedDrl convert(String source) {
        CompilationUnit compilationUnit = parse(source);
        ClassOrInterfaceDeclaration type = findDrlDefinition(compilationUnit);
        String ruleId = annotationValue(type, "DrlDefinition")
                .orElseThrow(() -> error(type, "@DrlDefinition requires a value"));
        if (ruleId.isBlank()) {
            throw error(type, "@DrlDefinition value must not be blank");
        }

        List<String> imports = imports(compilationUnit);
        List<String> globals = globals(type);
        List<String> rules = rules(type);
        if (rules.isEmpty()) {
            throw error(type, "At least one @DrlRule method is required");
        }

        StringBuilder drl = new StringBuilder();
        appendLine(drl, IMPORT_JAVA_LANG);
        for (String importLine : imports) {
            appendLine(drl, importLine);
        }
        appendLine(drl, "");
        for (String global : globals) {
            appendLine(drl, global);
        }
        appendLine(drl, "");
        for (String rule : rules) {
            appendLine(drl, rule);
            appendLine(drl, "");
        }

        return new ConvertedDrl(ruleId.trim(), drl.toString());
    }

    private CompilationUnit parse(String source) {
        try {
            return StaticJavaParser.parse(source);
        } catch (RuntimeException exception) {
            throw new DrlConversionException("Unable to parse Java DRL source", exception);
        }
    }

    private ClassOrInterfaceDeclaration findDrlDefinition(CompilationUnit compilationUnit) {
        List<ClassOrInterfaceDeclaration> types = compilationUnit.findAll(ClassOrInterfaceDeclaration.class).stream()
                .filter(type -> hasAnnotation(type.getAnnotations(), "DrlDefinition"))
                .toList();
        if (types.isEmpty()) {
            throw new DrlConversionException("Missing @DrlDefinition on source class");
        }
        if (types.size() > 1) {
            throw new DrlConversionException("Only one @DrlDefinition class is supported per source file");
        }
        return types.get(0);
    }

    private List<String> imports(CompilationUnit compilationUnit) {
        return compilationUnit.getImports().stream()
                .filter(importDeclaration -> !importDeclaration.getNameAsString().startsWith(ANNOTATION_PACKAGE))
                .map(Object::toString)
                .map(String::trim)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private List<String> globals(ClassOrInterfaceDeclaration type) {
        List<String> globals = new ArrayList<>();
        for (FieldDeclaration field : type.getFields()) {
            if (!hasAnnotation(field.getAnnotations(), "DrlGlobal")) {
                continue;
            }
            for (VariableDeclarator variable : field.getVariables()) {
                globals.add("global " + variable.getType() + " " + variable.getNameAsString() + ";");
            }
        }
        return globals;
    }

    private List<String> rules(ClassOrInterfaceDeclaration type) {
        List<String> rules = new ArrayList<>();
        for (MethodDeclaration method : type.getMethods()) {
            Optional<AnnotationExpr> ruleAnnotation = annotation(method.getAnnotations(), "DrlRule");
            if (ruleAnnotation.isEmpty()) {
                continue;
            }
            String ruleName = ruleName(ruleAnnotation.get())
                    .orElseThrow(() -> error(method, "@DrlRule requires value or name"));
            String when = annotationStringValue(ruleAnnotation.get(), "when")
                    .orElseThrow(() -> error(method, "@DrlRule requires when"));
            BlockStmt body = method.getBody()
                    .orElseThrow(() -> error(method, "@DrlRule method requires a method body"));
            if (ruleName.isBlank()) {
                throw error(method, "@DrlRule name must not be blank");
            }
            if (when.isBlank()) {
                throw error(method, "@DrlRule when must not be blank");
            }
            rules.add(renderRule(ruleName, when, body));
        }
        return rules;
    }

    private String renderRule(String ruleName, String when, BlockStmt body) {
        StringBuilder rule = new StringBuilder();
        appendLine(rule, "rule \"" + ruleName.trim() + "\"");
        appendLine(rule, "when");
        appendLine(rule, "   " + when.trim());
        appendLine(rule, "then");
        appendConsequence(rule, body);
        appendLine(rule, "end");
        return rule.toString();
    }

    private void appendConsequence(StringBuilder rule, BlockStmt body) {
        String bodyText = body.toString();
        int openBrace = bodyText.indexOf('{');
        int closeBrace = bodyText.lastIndexOf('}');
        if (openBrace < 0 || closeBrace < 0 || closeBrace <= openBrace) {
            return;
        }
        String innerBody = bodyText.substring(openBrace + 1, closeBrace).strip();
        if (innerBody.isBlank()) {
            return;
        }
        for (String line : innerBody.split("\\R")) {
            appendLine(rule, "   " + line);
        }
    }

    private Optional<String> annotationValue(ClassOrInterfaceDeclaration type, String annotationName) {
        return annotationValue(type.getAnnotations(), annotationName);
    }

    private Optional<String> annotationValue(List<AnnotationExpr> annotations, String annotationName) {
        return annotation(annotations, annotationName).flatMap(this::annotationStringValue);
    }

    private Optional<AnnotationExpr> annotation(List<AnnotationExpr> annotations, String annotationName) {
        return annotations.stream()
                .filter(annotation -> annotation.getNameAsString().equals(annotationName))
                .findFirst();
    }

    private Optional<String> annotationStringValue(AnnotationExpr annotation) {
        if (annotation.isSingleMemberAnnotationExpr()
                && annotation.asSingleMemberAnnotationExpr().getMemberValue().isStringLiteralExpr()) {
            return Optional.of(annotation.asSingleMemberAnnotationExpr().getMemberValue().asStringLiteralExpr()
                    .asString());
        }
        if (annotation.isNormalAnnotationExpr()) {
            return annotationStringValue(annotation, "value");
        }
        return Optional.empty();
    }

    private Optional<String> annotationStringValue(AnnotationExpr annotation, String attributeName) {
        if (!annotation.isNormalAnnotationExpr()) {
            return Optional.empty();
        }
        return annotation.asNormalAnnotationExpr().getPairs().stream()
                .filter(pair -> pair.getNameAsString().equals(attributeName))
                .map(MemberValuePair::getValue)
                .filter(value -> value.isStringLiteralExpr())
                .map(value -> value.asStringLiteralExpr())
                .map(StringLiteralExpr::asString)
                .findFirst();
    }

    private Optional<String> ruleName(AnnotationExpr annotation) {
        Optional<String> explicitName = annotationStringValue(annotation, "name");
        if (explicitName.isPresent() && !explicitName.get().isBlank()) {
            return explicitName;
        }
        return annotationStringValue(annotation);
    }

    private boolean hasAnnotation(List<AnnotationExpr> annotations, String annotationName) {
        return annotations.stream().anyMatch(annotation -> annotation.getNameAsString().equals(annotationName));
    }

    private DrlConversionException error(com.github.javaparser.ast.Node node, String message) {
        String location = node.getRange()
                .map(range -> " at line " + range.begin.line)
                .orElse("");
        return new DrlConversionException(message + location);
    }

    private void appendLine(StringBuilder builder, String line) {
        builder.append(line).append(System.lineSeparator());
    }
}
