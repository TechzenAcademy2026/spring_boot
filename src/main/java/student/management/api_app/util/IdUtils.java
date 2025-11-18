package student.management.api_app.util;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class IdUtils {
    private IdUtils() {}

    public static <T> Set<T> distinctNonNull(Collection<T> ids) {
        if (ids == null || ids.isEmpty()) return Set.of();
        return ids.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
