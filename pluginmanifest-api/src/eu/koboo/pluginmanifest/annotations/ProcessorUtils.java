package eu.koboo.pluginmanifest.annotations;

import lombok.experimental.UtilityClass;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;

@UtilityClass
public class ProcessorUtils {

    public static final String COMPONENT_RESOURCE_DIRECTORY = "components/";

    public TypeElement getTypeElement(ProcessingEnvironment processingEnv, Element element) {
        if (!(element instanceof TypeElement typeElement)) {
            error(processingEnv, "element is not a class", element);
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

    public void writeResourceFile(ProcessingEnvironment processingEnv, String content, String file) {
        try {
            FileObject fileObject = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", file);
            try (Writer writer = fileObject.openWriter()) {
                writer.write(content);
                writer.flush();
            }
            // try with resources will close the Writer since it implements Closeable
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}