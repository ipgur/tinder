package api.model;

import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;

@Immutable
public interface Bean {
  @Nullable Long id();
  @Nullable Long limit();
  @Nullable Long length();
}
