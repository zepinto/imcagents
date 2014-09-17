package pt.lsts.imc.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import pt.lsts.imc.IMCMessage;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Agent {
	String name();
	Class<? extends IMCMessage>[] publishes();	
}
