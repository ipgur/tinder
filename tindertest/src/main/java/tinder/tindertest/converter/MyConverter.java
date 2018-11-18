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
package tinder.tindertest.converter;

import tinder.tinder.TypeConverter;

/**
 *
 * @author Raffaele Ragni
 */
public class MyConverter implements TypeConverter {
  // Default impl already deals with simple java types...
  // You can go ahead and make jackson do the rest by ovrriding fromString(...)
}
