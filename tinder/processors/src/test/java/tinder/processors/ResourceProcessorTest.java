/*
 * Copyright 2019 rragni16.
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

import com.google.testing.compile.Compilation;
import static com.google.testing.compile.Compiler.javac;
import com.google.testing.compile.JavaFileObjects;
import javax.tools.JavaFileObject;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author rragni16
 */
public class ResourceProcessorTest {

  @Test
  public void testResourceProcessor() {

    JavaFileObject tocompile1 = JavaFileObjects.forResource(ResourceProcessorTest.class.getResource("ResourceProcessorTarget1.java"));
    JavaFileObject tocompile2 = JavaFileObjects.forResource(ResourceProcessorTest.class.getResource("ResourceProcessorTarget2.java"));
    JavaFileObject tocompilebad = JavaFileObjects.forResource(ResourceProcessorTest.class.getResource("ResourceProcessorTargetBad.java"));

    Compilation compilation = javac()
        .withProcessors(new ResourceProcessor())
        .compile(tocompile1);
    assertTrue(compilation.status() == Compilation.Status.SUCCESS);

    compilation = javac()
        .withProcessors(new ResourceProcessor())
        .compile(tocompile2);
    assertTrue(compilation.status() == Compilation.Status.SUCCESS);

    compilation = javac()
        .withProcessors(new ResourceProcessor())
        .compile(tocompilebad);
    assertTrue(compilation.status() == Compilation.Status.FAILURE);
  }

}
