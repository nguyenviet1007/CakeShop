package bakery.validation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = UniqueProductNameValidator.class)
@Target({ElementType.TYPE})  // <-- CLASS level
@Retention(RetentionPolicy.RUNTIME)
public @interface UniqueProductName {

    String message() default "Tên bánh đã tồn tại, vui lòng chọn tên khác";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
