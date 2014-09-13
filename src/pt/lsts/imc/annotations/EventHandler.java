package pt.lsts.imc.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used in methods that wish to handle events of a specific
 * type / topic.
 * 
 * @author zp
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EventHandler {
	/**
	 * @return The topic of the event
	 */
	String value();
}
