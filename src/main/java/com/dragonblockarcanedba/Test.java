import net.minecraft.world.item.Item;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Test {
    public static void main(String[] args) {
        for (Method m : Item.Properties.class.getDeclaredMethods()) {
            if (Modifier.isPublic(m.getModifiers())) {
                System.out.println(m.getName() + ": " + m.getReturnType().getSimpleName());
            }
        }
    }
}
