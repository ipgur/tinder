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
 * Authentication endpoint resources.
 * 
 * This tool will install resources for authentication, based on which ones
 * are requested via the addXX methods.
 * 
 * This tool may be used embedded in your API if it is self standing, but also
 * you can build a central authentication API from this and then rely on the
 * check endpoint from satellite APIs, to verify tokens.
 * 
 * Main endpoints being provided:
 * 
 * /register
 * addRegisterResource(path with default to /register,
 *      code -> {... what to do with confirmation code})
 * - this one accepts email+password and created the entry in the db,
 *   and returns a confirmation code to activate the user later.
 * - the developer will deal with sending codes via email/sms whatever (lambda)
 *
 * /register/verify?code=XXX
 * this to be called once via maybe a GET link from an email:
 * AuthenticationResources.confirm(email, code)
 * - confirms and enable a user, updates the db with a user to be enabled this
 *   way (post registration process finalization)
 * Make a resource endpoint or just leave the dev to implement this?
 * 
 * /login
 * addLoginResource(path with default to login)
 * - adds the login endpoint, JSONs and protocol is only the one from the
 *   provided layer, else the developer has to implement all by himself.
 * - accepts a JSON with {email: "", password: ""} for login.
 * - returns a JSON with the user profile and 200 OK if login was correct.
 * 
 * /checktoken
 * addCheckTokenResource(path with default to /checktoken)
 * - checks if a token is valid or not, this is used if you want to build an
 *   auth api and rely on that, by using the addAPIBasedFilter() from another
 *   upstream api.
 *   should return all the possible user info as a json when all is correct.
 *   it does not do profilation but that doesn't mean you can add a /profile
 *   endpoint yourself and use a addDatabaseBasedFilter() in the main
 *   auth/central api.
 *
 * @author Raffaele Ragni
 */
public class AuthenticationResources {
  
}
