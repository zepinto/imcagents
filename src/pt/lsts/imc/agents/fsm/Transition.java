package pt.lsts.imc.agents.fsm;

public @interface Transition {
	String to();
	String guard();
	String event() default "";
}
