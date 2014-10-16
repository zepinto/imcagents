package pt.lsts.imc.annotations;

public @interface Transition {
	String to();
	String guard();
	String event() default "";
}
