package com.fillumina.buildercreator;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import org.netbeans.api.java.source.TreeMaker;

/**
 *
 * @author Francesco Illuminati <fillumina@gmail.com>
 */
class FluentSettersMaker {

    private final TreeMaker make;
    private final List<Tree> members;
    private final List<VariableElement> elements;
    private final String className;

    public FluentSettersMaker(TreeMaker make,
            List<Tree> members,
            List<VariableElement> elements,
            String className) {
        this.make = make;
        this.members = members;
        this.elements = elements;
        this.className = className;
    }

    int removeExistingFluentSetters(int index) {
        int counter = 0;
        for (Iterator<Tree> treeIt = members.iterator(); treeIt.hasNext();) {
            Tree member = treeIt.next();

            if (member.getKind().equals(Tree.Kind.METHOD)) {
                MethodTree mt = (MethodTree) member;
                for (Element element : elements) {
                    if (mt.getName().contentEquals(getSetterName(element.getSimpleName())) &&
                            mt.getParameters().size() == 1 &&
                            mt.getReturnType() != null &&
                            mt.getReturnType().getKind() == Tree.Kind.IDENTIFIER) {
                        treeIt.remove();
                        if (index > counter) {
                            index--;
                        }
                        break;
                    }
                }
            }
            counter++;
        }
        return index;
    }

    void addFluentSetters(int index) {
        Set<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC);
        List<AnnotationTree> annotations = new ArrayList<>();

        int position = index - 1;
        for (VariableElement element : elements) {
            VariableTree parameter =
                    make.Variable(make.Modifiers(Collections.<Modifier>singleton(Modifier.FINAL),
                            Collections.<AnnotationTree>emptyList()),
                            element.getSimpleName(),
                            make.Identifier(toStringWithoutPackages(element)),
                            null);

            ExpressionTree returnType = make.QualIdent(className);

            final String bodyText = createFluentSetterMethodBody(element);

            MethodTree method = make.Method(
                    make.Modifiers(modifiers, annotations),
                    getSetterName(element.getSimpleName()),
                    returnType,
                    Collections.<TypeParameterTree>emptyList(),
                    Collections.<VariableTree>singletonList(parameter),
                    Collections.<ExpressionTree>emptyList(),
                    bodyText,
                    null);

            position = Math.min(position + 1, members.size());
            members.add(position, method);
        }
    }
    
    private static CharSequence getSetterName(CharSequence fieldName) {
        return String.format("with%s%s",
                Character.toUpperCase(fieldName.charAt(0)),
                fieldName.subSequence(1, fieldName.length()));
    }

    private static String createFluentSetterMethodBody(Element element) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n")
          .append("this.").append(element.getSimpleName())
          .append(" = ")
          .append(element.getSimpleName()).append(";\n")
          .append("return this;\n")
          .append("}");
        return sb.toString();
    }

    void addFields() {
        for (VariableElement element : elements) {
            VariableTree field =
                    make.Variable(make.Modifiers(
                            EnumSet.of(Modifier.PRIVATE),
                            Collections.<AnnotationTree>emptyList()),
                            element.getSimpleName().toString(),
                            make.Identifier(toStringWithoutPackages(element)),
                            null);

            members.add(field);
        }
    }

    private static String toStringWithoutPackages(VariableElement element) {
        return PackageHelper.removePackagesFromGenericsType(
                element.asType().toString());
    }
}
