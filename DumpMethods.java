import java.lang.reflect.Method;
public class DumpMethods {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("net.minecraft.client.gui.screens.inventory.AbstractContainerScreen");
        for (Method m : clazz.getDeclaredMethods()) {
            System.out.println(m.getReturnType().getSimpleName() + " " + m.getName() + "(...)");
        }
    }
}
