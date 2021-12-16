package com.scindapsus.apt;

import com.google.auto.service.AutoService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.util.StringUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 编译期根据标记{@link FastJPA}注解动态生成类
 * <p>
 * 使用方式:<p>
 * 如不使用google的autoService{@link AutoService},
 * 则需要自己创建{@code META-INF/services/javax.annotation.processing.Processor}文件,
 * 内容为自定义继承{@link AbstractProcessor}的processor的全限定名<p>
 * 引入jar后执行{@code mvn clean compile}<p>
 * 生成文件详见{@code /target/generated-sources}
 *
 * @author wyh
 * @since 1.0
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes({"com.scindapsus.apt.FastJPA"})
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class FastJPAProcessor extends AbstractProcessor {

    private static final String DOT = ".";
    private static final String IMPORT = "import ";
    private static final String SEMICOLON = ";";


    private Types typeUtils;
    private Elements elementUtils;
    private Filer filer;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        for (Element element : roundEnv.getElementsAnnotatedWith(FastJPA.class)) {

            String targetClassName = element.getSimpleName().toString();
            String className = targetClassName + "Repository";

            TypeElement typeElement = (TypeElement) element;
            FastJPA annotation = typeElement.getAnnotation(FastJPA.class);
            String targetQualifiedName = typeElement.getQualifiedName().toString();

            String packageName = annotation.basePackage();
            if (!StringUtils.hasText(packageName)) {
                int lastIndex = targetQualifiedName.lastIndexOf(DOT);
                packageName = targetQualifiedName.substring(0, lastIndex);
            }

            Element idEle = null;
            List<? extends Element> enclosedElements = typeElement.getEnclosedElements();
            List<? extends Element> idFields = enclosedElements.stream()
                    .filter(x -> x instanceof VariableElement)
                    .filter(x -> x.getAnnotation(Id.class) != null).collect(Collectors.toList());
            IdClass idAnnotation = typeElement.getAnnotation(IdClass.class);
            if (idAnnotation != null && !idFields.isEmpty()) {
                try {
                    //apt中获取注解的class会报错,捕捉错误利用错误来实现获取
                    idAnnotation.value();
                } catch (MirroredTypeException e) {
                    idEle = typeUtils.asElement(e.getTypeMirror());
                }
            } else if (idFields.size() == 1) {
                idEle = idFields.get(0);
            } else {
                error(typeElement, "Must contain an id field.");
                return true;
            }
            //将单个id情况时的变量元素转为类元素
            TypeElement id = null;
            try {
                id = (TypeElement) typeUtils.asElement(Objects.requireNonNull(idEle).asType());
            } catch (Exception e) {
                error(typeElement, "Get id element error.");
                return true;
            }

            JavaFileObject classFile;
            try {
                classFile = filer.createSourceFile(packageName + DOT + className);
            } catch (IOException e) {
                error(element, "Create source file failed, reason %s.", e.getMessage());
                return true;
            }
            try (PrintWriter writer = new PrintWriter(classFile.openWriter())) {
                writer.println("package " + packageName + SEMICOLON);
                writer.println();
                writer.println();
                writer.println(IMPORT + targetQualifiedName + SEMICOLON);
                writer.println(IMPORT + JpaRepository.class.getCanonicalName() + SEMICOLON);
                writer.println(IMPORT + id.getQualifiedName() + SEMICOLON);
                writer.println();
                writer.println();
                writer.println("//Generated By Scindapsus-APT");
                writer.println("public interface " + className + " extends JpaRepository<" + targetClassName + "," + id.getSimpleName() + "> {");
                writer.println();
                writer.println("}");
            } catch (Exception e) {
                error(element, "Write file content failed, reason %s.", e.getMessage());
                return true;
            }
        }
        return true;
    }

    /**
     * 打印错误信息
     *
     * @param e    元素类型
     * @param msg  错误消息
     * @param args 参数
     */
    private void error(Element e, String msg, Object... args) {
        messager.printMessage(
                Diagnostic.Kind.ERROR,
                String.format(msg, args),
                e);
    }
}