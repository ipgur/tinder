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

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.io.Writer;
import static java.lang.String.format;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;
import tinder.core.PeriodicallyScheduled;

/**
 * Annotation processor
 *
 * @author Raffaele Ragni
 */
@SupportedAnnotationTypes("tinder.core.Scheduling")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ScheduledProcessor extends AbstractProcessor {

  private static final Logger LOG = Logger.getLogger(ScheduledProcessor.class.getName());

  @Override
  public boolean process(
      Set<? extends TypeElement> annotations, RoundEnvironment env) {

    for (TypeElement annotation : annotations) {
      Set<? extends Element> elements = env.getElementsAnnotatedWith(annotation);
      for (Element element : elements) {
        try {
          generateSchedulerFor(element);
        } catch (IOException ex) {
          Logger.getLogger(ScheduledProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
      }
    }

    return true;
  }

  private void generateSchedulerFor(Element element) throws IOException {

    Elements elements = processingEnv.getElementUtils();

    String simpleClassName = "Scheduled" + element.getSimpleName();
    String packageName = elements.getPackageOf(element).toString();
    String fullClassName = packageName + "." + simpleClassName;

    LOG.fine(() -> format(
        "Will generate class %s with full name %s",
        simpleClassName,
        fullClassName));

    MethodSpec.Builder builder = MethodSpec.methodBuilder("start")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(TypeName.get(element.asType()), "source")
        .returns(ScheduledExecutorService.class)
        .addComment("Main implementation assumes 1 thread per generated class")
        .addComment("delays still work and produce multiple class, just they are all serial if they happen to")
        .addComment("run at the same time. If you need, create more classes to have more thread.")
        .addStatement("$T scheduler = $T.newScheduledThreadPool(1)",
            ScheduledExecutorService.class,
            Executors.class)
        .addComment("We shut it down in case of jvm termination")
        .addStatement("Runtime.getRuntime().addShutdownHook(new Thread(() -> scheduler.shutdownNow()))")
        .addComment("Scheduled methods follow...");

    for (Element e : elements.getAllMembers((TypeElement) element)) {
      if (e.getAnnotation(PeriodicallyScheduled.class) != null) {
        PeriodicallyScheduled annotation = e.getAnnotation(PeriodicallyScheduled.class);
        String methodName = annotation.waitBeforeRestart() ?  "scheduleWithFixedDelay" : "scheduleAtFixedRate";
        long wait = annotation.value();
        long initialWait = annotation.immediateStart() ? 0 : wait;
        builder.addStatement("scheduler.$L(() -> source.$L(), $L, $L, $T.$L)",
            methodName,
            e.getSimpleName(),
            initialWait,
            wait,
            TimeUnit.class,
            annotation.unit());
      }
    }

    MethodSpec methodStart = builder
        .addComment("We let the caller have access to the scheduler, in order to shut it down.")
        .addComment("This is optional because the scheduler is shut down at jvm termination itself,")
        .addComment("but there are cases where control of it is needed.")
        .addStatement("return scheduler")
        .build();

    TypeSpec schedulerClass = TypeSpec.classBuilder(simpleClassName)
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
        .addMethod(methodStart)
        .build();

    JavaFile javaFile = JavaFile.builder(packageName, schedulerClass)
        .build();

    JavaFileObject obj = processingEnv.getFiler()
        .createSourceFile(fullClassName, element);
    try (Writer w = obj.openWriter()) {
      javaFile.writeTo(w);
    }

  }

}
