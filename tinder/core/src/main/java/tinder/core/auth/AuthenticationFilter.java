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

/**
 * Applies authentication filtering to the current sparkjava set.
 * It comes with some default API designs, but remains parametric with endpoint
 * URIs and filter patterns.
 * The basic back end implementation is what remains mostly unchanged.
 * 
 * authentication: filters
 * 
 * AuthenticationFilter.addDatabaseBasedFilter()
 * - will filter all requests and check db for the Bearer token.
 * - can specify which requests to filter or to exclude although a default
 *   signature will be there (ex, excluding /login /register...)
 * - option and parameters: table name and/or column name for tokens
 * 
 * AuthenticationFilter.addAPIBasedFilter()
 * - Same applying filter and options as before...
 * - but with added lambda function to be implemented by dev that will call the
 *   auth api
 * - also provide a basic impl that calls another api that was build up by this
 *   own library in the /check default endpoint, or customize the url but at
 *   least implement the protocol because the jsons will be the same.
 * 
 * These filters automatically have a default ignore path list:
 * - /login
 * - /register
 * - /check
 * These are not checked against authentication for obvous reasons, everything
 * else will be locked by authentication.
 * That's the default. It can be changed by user.
 * (optional list in filter methods?)
 * 
 * @author Raffaele Ragni
 */
public class AuthenticationFilter {
  
}
