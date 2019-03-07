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
import io.javalin.Context;
import io.javalin.Javalin;
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
import tinder.core.ResourceEvents;
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
  private static final String PARAM_CTX = "ctx";
  private static final String PARAM_JAVALIN = "javalin";

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
    Types types = processingEnv.getTypeUtils();

    MethodSpec.Builder bindMethod = MethodSpec.methodBuilder("bind")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(Javalin.class, PARAM_JAVALIN)
        .addParameter(TypeName.get(element.asType()), PARAM_SOURCECLASS);

    // Determine the root path if there is any, the annotation shouldbe there.
    String rootPath = "";
    if (element.getAnnotation(Path.class) != null) {
      rootPath = Optional.ofNullable(element.getAnnotation(Path.class).value()).orElse("");
    }

    bindMethod = bindMethod.addStatement("$T $L = new $T(){}", TypeConverter.class, PARAM_CONVERTER, TypeConverter.class);

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

    TypeMirror resourceEventsType = elements.getTypeElement(ResourceEvents.class.getName()).asType();
    boolean needsInit = types.isAssignable(element.asType(), resourceEventsType);
    if (needsInit) {
      bindMethod = bindMethod
          .addComment("Call the init of the resource when all is connected")
          .addStatement("$L.init()", PARAM_SOURCECLASS);
    }

    restClass.addMethod(bindMethod.build());
  }

  private MethodSpec.Builder methodRoute(
      MethodSpec.Builder bindMethod, Element method, String httpMethod, String path) {

    bindMethod = bindMethod
        .addCode("\n")
        .addComment(httpMethod.toUpperCase() + " " + path)
        .addCode("$L.$L($S, ($L) -> {\n", PARAM_JAVALIN, httpMethod.toLowerCase(), path, PARAM_CTX)
        .addCode(CodeBlock.builder()
            .add(callBlock(method))
            .build())
        .addCode("});\n\n");
    return bindMethod;
  }

  private CodeBlock callBlock(Element method) {

    Elements elements = processingEnv.getElementUtils();
    Types types = processingEnv.getTypeUtils();

    CodeBlock.Builder result = CodeBlock.builder().indent();

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

      // As a special case we skip conversion and pass directly these types if present.
      if (types.isSameType(paramType, elements.getTypeElement(Context.class.getName()).asType())) {
        String name = PARAM_CTX;
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
      if (headerParam != null) {
        blockStrings.addStatement("$T str$L = $L.header($S)", String.class, name, PARAM_CTX, headerParam.value());
      } else if (queryParam != null) {
        blockStrings.addStatement("$T str$L = $L.queryParam($S)", String.class, name, PARAM_CTX, queryParam.value());
      } else if (pathParam != null) {
        blockStrings.addStatement("$T str$L = $L.pathParam($S)", String.class, name, PARAM_CTX, ":" + pathParam.value());
      } else {
        // No annotation, we go from body. There should be only one of these.
        blockStrings.addStatement("$T str$L = $L.body()", String.class, name, PARAM_CTX);
      }
      blockVars.addStatement("$T $L = $L.fromString(str$L, $T.class)",
          ClassName.get(paramType), name, PARAM_CONVERTER, name, ClassName.get(paramType));

    }

    result.add(blockStrings.build());
    result.add(blockVars.build());
    if (isReturnVoid) {
      if (paramNames == null) {
        result.addStatement("$L.$L()", PARAM_SOURCECLASS, method.getSimpleName());
      } else {
        result.addStatement("$L.$L($L)", PARAM_SOURCECLASS, method.getSimpleName(), paramNames);
      }
    } else {
      if (paramNames == null) {
        result.addStatement("$L.json($L.$L())", PARAM_CTX, PARAM_SOURCECLASS, method.getSimpleName());
      } else {
        result.addStatement("$L.json($L.$L($L))", PARAM_CTX, PARAM_SOURCECLASS, method.getSimpleName(), paramNames);
      }
    }

    return result.unindent().build();
  }

}
