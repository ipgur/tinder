/*
 * Copyright 2019 Raffaele Ragni.
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
package tinder.core.auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.BCrypt.Result;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Raffaele Ragni
 */
public class AuthenticationResourcesTest {

  @Test
  public void testHash() {
    char[] pw = "12345678".toCharArray();
    String result = new String(AuthenticationResources.hashAndClean(pw));
    String aftpw = new String(pw);
    Assertions.assertEquals("        ", aftpw);
    System.out.println(result);
    Result verifyresult = BCrypt.verifyer().verify("12345678".toCharArray(), result.toCharArray());
    Assertions.assertTrue(verifyresult.verified);
  }

}
