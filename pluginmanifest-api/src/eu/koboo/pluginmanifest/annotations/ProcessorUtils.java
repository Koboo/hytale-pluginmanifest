package eu.koboo.pluginmanifest.annotations;

import lombok.experimental.UtilityClass;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@UtilityClass
public class ProcessorUtils {

    public TypeElement getTypeElement(ProcessingEnvironment processingEnv, Element element) {
        if (!(element instanceof TypeElement typeElement)) {
            error(processingEnv, "Element is not a class", element);
            return null;
        }

        if (!(typeElement.getEnclosingElement() instanceof PackageElement)) {
            error(processingEnv, "Element class is not a top-level class", typeElement);
            return null;
        }

        if (typeElement.getModifiers().contains(Modifier.STATIC)) {
            error(processingEnv, "Element class cannot be static nested", typeElement);
            return null;
        }

        if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
            error(processingEnv, "Element class cannot be abstract", typeElement);
            return null;
        }
        return typeElement;
    }

    public void error(ProcessingEnvironment processingEnv, String message) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message);
    }

    public void error(ProcessingEnvironment processingEnv, String message, Element element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}