package student.management.api_app.repository.specification;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import student.management.api_app.model.Major;

public class MajorSpecifications {

    public static Specification<Major> nameContains(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) return null;
            return cb.like(cb.lower(root.get("name")), SpecUtils.likePattern(keyword));
        };
    }

    public static Specification<Major> codeContains(String code) {
        return (root, query, cb) ->
                !StringUtils.hasText(code)
                        ? null : cb.like(cb.lower(root.get("code")), SpecUtils.likePattern(code));
    }
}
