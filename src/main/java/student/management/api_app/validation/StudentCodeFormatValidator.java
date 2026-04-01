package student.management.api_app.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StudentCodeFormatValidator implements ConstraintValidator<StudentCodeFormat, String> {

    private static final String STUDENT_CODE_PATTERN = "^STU\\d{3}$";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true; // để @NotBlank xử lý null
        return value.matches(STUDENT_CODE_PATTERN);
    }
}
