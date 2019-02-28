
package tinder.core.helpers;

import com.google.gson.Gson;
import io.jsonwebtoken.io.DeserializationException;
import io.jsonwebtoken.io.Deserializer;
import io.jsonwebtoken.lang.Assert;
import java.io.IOException;

// This class has been ported over from a PR that is currently not merged in jjwt: https://github.com/jwtk/jjwt/pull/414
public class GsonDeserializer<T> implements Deserializer<T> {

  private final Class<T> returnType;
  private final Gson gson;

  @SuppressWarnings("unused") //used via reflection by RuntimeClasspathDeserializerLocator
  public GsonDeserializer() {
    this(GsonSerializer.DEFAULT_GSON);
  }

  @SuppressWarnings({"unchecked", "WeakerAccess", "unused"}) // for end-users providing a custom gson
  public GsonDeserializer(Gson gson) {
    this(gson, (Class<T>) Object.class);
  }

  private GsonDeserializer(Gson gson, Class<T> returnType) {
    Assert.notNull(gson, "gson cannot be null.");
    Assert.notNull(returnType, "Return type cannot be null.");
    this.gson = gson;
    this.returnType = returnType;
  }

  @Override
  public T deserialize(byte[] bytes) throws DeserializationException {
    try {
      return readValue(bytes);
    } catch (IOException e) {
      String msg = "Unable to deserialize bytes into a " + returnType.getName() + " instance: " + e.getMessage();
      throw new DeserializationException(msg, e);
    }
  }

  protected T readValue(byte[] bytes) throws IOException {
    return gson.fromJson(new String(bytes), returnType);
  }
}
