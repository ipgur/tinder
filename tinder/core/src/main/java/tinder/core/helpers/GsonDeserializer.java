
package tinder.core.helpers;

import com.google.gson.Gson;
import io.jsonwebtoken.io.DeserializationException;
import io.jsonwebtoken.io.Deserializer;
import io.jsonwebtoken.lang.Assert;

// This class has been ported over from a PR that is currently not merged in jjwt: https://github.com/jwtk/jjwt/pull/414
public class GsonDeserializer<T> implements Deserializer<T> {

  private final Class<T> returnType;
  private final Gson gson;

  public GsonDeserializer() {
    this(GsonSerializer.DEFAULT_GSON);
  }

  @SuppressWarnings("unchecked")
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
    return readValue(bytes);
  }

  protected T readValue(byte[] bytes) {
    return gson.fromJson(new String(bytes), returnType);
  }
}
