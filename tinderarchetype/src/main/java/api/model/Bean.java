package api.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javax.annotation.Nullable;
import org.immutables.value.Value.Immutable;

@Immutable
@JsonSerialize(as = ImmutableBean.class)
@JsonDeserialize(as = ImmutableBean.class)
public interface Bean {
  @Nullable Long id();
  @Nullable Long limit();
  @Nullable Long length();
}
