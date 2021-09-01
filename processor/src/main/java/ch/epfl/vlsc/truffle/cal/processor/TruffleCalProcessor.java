package ch.epfl.vlsc.truffle.cal.processor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

public abstract class TruffleCalProcessor extends AbstractProcessor {
    ProcessingEnvironment getProcessingEnvironment() {
        return processingEnv;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    boolean isSameType(TypeMirror type1, TypeMirror type2) {
        return processingEnv.getTypeUtils().isSameType(type1, type2);
    }

    void error(String message, Element element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, message, element);
    }
}
