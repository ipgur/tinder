/*
 * Copyright 2018 Raffaele Ragni.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tinder.processors;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.Writer;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import static javax.tools.Diagnostic.Kind.ERROR;
import javax.tools.JavaFileObject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import spark.Spark;
import tinder.core.Converter;
import tinder.core.JsonTransformer;
import tinder.core.TypeConverter;

/**
 *
 * @author Raffaele Ragni
 */
@SupportedAnnotationTypes("tinder.core.Resource")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ResourceProcessor extends AbstractProcessor {

  private static final String PREFIX = "Resource";
  private static final String PARAM_SOURCECLASS = "source";
  private static final String PARAM_CONVERTER = "converter";
  private static final String PARAM_REQUEST = "request";
  private static final String PARAM_RESPONSE = "response";

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {

    for (TypeElement annotation : annotations) {
      Set<? extends Element> elements = env.getElementsAnnotatedWith(annotation);
      for (Element element : elements) {
        try {
          generateRESTFor(element);
        } catch (IOException ex) {
          Logger.getLogger(ResourceProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }

    return true;
  }

  private void generateRESTFor(Element element) throws IOException {

    Elements elements = processingEnv.getElementUtils();

    String simpleClassName = PREFIX + element.getSimpleName();
    String packageName = elements.getPackageOf(element).toString();
    String fullClassName = packageName + "." + simpleClassName;

    TypeSpec.Builder restClass = TypeSpec.classBuilder(simpleClassName)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    makeRESTBody(element, restClass);

    JavaFile javaFile = JavaFile.builder(packageName, restClass.build())
        .build();

    JavaFileObject obj = processingEnv.getFiler()
        .createSourceFile(fullClassName, element);
    try (Writer w = obj.openWriter()) {
      javaFile.writeTo(w);
    }

  }

  private void makeRESTBody(Element element, TypeSpec.Builder restClass) {

    Elements elements = processingEnv.getElementUtils();

    MethodSpec.Builder bindMethod = MethodSpec.methodBuilder("bind")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(TypeName.get(element.asType()), PARAM_SOURCECLASS);

    // Determine the root path if there is any, the annotation shouldbe there.
    String rootPath = "";
    if (element.getAnnotation(Path.class) != null) {
      rootPath = Optional.ofNullable(element.getAnnotation(Path.class).value()).orElse("");
    }

    // Check if a converter was specified, otherwise we make our own.
    Converter converterAnn = element.getAnnotation(Converter.class);
    if (converterAnn != null) {
      // Because of some crap thing/impl, cannot get classes out of annotations and need a trick for it...
      TypeMirror value = null;
      try {
        bindMethod = bindMethod.addParameter(converterAnn.value(), PARAM_CONVERTER);
      } catch (MirroredTypeException mte) {
        bindMethod = bindMethod.addParameter(TypeName.get(mte.getTypeMirror()), PARAM_CONVERTER);
      }
    } else {
      bindMethod = bindMethod.addStatement("$T $L = new $T(){}", TypeConverter.class, PARAM_CONVERTER, TypeConverter.class);
    }

    for (Element e : elements.getAllMembers((TypeElement) element)) {
      if (e.getAnnotation(Path.class) != null) {
        // Every methods with a @Path goes into the binding, if not they don't
        String path = Optional.ofNullable(e.getAnnotation(Path.class).value()).orElse("");
        rootPath = rootPath.trim();
        path = path.trim();
        String finalPath;
        if (!rootPath.endsWith("/") && !path.startsWith("/")) {
          finalPath = rootPath + "/" + path;
        } else {
          finalPath = rootPath + path;
        }
        finalPath = finalPath.replaceAll("//", "/");

        finalPath = finalPath.replaceAll("\\{([^\\}]*)\\}", ":$1");

        if (e.getAnnotation(GET.class) != null) {
          methodRoute(bindMethod, e, "GET", finalPath);
        }
        if (e.getAnnotation(POST.class) != null) {
          methodRoute(bindMethod, e, "POST", finalPath);
        }
        if (e.getAnnotation(PUT.class) != null) {
          methodRoute(bindMethod, e, "PUT", finalPath);
        }
        if (e.getAnnotation(PATCH.class) != null) {
          methodRoute(bindMethod, e, "PATCH", finalPath);
        }
        if (e.getAnnotation(DELETE.class) != null) {
          methodRoute(bindMethod, e, "DELETE", finalPath);
        }

      }
    }

    bindMethod = bindMethod
        .addComment("Call the init of the resource when all is connected")
        .addStatement("$L.init()", PARAM_SOURCECLASS);

    restClass.addMethod(bindMethod.build());
  }

  private MethodSpec.Builder methodRoute(
      MethodSpec.Builder bindMethod, Element method, String httpMethod, String path) {

    // Check if a transformer was specified, otherwise we make our own.
    Object transformerClazz;
    Converter converterAnn = method.getAnnotation(Converter.class);
    if (converterAnn != null) {
      // Because of some crap thing/impl, cannot get classes out of annotations and need a trick for it...
      try {
        transformerClazz = converterAnn.value();
      } catch (MirroredTypeException mte) {
        transformerClazz = TypeName.get(mte.getTypeMirror());
      }
    } else {
      transformerClazz = JsonTransformer.class;
    }

    bindMethod = bindMethod
        .addCode("\n")
        .addComment(httpMethod.toUpperCase() + " " + path)
        .addCode("$T.$L($S, ($L, $L) -> {\n", Spark.class, httpMethod.toLowerCase(), path, PARAM_REQUEST, PARAM_RESPONSE)
        .addCode(CodeBlock.builder()
            .add(callBlock(method))
            .build())
        .addCode("}, new $T());\n\n", transformerClazz);
    return bindMethod;
  }

  private CodeBlock callBlock(Element method) {

    Elements elements = processingEnv.getElementUtils();
    Types types = processingEnv.getTypeUtils();

    CodeBlock.Builder result = CodeBlock.builder().indent();

    if (method.getKind() != ElementKind.METHOD) {
      return result.unindent().build();
    }

    // Adding the binding here...
    // Type conversion is part of this.
    ExecutableType execType = (ExecutableType) method.asType();
    ExecutableElement execElement = (ExecutableElement) method;
    boolean isReturnVoid = execType.getReturnType().getKind() == TypeKind.VOID;
    String paramNames = null;
    CodeBlock.Builder blockStrings = CodeBlock.builder();
    CodeBlock.Builder blockVars = CodeBlock.builder();

    for (VariableElement param: execElement.getParameters()) {

      TypeMirror paramType = param.asType();

      // As a special case we skip conversioni and pass directly these types if present.
      if (types.isSameType(paramType, elements.getTypeElement("spark.Request").asType())) {
        String name = PARAM_REQUEST;
        paramNames = paramNames == null ? name : paramNames + ", " + name;
        continue;
      } else if (types.isSameType(paramType, elements.getTypeElement("spark.Response").asType())) {
        String name = PARAM_RESPONSE;
        paramNames = paramNames == null ? name : paramNames + ", " + name;
        continue;
      }

      // All other cannot be primitive.. because can be null from conversion
      if (paramType.getKind().isPrimitive()) {
        processingEnv.getMessager().printMessage(ERROR, "parameters cannot be primitive", method);
      }

      String name = param.getSimpleName().toString();
      paramNames = paramNames == null ? name : paramNames + ", " + name;
      HeaderParam headerParam = param.getAnnotation(HeaderParam.class);
      QueryParam queryParam = param.getAnnotation(QueryParam.class);
      PathParam pathParam = param.getAnnotation(PathParam.class);
      boolean createdVar = false;
      if (headerParam != null) {
        blockStrings.addStatement("$T str$L = $L.headers($S)", String.class, name, PARAM_REQUEST, headerParam.value());
        createdVar = true;
      } else if (queryParam != null) {
        blockStrings.addStatement("$T str$L = $L.queryParams($S)", String.class, name, PARAM_REQUEST, queryParam.value());
        createdVar = true;
      } else if (pathParam != null) {
        blockStrings.addStatement("$T str$L = $L.params($S)", String.class, name, PARAM_REQUEST, ":" + pathParam.value());
        createdVar = true;
      } else {
        // No annotation, we go from body. There should be only one of these.
        blockStrings.addStatement("$T str$L = $L.body()", String.class, name, PARAM_REQUEST);
        createdVar = true;
      }
      if (createdVar) {
        blockVars.addStatement("$T $L = $L.fromString(str$L, $T.class)",
            ClassName.get(paramType), name, PARAM_CONVERTER, name, ClassName.get(paramType));
      }

    }

    result.add(blockStrings.build());
    result.add(blockVars.build());
    if (isReturnVoid) {
      if (paramNames == null) {
        result.addStatement("$L.$L()", PARAM_SOURCECLASS, method.getSimpleName());
      } else {
        result.addStatement("$L.$L($L)", PARAM_SOURCECLASS, method.getSimpleName(), paramNames);
      }
      result.addStatement("return \"\"");
    } else {
      if (paramNames == null) {
        result.addStatement("return $L.$L()", PARAM_SOURCECLASS, method.getSimpleName());
      } else {
        result.addStatement("return $L.$L($L)", PARAM_SOURCECLASS, method.getSimpleName(), paramNames);
      }
    }

    return result.unindent().build();
  }

}
