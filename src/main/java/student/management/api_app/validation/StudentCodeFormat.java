package student.management.api_app.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = StudentCodeFormatValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface StudentCodeFormat {
    String message() default "Student code must start with 'STU' followed by 3 digits (e.g. STU001)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
