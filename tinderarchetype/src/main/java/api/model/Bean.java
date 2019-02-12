package api.model;

import javax.annotation.Nullable;
import org.immutables.gson.Gson;
import org.immutables.value.Value.Immutable;

@Immutable
@Gson.TypeAdapters
public interface Bean {
  @Nullable Long id();
  @Nullable Long limit();
  @Nullable Long length();
}
