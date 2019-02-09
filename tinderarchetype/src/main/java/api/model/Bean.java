package api.model;

import org.immutables.value.Value.Immutable;

@Immutable
public interface Bean {
  long id();
  String name();
}
