package thebombzen.tumblgififier;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;


@Retention(SOURCE)
@Target(TYPE)
/**
 * Place the annotation on classes that are safe to reference before attaching external libraries.
 */
@PreLoadable
public @interface PreLoadable {
	
}
