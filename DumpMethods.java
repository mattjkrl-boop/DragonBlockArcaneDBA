import java.lang.reflect.Method;
public class DumpMethods {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("net.minecraft.client.Minecraft");
        for (Method m : clazz.getMethods()) {
            if (m.getName().toLowerCase().contains("screen")) {
                System.out.println(m.getReturnType().getSimpleName() + " " + m.getName() + "(...)");
            }
        }
        for (java.lang.reflect.Field f : clazz.getFields()) {
            if (f.getName().toLowerCase().contains("screen")) {
                System.out.println("FIELD: " + f.getType().getSimpleName() + " " + f.getName());
            }
        }
    }
}
